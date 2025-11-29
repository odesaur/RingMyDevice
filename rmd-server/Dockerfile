FROM golang:1.24-bookworm AS builder

WORKDIR /go/src/rmd-server
ENV GOPATH=/go

# pre-download and only redownload in subsequent builds if they change
COPY go.mod go.sum ./
RUN go mod download && go mod verify

COPY go.mod .
COPY go.sum .
COPY main.go .
COPY cmd/ cmd/
COPY backend/ backend/
COPY config/ config/
COPY metrics/ metrics/
COPY migrations/ migrations/
COPY user/ user/
COPY utils/ utils/
COPY version/ version/
COPY web/ web/

RUN go build -o /tmp/rmd main.go


FROM debian:bookworm-slim

RUN apt update && \
    apt install --no-install-recommends -y ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# Create user
RUN useradd --no-create-home --uid 1000 rmd-server

# Copy files and configure permissions
# Note that the directories must be executable (for file listing, etc.)

ARG BIN_FILE=/opt/rmd-server
ARG DB_DIR=/var/lib/rmd-server/db

COPY --from=builder /tmp/rmd "$BIN_FILE"

RUN chown rmd-server:rmd-server "$BIN_FILE" && \
    chmod 0550 "$BIN_FILE"

RUN mkdir -p "$DB_DIR"

RUN chown -R rmd-server:rmd-server "$DB_DIR" && \
    chmod -R 0660 "$DB_DIR" && \
    chmod 0770 "$DB_DIR"

# Change to user
USER rmd-server

EXPOSE 8080/tcp
EXPOSE 8443/tcp

# XXX: Using $BIN_FILE doesn't work
ENTRYPOINT ["/opt/rmd-server", "serve", "--db-dir", "/var/lib/rmd-server/db"]
