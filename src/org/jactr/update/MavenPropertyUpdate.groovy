package org.jactr.update;

/**
 * Updates a property in a Maven POM file that specifies a version of a dependency.
 * The names of the properties to be updated are equal to {@code <mavenGroupId>.<mavenArtifactId>}
 * where {@code <mavenGroupId>} and {@code <mavenArtifactId>} identify the dependency whose version
 * is to be set in the respective property.
 */
public class MavenPropertyUpdate /* extends AbstractDependencyUpdate */ implements Serializable  {
    
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
     * Create a new property update to update Maven properties for dependencies in the POM file whose path
     * is given.
     * <p> 
     * The names of the properties to be updated are equal to {@code <mavenGroupId>.<mavenArtifactId>}
     * where {@code <mavenGroupId>} and {@code <mavenArtifactId>} identify the dependency whose version
     * is to be set in the respective property.
     */
    public MavenPropertyUpdate(String pomPath) {
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
        def propertyForDependency = dependencyToUpdateForMaven.replace(':', '.')
        script.sh '''mvn \
                 --errors \
                 --settings $PATH_TO_SETTINGS_XML \
                 --file '''+pomPath+''' \
                 -Dproperty='''+propertyForDependency+''' \
                 -DnewVersion='''+newVersionForMaven+''' \
                 versions:update-property'''
    }

}