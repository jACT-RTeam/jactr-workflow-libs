package org.jactr;

/**
 * A configuration for a {@link Build}.
 */
class Config implements Serializable {
    
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
     * If set, the jobs named in this list will be triggered if the configured job completes successfully.
     * Triggering will include job parameters to have these jobs update their dependencies to cover the new
     * version created by the configured job.
     */
    public List<String> jobsToUpdateToNewlyBuiltVersion
    
    /**
     * The Maven groupId obtained from {@link #releaseMetaDataURL} before the configuration is build.
     *
     * @see #parseMavenMetadata()
     * @see #build()
     */
    public final String mavenGroupId
    
    /**
     * The Maven artifactId obtained from {@link #releaseMetaDataURL} before the configuration is build.
     *
     * @see #parseMavenMetadata()
     * @see #build()
     */
    public final String mavenArtifactId
    
    /**
     * The last released Maven version obtained from {@link #releaseMetaDataURL} before the configuration is build.
     *
     * @see #parseMavenMetadata()
     * @see #build()
     */
    public final String mavenCurrentReleaseVersion
	
    /**
     * This constructor shall only be used by {@link ConfigBuilder}.
     */
    public Config(String releaseMetaDataURL,
           String propertyForEclipseVersion,
           String gitRepoURL,
           String gitCredentialsId,
           Boolean isTychoBuild,
           Integer displayNumber,
           String labelForJenkinsNode,
           List<String> jobsToUpdateToNewlyBuiltVersion,
           String mavenGroupId,
           String mavenArtifactId,
           String mavenCurrentReleaseVersion) {  	
        this.releaseMetaDataURL = releaseMetaDataURL
        this.propertyForEclipseVersion = propertyForEclipseVersion
        this.gitRepoURL = gitRepoURL
        this.gitCredentialsId = gitCredentialsId
        this.isTychoBuild = isTychoBuild
        this.displayNumber = displayNumber
        this.labelForJenkinsNode = labelForJenkinsNode
        this.jobsToUpdateToNewlyBuiltVersion = jobsToUpdateToNewlyBuiltVersion
        this.mavenGroupId = mavenGroupId
        this.mavenArtifactId = mavenArtifactId
        this.mavenCurrentReleaseVersion = mavenCurrentReleaseVersion
    }
  	
}