package org.jactr;

/**
 * A builder for a {@link Config} that will be used in a Jenkins pipeline or multi-branch
 * pipeline job.
 */
public class ConfigBuilder {
	
	/**
  	 * The URL referring to a maven-metadata.xml file of a Maven repository that each successful build
  	 * using the configured job deploys to.
	 */
	private final String releaseMetaDataURL
	
	/**
     * The URL referring to the Git repository that will be checked out to base the build on.
     */
	private final String gitRepoURL
	
	/**
	 * Optional credentials required to access the Git repository in case it is not public.
	 * {@code null}, if the Git repository is public and does not require credentials. Defaults to {@code null}.
	 */
	private String gitCredentialsId
	
    /**
     * The label of the Jenkins node to be used to execute the job run
     * from the configuration created by the builder. Has a default value.
     */
	private labelForJenkinsNode = "2gb";
	
	/**
	 * The Maven property that will be set with the next version in Eclipse format. Has a default value.
	 */
	private String propertyForEclipseVersion = "newVersionForEclipseIsNotUsed"
	
	/**
	 * True, if the configured build uses <a href="https://eclipse.org/tycho/sitedocs/tycho-release/">Tycho</a> to build
	 * e.g. Eclipse plug-ins. Defaults to {@code false}.
	 */
	private boolean isTychoBuild = false
	
	/**
	 * If set, a <a href="http://linux.die.net/man/1/xvfb">Xvfb</a> and a
	 * <a href="http://www.nongnu.org/ratpoison/">ratpoison window manager</a> shall be created for the display
	 * {@code :<displayNumber>}. Defaults to {@code null}.
	 */
	private Integer displayNumber = null

    /**
     * If set, the jobs named in this list will be triggered if the configured job completes successfully.
     * Triggering will include job parameters to have these jobs update their dependencies to cover the new
     * version created by the configured job. Defaults to {@code null}.
     */
	private List<String> jobsToUpdateToNewlyBuiltVersion = null

    /**
     * The Maven groupId obtained from {@link #releaseMetaDataURL} before the configuration is build.
     *
     * @see #parseMavenMetadata()
     * @see #build()
     */
	private String mavenGroupId = null
	
    /**
     * The Maven artifactId obtained from {@link #releaseMetaDataURL} before the configuration is build.
     *
     * @see #parseMavenMetadata()
     * @see #build()
     */
	private String mavenArtifactId = null
	
    /**
     * The Maven last released version obtained from {@link #releaseMetaDataURL} before the configuration is build.
     *
     * @see #parseMavenMetadata()
     * @see #build()
     */
	private String mavenCurrentReleaseVersion = null

    /**
     * Create a builder and supply the required configuration information to build
     * from a public Git repository.
     * 
	 * @param releaseMetaDataURL A URL referring to a maven-metadata.xml file of a Maven repository
	 *							 that each successful build using the configured job deploys to. The meta-data
	 *							 will be used to obtain the last version number in order to increment it when
	 *							 starting a new build (see {@link Build#getNextVersion()}).
     * @param gitRepoURL A URL referring to the Git repository that will be checked out to base the build on.
     */
	public ConfigBuilder(String releaseMetaDataURL,
						 String gitRepoURL) {
		this.releaseMetaDataURL = releaseMetaDataURL
		this.gitRepoURL = gitRepoURL
	}
	
	/**
	 * Configure a custom label to be used to choose the node to run the configured job on.
	 */
	public ConfigBuilder useJenkinsNodeWithLabel(String label) {
		this.labelForJenkinsNode = label
	}
	
	/**
	 * Set the ID of the Jenkins-managed credentials to be used to access a Git repository that
	 * requires credentials.
	 */
	public ConfigBuilder gitCredentialsId(String credentialsId) {
		this.gitCredentialsId = credentialsId
	}
	
	/**
	 * Sets the Maven property that will be set with the version (in Eclipse format) that will
	 * be build by the configured job.
	 *
	public ConfigBuilder usePropertyForEclipseVersion(String propertyName) {
		this.propertyForEclipseVersion = propertyName
	}
	
	/**
	 * Configures the build to be <a href="https://eclipse.org/tycho/sitedocs/tycho-release/">Tycho</a>-specific.
	 */
	public ConfigBuilder isTychoBuild() {
		this.isTychoBuild = true
	}
	
	/**
	 * Configures the build to have a <a href="http://linux.die.net/man/1/xvfb">Xvfb</a> and a
	 * <a href="http://www.nongnu.org/ratpoison/">ratpoison window manager</a> for the display
	 * {@code :<displayNumber>}.
	 */
	public ConfigBuilder provideDisplayAndWindowManager(int displayNumber) {
		this.displayNumber = displayNumber
	}

    /**
     * Set a list of job names that will be triggered if the configured job completes successfully.
     * Triggering will include job parameters to have these jobs update their dependencies to cover the new
     * version created by the configured job.
     */
	public ConfigBuilder updateDependentJobsToNewlyBuiltVersion(String... jobNames) {
		this.jobsToUpdateToNewlyBuiltVersion = java.util.Arrays.asList(jobNames)
	}

    /**
     * Create a new configuration from the information supplied to the builder.
     */
	public Config build() {
		parseMavenMetadata()
	    return new Config(
	    	this.releaseMetaDataURL,
	    	this.propertyForEclipseVersion,
	    	this.gitRepoURL,
	    	this.gitCredentialsId,
	    	this.isTychoBuild,
	    	this.displayNumber,
	    	this.labelForJenkinsNode,
	    	this.jobsToUpdateToNewlyBuiltVersion,
	    	this.mavenGroupId,
	    	this.mavenArtifactId,
	    	this.mavenCurrentReleaseVersion)
	}
	
  	private void parseMavenMetadata() {
		node(this.labelForJenkinsNode) {
			def tmpDir=pwd tmp: true
		
			// Get the Maven meta-data: groupId, artifactId, release version
			def mavenMetaDataFile = tmpDir+'/maven-metadata.xml'
			def groupIdFile = tmpDir+'/maven.groupId'
			def artifactIdFile = tmpDir+'/maven.artifactId'
			def versionFile = tmpDir+'/maven.release'
			sh 'curl --silent '+config.releaseMetaDataURL+' > '+mavenMetaDataFile
			sh 'xpath -e metadata/groupId -q '+mavenMetaDataFile+' | sed --regexp-extended "s/<\\/?groupId>//g" > '+groupIdFile
			sh 'xpath -e metadata/artifactId -q '+mavenMetaDataFile+' | sed --regexp-extended "s/<\\/?artifactId>//g" > '+artifactIdFile
			sh 'xpath -e metadata/versioning/release -q '+mavenMetaDataFile+' | sed --regexp-extended "s/<\\/?release>//g" > '+versionFile
			this.mavenGroupId = readFile(groupIdFile).trim()
			this.mavenArtifactId = readFile(artifactIdFile).trim()
			this.mavenCurrentReleaseVersion = readFile(versionFile).trim()
			sh 'rm '+mavenMetaDataFile
			sh 'rm '+groupIdFile
			sh 'rm '+artifactIdFile
			sh 'rm '+versionFile
		}
  	}
}