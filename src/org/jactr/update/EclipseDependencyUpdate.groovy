package org.jactr.update;

/**
 * Updates all dependencies with a given Maven groupId in a {@code META-INF/MANIFEST.MF} file.
 */
public class EclipseDependencyUpdate /* extends AbstractDependencyUpdate */ implements Serializable {
    
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
    
    public EclipseDependencyUpdate(String pathToManifestMf) {
        this.modifiedFilesPattern = pathToManifestMf
        this.pathToManifestMf = pathToManifestMf
    }

    /**
     * Updates all dependencies with the groupId from {@cpde dependencyToUpdateForMaven}
     * to the given new version.
     */
    public void updateDependency(script,
                                 dependencyToUpdateForMaven,
                                 dependencyToUpdateForEclipse,
                                 newVersion) {
        def dependencyMavenGroupId=dependencyToUpdateForMaven.split(':')[0]
        script.sh '''sed \
                     --in-place \
                     --regexp-extended \
                       \'s/('''+dependencyMavenGroupId+'''[^;]*);bundle-version="[^"]*"/\\1;bundle-version="'''+newVersion+'''"/g\' \
                     '''+pathToManifestMf
    }

}