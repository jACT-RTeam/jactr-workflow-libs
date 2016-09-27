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
     * The names of jobs to be triggered when the configured job completed.
     */
    public final List<String> jobsToTrigger
    
    /**
     * The Maven groupId of the current release.
     */
    public final String mavenGroupId
    
    /**
     * The Maven artifactId of the current release.
     */
    public final String mavenArtifactId
    
    public final String versionNumberToIncrementInInitialBuild
	
    /**
     * This constructor shall only be used by {@link ConfigBuilder}.
     */
    public Config(script,
           String propertyForEclipseVersion,
           String gitRepoURL,
           String gitCredentialsId,
           Boolean isTychoBuild,
           Integer displayNumber,
           String labelForJenkinsNode,
           dependencyUpdate,
           List<String> jobsToTrigger,
           String mavenGroupId,
           String mavenArtifactId,
           String versionNumberToIncrementInInitialBuild) {
        this.script = script
        this.propertyForEclipseVersion = propertyForEclipseVersion
        this.gitRepoURL = gitRepoURL
        this.gitCredentialsId = gitCredentialsId
        this.isTychoBuild = isTychoBuild
        this.displayNumber = displayNumber
        this.labelForJenkinsNode = labelForJenkinsNode
        this.dependencyUpdate = dependencyUpdate
        this.jobsToTrigger = jobsToTrigger
        this.mavenGroupId = mavenGroupId
        this.mavenArtifactId = mavenArtifactId
        this.versionNumberToIncrementInInitialBuild = versionNumberToIncrementInInitialBuild
    }
  	
}