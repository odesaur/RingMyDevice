# RMD Server

Self-hosted backend for [RingMyDevice Android](https://github.com/odesaur/RingMyDevice), written in Go.
Register from the RMD app, store location history, and trigger commands (ring, lock, camera) from the web portal or app.

## Running RMD Server

### One-click local run (recommended for testing)

```bash
./rmd-server.sh
```

This builds the Docker image, starts HTTP on port 8080, and stores the SQLite DB in `./rmddata/db`.
Open the portal at `http://localhost:8080` (or `http://<your-LAN-IP>:8080` from your phone).
Use the RMD app to register/login with that URL.

### Manual Docker run

```bash
docker build -t rmd-server .
docker run --rm -p 8080:8080 -v "$(pwd)/rmddata/db/:/var/lib/rmd-server/db/" rmd-server
```

### Optional TLS
- Generate a self-signed cert: `cd certs && ./cert_gen.sh <LAN_IP_or_hostname>`
- Then run HTTPS (after certs exist):
  ```bash
  docker run --rm \
    -p 8443:8443 \
    -v "$(pwd)/rmddata/db:/var/lib/rmd-server/db" \
    -v "$(pwd)/certs/server.crt:/etc/rmd-server/server.crt:ro" \
    -v "$(pwd)/certs/server.key:/etc/rmd-server/server.key:ro" \
    -e RMD_PORTSECURE=8443 -e RMD_PORTINSECURE=-1 \
    -e RMD_SERVERCRT=/etc/rmd-server/server.crt -e RMD_SERVERKEY=/etc/rmd-server/server.key \
    rmd-server
  ```
  Then open `https://<LAN_IP_or_hostname>:8443` and accept the cert. Use HTTPS on non-localhost if you want WebCrypto in browsers.

## Paths (defaults)
Config: `./config.yml`  
DB: `./rmddata/db/` (or `./db/` if you prefer)  
Web assets: embedded (override with `--web-dir`)

## Self-hosting with Docker

> ⚠️ RMD Server is pre-1.0. Pin a version and review release notes before upgrading.

The following is an (incomplete) example `docker-compose.yml` for deploying RMD Server with Docker Compose.

```yml
services:
    rmd:
        build: .
        image: rmd-server:latest
        container_name: rmd
        ports:
         - 127.0.0.1:8080:8080
        volumes:
            - './rmddata/db/:/var/lib/rmd-server/db/'
        restart: unless-stopped
```

*Persisting storage:*
RMD has a database and needs to persist it across container restarts.
You need to mount a Docker volume to the directory `/var/lib/rmd-server/db/` (inside the container).
**It must be readable and writable by uid 1000** (ideally it is owned by uid 1000).

*Networking:*
RMD Server listens for HTTP connections on port 8080.
This example has a port mapping from "127.0.0.1:8080" (on the host) to port 8080 (inside the container).
You need to set up your own reverse proxy.
The reverse proxy should terminate TLS and forward connections to the RMD container.
Instead of the port binding you can also use Docker networks (e.g. to connect your proxy container to the RMD container).

Run with `docker compose up --build --detach`.

## Container hardening

It is recommended to harden your Docker containers as decribed by [OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html).
This means:

- Run a [read-only container](https://blog.ploetzli.ch/2025/docker-best-practices-read-only-containers/).
  - The only path that RMD Server writes to is the database directory, which should be mounted as a volume.
- Drop all capabilities.
- Disallow acquiring new privileges.

On the Docker CLI, pass:

```sh
docker run --read-only --cap-drop=all --security-opt=no-new-privileges # ... rest of command
```

In Docker Compose, set:

```yml
services:
    rmd:
        # other lines omitted
        read_only: true
        cap_drop: [ALL]
        security_opt: [no-new-privileges]
```

## Reverse Proxy

### With Caddy

Use the following Caddyfile:

```
rmd.example.com {
	reverse_proxy localhost:8080
}
```

Caddy will automatically obtain a certificate from Let's Encrypt for you.

### With nginx

See the [example nginx config](nginx-example.conf).

When uploading pictures you might see HTTP 413 errors in your proxy logs ("Content Too Large").
To fix this increase the maximum body size, e.g to 20 MB:

```
client_max_body_size 20m;
```

### Hosting in a subdirectory

The RMD Server binary (whether run in Docker or not) assumes that request paths start at the root ("/").
That is, it assumes that you host RMD Server on a (sub-)domain, e.g., `https://rmd.example.com`.

If you host RMD Server in a subdirectory, e.g., `https://example.com/rmd/`, you need to configure
your proxy to strip the subdirectory before forwarding the request to the backend.
RMD Server does not know how to resolve `/rmd/api/`, it only knows about `/api/`.

### Without Reverse Proxy

> ⚠️ This setup is not recommended and provided for your convenience only.

If you don't want to use a reverse proxy, RMD Server can terminate TLS for you.
However, you need to manage (and regularly renew!) the certificates.

1. Get a TLS certificate for your domain.
1. Set the `ServerCrt` and `ServerKey` in the config file (see below).
1. Mount the certificate and the private key into the container:

```yml
# other lines omitted
volumes:
    - ./server.crt:/etc/rmd-server/server.crt:ro
    - ./server.key:/etc/rmd-server/server.key:ro
```

## License

Forked and Inspired from FMD Server
RMD Server is published under [GPLv3-or-later](LICENSE).
