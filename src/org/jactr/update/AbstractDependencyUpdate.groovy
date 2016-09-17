package org.jactr.update;

/**
 * An update to a dependency.
 * <p>
 * This class is currently unused since Jenkins pipeline scripts do not support inheritance yet.
 */
abstract class AbstractDependencyUpdate implements Serializable {

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
    
    public AbstractDependencyUpdate(String gitRepoName,
                            String gitRepoURL,
                            String gitFileCredentialsId,
                            String modifiedFilesPattern) {
        this.gitRepoName = gitRepoName
        this.gitRepoURL = gitRepoURL
        this.gitFileCredentialsId = gitFileCredentialsId
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
     * @param newVersionForMaven the new version of the dependency in Maven format
     *                           ({@code <major>.<minor>.<patch>-<qualifier>}, e.g.
     *                           {@code 1.0.0-abcdef})
     * @param dependencyToUpdateForEclipse the reference to the dependency to be updated
     *                                     in Eclipse format ({@code <groupId>.<artifactId>}
     *                                     - with redundancy between groupId and artifactId
     *                                     eliminated, e.g. {@code de.monochromata.anaphors}
     *                                     for groupId {@code de.monochromata.anaphors} and
     *                                     artifactId {@code anaphors})
     * @param newVersionForEclipse the new version of the dependency in Eclipse format
     *                             ({@code <major>.<minor>.<patch>.<qualifier>}, e.g.
     *                             {@code 1.0.0.abcdef})
     */
    abstract void updateDependency(script,
                                   dependencyToUpdateForMaven, newVersionForMaven,
                                   dependencyToUpdateForEclipse, newVersionForEclipse)

}