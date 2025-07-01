#!/bin/bash

# This scripts reads the cytomine.template file in the parent directory,
# and output an array of components and versions in markdown format.
# Inspired from https://www.baeldung.com/linux/yq-utility-processing-yaml

declare -A content

CYTOMINE_COMMERCIAL=$(cat ../cytomine.template | yq '.global.versions.constant.CYTOMINE_COMMERCIAL')

printf "New release for \`%s\`.\n" "${CYTOMINE_COMMERCIAL}"
printf "\n"
printf "This release is composed of the following micro-services:\n"

while IFS="=" read -r key value; do content["$key"]=$value; done < <(
  yq '.global.images.constant | to_entries | map([.key, .value] | join("=")) | .[]' ../cytomine.template
)

printf "| Component | Version |\n"
printf "| --------- | ------- |\n"

for key in "${!content[@]}"; do printf "| \`%s\` | \`%s\` |\n" "$key" "${content[$key]}"; done