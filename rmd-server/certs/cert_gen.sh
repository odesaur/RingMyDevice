#!/usr/bin/env bash
set -euo pipefail

# Generate a self-signed TLS certificate for rmd-server, then print the HTTPS URL to use.
# Usage:
#   ./cert_gen.sh 172.16.125.243
#   ./cert_gen.sh my-lan-hostname.local
#
# Outputs:
#   server.crt (public cert)
#   server.key (private key, PEM, unencrypted)
#   https://<CN>:8443 (suggested URL)
#
# Notes:
# - CN should match the LAN IP/hostname you’ll use from phone/desktop.
# - For HTTPS with docker run, mount these to /etc/rmd-server/server.crt and /etc/rmd-server/server.key
#   and run the container with RMD_PORTSECURE=8443, RMD_PORTINSECURE=-1.

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <LAN_IP_or_hostname>"
  exit 1
fi

CN="$1"
OUT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

openssl req -x509 -newkey rsa:2048 -nodes \
  -keyout "${OUT_DIR}/server.key" \
  -out "${OUT_DIR}/server.crt" \
  -days 365 \
  -subj "/CN=${CN}"

echo "Generated certs:"
echo "  ${OUT_DIR}/server.crt"
echo "  ${OUT_DIR}/server.key"
echo "Use CN=${CN}"
echo ""
echo "Next steps (HTTPS run):"
echo "  docker run --rm \\"
echo "    -p 8443:8443 \\"
echo "    -v \"$(cd \"${OUT_DIR}\" && pwd)/server.crt:/etc/rmd-server/server.crt:ro\" \\"
echo "    -v \"$(cd \"${OUT_DIR}\" && pwd)/server.key:/etc/rmd-server/server.key:ro\" \\"
echo "    -v \"$(cd \"${OUT_DIR}\"/.. && pwd)/rmddata/db:/var/lib/rmd-server/db\" \\"
echo "    -e RMD_PORTSECURE=8443 -e RMD_PORTINSECURE=-1 \\"
echo "    -e RMD_SERVERCRT=/etc/rmd-server/server.crt -e RMD_SERVERKEY=/etc/rmd-server/server.key \\"
echo "    rmd-server"
echo ""
echo "Portal URL:"
echo "  https://${CN}:8443"
