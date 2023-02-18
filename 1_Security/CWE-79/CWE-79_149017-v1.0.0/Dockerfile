FROM openjdk:7
LABEL maintainer="SAMATE, NIST"
WORKDIR /sard
RUN apt-get update && apt-get install -y --no-install-recommends curl make unzip
ENV ANT_HOME="/usr/local/apache-ant-1.7.1"
RUN curl "https://archive.apache.org/dist/ant/binaries/apache-ant-1.7.1-bin.tar.gz" | tar -xz -C /usr/local && \
    ln -s ${ANT_HOME}/bin/ant /usr/bin/ant
COPY . .
