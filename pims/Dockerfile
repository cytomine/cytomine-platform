# All args are listed here at the top for readability
ARG GUNICORN_VERSION=22.0.0
ARG OPENJPEG_URL=https://github.com/uclouvain/openjpeg/archive
ARG OPENJPEG_VERSION=2.5.2
ARG PIMS_REVISION
ARG PIMS_VERSION
ARG PLUGIN_CSV=scripts/plugin-list.csv
ARG PY_VERSION=3.10
ARG SETUPTOOLS_VERSION=80.9.0
ARG UBUNTU_VERSION=22.04
ARG VIPS_URL=https://github.com/libvips/libvips/releases/download
ARG VIPS_VERSION=8.15.2

#######################################################################################
## Stage: Pims
FROM python:${PY_VERSION}-bookworm AS base-pims

ARG GUNICORN_VERSION
ARG OPENJPEG_URL
ARG OPENJPEG_VERSION
ARG PIMS_REVISION
ARG PIMS_VERSION
ARG PLUGIN_CSV
ARG SETUPTOOLS_VERSION
ARG UBUNTU_VERSION
ARG VIPS_URL
ARG VIPS_VERSION

ENV DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8 \
    MODULE_NAME="pims.application" \
    PLUGIN_INSTALL_PATH=/app/plugins \
    PORT=5000 \
    PYTHONPATH="/app:$PYTHONPATH"

RUN apt-get update && \
    apt-get -y install --no-install-recommends --no-install-suggests \
        libexif-dev \
        libgsf-1-dev \
        libimage-exiftool-perl \
        liblcms2-dev \
        libopenjp2-7-dev \
        libopenslide-dev \
        liborc-dev \
        libpng-dev \
        libtiff-dev \
        libvips-dev \
        libvips42 \
        libwebp-dev

# Download plugins
WORKDIR /app
COPY ./docker/plugins.py /app/plugins.py
COPY ${PLUGIN_CSV} /app/plugins.csv
COPY ./plugins/ /app/plugins/

RUN python plugins.py \
        --plugin_csv /app/plugins.csv \
        --checkerResolution_file_path /app/checkerResolution.csv \
        --method checker_resolution_file

# Run before_python() from plugins prerequisites
RUN python plugins.py \
        --plugin_csv /app/plugins.csv \
        --install_path ${PLUGIN_INSTALL_PATH} \
        --method dependencies_before_python

# Cleaning. Cannot be done before as plugin prerequisites could use apt-get.
RUN rm -rf /var/lib/apt/lists/*

# Install python requirements
COPY ./requirements.txt /app/requirements.txt
RUN pip install --upgrade pip && \
    pip install --no-cache-dir gunicorn==${GUNICORN_VERSION} && \
    pip install --no-cache-dir setuptools==${SETUPTOOLS_VERSION} && \
    pip install --no-cache-dir -r requirements.txt && \
    python plugins.py \
        --plugin_csv /app/plugins.csv \
        --install_path ${PLUGIN_INSTALL_PATH} \
        --method install && \
    rm -f /app/plugins.csv

# Add default config
COPY ./pims-config.env /app/pims-config.env
COPY ./logging-prod.yml /app/logging.yml
COPY ./docker/gunicorn_conf.py /app/gunicorn_conf.py

COPY ./docker/start.sh /start.sh
RUN chmod +x /start.sh

COPY ./docker/start-reload.sh /start-reload.sh
RUN chmod +x /start-reload.sh

COPY ./pims /app/pims

LABEL org.opencontainers.image.authors='uliege@cytomine.org' \
      org.opencontainers.image.url='https://uliege.cytomine.org/' \
      org.opencontainers.image.documentation='https://doc.uliege.cytomine.org/' \
      org.opencontainers.image.source='https://github.com/cytomine/Cytomine-pims' \
      org.opencontainers.image.vendor='Cytomine ULiege' \
      org.opencontainers.image.deps.gunicorn.version=${GUNICORN_VERSION} \
      org.opencontainers.image.deps.openjpeg.url=${OPENJPEG_URL} \
      org.opencontainers.image.deps.openjpeg.version=${OPENJPEG_VERSION} \
      org.opencontainers.image.deps.setuptools.version=${SETUPTOOLS_VERSION} \
      org.opencontainers.image.deps.ubuntu.version=${UBUNTU_VERSION} \
      org.opencontainers.image.deps.vips.url=${VIPS_URL} \
      org.opencontainers.image.deps.vips.version=${VIPS_VERSION} \
      org.opencontainers.image.plugin.csv=${PLUGIN_CSV} \
      org.opencontainers.image.revision=${PIMS_REVISION} \
      org.opencontainers.image.version=${PIMS_VERSION}

EXPOSE ${PORT}

#######################################################################################
## Stage: Pims (dev server)
FROM base-pims AS dev-server

RUN apt-get -y update && \
    apt-get -y install --no-install-recommends --no-install-suggests openssh-server rsync

#######################################################################################
## Stage: production
FROM base-pims AS production

CMD ["/start.sh"]
