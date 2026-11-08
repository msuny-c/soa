ARG PAYARA_IMAGE=payara/server-full:6.2025.11-jdk21

FROM maven:3.9-eclipse-temurin-21 AS build
ARG VITE_WORKER_API=https://localhost:24081/api/v1
ARG VITE_HR_API=https://localhost:24181/hr
WORKDIR /src
COPY pom.xml .
COPY .mvn .mvn
COPY openapi openapi
COPY config-repo config-repo
COPY services services
RUN --mount=type=cache,target=/root/.m2 --mount=type=cache,target=/root/.npm \
    VITE_WORKER_API=$VITE_WORKER_API VITE_HR_API=$VITE_HR_API \
    mvn -B -q clean package -DskipTests

# Self-signed certificates (same DNAME/SAN pattern as lab 2). Generated here because
# the build stage has both keytool (JDK) and openssl, unlike the slim runtime images.
ARG DNAME="CN=localhost,OU=Payara,O=Payara Foundation,L=Great Malvern,ST=Worcestershire,C=UK"
WORKDIR /certs
RUN keytool -genkeypair -alias s1as -keyalg RSA -keysize 2048 -validity 825 -dname "$DNAME" \
        -ext "SAN=dns:localhost,dns:worker,ip:127.0.0.1" \
        -keystore worker.p12 -storetype PKCS12 -storepass changeit -keypass changeit \
    && keytool -genkeypair -alias s1as -keyalg RSA -keysize 2048 -validity 825 -dname "$DNAME" \
        -ext "SAN=dns:localhost,dns:hr,ip:127.0.0.1" \
        -keystore hr.p12 -storetype PKCS12 -storepass changeit -keypass changeit \
    && keytool -exportcert -rfc -alias s1as -keystore worker.p12 -storepass changeit -file worker.crt \
    && keytool -exportcert -rfc -alias s1as -keystore hr.p12 -storepass changeit -file hr.crt \
    && openssl pkcs12 -in worker.p12 -passin pass:changeit -nodes -out worker.pem \
    && openssl pkcs12 -in hr.p12 -passin pass:changeit -nodes -out hr.pem \
    && chmod 644 /certs/*.pem
WORKDIR /src

# Payara instances (hr1, hr2): HTTPS-only listeners with the self-signed hr certificate.
FROM ${PAYARA_IMAGE} AS payara
ENV DOMAIN_CONFIG=/opt/payara/appserver/glassfish/domains/domain1/config \
    DEPLOY_PROPS=--force=true
ARG SERVER=hr
COPY --from=build --chown=payara:payara /certs/ /tmp/certs/
RUN keytool -delete -alias s1as -keystore $DOMAIN_CONFIG/keystore.p12 -storepass changeit \
    && keytool -importkeystore -noprompt -srckeystore /tmp/certs/$SERVER.p12 -srcstorepass changeit -srcalias s1as \
        -destkeystore $DOMAIN_CONFIG/keystore.p12 -deststorepass changeit -destkeypass changeit \
    && keytool -delete -alias s1as -keystore $DOMAIN_CONFIG/cacerts.p12 -storepass changeit \
    && keytool -importcert -noprompt -alias s1as -file /tmp/certs/$SERVER.crt \
        -keystore $DOMAIN_CONFIG/cacerts.p12 -storepass changeit \
    && rm -r /tmp/certs

# Spring Boot 2.3 (Spring Cloud Hoxton) does not run on JRE 17+ — pin Java 11 for all cloud services.
FROM eclipse-temurin:11-jre-jammy AS base-java11
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

FROM base-java11 AS worker
COPY --from=build /src/services/worker-service/target/worker-service.jar /app/
WORKDIR /app
ENTRYPOINT ["java", "-jar", "worker-service.jar"]

FROM base-java11 AS config-server
COPY --from=build /src/config-repo /app/config-repo
COPY --from=build /src/services/config-server/target/config-server.jar /app/
WORKDIR /app
ENTRYPOINT ["java", "-jar", "config-server.jar"]

FROM base-java11 AS eureka-server
COPY --from=build /src/services/eureka-server/target/eureka-server.jar /app/
WORKDIR /app
ENTRYPOINT ["java", "-jar", "eureka-server.jar"]

FROM base-java11 AS gateway
COPY --from=build /src/services/api-gateway/target/api-gateway.jar /app/
WORKDIR /app
ENTRYPOINT ["java", "-jar", "api-gateway.jar"]

FROM payara AS hr
COPY --chown=payara:payara docker/hr/ /opt/payara/config/
COPY --from=build --chown=payara:payara /src/services/hr-service/hr-ear/target/hr-service.ear /opt/payara/deployments/

# Second instance of the app server on a different port, same EAR deployment.
FROM payara AS hr2
COPY --chown=payara:payara docker/hr2/ /opt/payara/config/
COPY --from=build --chown=payara:payara /src/services/hr-service/hr-ear/target/hr-service.ear /opt/payara/deployments/

# TLS entry point for the browser: serves the SPA and proxies /api/ to the Zuul gateway.
FROM nginx:1.27-alpine AS web
COPY --from=build /src/services/site/dist/ /usr/share/nginx/html/
COPY --from=build /certs/worker.pem /etc/nginx/certs/worker.pem
COPY docker/web/default.conf /etc/nginx/conf.d/default.conf

# TLS entry point for the HR service: round-robin over both Payara instances.
# The image runs as the non-root haproxy user, so the key file must be readable.
FROM haproxy:3.1-alpine AS haproxy
COPY --from=build --chmod=644 /certs/hr.pem /usr/local/etc/haproxy/hr.pem
COPY docker/haproxy/haproxy.cfg /usr/local/etc/haproxy/haproxy.cfg
