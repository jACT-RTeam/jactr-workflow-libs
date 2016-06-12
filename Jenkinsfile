// This file requires that env.JENKINS_HOME/workflow-libs has been initialized with
// the following command: "git clone https://github.com/jACT-RTeam/jactr-workflow-libs.git ."
node("workflowLibs") {
	stage "Pull"
    sh 'cd '+env.JENKINS_HOME+'/workflow-libs && git pull'
}