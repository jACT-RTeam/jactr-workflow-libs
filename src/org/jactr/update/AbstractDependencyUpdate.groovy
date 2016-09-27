package org.jactr.update;

/**
 * An update to a dependency.
 * <p>
 * This class is currently unused since Jenkins pipeline scripts do not support inheritance yet.
 */
abstract class AbstractDependencyUpdate implements Serializable {
    
    /**
     * A pattern to match the files in the Git repository to be checked out,
     * modified and committed by this update.
     */
    public final String modifiedFilesPattern
    
    public AbstractDependencyUpdate(String modifiedFilesPattern) {
        this.modifiedFilesPattern = modifiedFilesPattern
    }
    
    /**
     * Implementations of this method modify to the files identified by
     * {@link #modifiedFilesPattern}.
     *
     * @param script the workflow script instance that provides access to job
     *               parameters and pipeline steps
     * @param dependencyToUpdateForMaven the reference to the dependency to be updated in
     *                                   Maven format ({@code <groupId>:<artifactId>},
     *                                   e.g. {@code de.monochromata.anaphors:anaphors})
     * @param dependencyToUpdateForEclipse the reference to the dependency to be updated
     *                                     in Eclipse format ({@code <groupId>.<artifactId>}
     *                                     - with redundancy between groupId and artifactId
     *                                     eliminated, e.g. {@code de.monochromata.anaphors}
     *                                     for groupId {@code de.monochromata.anaphors} and
     *                                     artifactId {@code anaphors})
     * @param newVersion the new version of the dependency in 
     *                           ({@code <major>.<minor>.<patch>} format, e.g.
     *                           {@code 1.0.0})
     */
    abstract void updateDependency(script,
                                   dependencyToUpdateForMaven,
                                   dependencyToUpdateForEclipse,
                                   newVersion)

}