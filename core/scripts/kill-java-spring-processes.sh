#!/bin/bash

# Find Java processes and kill them
pids=$(ps aux | grep spring | grep -v 'grep' | grep -v 'kill-java-spring-processes'| awk '{print $2}')

if [ -z "$pids" ]; then
  echo "$0 No Java Spring processes found."
else
  for pid in $pids; do
    kill "$pid"
    echo "$0 Killed Java Spring process with PID: $pid"
  done
fi