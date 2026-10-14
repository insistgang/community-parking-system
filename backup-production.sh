#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"
umask 077
mkdir -p backups
stamp="$(date +%Y%m%d-%H%M%S)"
output="backups/parking_produce-${stamp}.sql"

docker compose --env-file .env.production -f docker-compose.production.yml \
  exec -T mysql sh -c 'exec mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers parking_produce' \
  > "$output"

echo "MySQL backup: $output"
