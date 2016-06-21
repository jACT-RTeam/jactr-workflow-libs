# jACT-R workflow libs

Common classes and functions to be used in Jenkins pipelines.

To install the workflow libs in Jenkins slaves, a script like the one at
https://github.com/cloudbees/jenkins-scripts/blob/master/pipeline-global-lib-init.groovy
might be used as init script for Jenkins slaves.

The workflow libs provide two classes `Config` and `Build` which can be used to trigger
a standardized build for commonreality, jactr and jactr-eclipse in the following way (using
the example of commonreality):

	import org.jactr.Config
	import org.jactr.Build
	
	def config = new Config('http://monochromata.de/maven/releases/org.commonreality/org/commonreality/core/maven-metadata.xml',
						'https://github.com/monochromata/commonreality.git')
	new Build().run(config)
