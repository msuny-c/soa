#!/usr/bin/env bash
. "$(dirname "${BASH_SOURCE[0]}")/payara.sh"

DIST_DIR="$DEPLOY_DIR/../dist"
DB_PASSWORD_ALIAS=workers-db

escape_property() {
  printf '%s' "$1" | sed 's/[:=\\]/\\&/g'
}

require_java() {
  if ! command -v java >/dev/null && [ -z "${JAVA_HOME:-}" ]; then
    echo "На сервере не найдена Java (нужен JDK 17 или 21)" >&2
    exit 1
  fi
}

install_payara() {
  if [ -x "$ASADMIN" ]; then
    echo "==> Payara уже установлен в $PAYARA_HOME"
    return
  fi
  echo "==> Устанавливаю Payara в $PAYARA_HOME"
  local tmp
  tmp="$(mktemp -d)"
  unzip -q "$DIST_DIR/payara.zip" -d "$tmp"
  mkdir -p "$(dirname "$PAYARA_HOME")"
  mv "$tmp/payara6" "$PAYARA_HOME"
  rm -rf "$tmp" "$DIST_DIR/payara.zip"
}

db_password() {
  if [ -n "${DB_PASSWORD:-}" ]; then
    printf '%s' "$DB_PASSWORD"
    return
  fi
  if [ ! -f "$HOME/.pgpass" ]; then
    echo "Не найден ~/.pgpass, задайте DB_PASSWORD" >&2
    return 1
  fi
  local host port db user password
  while IFS=: read -r host port db user password; do
    if [[ ("$host" == "$DB_HOST" || "$host" == "*") && ("$port" == "$DB_PORT" || "$port" == "*") &&
          ("$db" == "$DB_NAME" || "$db" == "*") && ("$user" == "$DB_USER" || "$user" == "*") ]]; then
      printf '%s' "$password"
      return
    fi
  done < "$HOME/.pgpass"
  echo "В ~/.pgpass нет пароля для $DB_USER@$DB_HOST:$DB_PORT/$DB_NAME" >&2
  return 1
}

store_db_password() {
  local password file
  password="$(db_password)"
  file="$(mktemp)"
  printf 'AS_ADMIN_PASSWORD=%s\nAS_ADMIN_ALIASPASSWORD=%s\n' "$ADMIN_PASSWORD" "$password" > "$file"
  admin "$WORKER_ADMIN_PORT" delete-password-alias "$DB_PASSWORD_ALIAS" >/dev/null 2>&1 || true
  "$ASADMIN" --user admin --passwordfile "$file" --port "$WORKER_ADMIN_PORT" create-password-alias "$DB_PASSWORD_ALIAS"
  rm -f "$file"
}

create_domain() {
  local name="$1" portbase="$2"
  if [ -d "$(domain_dir "$name")" ]; then
    echo "==> Домен $name уже существует"
    return
  fi
  echo "==> Создаю домен $name (portbase $portbase)"
  "$ASADMIN" --user admin --passwordfile "$PASSWORD_FILE" create-domain \
    --portbase "$portbase" --savelogin=false "$name"
}

replace_certificate() {
  local name="$1" keystore storetype truststore cert
  keystore="$(store_of "$name" keystore)"
  storetype="$(store_type "$keystore")"
  truststore="$(store_of "$name" cacerts)"
  echo "==> Генерирую самоподписанный сертификат для $name ($(san_list))"
  keytool -delete -alias s1as -keystore "$keystore" -storetype "$storetype" \
    -storepass "$PAYARA_MASTER_PASSWORD" >/dev/null 2>&1 || true
  keytool -genkeypair -alias s1as -keyalg RSA -keysize 2048 -validity 825 \
    -dname "CN=localhost, OU=SOA, O=ITMO University, L=Saint Petersburg, C=RU" \
    -ext "SAN=$(san_list)" \
    -keystore "$keystore" -storetype "$storetype" \
    -storepass "$PAYARA_MASTER_PASSWORD" -keypass "$PAYARA_MASTER_PASSWORD"
  cert="$(mktemp)"
  keytool -exportcert -rfc -alias s1as -keystore "$keystore" -storetype "$storetype" \
    -storepass "$PAYARA_MASTER_PASSWORD" -file "$cert"
  keytool -delete -alias s1as -keystore "$truststore" -storetype "$(store_type "$truststore")" \
    -storepass "$PAYARA_MASTER_PASSWORD" >/dev/null 2>&1 || true
  keytool -importcert -noprompt -alias s1as -file "$cert" \
    -keystore "$truststore" -storetype "$(store_type "$truststore")" -storepass "$PAYARA_MASTER_PASSWORD"
  rm -f "$cert"
}

trust_worker_certificate() {
  local keystore truststore cert
  keystore="$(store_of "$WORKER_DOMAIN" keystore)"
  truststore="$(store_of "$HR_DOMAIN" cacerts)"
  cert="$(mktemp)"
  echo "==> Добавляю сертификат $WORKER_DOMAIN в truststore домена $HR_DOMAIN"
  keytool -exportcert -rfc -alias s1as -keystore "$keystore" -storetype "$(store_type "$keystore")" \
    -storepass "$PAYARA_MASTER_PASSWORD" -file "$cert"
  keytool -delete -alias worker-service -keystore "$truststore" -storetype "$(store_type "$truststore")" \
    -storepass "$PAYARA_MASTER_PASSWORD" >/dev/null 2>&1 || true
  keytool -importcert -noprompt -alias worker-service -file "$cert" \
    -keystore "$truststore" -storetype "$(store_type "$truststore")" -storepass "$PAYARA_MASTER_PASSWORD"
  rm -f "$cert"
}

configure_common() {
  local port="$1"
  echo "==> Отключаю HTTP (http-listener-1), оставляю только HTTPS"
  admin "$port" set configs.config.server-config.network-config.network-listeners.network-listener.http-listener-1.enabled=false
  admin "$port" delete-jvm-options -- -Xmx512m || true
  admin "$port" create-jvm-options -- "-Xmx$JVM_HEAP" || true
  admin "$port" create-jvm-options -- "-XX\\:ActiveProcessorCount=2" || true
  admin "$port" create-jvm-options -- "-XX\\:+UseSerialGC" || true
  admin "$port" set-hazelcast-configuration --enabled=false --dynamic=true
  admin "$port" enable-secure-admin
}

configure_worker_domain() {
  echo "==> Настраиваю JDBC-пул PostgreSQL"
  admin "$WORKER_ADMIN_PORT" add-library "$DIST_DIR/postgresql.jar" || true
  store_db_password
  if ! admin "$WORKER_ADMIN_PORT" list-jdbc-connection-pools | grep -qx workersPool; then
    admin "$WORKER_ADMIN_PORT" create-jdbc-connection-pool \
      --datasourceclassname org.postgresql.ds.PGSimpleDataSource \
      --restype javax.sql.DataSource \
      --property "serverName=$(escape_property "$DB_HOST"):portNumber=$DB_PORT:databaseName=$(escape_property "$DB_NAME"):user=$(escape_property "$DB_USER")" \
      workersPool
  fi
  admin "$WORKER_ADMIN_PORT" set "resources.jdbc-connection-pool.workersPool.property.password=\${ALIAS=$DB_PASSWORD_ALIAS}"
  if ! admin "$WORKER_ADMIN_PORT" list-jdbc-resources | grep -qx jdbc/workersDS; then
    admin "$WORKER_ADMIN_PORT" create-jdbc-resource --connectionpoolid workersPool jdbc/workersDS
  fi
  admin "$WORKER_ADMIN_PORT" ping-connection-pool workersPool
}

configure_hr_domain() {
  echo "==> Настраиваю MicroProfile Rest Client HR Service -> Worker Collection Service"
  admin "$HR_ADMIN_PORT" set-config-property --source=domain \
    --propertyName=worker-service/mp-rest/url --propertyValue="https://localhost:$WORKER_HTTPS_PORT/api/v1"
}

require_java
install_payara
write_password_file

create_domain "$WORKER_DOMAIN" "$WORKER_PORTBASE"
create_domain "$HR_DOMAIN" "$HR_PORTBASE"

"$ASADMIN" stop-domain "$WORKER_DOMAIN" >/dev/null 2>&1 || true
"$ASADMIN" stop-domain "$HR_DOMAIN" >/dev/null 2>&1 || true

replace_certificate "$WORKER_DOMAIN"
replace_certificate "$HR_DOMAIN"
trust_worker_certificate

"$ASADMIN" start-domain "$WORKER_DOMAIN"
configure_common "$WORKER_ADMIN_PORT"
configure_worker_domain
"$ASADMIN" stop-domain "$WORKER_DOMAIN"

"$ASADMIN" start-domain "$HR_DOMAIN"
configure_common "$HR_ADMIN_PORT"
configure_hr_domain
"$ASADMIN" stop-domain "$HR_DOMAIN"

"$ASADMIN" start-domain "$WORKER_DOMAIN"
"$ASADMIN" start-domain "$HR_DOMAIN"

echo "==> Готово"
echo "    Worker Collection Service: https://$PUBLIC_HOST:$WORKER_HTTPS_PORT/api/v1/workers"
echo "    HR Service:                https://$PUBLIC_HOST:$HR_HTTPS_PORT/hr/index/..."
echo "    Клиент:                    https://$PUBLIC_HOST:$WORKER_HTTPS_PORT/"
