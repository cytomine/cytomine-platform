#!/bin/sh

IP=$(getent hosts $PROXY_HOSTNAME | awk '{print $1}')

if [ -z $IP ]; then
  echo "Cannot resolve 'nginx' (no ip found), skipping /etc/hosts configuration."
  exit
else
  if [ ! -z $URL_CORE ]; then
    sed -i "/$URL_CORE/d" /local_etc/hosts
    entry="$IP $URL_CORE"
    echo $entry >> /local_etc/hosts
    echo "Added '$entry' to /etc/hosts"
  fi

  if [ ! -z $URL_IMS1 ]; then
    sed -i "/$URL_IMS1/d" /local_etc/hosts
    entry="$IP $URL_IMS1"
    echo $entry >> /local_etc/hosts
    echo "Added '$entry' to /etc/hosts"
  fi

  if [ ! -z $URL_IMS2 ]; then
    sed -i "/$URL_IMS2/d" /local_etc/hosts
    entry="$IP $URL_IMS2"
    echo $entry >> /local_etc/hosts
    echo "Added '$entry' to /etc/hosts"
  fi

  if [ ! -z $URL_UPLOAD ]; then
    sed -i "/$URL_UPLOAD/d" /local_etc/hosts
    entry="$IP $URL_UPLOAD"
    echo $entry >> /local_etc/hosts
    echo "Added '$entry' to /etc/hosts"
  fi
fi
