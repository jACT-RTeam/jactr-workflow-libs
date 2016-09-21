package org.jactr;

import org.jactr.update.EclipseDependencyUpdate;
import org.jactr.update.MavenDependencyUpdate;
import org.jactr.update.MavenPropertyUpdate;

/**
 * A builder for a {@link Config} that will be used in a Jenkins pipeline or multi-branch
 * pipeline job.
 */
public class ConfigBuilder implements Serializable {

    /**
     * The script in which the config builder is used.
     */
    private final script
	
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
    private String labelForJenkinsNode = "2gb";
    
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
     * {@code :<displayNumber>}. Defaults to {@code 0}.
     */
    private Integer displayNumber = 0
    
    /**
     * If set, the jobs named in this list will be triggered if the configured job completes successfully.
     * Triggering will include job parameters to have these jobs update their dependencies to cover the new
     * version created by the configured job. Defaults to {@code null}.
     */
    private List /*<AbstractDependencyUpdate>*/ dependenciesToUpdateToNewlyBuiltVersion = new java.util.ArrayList()
    
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
     * The commit hash extracted from the last released version obtained from {@link #releaseMetaDataURL} before
     * the configuration is build.
     *
     * @see #parseMavenMetadata()
     * @see #build()
     */
    private String currentReleaseCommitHash = null

    /**
     * Create a builder and supply the required configuration information to build
     * from a public Git repository.
     * 
     * @param script             The script in which the builder is used.
     * @param releaseMetaDataURL A URL referring to a maven-metadata.xml file of a Maven repository
     *                           that each successful build using the configured job deploys to. The meta-data
     *                           will be used to obtain the last version number in order to increment it when
     *                           starting a new build (see {@link Build#getNextVersion()}).
     * @param gitRepoURL A URL referring to the Git repository that will be checked out to base the build on.
     */
    public ConfigBuilder(script,
                         String releaseMetaDataURL,
    					 String gitRepoURL) {
        this.script = script
        this.releaseMetaDataURL = releaseMetaDataURL
        this.gitRepoURL = gitRepoURL
    }
	
    /**
     * Configure a custom label to be used to choose the node to run the configured job on.
     */
    public ConfigBuilder useJenkinsNodeWithLabel(String label) {
        this.labelForJenkinsNode = label
        return this
    }
	
    /**
     * Set the ID of the Jenkins-managed credentials to be used to access a Git repository that
     * requires credentials.
     */
    public ConfigBuilder gitCredentialsId(String credentialsId) {
        this.gitCredentialsId = credentialsId
        return this
    }
	
    /**
     * Sets the Maven property that will be set with the version (in Eclipse format) that will
     * be build by the configured job.
     */
    public ConfigBuilder propertyForEclipseVersion(String propertyName) {
        this.propertyForEclipseVersion = propertyName
        return this
    }
	
    /**
     * Configures the build to be <a href="https://eclipse.org/tycho/sitedocs/tycho-release/">Tycho</a>-specific.
     */
    public ConfigBuilder isTychoBuild() {
        this.isTychoBuild = true
        return this
    }
	
    /**
     * Configures the build to have a <a href="http://linux.die.net/man/1/xvfb">Xvfb</a> and a
     * <a href="http://www.nongnu.org/ratpoison/">ratpoison window manager</a> for the display
     * {@code :<displayNumber>}.
     */
    public ConfigBuilder provideDisplayAndWindowManager(int displayNumber) {
        this.displayNumber = displayNumber
        return this
    }
    
    /**
     * Configures an update of an Eclipse plug-in project. The update will be performed
     * if the configured job completes successfully.
     */
    public ConfigBuilder updateEclipseDependencyToNewlyBuiltVersion(
            String gitRepoName,
            String gitRepoURL,
            String gitFileCredentialsId,
            String pathToManifestMf) {
        def dependencyUpdate = new EclipseDependencyUpdate(gitRepoName, gitRepoURL, gitFileCredentialsId, pathToManifestMf)
        dependenciesToUpdateToNewlyBuiltVersion.add(dependencyUpdate)
        return this
    }

    /**
     * Configures an update of a dependency declaration in a Maven project. The update will be performed
     * if the configured job completes successfully.
     */
    public ConfigBuilder updateMavenDependencyToNewlyBuiltVersion(
            String gitRepoName,
            String gitRepoURL,
            String gitFileCredentialsId,
            String pomPath = "pom.xml") {
        def dependencyUpdate = new MavenDependencyUpdate(gitRepoName, gitRepoURL, gitFileCredentialsId, pomPath)
        dependenciesToUpdateToNewlyBuiltVersion.add(dependencyUpdate)
        return this
    }
    
    /**
     * Configures an update to a property in a Maven project. The update will be performed
     * if the configured job completes successfully.
     */
    public ConfigBuilder updateMavenPropertyToNewlyBuiltVersion(
            String gitRepoName,
            String gitRepoURL,
            String gitFileCredentialsId,
            String pomPath = "pom.xml",
            String propertyForDependency) {
        def dependencyUpdate = new MavenPropertyUpdate(gitRepoName, gitRepoURL, gitFileCredentialsId, pomPath, propertyForDependency)
        dependenciesToUpdateToNewlyBuiltVersion.add(dependencyUpdate)
        return this
    }

    /**
     * Create a new configuration from the information supplied to the builder.
     */
    public Config build() {
        parseMavenMetadata()
        def config = new org.jactr.Config(
            this.script,
            this.releaseMetaDataURL,
            this.propertyForEclipseVersion,
            this.gitRepoURL,
            this.gitCredentialsId,
            this.isTychoBuild,
            this.displayNumber,
            this.labelForJenkinsNode,
            this.dependenciesToUpdateToNewlyBuiltVersion,
            this.mavenGroupId,
            this.mavenArtifactId,
            this.mavenCurrentReleaseVersion,
            this.currentReleaseCommitHash)
        return config
    }
	
    private void parseMavenMetadata() {
        script.node(this.labelForJenkinsNode) {
            installToolsIfNecessary()
            def tmpDir=script.pwd tmp: true
            
            // Get the Maven meta-data: groupId, artifactId, release version
            def mavenMetaDataFile = tmpDir+'/maven-metadata.xml'
            def groupIdFile = tmpDir+'/maven.groupId'
            def artifactIdFile = tmpDir+'/maven.artifactId'
            def versionFile = tmpDir+'/maven.release'
            script.sh '''HTTP_STATUS_CODE=$(curl --silent \
                              --output '''+mavenMetaDataFile+''' \
                              --write-out "%{http_code}" \
                              '''+this.releaseMetaDataURL+''') \
                         && if [ "$HTTP_STATUS_CODE" -ne "200" ]; then
                                echo "Non-200 HTTP status code $HTTP_STATUS_CODE when retrieving '''+this.releaseMetaDataURL+'''";
                                exit 1;
                            fi'''
            script.sh 'xpath -e metadata/groupId -q '+mavenMetaDataFile+' | sed --regexp-extended "s/<\\/?groupId>//g" > '+groupIdFile
            script.sh 'xpath -e metadata/artifactId -q '+mavenMetaDataFile+' | sed --regexp-extended "s/<\\/?artifactId>//g" > '+artifactIdFile
            script.sh 'xpath -e metadata/versioning/release -q '+mavenMetaDataFile+' | sed --regexp-extended "s/<\\/?release>//g" > '+versionFile
            this.mavenGroupId = script.readFile(groupIdFile).trim()
            this.mavenArtifactId = script.readFile(artifactIdFile).trim()
            this.mavenCurrentReleaseVersion = script.readFile(versionFile).trim()
            String[] versionParts = this.mavenCurrentReleaseVersion.split('-')
            if(versionParts.length != 2) {
                throw new IllegalArgumentException("Last release version '"+this.mavenCurrentReleaseVersion+"' is not in format <major>.<minor>.<patch>-<commitHash>")
            }
            this.currentReleaseCommitHash = versionParts[1]
            script.sh 'rm '+mavenMetaDataFile
            script.sh 'rm '+groupIdFile
            script.sh 'rm '+artifactIdFile
            script.sh 'rm '+versionFile
        }
    }
    


    private void installToolsIfNecessary() {
        // Retry is necessary because downloads via apt-get are unreliable
        script.retry(3) {
            script.sh '''echo "deb http://http.debian.net/debian jessie-backports main" > /etc/apt/sources.list.d/jessie-backports.list \
                && apt-get update \
                && apt-get remove --yes openjdk-7-jdk \
                && apt-get install --yes openjdk-8-jre-headless openjdk-8-jdk \
                && /usr/sbin/update-java-alternatives -s java-1.8.0-openjdk-amd64 \
                && apt-get install --yes curl git maven libxml-xpath-perl \
                && apt-get install --yes xvfb ratpoison \
                && mkdir --parents /tmp/.X11-unix \
                && chmod 1777 /tmp/.X11-unix \
                && Xvfb -help \
                && ratpoison --version'''
        }
    }
}