#!/usr/bin/env bash
. "$(dirname "${BASH_SOURCE[0]}")/payara.sh"

WARS_DIR="$(cd "$DEPLOY_DIR/../wars" && pwd)"

write_password_file

echo "==> Деплой worker-service и site в домен $WORKER_DOMAIN"
admin "$WORKER_ADMIN_PORT" deploy --force=true --name worker-service --contextroot /api "$WARS_DIR/worker-service.war"
admin "$WORKER_ADMIN_PORT" deploy --force=true --name site --contextroot / "$WARS_DIR/site.war"

echo "==> Деплой hr-service в домен $HR_DOMAIN"
admin "$HR_ADMIN_PORT" deploy --force=true --name hr-service --contextroot /hr "$WARS_DIR/hr-service.war"

echo "==> Готово"
