#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GAME_DIR="$(cd "$SCRIPT_DIR/../game" && pwd)"

cd "$GAME_DIR"
mvn package spring-boot:repackage
docker build -t project-seeker .

cd "$SCRIPT_DIR"

docker stop project-seeker 2>/dev/null || true
docker rm project-seeker 2>/dev/null || true

docker run -d \
  --name project-seeker \
  --network host \
  --env-file .env \
  --restart always \
  project-seeker