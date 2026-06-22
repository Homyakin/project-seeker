#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/website-backend"
WEBSITE_DIR="$ROOT_DIR/website"

# Env file with DB connection settings (DATABASE_HOST, DATABASE_NAME, etc.)
# and WEBSITE_DOMAIN for Caddy.
ENV_FILE="$SCRIPT_DIR/.env.website"
ENV_ARG=()
if [ -f "$ENV_FILE" ]; then
  ENV_ARG=(--env-file "$ENV_FILE")
  # Pull WEBSITE_DOMAIN out of the env file so Caddy can pick it up below.
  WEBSITE_DOMAIN="$(grep -E '^WEBSITE_DOMAIN=' "$ENV_FILE" | tail -n1 | cut -d= -f2-)"
fi

# HTTPS only: a real domain is required so Caddy can provision a certificate.
if [ -z "$WEBSITE_DOMAIN" ]; then
  echo "WEBSITE_DOMAIN is required (set it in $ENV_FILE) for HTTPS." >&2
  exit 1
fi

# Shared bridge network so Caddy can reach the backend by container name.
docker network create web 2>/dev/null || true

# ---------------------------------------------------------------------------
# 1. Build and run the backend (no published ports, reachable only inside "web")
# ---------------------------------------------------------------------------
cd "$BACKEND_DIR"
mvn package spring-boot:repackage
docker build -t project-seeker-website-backend .

docker stop project-seeker-website-backend 2>/dev/null || true
docker rm project-seeker-website-backend 2>/dev/null || true

docker run -d \
  --name project-seeker-website-backend \
  --network web \
  "${ENV_ARG[@]}" \
  --restart always \
  project-seeker-website-backend

# ---------------------------------------------------------------------------
# 2. Run Caddy: publish 80/443, serve the static frontend, proxy /api to backend
# ---------------------------------------------------------------------------
docker stop project-seeker-caddy 2>/dev/null || true
docker rm project-seeker-caddy 2>/dev/null || true

docker run -d \
  --name project-seeker-caddy \
  --network web \
  -p 80:80 -p 443:443 \
  --restart always \
  -e WEBSITE_DOMAIN="$WEBSITE_DOMAIN" \
  -v "$SCRIPT_DIR/Caddyfile:/etc/caddy/Caddyfile:ro" \
  -v "$WEBSITE_DIR:/srv/website:ro" \
  -v project-seeker-caddy-data:/data \
  -v project-seeker-caddy-config:/config \
  caddy:2-alpine

echo "Website deployed. Frontend served by Caddy over HTTPS on ${WEBSITE_DOMAIN}, API proxied to backend."
