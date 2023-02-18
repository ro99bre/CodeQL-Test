FROM openjdk:7
LABEL maintainer="SAMATE, NIST"
WORKDIR /sard
RUN apt-get update && apt-get install -y --no-install-recommends curl make unzip ant
COPY . .
