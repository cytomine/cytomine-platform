# List ARGs here for better readability.
ARG CORE_VERSION
ARG CORE_REVISION
ARG GRADLE_VERSION=7.6-jdk17
ARG OPENJDK_VERSION=17-slim-bullseye
ARG PROXY_CACHE=registry.cytomine.org/docker

#######################################################################################
# Stage: core dependencies download via gradle
FROM ${PROXY_CACHE}/gradle:${GRADLE_VERSION}-alpine AS deps-downloader

# We first copy the build.gradle file and the binaries stored in the source repository.
# This way, we retrieve all gradle dependencies at the beginning. All these steps will be
# cached by Docker unless build.gradle. This means that we only retrieve all dependencies
# if we modify the dependencies definition.
RUN mkdir -p /opt/gradle/.gradle
ENV GRADLE_USER_HOME=/opt/gradle/.gradle

WORKDIR /app
COPY ./build.gradle /app/build.gradle

RUN gradle clean build --no-daemon --console=verbose

#######################################################################################
## Stage: building the core jar file
FROM ${PROXY_CACHE}/gradle:${GRADLE_VERSION}-alpine AS jar-builder

ENV GRADLE_USER_HOME=/opt/gradle/.gradle
COPY --from=deps-downloader /opt/gradle/.gradle /opt/gradle/.gradle

WORKDIR /app
COPY ./src /app/src
COPY ./build.gradle /app/build.gradle

ARG CORE_VERSION
ENV CORE_VERSION=$CORE_VERSION

RUN sed -i -- 's/version: 0.0.0/version: '$CORE_VERSION'/g' /app/src/main/resources/application.yml && \
    gradle bootJar --console=verbose

#######################################################################################
## Stage: Cytomine core development image
FROM ${PROXY_CACHE}/gradle:${GRADLE_VERSION}-jammy AS dev-server

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
      gettext=0.21-4ubuntu4 \
      openssh-server=1:8.9p1-3ubuntu0.13 \
      rsync=3.2.7-0ubuntu0.22.04.4

ENV GRADLE_USER_HOME=/opt/gradle/.gradle

WORKDIR /app

CMD ["/bin/sh"]

#######################################################################################
## Stage: Cytomine core image
FROM ${PROXY_CACHE}/openjdk:${OPENJDK_VERSION} AS production

ARG CORE_VERSION
ARG CORE_REVISION
ARG GRADLE_VERSION
ARG OPENJDK_VERSION

LABEL org.opencontainers.image.authors="dev@cytomine.com" \
      org.opencontainers.image.url="https://uliege.cytomine.org/" \
      org.opencontainers.image.documentation="https://doc.uliege.cytomine.org/" \
      org.opencontainers.image.source="https://github.com/cytomine/Cytomine-core" \
      org.opencontainers.image.vendor="Cytomine ULiège" \
      org.opencontainers.image.version="${CORE_VERSION}" \
      org.opencontainers.image.revision="${CORE_REVISION}" \
      org.opencontainers.image.deps.openjdk.version="${OPENJDK_VERSION}" \
      org.opencontainers.image.deps.gradle.version="${GRADLE_VERSION}"

ENV DEBIAN_FRONTEND=noninteractive
ENV LANG=C.UTF-8

# base librairies and configuration
RUN apt-get update -y \
    && apt-get install --no-install-recommends --no-install-suggests -y \
      logrotate=3.18* \
      gettext=0.21* \
    && rm -rf /var/lib/apt/lists/* \
    && sed -i "/su root syslog/c\su root root" /etc/logrotate.conf

COPY --from=jar-builder /app/build/libs/cytomine.jar /app/cytomine.jar

WORKDIR /app

CMD ["java", "-jar", "/app/cytomine.jar"]
