#!/bin/bash

BACKUP_DIR="/var/krezar-tavern-backups"
BACKUP_NAME="krezar-tavern-backup-$(date +%Y-%m-%d_%H-%M-%S).sql"

docker exec project-seeker_project-seeker-db_1 pg_dump -U dev seeker > "${BACKUP_DIR}/${BACKUP_NAME}"

# Optional: Delete backups, older than 30 days
find "${BACKUP_DIR}" -type f -name "*.sql" -mtime +30 -exec rm -f {} \;
