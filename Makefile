clean:
	sudo rm -rf ./.gradle/ ./build ./autobahn/build ./demo-gallery/build/

build_netty:
	time docker build \
		-t crossbario/autobahn-java:netty \
		-f ./docker/Dockerfile.netty .
