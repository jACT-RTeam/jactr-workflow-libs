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
    
    /**
     * The path to the {@code META-INF/MANIFEST.MF} to be modified by this update.
     * The path will differ from {@code META-INF/MANIFEST.MF} if e.g. a (sub-)module
     * of the referenced Maven project is to be altered.
     */
    public final String pathToManifestMf
    
    public EclipseDependencyUpdate(String gitRepoName,
                            String gitRepoURL,
                            String gitFileCredentialsId,
                            String pathToManifestMf) {
        this.gitRepoName = gitRepoName
        this.gitRepoURL = gitRepoURL
        this.gitFileCredentialsId = gitFileCredentialsId
        this.modifiedFilesPattern = pathToManifestMf
        this.pathToManifestMf = pathToManifestMf
    }

    /**
     * This method executes a shell script that expects the {@code PATH_TO_SETTINGS_XML}
     * environment variable to be set.
     */
    public void updateDependency(script,
                                 dependencyToUpdateForMaven, newVersionForMaven,
                                 dependencyToUpdateForEclipse, newVersionForEclipse) {
        def tmpDir=script.pwd tmp: true
        script.echo '''sed \
                     --in-place \
                     --regexp-extended \
                       \'s/'''+dependencyToUpdateForEclipse+''';bundle-version="[^"]*"/'''+dependencyToUpdateForEclipse+''';bundle-version="'''+newVersionForEclipse+'''"/g\' \
                     '''+tmpDir+'''/'''+gitRepoName+'''/'''+pathToManifestMf
        script.sh '''sed \
                     --in-place \
                     --regexp-extended \
                       \'s/'''+dependencyToUpdateForEclipse+''';bundle-version="[^"]*"/'''+dependencyToUpdateForEclipse+''';bundle-version="'''+newVersionForEclipse+'''"/g\' \
                     '''+tmpDir+'''/'''+gitRepoName+'''/'''+pathToManifestMf
    }

}