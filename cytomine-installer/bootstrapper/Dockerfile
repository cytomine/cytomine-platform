FROM python:3.10-rc-alpine

RUN apk upgrade --update-cache --available && \
    apk add openssl && \
    rm -rf /var/cache/apk/*

RUN python -m pip install --upgrade pip
RUN pip install pyyaml -y

WORKDIR /app
ADD configurator.py /app/configurator.py
