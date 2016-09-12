package org.jactr;

// This file does not declare a class explicitly, but only implicitly, to permit
// the contained methods to access standard pipeline step functions, see
// https://github.com/jenkinsci/workflow-cps-global-lib-plugin/blob/master/README.md#writing-shared-code

def run(Config config) {
	node(config.labelForJenkinsNode) {
	   withCredentials([[$class: 'FileBinding', credentialsId: 'settings.xml', variable: 'PATH_TO_SETTINGS_XML'],
	   					[$class: 'FileBinding', credentialsId: 'jarsigner.keystore', variable: 'PATH_TO_JARSIGNER_KEYSTORE'],
	   					[$class: 'FileBinding', credentialsId: 'pubring.gpg', variable: 'PATH_TO_GPG_PUBLIC_KEYRING'],
	   					[$class: 'FileBinding', credentialsId: 'secring.gpg', variable: 'PATH_TO_GPG_SECRET_KEYRING'],
	   					[$class: 'FileBinding', credentialsId: 'upload.server.ssh.signature.file', variable: 'PATH_TO_UPLOAD_SERVER_SSH_FINGERPRINT_FILE'],
	   					[$class: 'StringBinding', credentialsId: 'upload.server.name', variable: 'UPLOAD_SERVER_NAME'],]) {
	   					
		   stage 'Checkout'
		   if(config.gitCredentialsId) {
		   		git url: config.gitRepoURL, credentialsId: config.gitCredentialsId
		   } else {
		   		git url: config.gitRepoURL
		   }
		   
		   // Update dependencies
		   // Note that dependencyToUpdate and newDependencyVersion need to be configured as job parameters.
		   if(config.script.hasProperty("dependencyToUpdate") && config.script.hasProperty("newDependencyVersion")) {
		      maven('''-Dincludes='''+dependencyToUpdate+''' \
		               -DdepVersion='''+newDependencyVersion+''' \
		               versions:use-dep-version''')
		      // diff is included to ease debugging
		      sh '''git diff \
		            && git add pom.xml \
                    && git commit -m "Bump version of dependency '''+dependencyToUpdate+''' to '''+newDependencyVersion+'''" \
                    && git push'''
		   }
		   
		   stage name: 'Set versions', concurrency: 1
		   def newVersionForMaven = getNextVersion(config)
		   def newVersionForEclipse = newVersionForMaven.replaceAll('-', '.')
		   if(config.isTychoBuild) {
			   maven('''-DnewVersion='''+newVersionForEclipse+''' \
			   			-Dtycho.mode=maven \
					    org.eclipse.tycho:tycho-versions-plugin:0.26.0:set-version''')
		   } else {
			   maven('''--file parent/pom.xml \
	     				-DnewVersion='''+newVersionForMaven+''' \
			   			-D'''+config.propertyForEclipseVersion+'''='''+newVersionForEclipse+''' \
					    versions:set''')
		   }
	       
	       stage name: "Clean & verify", concurrency: 1
	       def tmpDir=pwd tmp: true
	       if(config.displayNumber) {
	       		sh '''Xvfb :'''+config.displayNumber+''' -screen 0 1920x1080x16 -nolisten tcp -fbdir /var/run &
	       			  echo $! > '''+tmpDir+'''/xvfb.pid;
	       		      while [ ! -f /tmp/.X'''+config.displayNumber+'''-lock ]; do echo "Waiting for display :'''+config.displayNumber+'''"; sleep 1; done
	       			  ratpoison --display :'''+config.displayNumber+''' &
	       			  echo $! > '''+tmpDir+'''/ratpoison.pid'''
	       }
	       try {
		       maven(config.displayNumber,
		       		 '''-DnewVersion='''+newVersionForMaven+''' \
	     				-D'''+config.propertyForEclipseVersion+'''='''+newVersionForEclipse+''' \
		       		    clean verify''')
   		   } finally {
   		       if(config.displayNumber) {
       		   	   sh '''kill $(cat '''+tmpDir+'''/ratpoison.pid);
       		   	         rm '''+tmpDir+'''/ratpoison.pid;
       		   	         kill $(cat '''+tmpDir+'''/xvfb.pid);
       		   	   		 rm '''+tmpDir+'''/xvfb.pid'''
   	   		   }
   		   }
	
	       stage name:"Deploy", concurrency: 1
	       // TODO: Deploy to Maven Central will require the maven central ssh fingerprint
	       sh '''touch ~/.ssh/known_hosts \
	       		 && ssh-keygen -f ~/.ssh/known_hosts -R $UPLOAD_SERVER_NAME \
	       		 && cat $PATH_TO_UPLOAD_SERVER_SSH_FINGERPRINT_FILE >> ~/.ssh/known_hosts'''
	       // Retry is necessary because upload is unreliable
	       retry(5) {
	       		maven('''-DnewVersion='''+newVersionForMaven+''' \
     					 -D'''+config.propertyForEclipseVersion+'''='''+newVersionForEclipse+''' \
	       				 -DskipTests=true \
	       				 -DskipITs=true \
	       				 deploy''')
	       }
	             
	       stage name:"Site deploy", concurrency: 1
	       // Retry is necessary because upload is unreliable
	       retry(5) {
	       		maven('''-DnewVersion='''+newVersionForMaven+''' \
     					 -D'''+config.propertyForEclipseVersion+'''='''+newVersionForEclipse+''' \
	       				 -DskipTests=true \
	       				 -DskipITs=true \
	       				 site-deploy''')
	     	}
	     	
	     	// Trigger downstream jobs
	     	for(jobToTrigger in config.jobsToUpdateToNewlyBuiltVersion) {
                build job: jobToTrigger,
                      parameters: [
                            string(name: 'dependencyToUpdate', value: config.mavenGroupId+':'+config.mavenArtifactId),
                            string(name: 'newDependencyVersion', value: newVersionForMaven)],
                      wait: false
            }
	    }
	}
}

def maven(int displayNumber, String optionsAndGoals) {
   sh '''export DISPLAY=:'''+displayNumber+''' \
	     && mvn \
   		 -Djarsigner.keystore.path=$PATH_TO_JARSIGNER_KEYSTORE \
   		 -Dgpg.publicKeyring=$PATH_TO_GPG_PUBLIC_KEYRING \
   		 -Dgpg.secretKeyring=$PATH_TO_GPG_SECRET_KEYRING \
         --errors \
         --settings $PATH_TO_SETTINGS_XML \
         '''+optionsAndGoals
}

def maven(String optionsAndGoals) {
   sh '''mvn \
   		 -Djarsigner.keystore.path=$PATH_TO_JARSIGNER_KEYSTORE \
   		 -Dgpg.publicKeyring=$PATH_TO_GPG_PUBLIC_KEYRING \
   		 -Dgpg.secretKeyring=$PATH_TO_GPG_SECRET_KEYRING \
         --errors \
         --settings $PATH_TO_SETTINGS_XML \
         '''+optionsAndGoals
}

/**
 * Auto-assign a version number based on the last release published to the Maven repository. This
 * method shall be used to set version numbers via {@link Config#setNewVersion(String)}. By using this
 * method, the deployed version number will only be incremented upon successfully deployed builds.
 * 
 * <p>Version numbers returned by this method have the format {@code <majorPart>.<minorPart>.<patchPart>-<commitHash>}
 * where {@code <commitHash>} is the short hash of the last commit contained in the state checkout out of the repository
 * that this build uses. The hash is appended to the version number to correlate the state of the Git repository
 * with the release that it produced. This removes the need to tag the Git repository with release numbers, as
 * it is expected that the majority of pushed commits will yield a successful release.
 *
 * <p>The method increments major part of the version number and sets minor and patch parts to 0,
 * if the description of the last commit contains the string {@code +majorVersion}. It increments
 * the minor part and sets the patch part to 0, if the string
 * {@code +minorVersion} is found. The patch part is incremented in all other cases.
 *
 * <p>Note that the version numbers returned by this method comply to the format used by Maven
 * ({@code <majorPart>.<minorPart>.<patchPart>-<qualifier>}) but not to the format used by Eclipse
 * ({@code <majorPart>.<minorPart>.<patchPart>.<qualifier>}).
 */
def getNextVersion(Config config) {
	def tmpDir=pwd tmp: true

    // Determine last commit message
    def commitFile=tmpDir+'/last-commit-message.txt'
    sh 'git log --max-count=1 > '+commitFile
    def lastCommitMessage = readFile commitFile
    sh 'rm '+commitFile
    
    // Determine last commit hash
    def commitHashFile=tmpDir+'/last-commit-hash.txt'
    sh 'git log --oneline --max-count=1 | cut --delimiter=" " --fields=1 >'+commitHashFile
    def lastCommitHash = readFile(commitHashFile).trim()
    sh 'rm '+commitHashFile
	
	// Create new version number
	def newVersion = config.mavenCurrentReleaseVersion
	def oldVersionWithoutQualifier = config.mavenCurrentReleaseVersion.split("-")[0]
	String[] parts = oldVersionWithoutQualifier.split("\\.")
	if(lastCommitMessage.contains("+majorVersion")) {
		newVersion = (parts[0].toInteger()+1)+".0.0"
	} else if(lastCommitMessage.contains("+minorVersion")) {
		newVersion = parts[0]+"."+(parts[1].toInteger()+1)+".0"
	} else {
		newVersion = parts[0]+"."+parts[1]+"."+(parts[2].toInteger()+1)
	}
	
	// Add last commit hash so permit version numbers to be correlated with Git commits.
	// Note that the version number in this format of suitable for Maven, but not for Eclipse.
	// While Maven versions have the format /<major>.<minor>.<patch>-<qualifier>/ , 
	//     Eclipse versions have the format /<major>.<minor>.<patch>.<qualifier>/ ,
	// thus - needs to be replaced by . to create the latter out of the former.
	newVersion += '-'+lastCommitHash
	echo 'Updating version '+config.mavenCurrentReleaseVersion+' -> '+newVersion
	currentBuild.displayName = '#'+currentBuild.number+' v'+newVersion
	return newVersion
}