package org.jactr;

// This file does not declare a class explicitly, but only implicitly, to permit
// the contained methods to access standard pipeline step functions, see
// https://github.com/jenkinsci/workflow-cps-global-lib-plugin/blob/master/README.md#writing-shared-code

def run(Config config) {
	node("1gb") {
	   installToolsIfNecessary()
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
		   
		   stage name: 'Set versions', concurrency: 1
		   def newVersionForMaven = getNextVersion(config)
		   def newVersionForEclipse = newVersionForMaven.replaceAll('-', '.')
		   maven('''--file parent/pom.xml \
     				-DnewVersion='''+newVersionForMaven+''' \
		   			-D'''+config.propertyForEclipseVersion+'''='''+newVersionForEclipse+''' \
				    versions:set''')
		   if(config.isTychoBuild) {
			   maven('''-DnewVersion='''+newVersionForEclipse+''' \
					    tycho-versions:set-version''')
		   }
	       
	       stage name: "Clean & verify", concurrency: 1
	       maven('''-DnewVersion='''+newVersionForMaven+''' \
     				-D'''+config.propertyForEclipseVersion+'''='''+newVersionForEclipse+''' \
	       		    clean verify''')
	
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
	    }
	}
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

	// Get last release version
	def mavenMetaDataFile = tmpDir+'/maven-metadata.xml'
	def versionFile = tmpDir+'/maven.release'
	sh 'curl --silent '+config.releaseMetaDataURL+' > '+mavenMetaDataFile
	sh 'xpath -e metadata/versioning/release -q '+mavenMetaDataFile+' | sed --regexp-extended "s/<\\/?release>//g" > '+versionFile
	def oldVersion = readFile(versionFile).trim()
	sh 'rm '+mavenMetaDataFile
	sh 'rm '+versionFile

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
	def newVersion = oldVersion
	def oldVersionWithoutQualifier = oldVersion.split("-")[0]
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
	echo "Updating version $oldVersion -> $newVersion"
	currentBuild.displayName = '#'+currentBuild.number+' v'+newVersion
	return newVersion
}

def installToolsIfNecessary() {
    // Retry is necessary because downloads via apt-get are unreliable
   	retry(3) {
	   sh '''echo "deb http://http.debian.net/debian jessie-backports main" > /etc/apt/sources.list.d/jessie-backports.list \
	        && apt-get update \
	        && apt-get remove --yes openjdk-7-jdk \
	        && apt-get install --yes openjdk-8-jre-headless openjdk-8-jdk \
	        && /usr/sbin/update-java-alternatives -s java-1.8.0-openjdk-amd64 \
	        && apt-get install --yes curl git maven libxml-xpath-perl'''
    }
}