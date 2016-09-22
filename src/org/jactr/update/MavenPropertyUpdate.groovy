package org.jactr.update;

/**
 * Updates a property in a Maven POM file that specifies a version of a dependency.
 */
public class MavenPropertyUpdate /* extends AbstractDependencyUpdate */ implements Serializable  {

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
    
    /**
     * A property name used in the POM referenced by {@link #pomPath} to specify a dependency on
     * the project build by the configuration to which this update is added.
     */
    public final String propertyForDependency
    
    public MavenPropertyUpdate(String gitRepoName,
                            String gitRepoURL,
                            String gitFileCredentialsId,
                            String pomPath,
                            String propertyForDependency) {
        this.gitRepoName = gitRepoName
        this.gitRepoURL = gitRepoURL
        this.gitFileCredentialsId = gitFileCredentialsId
        this.modifiedFilesPattern = pomPath
        this.pomPath = pomPath
        this.propertyForDependency = propertyForDependency
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
                 -Dproperty='''+propertyForDependency+''' \
                 -DnewVersion='''+newVersionForMaven+''' \
                 versions:update-property'''
    }

}