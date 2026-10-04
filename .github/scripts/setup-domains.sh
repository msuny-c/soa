#!/usr/bin/env bash
. "$(dirname "${BASH_SOURCE[0]}")/payara.sh"

escape_property() {
  printf '%s' "$1" | sed 's/[:=\\]/\\&/g'
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
  local name="$1" keystore storetype
  keystore="$(store_of "$name" keystore)"
  storetype="$(store_type "$keystore")"
  echo "==> Генерирую самоподписанный сертификат для $name ($(san_list))"
  keytool -delete -alias s1as -keystore "$keystore" -storetype "$storetype" \
    -storepass "$PAYARA_MASTER_PASSWORD" 2>/dev/null || true
  keytool -genkeypair -alias s1as -keyalg RSA -keysize 2048 -validity 825 \
    -dname "CN=localhost, OU=SOA, O=ITMO University, L=Saint Petersburg, C=RU" \
    -ext "SAN=$(san_list)" \
    -keystore "$keystore" -storetype "$storetype" \
    -storepass "$PAYARA_MASTER_PASSWORD" -keypass "$PAYARA_MASTER_PASSWORD"
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
    -storepass "$PAYARA_MASTER_PASSWORD" 2>/dev/null || true
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
  admin "$port" enable-secure-admin
}

configure_worker_domain() {
  echo "==> Настраиваю JDBC-пул PostgreSQL"
  admin "$WORKER_ADMIN_PORT" add-library "$PG_JDBC_JAR" || true
  if ! admin "$WORKER_ADMIN_PORT" list-jdbc-connection-pools | grep -qx workersPool; then
    admin "$WORKER_ADMIN_PORT" create-jdbc-connection-pool \
      --datasourceclassname org.postgresql.ds.PGSimpleDataSource \
      --restype javax.sql.DataSource \
      --property "serverName=$(escape_property "$DB_HOST"):portNumber=$DB_PORT:databaseName=$(escape_property "$DB_NAME"):user=$(escape_property "$DB_USER"):password=$(escape_property "$DB_PASSWORD")" \
      workersPool
  fi
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

write_password_file

create_domain "$WORKER_DOMAIN" "$WORKER_PORTBASE"
create_domain "$HR_DOMAIN" "$HR_PORTBASE"

"$ASADMIN" stop-domain "$WORKER_DOMAIN" >/dev/null 2>&1 || true
"$ASADMIN" stop-domain "$HR_DOMAIN" >/dev/null 2>&1 || true

replace_certificate "$WORKER_DOMAIN"
replace_certificate "$HR_DOMAIN"
trust_worker_certificate

"$ASADMIN" start-domain "$WORKER_DOMAIN"
"$ASADMIN" start-domain "$HR_DOMAIN"

configure_common "$WORKER_ADMIN_PORT"
configure_common "$HR_ADMIN_PORT"
configure_worker_domain
configure_hr_domain

"$ASADMIN" restart-domain "$WORKER_DOMAIN"
"$ASADMIN" restart-domain "$HR_DOMAIN"

echo "==> Готово"
echo "    Worker Collection Service: https://$PUBLIC_HOST:$WORKER_HTTPS_PORT/api/v1/workers"
echo "    HR Service:                https://$PUBLIC_HOST:$HR_HTTPS_PORT/hr/index/..."
echo "    Клиент:                    https://$PUBLIC_HOST:$WORKER_HTTPS_PORT/"
