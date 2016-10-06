package org.jactr;

// This file does not declare a class explicitly, but only implicitly, to permit
// the contained methods to access standard pipeline step functions, see
// https://github.com/jenkinsci/workflow-cps-global-lib-plugin/blob/master/README.md#writing-shared-code

def run(ConfigBuilder configBuilder) {
    // The milestone step, together with lock(..., inversePrecedence: true) below cancels all but the youngest build
    // if more than one build is waiting to be started in front of this milestone/lock combination (the currently
    // running build within the milestone, that has the lock, will run to completion, though). 
    milestone 1
    // Ensure that the build does not run in parallel as it modifies the dependencies declarations and version numbers
    // e.g. in pom.xml files. If two builds ran in parallel, they would introduce merge conflicts. Note that merge
    // conflicts may still occur if the Git repository is updated while a build is running.
    lock(resource: env.JOB_NAME+'-build', inversePrecedence: true) {
    	node(configBuilder.labelForJenkinsNode) {
    	   withCredentials([[$class: 'FileBinding', credentialsId: 'settings.xml', variable: 'PATH_TO_SETTINGS_XML'],
    	   					[$class: 'FileBinding', credentialsId: 'jarsigner.keystore', variable: 'PATH_TO_JARSIGNER_KEYSTORE'],
    	   					[$class: 'FileBinding', credentialsId: 'pubring.gpg', variable: 'PATH_TO_GPG_PUBLIC_KEYRING'],
    	   					[$class: 'FileBinding', credentialsId: 'secring.gpg', variable: 'PATH_TO_GPG_SECRET_KEYRING'],
    	   					[$class: 'FileBinding', credentialsId: 'upload.server.ssh.signature.file', variable: 'PATH_TO_UPLOAD_SERVER_SSH_FINGERPRINT_FILE'],
    	   					[$class: 'StringBinding', credentialsId: 'upload.server.name', variable: 'UPLOAD_SERVER_NAME'],]) {

               installToolsIfNecessary()
               def tmpDir=pwd tmp: true
    	   					
    		   stage('Checkout') {
    		       checkout(configBuilder)
    		   }
    		   
               // Obtain config - building the config might access the checked out workspace.
               def config = configBuilder.build()
    		   
               stage('Update dependencies') {
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
                                    
                        // Update version in the dependency declaration
                        config.dependencyUpdate.updateDependency(config.script,
                            dependencyToUpdateForMaven,
                            dependencyToUpdateForEclipse,
                            config.script.newDependencyVersion)
                            
                        // Push the change
                        gitCommands(config, 
                            """git add """+config.dependencyUpdate.modifiedFilesPattern+""" \
                            && git commit -m 'Update dependency """+dependencyToUpdateForMaven+""" to """+config.script.newDependencyVersion+""" in """+config.dependencyUpdate.modifiedFilesPattern+"""' \
                            && export DEPENDENCIES_UPDATE_COMMIT_HASH=\$(git log --oneline --max-count=1 | cut --delimiter=" " --fields=1) \
                            && git branch -f release \$DEPENDENCIES_UPDATE_COMMIT_HASH \
                            && git checkout master \
                            && git merge --commit --no-edit dependencies \
                            && git pull --commit --no-edit \
                            && git push \
                            && git branch --delete dependencies \
                            && git checkout release""")
                   }
               }
    		   
               def currentReleaseAndItsCommitHash = getCurrentReleaseVersionAndItsCommitHash(config)
               def currentReleaseVersion = currentReleaseAndItsCommitHash['version']
               def currentReleaseCommitHash = currentReleaseAndItsCommitHash['commitHash']
               def oneLineGitLogSinceCurrentRelease = getOneLineGitLogSinceCurrentRelease(config, currentReleaseCommitHash)
               def newVersion = getNextVersion(config, currentReleaseVersion, oneLineGitLogSinceCurrentRelease)
    		   stage('Set versions') {
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
       		   }
    	
    	       stage('Deploy') {
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
    	       }
    	             
    	       stage('Site deploy') {
        	       // Retry is necessary because upload is unreliable
        	       retry(5) {
        	       		maven('''-DnewVersion='''+newVersion+''' \
             					 -D'''+config.propertyForEclipseVersion+'''='''+newVersion+''' \
        	       				 -DskipTests=true \
        	       				 -DskipITs=true \
        	       				 site-deploy''')
        	     	}
        	     	pushNewVersionNumberToGit(config, "*", newVersion)
    	     	}
    	     	
    	     	stage('Trigger dependent jobs') {
        	     	for(String jobToTrigger in config.jobsToTrigger) {
        	     	     build job: jobToTrigger,
        	     	           parameters: [string(name: 'dependencyToUpdate', value: config.mavenGroupId+':'+config.mavenArtifactId),
        	     	                        string(name: 'newDependencyVersion', value: newVersion)],
                               wait: false
        	     	}
    	     	}
    	    }
    	}
	}
}

def installToolsIfNecessary() {
    // Retry is necessary because downloads via apt-get are unreliable
    retry(3) {
        sh '''echo "deb http://http.debian.net/debian jessie-backports main" > /etc/apt/sources.list.d/jessie-backports.list \
            && apt-get update \
            && apt-get remove --yes openjdk-7-jdk \
            && apt-get install --yes openjdk-8-jre-headless openjdk-8-jdk \
            && /usr/sbin/update-java-alternatives -s java-1.8.0-openjdk-amd64 \
            && apt-get install --yes curl git maven libxml-xpath-perl \
            && apt-get install --yes xvfb ratpoison \
            && mkdir --parents /tmp/.X11-unix \
            && chmod 1777 /tmp/.X11-unix \
            && Xvfb -help \
            && ratpoison --version'''
    }
}

def checkout(ConfigBuilder configBuilder) {
    // Use a more complex configuration instead of
    // git url: configBuilder.gitRepoURL, credentialsId: configBuilder.credentialsID
    // to be able to exclude Jenkins jobs from triggering themselves. This leaves the Git repository in detached head
    // state which needs to be overcome using git checkout master before proceeding.
    checkout([$class: 'GitSCM',
        branches: [[name: 'refs/heads/master']],
        doGenerateSubmoduleConfigurations: false,
        extensions: [[$class: 'UserExclusion', excludedUsers: 'Jenkins Monochromata']],
        gitTool: 'Default',
        submoduleCfg: [],
        userRemoteConfigs:
            configBuilder.gitCredentialsId != null 
                ? [[url: configBuilder.gitRepoURL, credentialsId: configBuilder.gitCredentialsId]]
                : [[url: configBuilder.gitRepoURL]]
            ])
    sh '''git branch -f dependencies \
          && git checkout dependencies'''
}

def pushNewVersionNumberToGit(Config config, String patternOfFilesContainingTheVersionNumber, String newVersion) {
    gitCommands(config,
        """git add """+patternOfFilesContainingTheVersionNumber+""" \
           && git commit -m 'Release version"""+newVersion+"""' \
           && git checkout master \
           && git merge --commit --no-edit release \
           && git pull --commit --no-edit \
           && git push \
           && git branch --delete release""")
}

private void gitCommands(Config config, String gitCommands) {
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
                && """+gitCommands
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

    
def getCurrentReleaseVersionAndItsCommitHash(Config config) {
    def tmpDir=pwd tmp:true
    def gitLogFile=tmpDir+'/git.log'
    sh '''git log --oneline --max-count=1 --grep "^Release version [\\.0-9a-f\\-]\\{1,\\}$" > '''+gitLogFile
    def gitLog=readFile(gitLogFile).trim()
    sh 'rm '+gitLogFile
    if(gitLog) {
        // Split e.g. "f44356b Release version 1.0.10-2de0bc4" or
        //            "f44356b Release version 1.0.10" 
        def commitHashAndReleaseMessage=gitLog.split(" ")
        return [ version:    commitHashAndReleaseMessage[3],
                 commitHash: commitHashAndReleaseMessage[0] ]
    } else {
        return [ version:    config.versionNumberToIncrementInInitialBuild,
                 commitHash: null ]
    } 
}

def getOneLineGitLogSinceCurrentRelease(Config config, String currentReleaseCommitHash) {
    def tmpDir=pwd tmp: true
    def logFile = tmpDir+'/last-commits-one-line.txt'
    if(currentReleaseCommitHash) {
        sh 'git log --oneline '+currentReleaseCommitHash+'..HEAD > '+logFile
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
def getNextVersion(Config config, String currentReleaseVersion, String oneLineGitLogSinceCurrentRelease) {
	def tmpDir=pwd tmp: true
	
	// Create new version number
	def newVersion = currentReleaseVersion
	// The first split removes an optional -<commitHash> as per the old versioning scheme.
	String[] parts = currentReleaseVersion.split("\\-")[0].split("\\.")
	if(oneLineGitLogSinceCurrentRelease.contains("+majorVersion")) {
		newVersion = (parts[0].toInteger()+1)+".0.0"
	} else if(oneLineGitLogSinceCurrentRelease.contains("+minorVersion")) {
		newVersion = parts[0]+"."+(parts[1].toInteger()+1)+".0"
	} else {
		newVersion = parts[0]+"."+parts[1]+"."+(parts[2].toInteger()+1)
	}
	
	echo 'Updating version '+currentReleaseVersion+' -> '+newVersion
	currentBuild.displayName = '#'+currentBuild.number+' v'+newVersion
	return newVersion
}