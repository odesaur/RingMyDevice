# RMD Server

This is the official server for [RingMyDevice Android](https://github.com/odesaur/RingMyDevice)
written in Go.

The RMD app can register an account on RMD Server.
The app can then upload its location at regular intervals.
You can also push commands to the RMD app on your device from RMD Server,
e.g. to make your device ring.

## Running RMD Server

At its core, RMD is just a binary that you can run directly.
If you are experienced and have settled on your own way to deploy applications,
feel free to stick to that.

```bash
go run main.go serve
# or
go build
./rmd-server serve
```

Alternatively, or if you are new to hosting applications,
we recommend running RMD Server with Docker.

Quickly try RMD Server on your laptop from the command line:

```bash
docker build -t rmd-server .
docker run --rm -p 8080:8080 -v "$(pwd)/rmddata/db/:/var/lib/rmd-server/db/" rmd-server
```

You can now visit RMD Server's web interface in your browser at http://localhost:8080.
You can register your RMD app using the server URL `http://<your-laptops-ip>:8080`.

Note that these steps are only for quick on-laptop testing and NOT for production!

⚠️ In particular, the web interface will only work over HTTP on localhost.
On all other origins **the web interface only works over HTTPS**.
(This is a requirement of the WebCrypto API.
RMD Server's API (and hence the app) always works over HTTP - but this is highly discouraged in production.)

## Paths

RMD Server uses the following paths:

|                                    | Default location | Recommended location         |
|------------------------------------|------------------|------------------------------|
| Config file                        | `./config.yml`   | `/etc/rmd-server/config.yml` |
| Directory with the SQLite database | `./db/`          | `/var/lib/rmd-server/db/`    |
| Directory with web static files    | `""` (embedded)  | `/usr/share/rmd-server/web/` |

These can be configured via CLI flags.
The directories can also be configured in the config file.

The default location is the current working directory, because it is expected to be writable by the current user.

When installing RMD Server as an admin, use the recommended locations for a more Unix-like setup.
However, this requires root privileges to create and chown the required locations (hence it is not the default).

The Dockerfile uses the recommended locations, so mount your volumes there (as shown below).

### Config file and packaging

When `/etc/rmd-server/config.yml` is present and used, RMD Server also reads in `/etc/rmd-server/local.yml`.

This is similar to how fail2ban uses jail.conf and jail.local:
it allows packagers to use config.yml and allows admins put their settings in local.yml.
Thus admins don't have to edit the packager's config.yml (which would
cause conflicts if a package update changes the config.yml).

Values in local.yml override their counterpart in config.yml.

## Self-hosting with Docker

> ⚠️ RMD Server is still pre-1.0. Minor versions can introduce breaking changes.
> It is recommended to pin a version and review release notes in this repository before upgrading.

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

Build the image locally from this repository until published images are available.

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

## Configuring RMD Server

### Via config file

The [`config.example.yml`](config.example.yml) contains the available options to configure RMD Server.
Copy this file to `config.yml` and edit it to your liking.

By default, RMD Server will look for the `config.yml` at `/etc/rmd-server/config.yml`
and in the current working directory.
You can pass a custom location with `--config`.

With Docker you can mount it with `-v ./config.yml:/etc/rmd-server/config.yml:ro` (for CLI)
or for Compose:

```yml
# other lines omitted
volumes:
    - ./config.yml:/etc/rmd-server/config.yml:ro
```

NOTE: `yml` not `yaml`!

### Via environment variables

All values that can be set in the config file can also be set via environment variables.
Simply set `RMD_CONFIGFIELDNAME`, e.g. `RMD_PORTINSECURE`.

```yml
services:
  rmd:
    environment:
      RMD_PORTINSECURE: 8888
    # other lines omitted
```

### Via CLI flags

Some values can also be set via CLI flags.
See `rmd-server serve --help` for details.

### Precedence

RMD Server uses [Viper](https://github.com/spf13/viper), which has the following precedence rules
(from highest to lowest):

CLI flag > env var > config file value > default value

## Web static files

The static files for the website are included in the Go binary using [`go:embed`](https://pkg.go.dev/embed).
This is the recommended way to use RMD Server.

If you want to manually provide the `web/` directory (for example, for custom styling), you can provide a custom path with the `--web-dir` option.
This disables the embedded static files and instead reads all static files from the provided path.

## Other ways to install

- [AUR package](https://aur.archlinux.org/packages/findmydeviceserver), maintained by @Chris__

## Logs

Logs are written to stderr and to syslog.

To view the messages in syslog:

```sh
journalctl -t rmd-server
less /var/log/syslog | grep rmd-server
```

## Metrics

RMD Server exposes metrics that can be scraped by [Prometheus](https://prometheus.io/).
There is also a [Grafana template](grafana-template.json).

By default, metrics are exposed on `[::1]:9100/metrics`.
Using localhost is intentional, for security reasons.

Note that the metrics address/port is independent of the main server address/port.
RMD Server can serve both independently of each other, including on separate addresses and ports.

You can change the metrics endpoint to a different address and port in the `config.yml`.
For example, when running in a container you want to listen on a specific IP address
or on all interfaces *inside* the container.

## Donate

<script src="https://liberapay.com/RMD/widgets/button.js"></script>
<noscript><a href="https://liberapay.com/RMD/donate"><img alt="Donate using Liberapay" src="https://liberapay.com/assets/widgets/donate.svg"></a></noscript>

<a href='https://ko-fi.com/H2H35JLOY' target='_blank'><img height='36' style='border:0px;height:36px;' src='https://cdn.ko-fi.com/cdn/kofi4.png?v=2' border='0' alt='Buy Me a Coffee at ko-fi.com' /></a>

## Funding

<div style="display: inline-flex; align-items: center;">
    <a href="https://nlnet.nl/" target="_blank">
        <img src="https://nlnet.nl/logo/banner.svg" alt="nlnet" height="50">
    </a>
    <a href="https://nlnet.nl/taler" target="_blank">
        <img src="https://nlnet.nl/image/logos/NGI_Mobifree_tag.svg" alt="NextGenerationInternet" height="50">
    </a>
</div>

This project was funded through the NGI Mobifree Fund.
For more details, visit our [project page](https://nlnet.nl/project/RMD/)

## License

RMD Server is published under [GPLv3-or-later](LICENSE).
