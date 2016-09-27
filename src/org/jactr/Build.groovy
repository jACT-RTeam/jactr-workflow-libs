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
	   					
           def tmpDir=pwd tmp: true
	   					
		   stage name: 'Checkout', concurrency: 1
		   checkout(config)
		   
           stage name:"Update dependencies", concurrency: 1
           // Update dependency version. The properties dependencyToUpdate and newDependencyVersion
           // are job parameters.
           if(config.script.dependencyToUpdate) {
                def dependencyToUpdateForMaven=dependencyToUpdate
                def dependencyGA=dependencyToUpdate.split(':')
                def dependencyMavenGroupId=dependencyGA[0]
                def dependencyMavenArtifactId=dependencyGA[1]
                def dependencyToUpdateForEclipse=dependencyMavenGroupId
                if(!dependencyToUpdateForEclipse.endsWith(dependencyMavenArtifactId)) {
                     dependencyToUpdateForEclipse += "."+dependencyMavenArtifactId
                }
                // See git man page for the git store credential for information on the file format.
                // https://git-scm.com/docs/git-credential-store
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: config.gitCredentialsId, usernameVariable: 'GIT_REPO_USER', passwordVariable: 'GIT_REPO_PASSWORD'],
                                 [$class: 'FileBinding', credentialsId: config.gitCredentialsId+'File', variable: 'GIT_CREDENTIALS_FILE']]) {
                            
                    // Update version in the dependency declaration
                    config.dependencyUpdate.updateDependency(config.script,
                        dependencyToUpdateForMaven,
                        dependencyToUpdateForEclipse,
                        config.script.newDependencyVersion)
                        
                    // Push the change
                    gitPush(config, config.dependencyUpdate.modifiedFilesPattern,
                        'Bump version of dependency '+dependencyToUpdateForMaven+' to '+config.script.newDependencyVersion+' in '+config.dependencyUpdate.modifiedFilesPattern)
                }
           }
		   
		   stage name: 'Set versions', concurrency: 1
		   def oneLineGitLogSinceCurrentRelease = getOneLineGitLogSinceCurrentRelease(config)
		   def newVersion = getNextVersion(config, oneLineGitLogSinceCurrentRelease)
		   if(config.isTychoBuild) {
			   maven('''-DnewVersion='''+newVersion+''' \
			   			-Dtycho.mode=maven \
					    org.eclipse.tycho:tycho-versions-plugin:0.26.0:set-version''')
		   } else {
			   maven('''--file parent/pom.xml \
	     				-DnewVersion='''+newVersion+''' \
			   			-D'''+config.propertyForEclipseVersion+'''='''+newVersion+''' \
					    versions:set''')
		   }
	       
	       stage name: "Clean & verify", concurrency: 1
	       if(config.displayNumber) {
	       		sh '''Xvfb :'''+config.displayNumber+''' -screen 0 1920x1080x16 -nolisten tcp -fbdir /var/run &
	       			  echo $! > '''+tmpDir+'''/xvfb.pid;
	       		      while [ ! -f /tmp/.X'''+config.displayNumber+'''-lock ]; do echo "Waiting for display :'''+config.displayNumber+'''"; sleep 1; done
	       			  ratpoison --display :'''+config.displayNumber+''' &
	       			  echo $! > '''+tmpDir+'''/ratpoison.pid'''
	       }
	       try {
		       maven(config.displayNumber,
		       		 '''-DnewVersion='''+newVersion+''' \
	     				-D'''+config.propertyForEclipseVersion+'''='''+newVersion+''' \
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
	       		maven('''-DnewVersion='''+newVersion+''' \
     					 -D'''+config.propertyForEclipseVersion+'''='''+newVersion+''' \
	       				 -DskipTests=true \
	       				 -DskipITs=true \
	       				 deploy''')
	       }
	             
	       stage name:"Site deploy", concurrency: 1
	       // Retry is necessary because upload is unreliable
	       retry(5) {
	       		maven('''-DnewVersion='''+newVersion+''' \
     					 -D'''+config.propertyForEclipseVersion+'''='''+newVersion+''' \
	       				 -DskipTests=true \
	       				 -DskipITs=true \
	       				 site-deploy''')
	     	}
	     	pushNewVersionNumberToGit(config, "*", newVersion)
	     	
	     	stage name: "Trigger dependent jobs", concurrency: 1
	     	for(String jobToTrigger in config.jobsToTrigger) {
	     	     build job: jobToTrigger,
	     	           parameters: [string(name: 'dependencyToUpdate', value: config.mavenGroupId+':'+config.mavenArtifactId),
	     	                        string(name: 'newDependencyVersion', value: newVersion)],
                       wait: false
	     	}
	    }
	}
}

def checkout(Config config) {
    // Use a more complex configuration instead of
    // git url: config.gitRepoURL, credentialsId: config.credentialsID
    // to be able to exclude Jenkins jobs from triggering themselves. This leaves the Git repository in detached head
    // state which needs to be overcome using git checkout master before proceeding.
    checkout([$class: 'GitSCM',
        branches: [[name: 'refs/heads/master']],
        doGenerateSubmoduleConfigurations: false,
        extensions: [[$class: 'UserExclusion', excludedUsers: 'Jenkins Monochromata']],
        gitTool: 'Default',
        submoduleCfg: [],
        userRemoteConfigs:
            config.gitCredentialsId != null 
                ? [[url: config.gitRepoURL, credentialsId: config.gitCredentialsId]]
                : [[url: config.gitRepoURL]]
            ])
    sh '''git branch -f temp \
          && git checkout master \
          && git merge --commit --no-edit temp \
          && git branch -d temp'''
}

def pushNewVersionNumberToGit(Config config, String patternOfFilesContainingTheVersionNumber, String newVersion) {
    gitPush(config, patternOfFilesContainingTheVersionNumber, 'Release version '+newVersion)
}

private void gitPush(Config config, String patternOfFilesToAdd, String commitMessage) {
    // See git man page for the git store credential for information on the file format.
    // https://git-scm.com/docs/git-credential-store
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: config.gitCredentialsId, usernameVariable: 'GIT_REPO_USER', passwordVariable: 'GIT_REPO_PASSWORD'],
                     [$class: 'FileBinding', credentialsId: config.gitCredentialsId+'File', variable: 'GIT_CREDENTIALS_FILE']]) {
        try {
            sh """git config --local user.name 'Jenkins Monochromata' \
                && git config --local user.email 'info@monochromata.de' \
                && git config --local credential.username '"""+env.GIT_REPO_USER+"""' \
                && git config --local credential.helper 'store --file="""+env.GIT_CREDENTIALS_FILE+"""' \
                && git config --local push.default 'matching' \
                && git add """+patternOfFilesToAdd+""" \
                && git commit -m '"""+commitMessage+"""' \
                && git push"""
        } finally {
            sh """git config --local --remove-section user; \
                git config --local --remove-section credential; \
                git config --local --remove-section push"""
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

def getOneLineGitLogSinceCurrentRelease(Config config) {
    def tmpDir=pwd tmp: true
    def logFile = tmpDir+'/last-commits-one-line.txt'
    if(config.currentReleaseCommitHash) {
        sh 'git log --oneline '+config.currentReleaseCommitHash+'..HEAD > '+logFile
    } else {
        // There is no release yet, consider +majorVersion/+minorVersion in the entire git log.
        sh 'git log --oneline > '+logFile
    }
    def oneLineGitLogSinceCurrentRelease = readFile logFile
    sh 'rm '+logFile
    return oneLineGitLogSinceCurrentRelease
}

/**
 * Auto-assign a version number based on the last release announced in a commit message in the Git repository.
 * 
 * <p>Version numbers returned by this method have the format {@code <majorPart>.<minorPart>.<patchPart>}.
 *
 * <p>The method increments major part of the version number and sets minor and patch parts to 0,
 * if the description of the commits since the last release contain the string {@code +majorVersion}.
 * It increments the minor part and sets the patch part to 0, if the string
 * {@code +minorVersion} is found. The patch part is incremented in all other cases.
 *
 * <p>Note that the version numbers returned by this method comply to the format used by both Maven and Eclipse.
 */
def getNextVersion(Config config, String oneLineGitLogSinceCurrentRelease) {
	def tmpDir=pwd tmp: true
	
	// Create new version number
	def newVersion = config.currentReleaseVersion
	def oldVersionWithoutQualifier = config.currentReleaseVersion.split("-")[0]
	String[] parts = oldVersionWithoutQualifier.split("\\.")
	if(oneLineGitLogSinceCurrentRelease.contains("+majorVersion")) {
		newVersion = (parts[0].toInteger()+1)+".0.0"
	} else if(oneLineGitLogSinceCurrentRelease.contains("+minorVersion")) {
		newVersion = parts[0]+"."+(parts[1].toInteger()+1)+".0"
	} else {
		newVersion = parts[0]+"."+parts[1]+"."+(parts[2].toInteger()+1)
	}
	
	echo 'Updating version '+config.currentReleaseVersion+' -> '+newVersion
	currentBuild.displayName = '#'+currentBuild.number+' v'+newVersion
	return newVersion
}