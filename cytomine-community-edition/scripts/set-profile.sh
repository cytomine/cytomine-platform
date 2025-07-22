#!/bin/bash

# Check if -h or --help flag is provided
for arg in "$@"; do
  if [[ "$arg" == "-h" || "$arg" == "--help" ]]; then
    echo "Usage: $0 [service1 service2 ...]"
    echo "Configure development environment by setting services in dev mode."
    echo "To disable a service, use '~service-name'"
    echo "Service short names: 'ae', 'core', 'iam', 'ims', 'ui'"
    exit 0
  fi
done

# Define array of valid service short names
valid_services=('ae' 'core' 'iam' 'ims' 'ui')

# Function to check if a service is valid
is_valid_service() {
  local service=$1
  for valid_service in "${valid_services[@]}"; do
    if [ "$valid_service" = "$service" ]; then
      return 0
    fi
  done
  return 1
}

# Check if no arguments provided
if [ $# -eq 0 ]; then
  echo "All services in production mode. .dev.env file is empty."
  exit 0
fi

DEV_ENV_FILE="$(pwd)/.dev.env"

# Create or truncate .dev.env file
> $DEV_ENV_FILE

services_in_dev_mode=0

# Process each argument
for arg in "$@"; do
  # Check if service is to be disabled
  if [[ "$arg" == ~* ]]; then
    service="${arg:1}"  # Remove the tilde (~) prefix
    # Check if service is valid
    if ! is_valid_service "$service"; then
      echo "Invalid service: '$service'"
      exit 2
    fi
    # Write to .dev.env file
    echo "${service}=disabled" >> ${DEV_ENV_FILE}
  else
    # Check if service is valid
    if ! is_valid_service "$arg"; then
      echo "Invalid service: $arg"
      exit 2
    fi
    # Write to .dev.env file
    echo "${arg}=dev" >> ${DEV_ENV_FILE}
    services_in_dev_mode=1
  fi
done
