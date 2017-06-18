clean:
	sudo rm -rf ./.gradle/ ./build ./autobahn/build ./demo-gallery/build/

build_netty:
	time docker build \
		-t crossbario/autobahn-java:netty \
		-f ./docker/Dockerfile.netty .

demo_wamp_netty:
	docker run -it --rm \
		--link crossbar \
		-v ${shell pwd}:/workspace \
		crossbario/autobahn-java:netty \
			/bin/bash -c "gradle installDist -PbuildPlatform=netty && demo-gallery/build/install/demo-gallery/bin/demo-gallery ws://crossbar:8080/ws"

#                ws://$(shell docker inspect --format '{{ .NetworkSettings.IPAddress }}' crossbar):8080/ws"

run_crossbar:
	docker run \
		--rm -it -p 8080:8080 --name crossbar \
		crossbario/crossbar
