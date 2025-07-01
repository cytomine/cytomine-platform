#!/bin/sh

echo >&2 "$0: Starting with default config interpolation ..."

/docker-entrypoint-cytomine.d/500-envsubst-on-templates-and-move.sh /cm_configs_default

echo >&2 "$0: Default configuration applied."

# executing next entrypoint
exec "$@"