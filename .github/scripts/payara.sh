#!/usr/bin/env bash
. "$(dirname "${BASH_SOURCE[0]}")/common.sh"

ASADMIN="$PAYARA_HOME/bin/asadmin"
export AS_ADMIN_INTERACTIVE=false
export JAVA_TOOL_OPTIONS="-Xmx256m"
PASSWORD_FILE="$DEPLOY_DIR/.asadmin-password"
PAYARA_MASTER_PASSWORD=changeit

write_password_file() {
  umask 077
  printf 'AS_ADMIN_PASSWORD=%s\nAS_ADMIN_NEWPASSWORD=%s\n' "$ADMIN_PASSWORD" "$ADMIN_PASSWORD" > "$PASSWORD_FILE"
}

admin() {
  local port="$1"
  shift
  "$ASADMIN" --user admin --passwordfile "$PASSWORD_FILE" --port "$port" "$@"
}

domain_dir() {
  echo "$PAYARA_HOME/glassfish/domains/$1"
}

store_of() {
  local config
  config="$(domain_dir "$1")/config"
  if [ -f "$config/$2.p12" ]; then
    echo "$config/$2.p12"
  else
    echo "$config/$2.jks"
  fi
}

store_type() {
  if [[ "$1" == *.jks ]]; then echo JKS; else echo PKCS12; fi
}

san_list() {
  local result="" host
  IFS=',' read -ra hosts <<< "$CERT_HOSTNAMES"
  for host in "${hosts[@]}"; do
    result="${result:+$result,}dns:$host"
  done
  echo "$result,ip:127.0.0.1"
}
