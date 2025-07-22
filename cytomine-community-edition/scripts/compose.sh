#!/bin/bash

# Define paths
ROOT_PATH="$(pwd)"
REPO_PATH="../"
DEV_PATH="./docker"

# Check if .dev.env file exists
if [ ! -f .dev.env ]; then
  echo "Error: .dev.env file not found."
  exit 1
fi

echo "Running installer..."
docker run -v ${ROOT_PATH}:/install --user "$(id -u):$(id -g)" --rm -it cytomine/installer:latest deploy -s /install
installer_exit_code=$?

# Check installer return code
if [ $installer_exit_code -ne 0 ]; then
  echo "Error: Installer execution failed."
  exit $installer_exit_code
fi

# Extract services from .dev.env file
valid_services=('ae' 'core' 'iam' 'ims' 'ui')

# Build profiles and docker compose override files for dev services
active_profiles=""
active_dev_overrides=()
feature_flags=()

for service in "${valid_services[@]}"; do
  if [ ! -z "$service" ]; then
    if grep -q "${service}=disabled" .dev.env; then
      # Do not add the service to profiles if it is disabled
      feature_flags+=("FF_${service^^}=false")
      continue
    fi

    active_profiles+="dev-${service},"
    feature_flags+=("FF_${service^^}=true")

    if grep -q "${service}=dev" .dev.env; then
      dev_override_file="${DEV_PATH}/docker-compose.override.${service}.yaml"
      if [ -f "$dev_override_file" ]; then
        active_dev_overrides+=(-f)
        active_dev_overrides+=("$dev_override_file")
      fi
    fi
  fi
done

active_profiles+="dev"

# Remove trailing comma from active_profiles
active_profiles=$(echo "$active_profiles" | sed 's/,$//')

echo "Running compose command '$@' with profiles: ${active_profiles}"

# define env variables for compose
export ROOT_PATH=${ROOT_PATH}
export REPO_PATH=${REPO_PATH}
export DEV_PATH=${DEV_PATH}
export COMPOSE_PROFILES=${active_profiles}
export COMPOSE_PROJECT_NAME="dev"
for var in "${feature_flags[@]}"; do
  export "$var"
done

# Start Docker Compose configuration
docker compose \
  -f ${ROOT_PATH}/docker-compose.yml \
  -f ${ROOT_PATH}/docker-compose.override.yml \
  -f ${DEV_PATH}/docker-compose.override.main.yaml \
  ${active_dev_overrides[@]} \
  --env-file ${ROOT_PATH}/.env \
  "$@"

exit $?