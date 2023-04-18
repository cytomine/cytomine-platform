#!/bin/bash

set -o xtrace
set -o errexit

srcRoot=$(git rev-parse --show-toplevel)
cd $srcRoot

# get version number from git
gitLongTag=$(git describe --long)
# get the branch name from first arg or from git
branchName=${1:-$(git rev-parse --abbrev-ref HEAD)}
echo $gitLongTag
# check if tag is an official release (v1.2.3) + no other commit behind (or dirty)
if [[ $gitLongTag =~ v[0-9]+.[0-9]+.[0-9]+-beta.[0-9]+-EE-0-[0-9a-g]{8,9}$ ]]; then
  echo "BETA EE"
  versionNumber=$(echo $gitLongTag | sed -r "s/v([0-9]+\.[0-9]+\.[0-9]+-beta.[0-9]+)-EE-[0-9]+-.+/\1/")
  tag=betaEE
elif [[ $gitLongTag =~ v[0-9]+.[0-9]+.[0-9]+-EE-0-[0-9a-g]{8,9}$ ]]; then
  echo "OFFICIAL EE"
  versionNumber=$(echo $gitLongTag | sed -r "s/v([0-9]+\.[0-9]+\.[0-9]+)-EE-[0-9]+-.+/\1/")
  tag=officialEE
elif [[ $gitLongTag =~ v[0-9]+.[0-9]+.[0-9]+-beta.[0-9]+-0-[0-9a-g]{8,9}$ ]]; then
  echo "BETA"
  versionNumber=$(echo $gitLongTag | sed -r "s/v([0-9]+\.[0-9]+\.[0-9]+-beta.[0-9]+)-[0-9]+-.+/\1/")
  tag=beta
elif [[ $gitLongTag =~ v[0-9]+.[0-9]+.[0-9]+-0-[0-9a-g]{8,9}$ ]]; then
  echo "OFFICIAL"
  versionNumber=$(echo $gitLongTag | sed -r "s/v([0-9]+\.[0-9]+\.[0-9]+)-[0-9]+-.+/\1/")
  tag=official
else
  echo "WARNING: invalid tag for an official release $gitLongTag"
  versionNumber=$branchName-$(date "+%Y%m%d%H%M%S")
  tag=none
fi
echo $versionNumber
echo $versionNumber > ./ci/version

echo $tag
echo $tag > ./ci/tag
