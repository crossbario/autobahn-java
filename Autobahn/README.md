# Building

Clean build

	mvn clean install


# Deploying

For Maven/Sonatype deployment docs, see [here](https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide).

 1. To deploy via Maven, make sure Maven's `settings.xml` contains

	    <server>
	      <id>sonatype-nexus-snapshots</id>
	      <username>oberstet</username>
	      <password>mypassword</password>
	    </server>
	    <server>
	      <id>sonatype-nexus-staging</id>
	      <username>oberstet</username>
	      <password>mypassword</password>
	    </server>
	
    in the `servers` section.

 2. Check the effective settings using `mvn help:effective-settings`

## Deploying Snapshots

Clean build a snapshot using

	mvn clean deploy

Snapshots should then appear [here](https://oss.sonatype.org/content/repositories/snapshots/de/tavendo/autobahn-android/).

## Deploying Releases

Prepare the release

	mvn release:clean
	mvn release:prepare
	
Publish the release

	mvn release:perform




