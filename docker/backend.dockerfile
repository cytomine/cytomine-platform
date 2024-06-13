# All args are listed here at the top for readability
ARG ENTRYPOINT_SCRIPTS_VERSION=1.4.0
ARG GUNICORN_VERSION=22.0.0
ARG OPENJPEG_URL=https://github.com/uclouvain/openjpeg/archive
ARG OPENJPEG_VERSION=2.5.2
ARG PIMS_REVISION
ARG PIMS_VERSION
ARG PLUGIN_CSV=scripts/plugin-list.csv
ARG PY_VERSION=3.8
ARG SETUPTOOLS_VERSION=59.6.0
ARG UBUNTU_VERSION=20.04
ARG VIPS_URL=https://github.com/libvips/libvips/releases/download
ARG VIPS_VERSION=8.15.2

#######################################################################################
## Stage: entrypoint script. Use a multi-stage because COPY --from cannot interpolate variables
FROM cytomine/entrypoint-scripts:${ENTRYPOINT_SCRIPTS_VERSION} as entrypoint-scripts

#######################################################################################
## Stage: Pims
FROM ubuntu:${UBUNTU_VERSION} as base-pims

ENV LANG C.UTF-8
ENV DEBIAN_FRONTEND noninteractive

ARG PY_VERSION=3.8

RUN apt-get -y update && apt-get -y install --no-install-recommends --no-install-suggests \
        `# Essentials` \
        automake \
        bc \
        build-essential \
        ca-certificates \
        cmake \
        git \
        gcc \
        net-tools \
        python${PY_VERSION} \
        python${PY_VERSION}-dev \
        python${PY_VERSION}-distutils \
        ninja-build \
        wget \
        software-properties-common \
        `# Vips dependencies` \
        pkg-config \
        glib2.0-dev \
        libexpat-dev \
        libtiff-dev \
        libjpeg-turbo8-dev \
        libgsf-1-dev \
        libexif-dev \
        liborc-dev \
        libwebp-dev \
        liblcms2-dev \
        libpng-dev \
        gobject-introspection \
        `# Other tools` \
        libimage-exiftool-perl

RUN cd /usr/bin && \
    ln -s python${PY_VERSION} python

# Official pip install: https://pip.pypa.io/en/stable/installation/#get-pip-py
RUN cd /tmp && \
    wget https://bootstrap.pypa.io/get-pip.py && \
    python get-pip.py && \
    rm -rf get-pip.py

# openjpeg 2.4 is required by vips (J2000 support)
ARG OPENJPEG_VERSION=2.5.2
ARG OPENJPEG_URL=https://github.com/uclouvain/openjpeg/archive
RUN cd /usr/local/src && \
    wget ${OPENJPEG_URL}/v${OPENJPEG_VERSION}/openjpeg-${OPENJPEG_VERSION}.tar.gz && \
    tar -zxvf openjpeg-${OPENJPEG_VERSION}.tar.gz && \
    rm -rf openjpeg-${OPENJPEG_VERSION}.tar.gz && \
    cd openjpeg-${OPENJPEG_VERSION} && \
    mkdir build && cd build && \
    cmake -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX=/usr -DBUILD_STATIC_LIBS=ON .. && \
    make && \
    make install && \
    make clean && \
    ldconfig

# Download plugins
ARG PLUGIN_CSV=scripts/plugin-list.csv
WORKDIR /app
COPY ./docker/plugins.py /app/plugins.py
COPY ${PLUGIN_CSV} /app/plugins.csv

# ="enabled,name,git_url,git_branch\n"
ENV PLUGIN_INSTALL_PATH /app/plugins
RUN python plugins.py \
   --plugin_csv /app/plugins.csv \
   --install_path ${PLUGIN_INSTALL_PATH} \
   --method download

RUN python plugins.py \
   --plugin_csv /app/plugins.csv \
   --checkerResolution_file_path /app/checkerResolution.csv \
   --method checker_resolution_file

# Run before_vips() from plugins prerequisites
RUN python plugins.py \
   --plugin_csv /app/plugins.csv \
   --install_path ${PLUGIN_INSTALL_PATH} \
   --method dependencies_before_vips

# vips
ARG VIPS_VERSION=8.15.2
ARG VIPS_URL=https://github.com/libvips/libvips/releases/download
RUN pip install --no-cache-dir meson
RUN cd /usr/local/src && \
    wget ${VIPS_URL}/v${VIPS_VERSION}/vips-${VIPS_VERSION}.tar.xz && \
    tar -xvf vips-${VIPS_VERSION}.tar.xz && \
    rm -rf vips-${VIPS_VERSION}.tar.xz && \
    cd vips-${VIPS_VERSION} && \
    meson build --libdir lib -Dintrospection=disabled --buildtype release && \
    cd build && \
    ninja && \
    ninja install && \
    ldconfig

# Run before_python() from plugins prerequisites
RUN python plugins.py \
   --plugin_csv /app/plugins.csv \
   --install_path ${PLUGIN_INSTALL_PATH} \
   --method dependencies_before_python


# Cleaning. Cannot be done before as plugin prerequisites could use apt-get.
RUN rm -rf /var/lib/apt/lists/*

# Install python requirements
ARG GUNICORN_VERSION=22.0.0
ARG SETUPTOOLS_VERSION=59.6.0
COPY ./requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir gunicorn==${GUNICORN_VERSION} && \
    pip install --no-cache-dir -r requirements.txt && \
    pip install --no-cache-dir setuptools==${SETUPTOOLS_VERSION} && \
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

ENV PYTHONPATH="/app:$PYTHONPATH"

# entrypoint scripts
RUN mkdir /docker-entrypoint-cytomine.d/
COPY --from=entrypoint-scripts --chmod=774 /cytomine-entrypoint.sh /usr/local/bin/
COPY --from=entrypoint-scripts --chmod=774 /envsubst-on-templates-and-move.sh /docker-entrypoint-cytomine.d/500-envsubst-on-templates-and-move.sh

ARG ENTRYPOINT_SCRIPTS_VERSION=1.4.0
ARG GUNICORN_VERSION=22.0.0
ARG OPENJPEG_URL=https://github.com/uclouvain/openjpeg/archive
ARG OPENJPEG_VERSION=2.5.2
ARG PIMS_PACKAGE_REVISION
ARG PIMS_PACKAGE_VERSION
ARG PIMS_VERSION
ARG PLUGIN_CSV=scripts/plugin-list.csv
ARG PY_VERSION=3.8
ARG SETUPTOOLS_VERSION=59.6.0
ARG UBUNTU_VERSION=20.04
ARG VIPS_URL=https://github.com/libvips/libvips/releases/download
ARG VIPS_VERSION=8.15.2

LABEL org.opencontainers.image.authors='support@cytomine.com' \
      org.opencontainers.image.url='https://www.cytomine.org/' \
      org.opencontainers.image.documentation='https://doc.cytomine.org/' \
      org.opencontainers.image.source='https://github.com/cytomine/pims' \
      org.opencontainers.image.vendor='Cytomine Corporation SA' \
      org.opencontainers.image.deps.entrypoint.scripts.version=${ENTRYPOINT_SCRIPTS_VERSION} \
      org.opencontainers.image.deps.gunicorn.version=${GUNICORN_VERSION} \
      org.opencontainers.image.deps.openjpeg.url=${OPENJPEG_URL} \
      org.opencontainers.image.deps.openjpeg.version=${OPENJPEG_VERSION} \
      org.opencontainers.image.deps.pims.version=${PIMS_VERSION} \
      org.opencontainers.image.version=${PIMS_PACKAGE_VERSION} \
      org.opencontainers.image.revision=${PIMS_PACKAGE_REVISION} \
      org.opencontainers.image.plugin.csv=${PLUGIN_CSV} \
      org.opencontainers.image.deps.setuptools.version=${SETUPTOOLS_VERSION} \
      org.opencontainers.image.deps.ubuntu.version=${UBUNTU_VERSION} \
      org.opencontainers.image.deps.vips.url=${VIPS_URL} \
      org.opencontainers.image.deps.vips.version=${VIPS_VERSION}

# Add app
COPY ./pims /app/pims
ENV MODULE_NAME="pims.application"
ENV PYTHONPATH="/app:$PYTHONPATH"

ENV PORT=5000
EXPOSE ${PORT}

#######################################################################################
## Stage: Pims (dev server)
FROM base-pims AS dev-server

RUN apt-get -y update && \
     apt-get -y install --no-install-recommends --no-install-suggests openssh-server rsync

# startup scripts
COPY --from=entrypoint-scripts --chmod=774 /setup-ssh-dev-env.sh /docker-entrypoint-cytomine.d/1-start-ssh-dev-env.sh

ENTRYPOINT ["cytomine-entrypoint.sh"]

#######################################################################################
## Stage: Pims (dev server)
FROM base-pims AS prod-server

RUN echo "Building production..."

ENTRYPOINT ["cytomine-entrypoint.sh"]
CMD ["/start.sh"]