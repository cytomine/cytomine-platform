FROM python:3.9-slim-bullseye AS base

RUN apt-get update \
    && apt-get install -y --no-install-recommends git \
    && rm -rf /var/lib/apt/lists/*

COPY cbir /app/cbir
COPY pyproject.toml poetry.lock README.md /app
COPY weights/resnet /app/weights/resnet

WORKDIR /app

RUN pip install --no-cache-dir --upgrade --extra-index-url https://download.pytorch.org/whl/cu118 .


FROM base AS dev-server

RUN apt-get -y update \
    && apt-get -y install --no-install-recommends --no-install-suggests openssh-server rsync


FROM base AS production

ARG CBIR_VERSION
ARG CBIR_REVISION

LABEL org.opencontainers.image.authors="uliege@cytomine.org" \
      org.opencontainers.image.url="https://uliege.cytomine.org/" \
      org.opencontainers.image.documentation="https://doc.uliege.cytomine.org/" \
      org.opencontainers.image.source="https://github.com/Cytomine-ULiege/Cytomine-cbir" \
      org.opencontainers.image.vendor="Cytomine ULiege" \
      org.opencontainers.image.version="${CBIR_VERSION}" \
      org.opencontainers.image.revision="${CBIR_REVISION}"

CMD ["uvicorn", "cbir.app:app", "--host", "0.0.0.0", "--port", "6000"]
