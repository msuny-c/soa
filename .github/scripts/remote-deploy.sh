#!/usr/bin/env bash
. "$(dirname "${BASH_SOURCE[0]}")/payara.sh"

WARS_DIR="$(cd "$DEPLOY_DIR/../wars" && pwd)"

write_password_file

if ! admin "$WORKER_ADMIN_PORT" list-jdbc-resources 2>/dev/null | grep -qx jdbc/workersDS; then
  echo "Домен $WORKER_DOMAIN не настроен или не запущен: нет jdbc/workersDS. Запустите деплой с настройкой доменов (--setup)." >&2
  exit 1
fi

echo "==> Деплой worker-service и site в домен $WORKER_DOMAIN"
admin "$WORKER_ADMIN_PORT" deploy --force=true --name worker-service --contextroot /api "$WARS_DIR/worker-service.war"
admin "$WORKER_ADMIN_PORT" deploy --force=true --name site --contextroot / "$WARS_DIR/site.war"

echo "==> Деплой hr-service в домен $HR_DOMAIN"
admin "$HR_ADMIN_PORT" deploy --force=true --name hr-service --contextroot /hr "$WARS_DIR/hr-service.war"

echo "==> Готово"
