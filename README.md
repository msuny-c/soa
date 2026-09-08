# Лабораторная работа №1 — Сервис-ориентированная архитектура

**Вариант:** 67208

OpenAPI-спецификации двух REST-сервисов (Worker Collection Service и HR Service) с интерактивной документацией Swagger UI.

## Структура
```
api/ # OpenAPI-спецификации (.yaml)
scripts/build-site.sh # Сборка Swagger UI
web/ # Инициализатор Swagger UI
site/ # (генерируется) Готовый сайт
```

## Требования

- Bash, Node.js ≥ 18, npm

## Запуск

```bash
# 1. Собрать сайт
chmod +x scripts/build-site.sh
./scripts/build-site.sh

# 2. Запустить локальный сервер
cd site
npx serve


# 3. Открыть http://localhost:3000
```

