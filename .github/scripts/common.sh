#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${ENV_FILE:-$DEPLOY_DIR/.env}"
if [ ! -f "$ENV_FILE" ]; then
  echo "Не найден $ENV_FILE (скопируйте .github/scripts/env.example в .github/scripts/.env и заполните)" >&2
  exit 1
fi
set -a
. "$ENV_FILE"
set +a

WORKER_HTTPS_PORT=$((WORKER_PORTBASE + 81))
WORKER_ADMIN_PORT=$((WORKER_PORTBASE + 48))
HR_HTTPS_PORT=$((HR_PORTBASE + 81))
HR_ADMIN_PORT=$((HR_PORTBASE + 48))

WORKER_API_URL="https://$PUBLIC_HOST:$WORKER_HTTPS_PORT/api/v1"
HR_API_URL="https://$PUBLIC_HOST:$HR_HTTPS_PORT/hr"
