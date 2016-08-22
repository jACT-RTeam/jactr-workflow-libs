package org.jactr;

/**
 * A configuration for a {@link Build}.
 */
class Config implements Serializable {

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
	 * Creates a new build configuration for a public Git repository.
	 * @param releaseMetaDataURL A URL referring to a maven-metadata.xml file of a Maven repository
	 *							 that each successful build using this configuration deploys to. The meta-data
	 *							 will be used to obtain the last version number in order to increment it when
	 *							 starting a new build (see {@link Build#getNextVersion()}).
	 * @param propertyForEclipseVersion The Maven property that will be set with the next version in Eclipse format.
	 * @param gitRepoURL A URL referring to the Git repository that will be checked out to base the build on.
	 */
	public Config(String releaseMetaDataURL,
	              String propertyForEclipseVersion,
	              String gitRepoURL) {
	      this(releaseMetaDataURL, propertyForEclipseVersion, gitRepoURL, null)
  	}
  	
	/**
	 * Creates a new build configuration for a private Git repository that requires authentication.
	 * @param releaseMetaDataURL A URL referring to a maven-metadata.xml file of a Maven repository
	 *							 that each successful build using this configuration deploys to. The meta-data
	 *							 will be used to obtain the last version number in order to increment it when
	 *							 starting a new build (see {@link Build#getNextVersion()}).
	 * @param propertyForEclipseVersion The Maven property that will be set with the next version in Eclipse format.
	 * @param gitRepoURL A URL referring to the Git repository that will be checked out to base the build on.
	 * @param gitCredentialsId The ID of the Jenkins-managed credentials required to access the repository. 
	 */
	public Config(String releaseMetaDataURL,
	              String propertyForEclipseVersion,
	              String gitRepoURL,
	              String gitCredentialsId) {
	      this.releaseMetaDataURL = releaseMetaDataURL
	      this.propertyForEclipseVersion = propertyForEclipseVersion
	      this.gitRepoURL = gitRepoURL
	      this.gitCredentialsId = gitCredentialsId
  	}
  	
}