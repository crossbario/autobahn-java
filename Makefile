BUILD_DATE=$(shell date -u +"%Y-%m-%d")
AUTOBAHN_JAVA_VERSION='17.10.5'
AUTOBAHN_JAVA_VCS_REF='unknown'

#export AUTOBAHN_TESTSUITE_VCS_REF=`git --git-dir="../autobahn-testsuite/.git" rev-list -n 1 v${AUTOBAHN_TESTSUITE_VERSION} --abbrev-commit`

#
# Demos
#
crossbar:
	docker run \
		--rm -it -p 8080:8080 --name crossbar \
		crossbario/crossbar

python:
	docker run \
		-it --rm --link crossbar \
		-v ${shell pwd}/demo-gallery/python:/test  \
		crossbario/autobahn-python \
		python -u /test/test_component2.py

java:
	docker run \
		-it --rm --link crossbar \
		-v ${shell pwd}:/workspace \
		crossbario/autobahn-java:netty \
			/bin/bash -c "gradle installDist -PbuildPlatform=netty && DEMO_GALLERY_OPTS="-DlogLevel=INFO" demo-gallery/build/install/demo-gallery/bin/demo-gallery ws://crossbar:8080/ws"

#
# Build
#
build_autobahn:
	docker run -it --rm \
		-v ${shell pwd}:/workspace \
		crossbario/autobahn-java:netty \
		gradle -PbuildPlatform=netty distZip

#
# Toolchain
#
build_toolchain:
	docker build \
		--build-arg BUILD_DATE=${BUILD_DATE} \
		--build-arg AUTOBAHN_JAVA_VCS_REF=${AUTOBAHN_JAVA_VCS_REF} \
		--build-arg AUTOBAHN_JAVA_VERSION=${AUTOBAHN_JAVA_VERSION} \
		-t crossbario/autobahn-java \
		-t crossbario/autobahn-java:netty \
		-t crossbario/autobahn-java:netty-${AUTOBAHN_JAVA_VERSION} \
		-f ./docker/Dockerfile.netty .

publish_toolchain:
	docker push crossbario/autobahn-java
	docker push crossbario/autobahn-java:netty
	docker push crossbario/autobahn-java:netty-${AUTOBAHN_JAVA_VERSION}

#
# Publish the libraries.
#

publish_android:
	sed -i 's/DEBUG = true/DEBUG = false/g' autobahn/src/main/java/io/crossbar/autobahn/utils/Globals.java
	AUTOBAHN_BUILD_VERSION=${AUTOBAHN_JAVA_VERSION} gradle bintrayUpload -PbuildPlatform=android
	sed -i 's/DEBUG = false/DEBUG = true/g' autobahn/src/main/java/io/crossbar/autobahn/utils/Globals.java

#publish_android_legacy:
#	sed -i 's/DEBUG = true/DEBUG = false/g' autobahn/src/main/java/io/crossbar/autobahn/utils/Globals.java
#	AUTOBAHN_BUILD_VERSION=${AUTOBAHN_JAVA_VERSION} gradle bintrayUpload -PbuildPlatform=android -PbuildLegacy=true
#	sed -i 's/DEBUG = false/DEBUG = true/g' autobahn/src/main/java/io/crossbar/autobahn/utils/Globals.java

publish_netty:
	sed -i 's/DEBUG = true/DEBUG = false/g' autobahn/src/main/java/io/crossbar/autobahn/utils/Globals.java
	AUTOBAHN_BUILD_VERSION=${AUTOBAHN_JAVA_VERSION} gradle bintrayUpload -PbuildPlatform=netty
	sed -i 's/DEBUG = false/DEBUG = true/g' autobahn/src/main/java/io/crossbar/autobahn/utils/Globals.java

generate_changelog:
	./changelog_gen.sh

check_toolchain:
	docker run -it --rm crossbario/autobahn-java:netty /bin/bash -c "ls -la /autobahn && du -hs /autobahn"

list:
	-docker images crossbario/autobahn-java:*

clean:
	sudo rm -rf ./.gradle/ ./build ./autobahn/build ./demo-gallery/build/
	./removeall.sh
