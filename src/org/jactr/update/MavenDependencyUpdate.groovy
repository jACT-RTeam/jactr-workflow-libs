package org.jactr.update;

/**
 * Updates a dependency in a Maven POM file.
 */
public class MavenDependencyUpdate extends AbstractDependencyUpdate {

    /**
     * The path to the Maven POM file to update. The path is relative to the root of the Git repository.
     */
    public final String pomPath
    
    public MavenDependencyUpdate(String gitRepoName,
                            String gitRepoURL,
                            String gitFileCredentialsId,
                            String pomPath) {
        super(gitRepoName, gitRepoURL, gitFileCredentialsId, pomPath)
        this.pomPath = pomPath
    }
    
    /**
     * This method executes a shell script that expects the {@code PATH_TO_SETTINGS_XML}
     * environment variable to be set.
     */
    public void updateDependency(script,
                                 dependencyToUpdateForMaven, newVersionForMaven,
                                 dependencyToUpdateForEclipse, newVersionForEclipse) {
        def tmpDir=script.pwd tmp: true
        script.sh '''mvn \
                 --errors \
                 --settings $PATH_TO_SETTINGS_XML \
                 --file '''+tmpDir+'''/'''+gitRepoName+'''/'''+pomPath+''' \
                 -Dincludes='''+dependencyToUpdateForMaven+''' \
                 -DdepVersion='''+newVersionForMaven+''' \
                 versions:use-dep-version'''
    }

}