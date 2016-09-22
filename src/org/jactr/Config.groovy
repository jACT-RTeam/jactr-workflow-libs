package org.jactr;

/**
 * A configuration for a {@link Build}.
 */
class Config implements Serializable {

    /**
     * The script in which the config is used.
     */
    public final script
    
    /**
     * The label of the Jenkins node to be used to execute the job run
     * from this configuration.
     */
    public final String labelForJenkinsNode
    
    /**
     * The URL referring to a maven-metadata.xml file of a Maven repository that each successful build
     * using this configuration deploys to.
     */
    public final String releaseMetaDataURL
    
    /**
     * The Maven property that will be set with the next version in Eclipse format.
     */
    public final String propertyForEclipseVersion
    
    /**
     * The URL referring to the Git repository that will be checked out to base the build on.
     */
    public final String gitRepoURL
    
    /**
     * The credentials required to access the Git repository. {@code null}, if the Git repository is public
     * and does not require credentials.
     */
    public final String gitCredentialsId
    
    /**
     * True, if this build uses <a href="https://eclipse.org/tycho/sitedocs/tycho-release/">Tycho</a> to build
     * e.g. Eclipse plug-ins.
     */
    public final Boolean isTychoBuild
    
    /**
     * If set, a <a href="http://linux.die.net/man/1/xvfb">Xvfb</a> and a
     * <a href="http://www.nongnu.org/ratpoison/">ratpoison window manager</a> shall be created for the display
     * {@code :<displayNumber>}.
     */
    public final Integer displayNumber
    
    /**
     * If set, a dependency of this project will be updated.
     */
    def dependencyUpdate
    
    /**
     * The Maven groupId of the current release.
     */
    public final String mavenGroupId
    
    /**
     * The Maven artifactId of the current release.
     */
    public final String mavenArtifactId
    
    /**
     * The last released Maven version.
     */
    public final String mavenCurrentReleaseVersion
    
    /**
     * The Git commit hash that is part of {@link #mavenCurrentReleaseVersion}. 
     */
    public final String currentReleaseCommitHash
	
    /**
     * This constructor shall only be used by {@link ConfigBuilder}.
     */
    public Config(script,
           String releaseMetaDataURL,
           String propertyForEclipseVersion,
           String gitRepoURL,
           String gitCredentialsId,
           Boolean isTychoBuild,
           Integer displayNumber,
           String labelForJenkinsNode,
           dependencyUpdate,
           String mavenGroupId,
           String mavenArtifactId,
           String mavenCurrentReleaseVersion,
           String currentReleaseCommitHash) {
        this.script = script
        this.releaseMetaDataURL = releaseMetaDataURL
        this.propertyForEclipseVersion = propertyForEclipseVersion
        this.gitRepoURL = gitRepoURL
        this.gitCredentialsId = gitCredentialsId
        this.isTychoBuild = isTychoBuild
        this.displayNumber = displayNumber
        this.labelForJenkinsNode = labelForJenkinsNode
        this.dependencyUpdate = dependencyUpdate
        this.mavenGroupId = mavenGroupId
        this.mavenArtifactId = mavenArtifactId
        this.mavenCurrentReleaseVersion = mavenCurrentReleaseVersion
        this.currentReleaseCommitHash = currentReleaseCommitHash
    }
  	
}