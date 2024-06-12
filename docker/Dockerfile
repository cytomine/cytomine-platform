# List ARGs here for better readability.
ARG APP_ENGINE_VERSION
ARG APP_ENGINE_REVISION
ARG ENTRYPOINT_SCRIPTS_VERSION=1.4.0
ARG GRADLE_VERSION=8.5.0-jdk17-alpine
ARG GRADLE_DEV_VERSION=8.5.0-jdk17-jammy
ARG OPENJDK_VERSION=17-slim-bullseye

#######################################################################################
# Stage: dependencies download via gradle
FROM gradle:${GRADLE_VERSION} AS deps-downloader

# We first copy the build.gradle file and the binaries stored in the source repository.
# This way, we retrieve all gradle dependencies at the beginning. All these steps will be
# cached by Docker unless build.gradle. This means that we only retrieve all dependencies
# if we modify the dependencies definition.
RUN mkdir -p /opt/gradle/.gradle
ENV GRADLE_USER_HOME=/opt/gradle/.gradle

WORKDIR /app
COPY ../src /app/src
COPY ../build.gradle /app/build.gradle

RUN gradle clean compileJava --no-daemon --console=verbose

#######################################################################################
## Stage: building the jar file
FROM gradle:${GRADLE_VERSION} AS jar-builder

ENV GRADLE_USER_HOME=/opt/gradle/.gradle
COPY --from=deps-downloader /opt/gradle/.gradle /opt/gradle/.gradle

WORKDIR /app
COPY ../src /app/src
COPY ../build.gradle /app/build.gradle

ARG APP_ENGINE_VERSION
ENV APP_ENGINE_VERSION=$APP_ENGINE_VERSION

RUN sed -i -- "s/version: ce-0.0.0/version: ${APP_ENGINE_VERSION}/g" /app/src/main/resources/application.yml  && \
    sed -i -- "s/version = 'ce-0.0.0-SNAPSHOT'/version = '${APP_ENGINE_VERSION}'/g" /app/build.gradle  && \
    gradle bootJar --console=verbose

#######################################################################################
## Stage: entrypoint script. Use a multi-stage because COPY --from cannot interpolate variables
FROM cytomine/entrypoint-scripts:${ENTRYPOINT_SCRIPTS_VERSION} as entrypoint-scripts

#######################################################################################
## Stage: App-Engine development image
FROM gradle:${GRADLE_DEV_VERSION} AS dev-server

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
      gettext=0.21-4ubuntu4 \
      openssh-server=1:8.9p1-3ubuntu0.7 \
      rsync=3.2.7-0ubuntu0.22.04.2

ENV GRADLE_USER_HOME=/opt/gradle/.gradle

# startup scripts
RUN mkdir /docker-entrypoint-cytomine.d/
COPY --from=entrypoint-scripts --chmod=774 /cytomine-entrypoint.sh /usr/local/bin/
COPY --from=entrypoint-scripts --chmod=774 /envsubst-on-templates-and-move.sh /docker-entrypoint-cytomine.d/500-envsubst-on-templates-and-move.sh
COPY --from=entrypoint-scripts --chmod=774 /setup-ssh-dev-env.sh /docker-entrypoint-cytomine.d/1-start-ssh-dev-env.sh

WORKDIR /app
ENTRYPOINT ["cytomine-entrypoint.sh"]

#######################################################################################
## Stage: Cytomine core image
FROM openjdk:${OPENJDK_VERSION} as production

ARG APP_ENGINE_VERSION
ARG APP_ENGINE_REVISION
ARG ENTRYPOINT_SCRIPTS_VERSION
ARG GRADLE_VERSION
ARG OPENJDK_VERSION

LABEL org.opencontainers.image.authors="dev@cytomine.com" \
      org.opencontainers.image.url="https://www.cytomine.com/" \
      org.opencontainers.image.documentation="https://doc.cytomine.org/" \
      org.opencontainers.image.source="https://github.com/cytomine/Cytomine-App-Engine" \
      org.opencontainers.image.vendor="Cytomine Corporation SA" \
      org.opencontainers.image.version="${APP_ENGINE_VERSION}" \
      org.opencontainers.image.revision="${APP_ENGINE_REVISION}" \
      org.opencontainers.image.deps.openjdk.version="${OPENJDK_VERSION}" \
      org.opencontainers.image.deps.gradle.version="${GRADLE_VERSION}" \
      org.opencontainers.image.deps.entrypoint.scripts.version="${ENTRYPOINT_SCRIPTS_VERSION}"

ENV DEBIAN_FRONTEND noninteractive
ENV LANG C.UTF-8

# base librairies and configuration
COPY --from=jar-builder /app/build/libs/cytomine-app-engine.jar /app/cytomine-app-engine.jar

# entrypoint scripts
RUN mkdir /docker-entrypoint-cytomine.d/
COPY --from=entrypoint-scripts --chmod=774 /cytomine-entrypoint.sh /usr/local/bin/
COPY --from=entrypoint-scripts --chmod=774 /envsubst-on-templates-and-move.sh /docker-entrypoint-cytomine.d/500-envsubst-on-templates-and-move.sh

WORKDIR /app
ENTRYPOINT ["cytomine-entrypoint.sh"]

CMD ["java", "-jar", "/app/cytomine-app-engine.jar"]
