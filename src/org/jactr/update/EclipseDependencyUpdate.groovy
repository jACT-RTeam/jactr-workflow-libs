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
    private final String pathToManifestMf
    
    /**
     * Optional path to a {@code feature.xml} file to be modified by this update.
     */
    private String pathToFeatureXml
    
    public EclipseDependencyUpdate(String pathToManifestMf) {
        this.modifiedFilesPattern = pathToManifestMf
        this.pathToManifestMf = pathToManifestMf
    }
    
    public EclipseDependencyUpdate(String pathToManifestMf, String pathToFeatureXml) {
        this(pathToManifestMf)
        this.pathToFeatureXml = pathToFeatureXml
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
        if(this.pathToFeatureXml) {
            script.sh '''sed \
                         --in-place \
                         --regexp-extended \
                           \'s/<import plugin="('''+dependencyMavenGroupId+'''[^;]*)" version="[^"]*"/<import plugin="\\1" version="'''+newVersion+'''"/g\' \
                         '''+pathToManifestMf
        }
    }

}