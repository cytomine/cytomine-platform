ARG FROM_NAMESPACE
ARG FROM_VERSION
FROM ${FROM_NAMESPACE}/pims:$FROM_VERSION

ENV C_FORCE_ROOT=1

COPY ./worker-start.sh /worker-start.sh
RUN chmod +x /worker-start.sh

CMD ["bash", "/worker-start.sh"]
