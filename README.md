# СОА, ЛР1 и ЛР2

Вариант 67208. ЛР1 — OpenAPI-спецификации и Swagger UI, ЛР2 — сервисы и клиент.

```
openapi/              спецификации; swagger-ui/ — документация
services/
  worker-service/     Spring MVC, Spring Data JPA, PostgreSQL
  hr-service/         JAX-RS, MicroProfile Rest Client
  site/               клиент: React, Ant Design; API-клиент генерируется из openapi/
docker/, compose.yaml запуск в Docker
.github/              CI и скрипты деплоя на helios
docs/                 задания и отчёты (make docs)
```

| Приложение | Адрес |
|---|---|
| worker-service | `https://localhost:24081/api/v1/workers` |
| site | `https://localhost:24081/` |
| hr-service | `https://localhost:24181/hr` |

Только HTTPS с самоподписанными сертификатами. В браузере их нужно принять один раз.

## Docker

```bash
docker compose up -d --build
```

Сборка проекта идёт внутри образа. `docker compose down -v` удаляет и данные БД.

## Сборка

JDK 17–21, Maven 3.9.

```bash
VITE_WORKER_API=https://localhost:24081/api/v1 VITE_HR_API=https://localhost:24181/hr \
mvn clean package -DskipTests
```

Тесты: `mvn -pl services/worker-service,services/hr-service test`.

## helios

1. На helios: Payara 6 в `~/payara6`, драйвер PostgreSQL в `~/lib/postgresql-42.7.4.jar`.
2. `cp .github/scripts/env.example .github/scripts/.env` и заполнить.
3. `./.github/scripts/deploy.sh --setup` — первый раз, с созданием доменов; дальше без `--setup`.
4. Туннель: `ssh -p 2222 -L 24081:localhost:24081 -L 24181:localhost:24181 s123456@se.ifmo.ru`.

Тот же `setup-domains.sh` поднимает домены и на локальном Payara: `ENV_FILE=local.env bash .github/scripts/setup-domains.sh`.

### GitHub Actions

- `build` — сборка и тесты на каждый push и pull request.
- `deploy-docs` — Swagger UI на helios при push в `main`.
- `deploy-services` — сервисы и клиент на helios, только вручную (Actions → Run workflow). Галочка «Создать и настроить домены» — для первого деплоя.

Для `deploy-services` в настройках репозитория нужны:

- variables: `HELIOS_HOST`, `HELIOS_PORT`, `WORKER_PORTBASE`, `HR_PORTBASE`, `PUBLIC_HOST` (`localhost` для доступа через туннель или `helios.cs.ifmo.ru`);
- secrets: `HELIOS_USER`, `HELIOS_PASSWORD`, `DB_PASSWORD`, `PAYARA_ADMIN_PASSWORD`.

## Swagger UI

```bash
./openapi/swagger-ui/build.sh && cd openapi/swagger-ui/dist && npx serve
```

## Проверка

```bash
curl -k 'https://localhost:24081/api/v1/workers?sort=-salary,name&filter=salary%5Bgte%5D%3D1000'
curl -k -X POST https://localhost:24181/hr/index/1/1.1
```
