package org.jactr.update;

/**
 * Updates a dependency in a {@code META-INF/MANIFEST.MF} file.
 */
public class EclipseDependencyUpdate /* extends AbstractDependencyUpdate */ implements Serializable {

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
    
    public EclipseDependencyUpdate(String gitRepoName,
                            String gitRepoURL,
                            String gitFileCredentialsId) {
        this.gitRepoName = gitRepoName
        this.gitRepoURL = gitRepoURL
        this.gitFileCredentialsId = gitFileCredentialsId
        this.modifiedFilesPattern = "META-INF/MANIFEST.MF"
    }

    /**
     * This method executes a shell script that expects the {@code PATH_TO_SETTINGS_XML}
     * environment variable to be set.
     */
    public void updateDependency(script,
                                 dependencyToUpdateForMaven, newVersionForMaven,
                                 dependencyToUpdateForEclipse, newVersionForEclipse) {
        script.sh '''sed \
                     --in-place \
                     --regexp-extended \
                       's/'''+dependencyToUpdateForEclipse+''';bundle-version="[^"]*"'''
                     +'''/'''+dependencyToUpdateForEclipse+''';bundle-version="'''+newVersionForEclipse+'''"/g' \
                     META-INF/MANIFEST.MF'''
    }

}