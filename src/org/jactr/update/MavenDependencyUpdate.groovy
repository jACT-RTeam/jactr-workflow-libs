package org.jactr.update;

/**
 * Updates a dependency in a Maven POM file.
 */
public class MavenDependencyUpdate /* extends AbstractDependencyUpdate */ implements Serializable  {

    /**
     * The name of the Git repository to update. 
     */
    public final String gitRepoName
    
    /**
     * The URL of the Git repository to clone and update.
     */
    public final String gitRepoURL
    
    /**
     * The ID of the file credentials that are used to authenticate to the Git repository.
     */
    public final String gitFileCredentialsId
    
    /**
     * A pattern to match the files in the Git repository to be checked out,
     * modified and committed by this update.
     */
    public final String modifiedFilesPattern

    /**
     * The path to the Maven POM file to update. The path is relative to the root of the Git repository.
     */
    public final String pomPath
    
    public MavenDependencyUpdate(String gitRepoName,
                            String gitRepoURL,
                            String gitFileCredentialsId,
                            String pomPath) {
        this.gitRepoName = gitRepoName
        this.gitRepoURL = gitRepoURL
        this.gitFileCredentialsId = gitFileCredentialsId
        this.modifiedFilesPattern = pomPath
        this.pomPath = pomPath
    }
    
    /**
     * This method executes a shell script that expects the {@code PATH_TO_SETTINGS_XML}
     * environment variable to be set.
     */
    public void updateDependency(script,
                                 dependencyToUpdateForMaven, newVersionForMaven,
                                 dependencyToUpdateForEclipse, newVersionForEclipse) {
        def tmpDir=script.pwd tmp: true
        script.sh '''mvn \
                 --errors \
                 --settings $PATH_TO_SETTINGS_XML \
                 --file '''+tmpDir+'''/'''+gitRepoName+'''/'''+pomPath+''' \
                 -Dincludes='''+dependencyToUpdateForMaven+''' \
                 -DdepVersion='''+newVersionForMaven+''' \
                 versions:use-dep-version'''
    }

}