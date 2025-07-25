#!/bin/bash

# Extract services from .dev.env file
valid_services=('ae' 'cbir' 'core' 'iam' 'ims' 'ui')

# Function to print colored symbols
print_symbol() {
  if [ "$1" = "enabled" ]; then
    echo -e "\e[32m\u2713\e[0m"  # Green checkmark
  elif [ "$1" = "disabled" ]; then
    echo -e "\e[31m\u2717\e[0m"  # Red cross
  else
    echo "Unknown"
  fi
}

# Print header
echo "Service Summary:"

# Print service state
for service in "${valid_services[@]}"; do
  if grep -q "${service}=dev" .dev.env; then
    state="$(print_symbol "enabled") (dev)"
  elif grep -q "${service}=disabled" .dev.env; then
    state=$(print_symbol "disabled")
  else
    state=$(print_symbol "enabled")
  fi

  # Space-padding for consistent length
  printf " - %-6s: %s\n" "$service" "$state"
done
