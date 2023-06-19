ARG IMAGE_VERSION
ARG IMAGE_REVISION
ARG PYTHON_VERSION=3.12

FROM python:${PYTHON_VERSION}-rc-slim-bullseye

RUN apt-get upgrade \
    && apt-get install openssl \
    && apt-get clean

RUN python -m pip install --upgrade pip

WORKDIR /app
COPY setup.cfg /app/setup.cfg
COPY setup.py /app/setup.py
COPY README.md /app/README.md
COPY requirements.txt /app/requirements.txt
COPY cytomine_installer /app/cytomine_installer
RUN pip install -e .

ARG IMAGE_VERSION
ARG IMAGE_REVISION
ARG PYTHON_VERSION
LABEL org.opencontainers.image.authors='support@cytomine.com' \
      org.opencontainers.image.url='https://www.cytomine.org/' \
      org.opencontainers.image.documentation='https://doc.cytomine.org/' \
      org.opencontainers.image.source='https://github.com/cytomine/Cytomine-installer' \
      org.opencontainers.image.vendor='Cytomine Corporation SA' \
      org.opencontainers.image.version=${IMAGE_VERSION} \
      org.opencontainers.image.revision=${IMAGE_REVISION} \
      org.opencontainers.image.deps.python.version=${PYTHON_VERSION}

ENTRYPOINT ["ctmn"]
