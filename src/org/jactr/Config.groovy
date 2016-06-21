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
     * The URL referring to the Git repository that will be checked out to base the build on.
     */
	public final String gitRepoURL
	
	/**
	 * Creates a new build configuration.
	 * @param releaseMetaDataURL A URL referring to a maven-metadata.xml file of a Maven repository
	 *							 that each successful build using this configuration deploys to. The meta-data
	 *							 will be used to obtain the last version number in order to increment it when
	 *							 starting a new build (see {@link Build#getNextVersion()}).
	 * @param gitRepoURL A URL referring to the Git repository that will be checked out to base the build on.
	 */
	public Config(String releaseMetaDataURL,
	              String gitRepoURL) {
          this.releaseMetaDataURL = releaseMetaDataURL
          this.gitRepoURL = gitRepoURL
  	}
  	
}