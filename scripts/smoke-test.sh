#!/usr/bin/env bash
# Prueba de humo end-to-end de Smart Finance Tracker (ver TESTING.md).
#
# Uso:
#   ./scripts/smoke-test.sh                  # pruebas básicas (no toca los contenedores)
#   ./scripts/smoke-test.sh --resilience     # además para el ML con `docker compose stop` y lo restaura
#
# Requisitos: curl y jq. El stack debe estar levantado (docker compose up --build).
set -euo pipefail

API="${API:-http://localhost:8080}"
ML="${ML_URL:-http://localhost:8000}"
KEY="${INTERNAL_API_KEY:-}"
if [[ -z "$KEY" && -f .env ]]; then
  KEY="$(grep -E '^INTERNAL_API_KEY=' .env | head -n1 | cut -d= -f2-)"
fi
WITH_RESILIENCE=false
[[ "${1:-}" == "--resilience" ]] && WITH_RESILIENCE=true

command -v jq >/dev/null   || { echo "Falta jq (https://jqlang.github.io/jq/)"; exit 2; }
command -v curl >/dev/null || { echo "Falta curl"; exit 2; }
[[ -n "$KEY" ]] || { echo "No encuentro INTERNAL_API_KEY (expórtala o ejecuta desde la raíz con .env)"; exit 2; }

TMP="$(mktemp)"; trap 'rm -f "$TMP"' EXIT
PASS=0; FAIL=0; TOKEN=""; STATUS=""; BODY=""

assert() { # descripción esperado obtenido
  if [[ "$2" == "$3" ]]; then echo "  ✅ $1"; PASS=$((PASS + 1))
  else echo "  ❌ $1 (esperado: $2 | obtenido: $3)"; FAIL=$((FAIL + 1)); fi
}

req() { # método ruta [json]   -> deja STATUS y BODY
  local method="$1" path="$2" data="${3:-}"
  local args=(-s -o "$TMP" -w '%{http_code}' -X "$method" "$API$path" -H "Content-Type: application/json")
  if [[ -n "$TOKEN" ]]; then args+=(-H "Authorization: Bearer $TOKEN"); fi
  if [[ -n "$data" ]]; then args+=(-d "$data"); fi
  STATUS="$(curl "${args[@]}")"
  BODY="$(cat "$TMP")"
}

ml_predict() { # descripción [api-key]  -> STATUS y BODY
  local args=(-s -o "$TMP" -w '%{http_code}' -X POST "$ML/predict" -H "Content-Type: application/json")
  if [[ -n "${2:-}" ]]; then args+=(-H "X-Internal-Api-Key: $2"); fi
  STATUS="$(curl "${args[@]}" -d "{\"description\":\"$1\",\"amount\":42.5}")"
  BODY="$(cat "$TMP")"
}

TODAY="$(date +%F)"
new_tx() { req POST /api/transactions "{\"amount\":42.50,\"type\":\"EXPENSE\",\"description\":\"$1\",\"occurredOn\":\"$TODAY\",\"categoryId\":null}"; }

echo "▶ 1. Salud de los servicios"
assert "Backend /actuator/health = UP" "UP" "$(curl -s "$API/actuator/health" | jq -r .status)"
assert "ML /health = ok" "ok" "$(curl -s "$ML/health" | jq -r .status)"

echo "▶ 2. ML service: API key y predicción"
ml_predict "Cena en restaurante italiano"
assert "Sin API key -> 403" "403" "$STATUS"
ml_predict "Cena en restaurante italiano" "clave-incorrecta"
assert "API key incorrecta -> 403" "403" "$STATUS"
ml_predict "Cena en restaurante italiano" "$KEY"
assert "Con API key -> 200" "200" "$STATUS"
assert "Predice Restaurantes" "Restaurantes" "$(jq -r .category <<<"$BODY")"
assert "source = ML" "ML" "$(jq -r .source <<<"$BODY")"

echo "▶ 3. Autenticación"
EMAIL="smoke-$RANDOM$RANDOM@example.com"
req POST /auth/register "{\"email\":\"$EMAIL\",\"password\":\"password123\",\"displayName\":\"Smoke\"}"
assert "Registro -> 201" "201" "$STATUS"
req POST /auth/login "{\"email\":\"$EMAIL\",\"password\":\"password123\"}"
assert "Login -> 200" "200" "$STATUS"
TOKEN="$(jq -r .token <<<"$BODY")"
[[ -n "$TOKEN" && "$TOKEN" != "null" ]] || { echo "  ❌ no se obtuvo token"; exit 1; }
req POST /auth/login "{\"email\":\"$EMAIL\",\"password\":\"mala-contraseña\"}"
assert "Login con contraseña incorrecta -> 401" "401" "$STATUS"
SAVED="$TOKEN"; TOKEN=""
req GET /api/transactions
assert "Ruta protegida sin token -> 401" "401" "$STATUS"
TOKEN="$SAVED"

echo "▶ 4. Transacciones (CRUD + categorización ML)"
new_tx "Cena en restaurante italiano"
assert "Crear sin categoría -> 201" "201" "$STATUS"
assert "categorySource = ML" "ML" "$(jq -r .categorySource <<<"$BODY")"
assert "Categoría = Restaurantes" "Restaurantes" "$(jq -r .category.name <<<"$BODY")"
assert "mlConfidence > 0" "true" "$(jq '.mlConfidence > 0' <<<"$BODY")"
TX_ID="$(jq -r .id <<<"$BODY")"
req GET /api/transactions
assert "Listar contiene la transacción" "1" "$(jq "[.[] | select(.id==$TX_ID)] | length" <<<"$BODY")"
req GET /api/categories
OCIO_ID="$(jq '.[] | select(.name=="Ocio") | .id' <<<"$BODY")"
req PATCH "/api/transactions/$TX_ID" "{\"categoryId\":$OCIO_ID}"
assert "PATCH -> 200" "200" "$STATUS"
assert "categorySource pasa a MANUAL" "MANUAL" "$(jq -r .categorySource <<<"$BODY")"
assert "mlConfidence se borra" "null" "$(jq -r .mlConfidence <<<"$BODY")"
req DELETE "/api/transactions/$TX_ID"
assert "DELETE -> 204" "204" "$STATUS"
req DELETE "/api/transactions/$TX_ID"
assert "DELETE repetido -> 404" "404" "$STATUS"

if $WITH_RESILIENCE; then
  echo "▶ 5. Resiliencia: ML caído"
  docker compose stop ml-service >/dev/null
  new_tx "Taxi al aeropuerto"
  assert "Con el ML parado sigue siendo 201" "201" "$STATUS"
  assert "categorySource = RULE" "RULE" "$(jq -r .categorySource <<<"$BODY")"
  assert "Categoría por reglas = Transporte" "Transporte" "$(jq -r .category.name <<<"$BODY")"
  DOWN_ID="$(jq -r .id <<<"$BODY")"
  new_tx "Compra sin palabras clave conocidas xyz"
  assert "Sin coincidencia -> Otros" "Otros" "$(jq -r .category.name <<<"$BODY")"
  OTHER_ID="$(jq -r .id <<<"$BODY")"

  echo "  … reiniciando el ML (el circuit breaker puede tardar hasta ~30 s en volver a cerrarse)"
  docker compose start ml-service >/dev/null
  RECOVERED=false
  for _ in $(seq 1 24); do
    sleep 5
    new_tx "Cena en restaurante italiano"
    REC_ID="$(jq -r .id <<<"$BODY")"
    if [[ "$(jq -r .categorySource <<<"$BODY")" == "ML" ]]; then RECOVERED=true; break; fi
    req DELETE "/api/transactions/$REC_ID"
  done
  assert "Tras reiniciar el ML vuelve a categorizar con ML" "true" "$RECOVERED"
  req DELETE "/api/transactions/$REC_ID" || true
  req DELETE "/api/transactions/$DOWN_ID" || true
  req DELETE "/api/transactions/$OTHER_ID" || true
fi

echo
echo "Resultado: $PASS correctas, $FAIL fallidas"
[[ "$FAIL" -eq 0 ]]
