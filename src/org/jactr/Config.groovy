package org.jactr;

/**
 * A configuration for a {@link Build}.
 */
class Config implements Serializable {

	private final String releaseMetaDataURL
	private final String gitRepoURL
	private String newVersion
	
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
  	
  	/**
  	 * @return The URL referring to a maven-metadata.xml file of a Maven repository that each successful build
  	 *		   using this configuration deploys to.
  	 */
  	public String getReleaseMetaDataURL() {
  		return this.releaseMetaDataURL
    }
    
    /**
     * @return The URL referring to the Git repository that will be checked out to base the build on.
     */
    public String getGitRepoURL() {
    	return this.gitRepoURL
    }
  	
  	/**
  	 * Set the version number that will be produced if this build passes.
  	 * @param newVersion the new version in Maven format ({@code <majorPart>.<minorPart>.<patchPart>-<qualifier>})
  	 * @see Build#getNextVersion()
  	 */
  	public void setNewVersion(String newVersion) {
  		this.newVersion = newVersion;
  	}
  	
  	/**
  	 * Return the version that will be produced if this build passes, using Maven formatting.
  	 * @return the version number in Maven format ({@code <majorPart>.<minorPart>.<patchPart>-<qualifier>})
  	 */
  	public String getNewVersionForMaven() {
  		return this.newVersion;
  	}
  	
  	/**
  	 * Return the version that will be produced if this build passes, using Eclipse formatting.
  	 * @return the version number in Eclipse format ({@code <majorPart>.<minorPart>.<patchPart>.<qualifier>})
  	 */
  	public String getNewVersionForEclipse() {
  		return getNewVersionForMaven().replaceAll('-', '.')
  	}
  	
}