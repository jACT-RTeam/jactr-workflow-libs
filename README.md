# jACT-R workflow libs

Common classes and functions to be used in Jenkins pipelines.

## System requirements

This workflow lib shall be used with version 2.3 or higher of the [versions-maven-plugin](http://www.mojohaus.org/versions-maven-plugin). That version supports the `use-dep-version` goal used by the workflow libs.

## Installation

To install the workflow libs in Jenkins slaves, a script like the one at
https://github.com/cloudbees/jenkins-scripts/blob/master/pipeline-global-lib-init.groovy
might be used as init script for Jenkins slaves.

## Usage

The workflow libs provide two classes `Config` and `Build` which can be used to trigger
a standardized build for commonreality, jactr and jactr-eclipse in the following way (using
the example of commonreality):

	import org.jactr.Config
	import org.jactr.Build
	
	def config = new Config('http://monochromata.de/maven/releases/org.commonreality/org/commonreality/core/maven-metadata.xml',
						'https://github.com/monochromata/commonreality.git')
	new Build().run(config)
