FROM ubuntu:16.04
LABEL maintainer="SAMATE, NIST"
WORKDIR /opt
ARG GRADLE_VERSION=2.11
ARG JAVA_VERSION=8
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends \
  libncurses5 libstdc++6 lib32stdc++6 lib32z1 zlib1g curl make unzip python pkg-config autoconf2.64 g++ git patch bison meson openjdk-${JAVA_VERSION}-jdk && \
    curl -LO https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip && \
    unzip gradle-${GRADLE_VERSION}-bin.zip && \
    rm gradle-${GRADLE_VERSION}-bin.zip
ENV ANDROID_HOME=/opt/android/sdk
ENV JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64
RUN mkdir -p ${ANDROID_HOME} && \
    curl -LO https://dl.google.com/android/repository/commandlinetools-linux-6858069_latest.zip && \
    unzip commandlinetools*.zip -d ${ANDROID_HOME} && \
    rm commandlinetools*.zip
ENV PATH=${ANDROID_HOME}/emulator:${ANDROID_HOME}/tools:${ANDROID_HOME}/tools/bin:${ANDROID_HOME}/cmdline-tools/bin:${ANDROID_HOME}/platform-tools:${PATH}
RUN mkdir ~/.android && echo '### User Sources for Android SDK Manager' > ~/.android/repositories.cfg
RUN yes | sdkmanager --sdk_root=$ANDROID_HOME --licenses
RUN sdkmanager --sdk_root=$ANDROID_HOME --install "platforms;android-25"
RUN sdkmanager --sdk_root=$ANDROID_HOME --install "build-tools;25.0.2"
ENV NDK_VERSION=18.1.5063045
RUN sdkmanager --sdk_root=$ANDROID_HOME --install "ndk;${NDK_VERSION}"
ENV ANDROID_SDK=${ANDROID_HOME}
ENV ANDROID_NDK=${ANDROID_HOME}/ndk/${NDK_VERSION}
RUN apt-get update && apt-get install -y --no-install-recommends curl make unzip
WORKDIR /sard
COPY . .
