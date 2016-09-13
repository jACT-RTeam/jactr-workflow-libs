package org.jactr;

/**
 * An update to a dependency.
 */
class DependencyUpdate implements Serializable {

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
     * The path to the Maven POM file to update. The path is relative to the root of the Git repository.
     */
    public final String pomPath
    
    public DependencyUpdate(String gitRepoName,
                            String gitRepoURL,
                            String gitFileCredentialsId,
                            String pomPath) {
        this.gitRepoName = gitRepoName
        this.gitRepoURL = gitRepoURL
        this.gitFileCredentialsId = gitFileCredentialsId
        this.pomPath = pomPath
    }

}