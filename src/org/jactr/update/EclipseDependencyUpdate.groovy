package org.jactr.update;

/**
 * Updates a dependency in a {@code META-INF/MANIFEST.MF} file.
 */
public class EclipseDependencyUpdate extends AbstractDependencyUpdate {

    public EclipseDependencyUpdate(String gitRepoName,
                            String gitRepoURL,
                            String gitFileCredentialsId) {
        super(gitRepoName, gitRepoURL, gitFileCredentialsId, "META-INF/MANIFEST.MF")
    }

    /**
     * This method executes a shell script that expects the {@code PATH_TO_SETTINGS_XML}
     * environment variable to be set.
     */
    public void updateDependency(script,
                                 dependencyToUpdateForMaven, newVersionForMaven,
                                 dependencyToUpdateForEclipse, newVersionForEclipse) {
        script.sh '''sed \
                     --in-place \
                     --regexp-extended \
                       's/'''+dependencyToUpdateForEclipse+''';bundle-version="[^"]*"'''
                     +'''/'''+dependencyToUpdateForEclipse+''';bundle-version="'''+newVersionForEclipse+'''"/g' \
                     META-INF/MANIFEST.MF'''
    }

}