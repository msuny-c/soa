#!/usr/bin/env bash
. "$(dirname "${BASH_SOURCE[0]}")/common.sh"

ROOT="$(cd "$DEPLOY_DIR/../.." && pwd)"
SSH=(ssh -p "$HELIOS_SSH_PORT")
SCP=(scp -P "$HELIOS_SSH_PORT")

echo "==> Сборка (VITE_WORKER_API=$WORKER_API_URL, VITE_HR_API=$HR_API_URL)"
(cd "$ROOT" && VITE_WORKER_API="$WORKER_API_URL" VITE_HR_API="$HR_API_URL" mvn -B package)

echo "==> Копирую артефакты на $HELIOS_SSH:~/$HELIOS_DIR"
"${SSH[@]}" "$HELIOS_SSH" "mkdir -p ~/$HELIOS_DIR/wars ~/$HELIOS_DIR/scripts"
"${SCP[@]}" \
  "$ROOT/services/worker-service/target/worker-service.war" \
  "$ROOT/services/hr-service/target/hr-service.war" \
  "$ROOT/services/site/target/site.war" \
  "$HELIOS_SSH:~/$HELIOS_DIR/wars/"
"${SCP[@]}" \
  "$DEPLOY_DIR/common.sh" "$DEPLOY_DIR/payara.sh" \
  "$DEPLOY_DIR/setup-domains.sh" "$DEPLOY_DIR/remote-deploy.sh" \
  "$ENV_FILE" \
  "$HELIOS_SSH:~/$HELIOS_DIR/scripts/"

if [ "${1:-}" = "--setup" ]; then
  "${SSH[@]}" -t "$HELIOS_SSH" "bash ~/$HELIOS_DIR/scripts/setup-domains.sh"
fi
"${SSH[@]}" -t "$HELIOS_SSH" "bash ~/$HELIOS_DIR/scripts/remote-deploy.sh"
