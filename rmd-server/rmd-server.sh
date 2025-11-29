#!/usr/bin/env bash

# Build and run rmd-server for local self-hosting.
# - HTTP on port 8080 (no TLS, so Web UI works best on localhost).
# - Data persisted in ./rmddata/db on the host.
# - Rebuilds the Docker image to include the latest web/app assets.
#
# Registration options:
# 1) From the Android app: set server URL to http://<your-LAN-IP>:8080, then register/login in-app.
# 2) From the web UI:
#    - On the same machine: http://localhost:8080 (WebCrypto is allowed on localhost).
#    - From another device over HTTP, the browser may block WebCrypto; use the app instead or enable an insecure-origin exception in your browser if needed.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

DB_DIR="${SCRIPT_DIR}/rmddata/db"
mkdir -p "${DB_DIR}"

IP_ADDR=$(hostname -I 2>/dev/null | awk '{print $1}')
if [[ -z "${IP_ADDR}" ]]; then
  IP_ADDR="127.0.0.1"
fi

echo "Building rmd-server image..."
docker build -t rmd-server "${SCRIPT_DIR}"

echo "Launching rmd-server..."
echo "HTTP: http://${IP_ADDR}:8080"
echo "DB  : ${DB_DIR}"

docker run --rm \
  -p 8080:8080 \
  -v "${DB_DIR}:/var/lib/rmd-server/db" \
  -e RMD_DATABASEDIR=/var/lib/rmd-server/db \
  -e RMD_PORTINSECURE=8080 \
  -e RMD_PORTSECURE=-1 \
  rmd-server
