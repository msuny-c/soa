#!/usr/bin/env bash
. "$(dirname "${BASH_SOURCE[0]}")/common.sh"

ROOT="$(cd "$DEPLOY_DIR/../.." && pwd)"
SSH=(ssh -p "$HELIOS_SSH_PORT")
SCP=(scp -P "$HELIOS_SSH_PORT")
if [ -n "${SSH_WRAPPER:-}" ]; then
  read -ra WRAPPER <<< "$SSH_WRAPPER"
  SSH=("${WRAPPER[@]}" "${SSH[@]}")
  SCP=("${WRAPPER[@]}" "${SCP[@]}")
fi
REMOTE_DIR="~/$HELIOS_DIR"

echo "==> Сборка (VITE_WORKER_API=$WORKER_API_URL, VITE_HR_API=$HR_API_URL)"
(cd "$ROOT" && VITE_WORKER_API="$WORKER_API_URL" VITE_HR_API="$HR_API_URL" mvn -B clean package)

echo "==> Копирую артефакты на $HELIOS_SSH:$REMOTE_DIR"
"${SSH[@]}" "$HELIOS_SSH" "mkdir -p $REMOTE_DIR/wars $REMOTE_DIR/scripts"
"${SCP[@]}" \
  "$ROOT/services/worker-service/target/worker-service.war" \
  "$ROOT/services/hr-service/target/hr-service.war" \
  "$ROOT/services/site/target/site.war" \
  "$HELIOS_SSH:$REMOTE_DIR/wars/"
"${SCP[@]}" \
  "$DEPLOY_DIR/common.sh" "$DEPLOY_DIR/payara.sh" \
  "$DEPLOY_DIR/setup-domains.sh" "$DEPLOY_DIR/remote-deploy.sh" \
  "$HELIOS_SSH:$REMOTE_DIR/scripts/"
"${SSH[@]}" "$HELIOS_SSH" "umask 077 && rm -f $REMOTE_DIR/scripts/.env && cat > $REMOTE_DIR/scripts/.env" < "$ENV_FILE"

if [ "${1:-}" = "--setup" ]; then
  "${SSH[@]}" "$HELIOS_SSH" "bash $REMOTE_DIR/scripts/setup-domains.sh"
fi
"${SSH[@]}" "$HELIOS_SSH" "bash $REMOTE_DIR/scripts/remote-deploy.sh"
