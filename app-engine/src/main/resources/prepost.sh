#!/bin/bash

# Function to fetch and unzip file
fetch_and_unzip() {

  local url="$1"
  local run_id="$2"
  local input="$3"

  #echo "\n fetch and unzip $input $url $run_id\n"

  # Fetch the zip file using curl
  local http_code=$(curl -o temp.zip -L -w "%{http_code}" -X GET "$url")
  echo "HTTP Status Code: $http_code"
  # Check if the HTTP status code is not 200
  if [ "$http_code" -ne 200 ]; then
    echo "Error: failed to download inputs."
    exit 1
  fi
  # Create directories for task
  mkdir -p "/tmp/appengine/$run_id/inputs"
  mkdir -p "/tmp/appengine/$run_id/outputs"


  # Unzip the file to the target directory
  unzip -q temp.zip -d "/tmp/appengine/$run_id/inputs"

  # Remove the temporary zip file
  rm temp.zip
}

# Function to zip and post content
zip_and_post() {

  local url="$1"
  local run_id="$2"
  local input="$3"
  local output_folder="/tmp/appengine/$run_id/outputs"

  echo "-- zip and post --"
  echo "content of output folder"
  ls -l ${output_folder}

  # Zip all content in the target directory
  current_folder=$(pwd)
  cd ${output_folder}
  sudo zip -r ${current_folder}/temp.zip *
  cd ${current_folder}

  # Post the zip file using curl
  local http_code=$(curl --location -L -w "%{http_code}" "$url" --form 'outputs=@"temp.zip"')
  # Extract the last 3 characters of the HTTP status code
  http_code_last_3="${http_code: -3}"
  echo "HTTP Status Code: $http_code_last_3"
  # Check if the HTTP status code is not 200
  if [ "$http_code_last_3" -ne 200 ]; then
    echo "Error: failed to upload outputs."
    exit 1
  fi

  # Remove the temporary zip file
  rm temp.zip
  rm -R "/tmp/appengine/$run_id"
}

input="$1"
url="$2"
run_id="$3"
echo "arguements : $input $url $run_id"
if [ "$input" = "true" ] ; then
  fetch_and_unzip "$url" "$run_id"
else
  zip_and_post "$url" "$run_id"
fi

# script url run input_flag