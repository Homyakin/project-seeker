#!/bin/bash
set -e

mvn package spring-boot:repackage && \
docker build -t project-seeker . && \
docker stop project-seeker 2>/dev/null; \
docker rm project-seeker 2>/dev/null; \
docker run -d \
  --name project-seeker \
  --network host \
  --env-file .env \
  --restart always \
  project-seeker