#!/usr/bin/env bash

# Build and run rmd-server for local self-hosting.
# - If certs/server.crt + certs/server.key exist, runs HTTPS on 8443 (insecure HTTP disabled).
# - Otherwise runs HTTP on 8080 (best for localhost testing).
# - Data persisted in ./rmddata/db on the host.
# - Rebuilds the Docker image to include the latest assets.
#
# To generate TLS certs (self-signed) first:
#   cd certs && ./cert_gen.sh <LAN_IP_or_hostname>
# Then re-run this script; it will detect the certs and start HTTPS on 8443.

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

CRT="${SCRIPT_DIR}/certs/server.crt"
KEY="${SCRIPT_DIR}/certs/server.key"

if [[ -f "${CRT}" && -f "${KEY}" ]]; then
  echo "Launching rmd-server with HTTPS (certs found)."
  echo "HTTPS: https://${IP_ADDR}:8443"
  echo "DB   : ${DB_DIR}"
  docker run --rm \
    -p 8443:8443 \
    -v "${DB_DIR}:/var/lib/rmd-server/db" \
    -v "${CRT}:/etc/rmd-server/server.crt:ro" \
    -v "${KEY}:/etc/rmd-server/server.key:ro" \
    -e RMD_DATABASEDIR=/var/lib/rmd-server/db \
    -e RMD_PORTSECURE=8443 \
    -e RMD_PORTINSECURE=-1 \
    -e RMD_SERVERCRT=/etc/rmd-server/server.crt \
    -e RMD_SERVERKEY=/etc/rmd-server/server.key \
    rmd-server
else
  echo "Launching rmd-server over HTTP (no certs found)."
  echo "HTTP: http://${IP_ADDR}:8080"
  echo "DB  : ${DB_DIR}"
  docker run --rm \
    -p 8080:8080 \
    -v "${DB_DIR}:/var/lib/rmd-server/db" \
    -e RMD_DATABASEDIR=/var/lib/rmd-server/db \
    -e RMD_PORTINSECURE=8080 \
    -e RMD_PORTSECURE=-1 \
    rmd-server
fi
