#!/usr/bin/env bash

# Script to build and bundle RMD Server.
# To have a reproducible, stable Go version, this script runs the actual bundle.sh script
# in a Debian/stable container

set -eu

REF=${1-}

if [[ -z "$REF" ]]; then
    echo "Error: missing git ref to build" >&2
    echo "Usage: $0 <git-ref>" >&2
    exit 1
fi

# This is expected to be executed from the root of the RMD Server git repository
docker build --pull --tag rmd-builder --file ./scripts/Dockerfile_build .
docker run --rm --workdir /build --volume .:/build rmd-builder ./scripts/bundle.sh "$REF"
