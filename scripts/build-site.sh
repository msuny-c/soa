#!/usr/bin/env bash
# Собирает статический сайт документации: Swagger UI + спецификации.
# Использование: ./scripts/build-site.sh [output-dir]
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="${1:-$ROOT/site}"
# Версия пакета swagger-ui-dist (диапазон semver допустим).
SWAGGER_UI_VERSION="${SWAGGER_UI_VERSION:-5}"

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

echo "==> Скачиваю swagger-ui-dist@${SWAGGER_UI_VERSION}"
(
  cd "$WORK"
  TARBALL="$(npm pack "swagger-ui-dist@${SWAGGER_UI_VERSION}" --silent)"
  tar -xzf "$TARBALL"
)

echo "==> Собираю сайт в $OUT"
rm -rf "$OUT"
mkdir -p "$OUT"

# Из пакета нужен только тот минимум, на который ссылается index.html:
# остальные сборки (ES-модули) и source maps весят десятки мегабайт и не нужны.
for f in index.html oauth2-redirect.html swagger-ui.css index.css \
         swagger-ui-bundle.js swagger-ui-standalone-preset.js \
         favicon-16x16.png favicon-32x32.png; do
  cp "$WORK/package/$f" "$OUT/$f"
done

# Свой инициализатор (переключатель между двумя спецификациями).
cp "$ROOT/web/swagger-initializer.js" "$OUT/swagger-initializer.js"

# Спецификации кладём рядом с index.html — на них ссылается инициализатор.
cp "$ROOT/api/worker-service.yaml" "$ROOT/api/hr-service.yaml" "$OUT/"

echo "==> Готово: $(find "$OUT" -type f | wc -l | tr -d ' ') файлов в $OUT"
