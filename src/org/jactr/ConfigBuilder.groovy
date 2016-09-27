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
     * If set, other jobs may trigger this job to update a dependency in this job. Defaults to {@code null}.
     */
    private Object dependencyUpdate = null
    
    /**
     * The names of jobs to be triggered by the configured job.
     */
    private List<String> jobsToTrigger = new ArrayList<String>()
    
    /**
     * An optional path to a {@code pom.xml} file from which {@link #mavenGroupId}, {@link #mavenArtifactId}
     * and {@link #versionNumberToIncrementInInitialBuild} will be read if they have not been passed to the
     * constructor.
     */
    private String pomPath = null
    
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
    
    private String versionNumberToIncrementInInitialBuild = null

    /**
     * Create a builder and supply the required configuration information to build
     * from a public Git repository.
     * 
     * @param script                 The script in which the builder is used.
     * @param mavenGroupId           The Maven groupId of the artifact built by the configured job.
     * @param mavenArtifactId        The Maven artifactId of the artifact built by the configured job.
     * @param versionNumberToIncrementInInitialBuild The version number to use be incremented during the initial build.
     * @param gitRepoURL             A URL referring to the Git repository that will be checked out to base the build on.
     */
    public ConfigBuilder(script,
                         String mavenGroupId,
                         String mavenArtifactId,
                         String versionNumberToIncrementInInitialBuild,
    					 String gitRepoURL) {
        this.script = script
        this.mavenGroupId = mavenGroupId
        this.mavenArtifactId = mavenArtifactId
        this.versionNumberToIncrementInInitialBuild = versionNumberToIncrementInInitialBuild
        this.gitRepoURL = gitRepoURL
    }
    
    /**
     * Create a builder and supply the required configuration information to build
     * from a public Git repository.
     * 
     * @param script                 The script in which the builder is used.
     * @param mavenGroupId           The Maven groupId of the artifact built by the configured job.
     * @param mavenArtifactId        The Maven artifactId of the artifact built by the configured job.
     * @param versionNumberToIncrementInInitialBuild The version number to use be incremented during the initial build.
     * @param gitRepoURL             A URL referring to the Git repository that will be checked out to base the build on.
     * @param dependencyUpdate configures how to update the dependencies in this job
     */
    public ConfigBuilder(script,
                         String mavenGroupId,
                         String mavenArtifactId,
                         String versionNumberToIncrementInInitialBuild,
                         String gitRepoURL,
                         dependencyUpdate) {
        this(script, mavenGroupId, mavenArtifactId, versionNumberToIncrementInInitialBuild, gitRepoURL)
        this.dependencyUpdate = dependencyUpdate
    }
    
    /**
     * Create a builder and supply the required configuration information to build
     * from a public Git repository.
     * <p>
     * The version provided in the pom.xml whose path is provided will be used as the base of the increment
     * of the version number in the first build.
     * 
     * @param script     The script in which the builder is used.
     * @param pomPath    The path to a {@code pom.xml} file from which the following info is read:
     *                   the Maven groupId and artifactId and the version number for the first build.
     * @param gitRepoURL A URL referring to the Git repository that will be checked out to base the build on.
     */
    public ConfigBuilder(script,
                         String pomPath,
                         String gitRepoURL) {
        this(script, null, null, null, gitRepoURL)
        this.pomPath = pomPath
    }
    
    /**
     * Create a builder and supply the required configuration information to build
     * from a public Git repository.
     * <p>
     * The version provided in the pom.xml whose path is provided will be used as the base of the increment
     * of the version number in the first build.
     * 
     * @param script     The script in which the builder is used.
     * @param pomPath    The path to a {@code pom.xml} file from which the following info is read:
     *                   the Maven groupId and artifactId and the version number for the first build.
     * @param gitRepoURL A URL referring to the Git repository that will be checked out to base the build on.
     * @param dependencyUpdate configures how to update the dependencies in this job
     */
    public ConfigBuilder(script,
                         String pomPath,
                         String gitRepoURL,
                         dependencyUpdate) {
        this(script, pomPath, gitRepoURL)
        this.dependencyUpdate = dependencyUpdate
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
     * Add the given job name to the list of jobs to be triggered after the configured job completed.
     */
    public ConfigBuilder triggerJob(String jobName) {
        this.jobsToTrigger.add(jobName)
        return this
    }

    /**
     * Create a new configuration from the information supplied to the builder.
     */
    public Config build() {
        installToolsIfNecessary()
        if(!this.mavenGroupId) {
            this.mavenGroupId = readMavenGroupIdFrom(pomPath)
        }
        if(!this.mavenArtifactId) {
            this.mavenArtifactId = readMavenArtifactIdFrom(pomPath)
        }
        if(!this.versionNumberToIncrementInInitialBuild) {
            this.versionNumberToIncrementInInitialBuild = readVersionNumberToIncrementInInitialBuildFrom(pomPath)
        }
        def config = new org.jactr.Config(
            this.script,
            this.propertyForEclipseVersion,
            this.gitRepoURL,
            this.gitCredentialsId,
            this.isTychoBuild,
            this.displayNumber,
            this.labelForJenkinsNode,
            this.dependencyUpdate,
            this.jobsToTrigger,
            this.mavenGroupId,
            this.mavenArtifactId,
            this.versionNumberToIncrementInInitialBuild)
        return config
    }
	
    private String readMavenGroupIdFrom(String pomPath) {
        return readMavenPomProjectElement(pomPath, 'groupId')
    }
    
    private String readMavenArtifactIdFrom(String pomPath) {
        return readMavenPomProjectElement(pomPath, 'artifactId')
    }
    
    private String readVersionNumberToIncrementInInitialBuildFrom(String pomPath) {
        return readMavenPomProjectElement(pomPath, 'version')
    }
    
    private String readMavenPomProjectElement(String pomPath, String elementName) {
        script.node(this.labelForJenkinsNode) {
            def tmpDir=script.pwd tmp: true
            def elementFile = tmpDir+'/maven.'+elementName
            script.sh 'xpath -e project/'+elementName+' -q '+pomPath+' | sed --regexp-extended "s/<\\/?'+elementName+'>//g" > '+elementFile
            def element = script.readFile(elementFile).trim()
            script.sh 'rm '+elementFile
            return element
        }
    }

    private void installToolsIfNecessary() {
        script.node(this.labelForJenkinsNode) {
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
}