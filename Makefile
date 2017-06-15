clean:
	sudo rm -rf ./.gradle/ ./build ./autobahn/build ./demo-gallery/build/

build_netty:
	time docker build \
		-t crossbario/autobahn-java:netty \
		-f ./docker/Dockerfile.netty .

demo_wamp_netty:
	docker run -v $(shell pwd):/workspace \
		-it --rm crossbario/autobahn-java:netty /bin/bash -c \
                "gradle installDist -PbuildPlatform=netty && \
		demo-gallery/build/install/demo-gallery/bin/demo-gallery \
                ws://$(shell docker inspect --format '{{ .NetworkSettings.IPAddress }}' crossbar):8080/ws"
