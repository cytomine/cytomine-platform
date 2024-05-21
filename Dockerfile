# List ARGs here for better readability.
ARG ENTRYPOINT_SCRIPTS_VERSION=1.4.0
ARG KEYCLOAK_VERSION=24.0.2
ARG UBI_VERSION=9.3-1610
ARG IMAGE_VERSION
ARG IMAGE_REVISION

#######################################################################################
## Stage: IAM build
FROM quay.io/keycloak/keycloak:${KEYCLOAK_VERSION} as builder

# Enable health and metrics support
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

# Configure a database vendor
ENV KC_DB=postgres

WORKDIR /opt/keycloak

# Add the provider JAR file to the providers directory
# ADD --chown=keycloak:keycloak <MY_PROVIDER_JAR_URL> /opt/keycloak/providers/myprovider.jar

RUN /opt/keycloak/bin/kc.sh build

#######################################################################################
## Stage: entrypoint script. Use a multi-stage because COPY --from cannot interpolate variables
FROM cytomine/entrypoint-scripts:${ENTRYPOINT_SCRIPTS_VERSION} as entrypoint-scripts

#######################################################################################
## Stage: Cytomine IAM dev external tools build
FROM registry.access.redhat.com/ubi9:${UBI_VERSION} AS dev-tools-builder
RUN mkdir -p /mnt/rootfs
RUN dnf install --installroot /mnt/rootfs findutils gettext openssh-server rsync --releasever 9 --setopt install_weak_deps=false --nodocs -y && \
    dnf --installroot /mnt/rootfs clean all && \
    rpm --root /mnt/rootfs -e --nodeps setup

# startup scripts
RUN mkdir /docker-entrypoint-cytomine.d/
COPY --from=entrypoint-scripts --chmod=774 /envsubst-on-templates-and-move.sh /docker-entrypoint-cytomine.d/500-envsubst-on-templates-and-move.sh
#COPY --from=entrypoint-scripts --chmod=774 /setup-ssh-dev-env.sh /docker-entrypoint-cytomine.d/1-start-ssh-dev-env.sh

#######################################################################################
## Stage: Cytomine IAM development image
FROM quay.io/keycloak/keycloak:${KEYCLOAK_VERSION} AS dev-server

COPY --from=builder /opt/keycloak/ /opt/keycloak/
COPY --from=dev-tools-builder /mnt/rootfs /
COPY --from=dev-tools-builder /docker-entrypoint-cytomine.d/ /docker-entrypoint-cytomine.d/
COPY --from=entrypoint-scripts --chmod=774 /cytomine-entrypoint.sh /usr/local/bin/

ENV KC_DB=postgres
ENTRYPOINT ["cytomine-entrypoint.sh", "/opt/keycloak/bin/kc.sh"]
CMD ["start-dev"]

#######################################################################################
## Stage: Cytomine IAM external tools build
FROM registry.access.redhat.com/ubi9:${UBI_VERSION} AS prod-tools-builder
RUN mkdir -p /mnt/rootfs
RUN dnf install --installroot /mnt/rootfs findutils gettext --releasever 9 --setopt install_weak_deps=false --nodocs -y && \
    dnf --installroot /mnt/rootfs clean all && \
    rpm --root /mnt/rootfs -e --nodeps setup

# startup scripts
RUN mkdir /docker-entrypoint-cytomine.d/
COPY --from=entrypoint-scripts --chmod=774 /envsubst-on-templates-and-move.sh /docker-entrypoint-cytomine.d/500-envsubst-on-templates-and-move.sh

#######################################################################################
## Stage: Cytomine IAM image
FROM quay.io/keycloak/keycloak:${KEYCLOAK_VERSION} AS prodcution

ARG ENTRYPOINT_SCRIPTS_VERSION
ARG KEYCLOAK_VERSION
ARG IMAGE_VERSION
ARG IMAGE_REVISION

LABEL org.opencontainers.image.authors="dev@cytomine.com" \
      org.opencontainers.image.url="https://www.cytomine.com/" \
      org.opencontainers.image.documentation="https://doc.cytomine.org/" \
      org.opencontainers.image.source="https://github.com/cytomine/Cytomine-IAM" \
      org.opencontainers.image.vendor="Cytomine Corporation SA" \
      org.opencontainers.image.version="${IMAGE_VERSION}" \
      org.opencontainers.image.revision="${IMAGE_REVISION}" \
      org.opencontainers.image.deps.keycloak.version="${KEYCLOAK_VERSION}" \
      org.opencontainers.image.deps.ubi.version="${UBI_VERSION}" \
      org.opencontainers.image.deps.entrypoint.scripts.version="${ENTRYPOINT_SCRIPTS_VERSION}"

COPY --from=builder /opt/keycloak/ /opt/keycloak/
COPY --from=prod-tools-builder /mnt/rootfs /
COPY --from=prod-tools-builder /docker-entrypoint-cytomine.d/ /docker-entrypoint-cytomine.d/
COPY --from=entrypoint-scripts --chmod=774 /cytomine-entrypoint.sh /usr/local/bin/

RUN mkdir /opt/keycloak/data/import
COPY configs/kc_config.json /opt/keycloak/data/import

ENV KC_DB=postgres

ENTRYPOINT ["cytomine-entrypoint.sh", "/opt/keycloak/bin/kc.sh"]
CMD ["start-dev" , "--http-port=8100" , "--import-realm" , "--hostname=iam:8100"  , "--hostname-strict-backchannel=true"]