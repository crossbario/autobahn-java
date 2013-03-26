Deploying
---------

For Maven/Sonatype deployment docs, see [here](https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide).

 1. To deploy via Maven, make sure Maven's `settings.xml` contains

	    <server>
	      <id>sonatype-nexus-snapshots</id>
	      <username>oberstet</username>
	      <password>mypassword</password>
	    </server>
	
    in the `servers` section.

 2. Check the effective settings using `mvn help:effective-settings`

 3. Clean build using `mvn clean install`

 4. Clean deploy using `mvn clean deploy`





