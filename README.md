# Сервис-ориентированная архитектура — лабораторные работы №1 и №2

**Вариант:** 67208

- **ЛР1** — OpenAPI-спецификации двух REST-сервисов (Worker Collection Service и HR Service) с интерактивной документацией Swagger UI.
- **ЛР2** — реализация обоих сервисов по спецификации и клиентское приложение.

## Структура
```
openapi/               # OpenAPI-спецификации (.yaml) — источник контракта для сервисов и клиента
swagger-ui/            # ЛР1: сборка Swagger UI (build-site.sh + инициализатор)
  site/                #   (генерируется) готовый сайт Swagger UI

pom.xml                # Maven multi-module (Java 17)
services/
  worker-service/      # Worker Collection Service: Spring MVC 6, Spring Data JPA, Bean Validation, Lombok (/api/v1)
  hr-service/          # HR Service: JAX-RS, MicroProfile Rest Client, Bean Validation (/hr)
  site/                # Клиент: React + Ant Design + TanStack Query, клиент API генерируется из openapi/*.yaml (/)
    src/               #   исходники React (Vite)
    webapp/            #   WEB-INF для site.war (вместо src/main/webapp, чтобы не смешивать с исходниками Vite)
.github/
  workflows/           # CI: проверка спецификаций и публикация Swagger UI на helios
  scripts/             # Скрипты настройки доменов Payara и деплоя сервисов на helios

docs/tasks/            # Тексты заданий
docs/report/           # Отчёты (Typst): common/ — титульный лист, lab-N/ — отчёт по работе
Makefile               # Сборка отчётов: make docs, make watch LAB=lab-1
```

Пакеты обоих сервисов (`ru.itmo.soa.workers.*`, `ru.itmo.soa.hr.*`) устроены одинаково:
`config` — конфигурация приложения, `controller` — REST-ресурсы и обработка ошибок, `dto` — объекты API,
`error` — исключения, `service` — бизнес-логика. Дополнительно: `domain`/`repository`/`query` в worker-service,
`client` (MicroProfile Rest Client к Worker Collection Service) в hr-service.

## ЛР1: Swagger UI

Требования: Bash, Node.js ≥ 18, npm.

```bash
./swagger-ui/build-site.sh
cd swagger-ui/site && npx serve   # http://localhost:3000
```

## ЛР2: сервисы и клиент

### Архитектура

| Компонент | Технология | Домен Payara | URL (порты по умолчанию) |
|---|---|---|---|
| Worker Collection Service | Spring MVC REST, PostgreSQL (`jdbc/workersDS`) | `soa-workers` | `https://localhost:24081/api/v1/workers` |
| Клиент | React SPA | `soa-workers` | `https://localhost:24081/` |
| HR Service | JAX-RS, вызывает Worker Collection Service по HTTPS | `soa-hr` | `https://localhost:24181/hr/index/...` |

- В обоих доменах `http-listener-1` (HTTP) отключён, работает только `http-listener-2` (HTTPS) с самоподписанным сертификатом (SAN: `localhost`, `helios.cs.ifmo.ru`, `se.ifmo.ru`, `127.0.0.1`). Админ-порт переведён на HTTPS (`enable-secure-admin`).
- HR Service вызывает Worker Collection Service через MicroProfile Rest Client; адрес задаётся ключом `worker-service/mp-rest/url` (config source домена Payara), а сертификат первого сервиса импортируется в стандартный truststore (`cacerts`) домена HR.
- Типы и HTTP-клиент фронтенда генерируются из OpenAPI-спецификаций (`openapi-typescript` + `openapi-fetch`) при каждой сборке — ручных DTO на клиенте нет.
- Таблица `soa_workers` создаётся при старте worker-service из `services/worker-service/src/main/resources/schema.sql`.

### Сборка

Требования: JDK 17, Maven 3.9 (Node.js для клиента скачивается плагином `frontend-maven-plugin`).

```bash
VITE_WORKER_API=https://localhost:24081/api/v1 \
VITE_HR_API=https://localhost:24181/hr \
mvn package
```

Результат: `services/worker-service/target/worker-service.war`, `services/hr-service/target/hr-service.war`, `services/site/target/site.war`.
Адреса сервисов для клиента обязательны на этапе сборки — без них сборка клиента завершится ошибкой.

Тесты: `mvn -pl services/worker-service,services/hr-service test`.

### Развёртывание на helios

1. Однократно на helios: скачать и распаковать Payara Server 6 (Full или Web) в `~/payara6`, положить JDBC-драйвер PostgreSQL в `~/lib/postgresql-42.7.4.jar`.
2. Локально: `cp .github/scripts/env.example .github/scripts/.env` и заполнить — логин helios, пароль БД (`~/.pgpass` на helios), свободные `WORKER_PORTBASE` / `HR_PORTBASE` (HTTPS-порт = portbase + 81, админ-порт = portbase + 48), пароль admin.
3. Первый деплой с настройкой доменов:
   ```bash
   ./.github/scripts/deploy.sh --setup
   ```
   Скрипт соберёт проект, скопирует WAR-файлы и скрипты на helios, создаст два домена, сгенерирует сертификаты, отключит HTTP, создаст JDBC-пул и задеплоит приложения. При первом обращении к админ-порту по HTTPS `asadmin` попросит подтвердить сертификат.
4. Последующие деплои: `./.github/scripts/deploy.sh`.

### Доступ из браузера

Порты helios недоступны снаружи, поэтому нужен SSH-туннель:

```bash
ssh -p 2222 -L 24081:localhost:24081 -L 24181:localhost:24181 s123456@se.ifmo.ru
```

Затем открыть `https://localhost:24181/hr/index/1/1` и `https://localhost:24081/` и **в обоих** принять самоподписанный сертификат (для HR ответ будет ошибкой 405 — это нормально, важно только принять сертификат). После этого клиент на `https://localhost:24081/` сможет обращаться к обоим сервисам.

### Проверка

```bash
curl -k 'https://localhost:24081/api/v1/workers?sort=-salary,name&filter=salary%5Bgte%5D%3D1000&page=0&size=5'
curl -k -X POST https://localhost:24181/hr/index/1/1.1
curl http://localhost:24080/api/v1/workers   # HTTP отключён — соединение отклоняется
```
