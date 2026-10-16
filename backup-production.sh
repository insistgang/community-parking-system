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
if [ -d application-documents ]; then
  document_output="backups/application_documents-${stamp}.tar.gz"
  document_tmp="${document_output}.part"
  docker compose --env-file .env.production -f docker-compose.production.yml \
    exec -T backend tar -czf - -C /app application-documents > "$document_tmp"
  test -s "$document_tmp"
  mv "$document_tmp" "$document_output"
  echo "Application documents backup: $document_output"
fi
