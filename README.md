# СОА, лабораторные работы №1 и №2

Вариант 67208.

- ЛР1: OpenAPI-спецификации Worker Collection Service и HR Service, документация в Swagger UI.
- ЛР2: реализация обоих сервисов и клиент.

## Структура

```
openapi/               спецификации сервисов
swagger-ui/            сборка Swagger UI (ЛР1), результат в swagger-ui/site/
services/
  worker-service/      Spring MVC, Spring Data JPA, PostgreSQL; /api/v1
  hr-service/          JAX-RS, MicroProfile Rest Client; /hr
  site/                клиент: React, Ant Design, TanStack Query; /
.github/workflows/     CI: проверка спецификаций и публикация Swagger UI
.github/scripts/       настройка доменов Payara и деплой на helios
docs/                  задания и отчёты (Typst), сборка отчётов — make docs
pom.xml                родительский Maven-проект
```

Пакеты сервисов: `config`, `controller` (REST-ресурсы и обработчики ошибок), `dto`, `error`, `service`.
В worker-service ещё `domain`, `repository`, `query` (разбор сортировки и фильтров), в hr-service — `client` (клиент Worker Collection Service).

В клиенте типы и HTTP-клиент генерируются из `openapi/*.yaml` при сборке (`openapi-typescript`, `openapi-fetch`).
Разделы: `#/workers`, `#/reports`, `#/indexation`.

## Swagger UI

Нужны Bash и Node.js 18+.

```bash
./swagger-ui/build-site.sh
cd swagger-ui/site && npx serve
```

## Сборка

Нужны JDK 17–21 и Maven 3.9. Node.js для клиента скачивает `frontend-maven-plugin`.

```bash
VITE_WORKER_API=https://localhost:24081/api/v1 \
VITE_HR_API=https://localhost:24181/hr \
mvn clean package -DskipTests
```

Адреса сервисов зашиваются в клиент при сборке, без них сборка клиента падает.
Получаются `worker-service.war`, `hr-service.war` и `site.war` в `services/*/target/`.

Тесты: `mvn -pl services/worker-service,services/hr-service test`.

## Развёртывание

| Приложение | Домен Payara | Адрес |
|---|---|---|
| worker-service | `soa-workers` | `https://localhost:24081/api/v1/workers` |
| site | `soa-workers` | `https://localhost:24081/` |
| hr-service | `soa-hr` | `https://localhost:24181/hr` |

`setup-domains.sh` создаёт оба домена и делает следующее:

- отключает HTTP-листенер, оставляет HTTPS с самоподписанным сертификатом, переводит админ-порт на HTTPS;
- создаёт JDBC-пул `jdbc/workersDS`; таблица создаётся при старте worker-service из `schema.sql`;
- добавляет сертификат `soa-workers` в truststore домена `soa-hr` и задаёт адрес Worker Collection Service ключом `worker-service/mp-rest/url`.

HTTPS-порт домена равен `portbase + 81`, админ-порт — `portbase + 48`.

### helios

1. На helios распаковать Payara 6 в `~/payara6` и положить драйвер PostgreSQL в `~/lib/postgresql-42.7.4.jar`.
2. Скопировать `.github/scripts/env.example` в `.github/scripts/.env` и заполнить: логин helios, пароль БД (из `~/.pgpass` на helios), `WORKER_PORTBASE`, `HR_PORTBASE`, пароль admin.
3. Первый деплой: `./.github/scripts/deploy.sh --setup`. Скрипт собирает проект, копирует WAR и скрипты на helios, настраивает домены и деплоит приложения.
4. Дальше: `./.github/scripts/deploy.sh`.

Порты helios снаружи закрыты, поэтому нужен туннель:

```bash
ssh -p 2222 -L 24081:localhost:24081 -L 24181:localhost:24181 s123456@se.ifmo.ru
```

В браузере нужно один раз принять сертификаты обоих доменов. Для этого открыть `https://localhost:24181/hr/index/1/1` (ответ 405 — так и должно быть) и `https://localhost:24081/`.

### Локально

Нужны Payara 6 на JDK 21, PostgreSQL и тот же env-файл с локальными значениями (`PAYARA_HOME`, `DB_HOST=localhost`, `DB_NAME` и т. д.).

```bash
ENV_FILE=path/to/local.env bash .github/scripts/setup-domains.sh

asadmin --port 24048 deploy --force=true --name worker-service --contextroot /api services/worker-service/target/worker-service.war
asadmin --port 24048 deploy --force=true --name site --contextroot / services/site/target/site.war
asadmin --port 24148 deploy --force=true --name hr-service --contextroot /hr services/hr-service/target/hr-service.war
```

`asadmin` берётся из `$PAYARA_HOME/bin`. После `setup-domains.sh` ему нужны `--user admin --passwordfile .github/scripts/.asadmin-password`.

## Проверка

```bash
curl -k 'https://localhost:24081/api/v1/workers?sort=-salary,name&filter=salary%5Bgte%5D%3D1000&page=0&size=5'
curl -k -X POST https://localhost:24181/hr/index/1/1.1
curl http://localhost:24080/api/v1/workers   # HTTP выключен, соединение отклоняется
```
