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

BROWSER_UNSAFE_PORTS=" 1719 1720 1723 2049 3659 4045 4190 5060 5061 6000 6566 6665 6666 6667 6668 6669 6697 10080 "
for port in "$WORKER_HTTPS_PORT" "$HR_HTTPS_PORT"; do
  if [[ "$BROWSER_UNSAFE_PORTS" == *" $port "* ]]; then
    echo "HTTPS-порт $port блокируется браузерами (ERR_UNSAFE_PORT), выберите другой WORKER_PORTBASE или HR_PORTBASE" >&2
    exit 1
  fi
done

WORKER_API_URL="https://$PUBLIC_HOST:$WORKER_HTTPS_PORT/api/v1"
HR_API_URL="https://$PUBLIC_HOST:$HR_HTTPS_PORT/hr"
