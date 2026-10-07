ARG PAYARA_IMAGE=payara/server-full:6.2025.11-jdk21

FROM maven:3.9-eclipse-temurin-21 AS build
ARG VITE_WORKER_API=https://localhost:24081/api/v1
ARG VITE_HR_API=https://localhost:24181/hr
WORKDIR /src
COPY pom.xml .
COPY .mvn .mvn
COPY openapi openapi
COPY services services
RUN --mount=type=cache,target=/root/.m2 --mount=type=cache,target=/root/.npm \
    VITE_WORKER_API=$VITE_WORKER_API VITE_HR_API=$VITE_HR_API mvn -B -q clean package -DskipTests \
    && mvn -B -q dependency:copy -Dartifact=org.postgresql:postgresql:42.7.4 -DoutputDirectory=/drivers

FROM ${PAYARA_IMAGE} AS certs
ARG DNAME="CN=localhost,OU=Payara,O=Payara Foundation,L=Great Malvern,ST=Worcestershire,C=UK"
WORKDIR /tmp/certs
RUN keytool -genkeypair -alias s1as -keyalg RSA -keysize 2048 -validity 825 -dname "$DNAME" \
        -ext "SAN=dns:localhost,dns:worker,ip:127.0.0.1" \
        -keystore worker.p12 -storetype PKCS12 -storepass changeit -keypass changeit \
    && keytool -genkeypair -alias s1as -keyalg RSA -keysize 2048 -validity 825 -dname "$DNAME" \
        -ext "SAN=dns:localhost,dns:hr,ip:127.0.0.1" \
        -keystore hr.p12 -storetype PKCS12 -storepass changeit -keypass changeit \
    && keytool -exportcert -rfc -alias s1as -keystore worker.p12 -storepass changeit -file worker.crt \
    && keytool -exportcert -rfc -alias s1as -keystore hr.p12 -storepass changeit -file hr.crt

FROM ${PAYARA_IMAGE} AS payara
ENV DOMAIN_CONFIG=/opt/payara/appserver/glassfish/domains/domain1/config \
    DEPLOY_PROPS=--force=true
ARG SERVER
COPY --from=certs --chown=payara:payara /tmp/certs/ /tmp/certs/
RUN keytool -delete -alias s1as -keystore $DOMAIN_CONFIG/keystore.p12 -storepass changeit \
    && keytool -importkeystore -noprompt -srckeystore /tmp/certs/$SERVER.p12 -srcstorepass changeit -srcalias s1as \
        -destkeystore $DOMAIN_CONFIG/keystore.p12 -deststorepass changeit -destkeypass changeit \
    && keytool -delete -alias s1as -keystore $DOMAIN_CONFIG/cacerts.p12 -storepass changeit \
    && keytool -importcert -noprompt -alias s1as -file /tmp/certs/$SERVER.crt \
        -keystore $DOMAIN_CONFIG/cacerts.p12 -storepass changeit \
    && keytool -importcert -noprompt -alias worker-service -file /tmp/certs/worker.crt \
        -keystore $DOMAIN_CONFIG/cacerts.p12 -storepass changeit \
    && rm -r /tmp/certs

FROM payara AS worker
COPY --from=build --chown=payara:payara /drivers/ /opt/payara/appserver/glassfish/domains/domain1/lib/
COPY --chown=payara:payara docker/worker/ /opt/payara/config/
COPY --from=build --chown=payara:payara /src/services/worker-service/target/worker-service.war /src/services/site/target/site.war /opt/payara/deployments/

FROM payara AS hr
COPY --chown=payara:payara docker/hr/ /opt/payara/config/
COPY --from=build --chown=payara:payara /src/services/hr-service/target/hr-service.war /opt/payara/deployments/
