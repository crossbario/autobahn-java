FROM gradle

USER root

ENV DEBIAN_FRONTEND noninteractive
ENV ANDROID_HOME /usr/local/android-sdk-linux
ENV PATH ${ANDROID_HOME}/tools:$ANDROID_HOME/platform-tools:$PATH

WORKDIR /workspace

RUN    apt-get update \
    && apt-get install -y unzip expect \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

RUN    wget https://dl.google.com/android/repository/sdk-tools-linux-3859397.zip -O tools.zip \
    && unzip tools.zip -d $ANDROID_HOME \
    && rm tools.zip

RUN /usr/bin/expect -c \
    "spawn $ANDROID_HOME/tools/bin/sdkmanager \"platforms;android-27\" \"build-tools;27.0.3\"; set timeout -1; expect \"Accept? (y/N): \"; send \"y\r\n\"; expect done;"

COPY ${PWD} /workspace

RUN gradle installDist -PbuildPlatform=android -PbuildVersion=${AUTOBAHN_JAVA_VERSION}

CMD ["gradle", "assemble"]
