# 🧪 Guía de pruebas — Smart Finance Tracker

Cómo verificar el sistema capa por capa, con el resultado esperado en cada paso.

- ⚡ **Atajo:** `./scripts/smoke-test.sh` automatiza las secciones 2 y 3 (y la 5 con `--resilience`). Requiere `curl` y `jq`.
- 🪟 **Windows:** los comandos `curl` de esta guía usan sintaxis bash. Ejecútalos en Git Bash o WSL (en PowerShell las comillas fallan), o usa Postman.
- Los valores de confianza del ML (`0.973`…) son orientativos y pueden variar.

## Índice

1. [Infraestructura (Docker)](#1-infraestructura-docker)
2. [ML Service (FastAPI)](#2-ml-service-fastapi)
3. [Backend (Spring Boot)](#3-backend-spring-boot)
4. [Resiliencia: ML caído](#4-resiliencia-ml-caído)
5. [Frontend (React)](#5-frontend-react)
6. [Pruebas automatizadas](#6-pruebas-automatizadas)
7. [Checklist final](#7-checklist-final)
8. [Problemas frecuentes](#8-problemas-frecuentes)

---

## 1. Infraestructura (Docker)

```bash
cp .env.example .env          # solo la primera vez
docker compose up --build
```

En otra terminal:

```bash
docker compose ps
```

**Esperado:** los cuatro servicios (`postgres`, `ml-service`, `backend`, `frontend`) en estado `Up ... (healthy)`.
El backend puede tardar ~40 s en pasar de `starting` a `healthy` (Flyway + arranque de Spring).

### Healthchecks

| Servicio | Comando | Esperado |
|---|---|---|
| PostgreSQL | `docker compose exec postgres pg_isready -U sft -d sft` | `... - accepting connections` |
| ML Service | `curl -s http://localhost:8000/health` | `{"status":"ok","model_loaded":true}` |
| Backend | `curl -s http://localhost:8080/actuator/health` | `{"status":"UP",...}` |

(Se usa `docker compose exec <servicio>` en lugar de `docker exec <contenedor>` porque no depende del nombre del contenedor, que cambia según la carpeta del proyecto.)

### Migraciones Flyway

```bash
docker compose exec postgres psql -U sft -d sft -c \
  "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
docker compose exec postgres psql -U sft -d sft -c \
  "SELECT count(*) AS categorias_sistema FROM categories WHERE is_system;"
```

**Esperado:** dos filas (`1 init schema`, `2 seed categories`) con `success = t`, y `12` categorías del sistema.

---

## 2. ML Service (FastAPI)

Carga tu clave en el shell:

```bash
export KEY=$(grep '^INTERNAL_API_KEY=' .env | cut -d= -f2-)
```

| # | Prueba | Comando | Esperado |
|---|---|---|---|
| 2.1 | Health (sin clave) | `curl -s localhost:8000/health` | `200` · `{"status":"ok","model_loaded":true}` |
| 2.2 | Predicción con clave | ver abajo | `200` · `{"category":"Restaurantes","confidence":0.97,"source":"ML"}` (o similar) |
| 2.3 | Sin clave | ver abajo | `403` |
| 2.4 | Clave incorrecta | ver abajo | `403` |

```bash
# 2.2 — con clave
curl -s -X POST http://localhost:8000/predict \
  -H "Content-Type: application/json" -H "X-Internal-Api-Key: $KEY" \
  -d '{"description": "Cena en restaurante italiano", "amount": 42.50}'

# 2.3 — sin clave: debe ser 403
curl -s -o /dev/null -w '%{http_code}\n' -X POST http://localhost:8000/predict \
  -H "Content-Type: application/json" -d '{"description": "Cena", "amount": 10}'

# 2.4 — clave incorrecta: debe ser 403
curl -s -o /dev/null -w '%{http_code}\n' -X POST http://localhost:8000/predict \
  -H "Content-Type: application/json" -H "X-Internal-Api-Key: incorrecta" \
  -d '{"description": "Cena", "amount": 10}'
```

### 2.5 Fallback interno: el modelo no está disponible

El modelo se carga **una vez al arrancar**, así que hay que reiniciar el servicio después de moverlo:

```bash
docker compose exec ml-service mv /app/models/category_model.joblib /app/models/category_model.joblib.bak
docker compose restart ml-service

curl -s http://localhost:8000/health        # {"status":"ok","model_loaded":false}
curl -s -i -X POST http://localhost:8000/predict \
  -H "Content-Type: application/json" -H "X-Internal-Api-Key: $KEY" \
  -d '{"description": "Cena en restaurante italiano", "amount": 42.50}'
```

**Esperado:** `HTTP/1.1 200 OK` (nunca 500) y cuerpo `{"category":"Otros","confidence":0.0,"source":"RULE"}`.

Restaura el modelo:

```bash
docker compose exec ml-service mv /app/models/category_model.joblib.bak /app/models/category_model.joblib
docker compose restart ml-service
curl -s http://localhost:8000/health        # model_loaded: true
```

> Si algo sale mal, `docker compose up --build -d ml-service` recrea la imagen y reentrena el modelo.

---

## 3. Backend (Spring Boot)

Swagger interactivo: http://localhost:8080/swagger-ui.html (botón *Authorize* para pegar el JWT).

### 3.1 Autenticación

```bash
API=http://localhost:8080
EMAIL="prueba-$RANDOM@example.com"

# Registro -> 201 con token
curl -s -i -X POST $API/auth/register -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"password123\",\"displayName\":\"Prueba\"}"

# Login -> 200; guarda el token
TOKEN=$(curl -s -X POST $API/auth/login -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"password123\"}" | jq -r .token)
echo "$TOKEN"
```

| Prueba | Esperado |
|---|---|
| Registro | `201` · `{"token":"...","tokenType":"Bearer","expiresIn":3600,"user":{...}}` |
| Registro repetido (mismo email) | `409` |
| Contraseña de menos de 8 caracteres | `400` con `fieldErrors` |
| Login correcto | `200` y un JWT (3 partes separadas por puntos) |
| Login con contraseña incorrecta | `401` |
| `GET /api/transactions` sin token | `401` |
| `GET /api/transactions` con token manipulado | `401` |

### 3.2 Transacciones (CRUD)

```bash
AUTH="Authorization: Bearer $TOKEN"

# CREAR con categoría automática
curl -s -i -X POST $API/api/transactions -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"amount":42.50,"type":"EXPENSE","description":"Cena en restaurante italiano","occurredOn":"2026-09-20","categoryId":null}'
```

**Esperado:** `201 Created`, `category.name = "Restaurantes"`, `categorySource = "ML"` y `mlConfidence > 0`.
Anota el `id` devuelto:

```bash
ID=1   # sustituye por el id real

# LISTAR (admite ?from=2026-09-01&to=2026-09-30&categoryId=...)
curl -s $API/api/transactions -H "$AUTH" | jq

# PATCH: cambiar la categoría a mano -> categorySource pasa a MANUAL
OCIO=$(curl -s $API/api/categories -H "$AUTH" | jq '.[] | select(.name=="Ocio") | .id')
curl -s -X PATCH $API/api/transactions/$ID -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"categoryId\": $OCIO}" | jq '{category: .category.name, categorySource, mlConfidence}'
# -> { "category": "Ocio", "categorySource": "MANUAL", "mlConfidence": null }

# RECATEGORIZAR con ML (vuelve a ML)
curl -s -X POST $API/api/transactions/$ID/recategorize -H "$AUTH" | jq '{category: .category.name, categorySource}'

# BORRAR -> 204; repetirlo -> 404
curl -s -o /dev/null -w '%{http_code}\n' -X DELETE $API/api/transactions/$ID -H "$AUTH"
curl -s -o /dev/null -w '%{http_code}\n' -X DELETE $API/api/transactions/$ID -H "$AUTH"
```

| Prueba | Esperado |
|---|---|
| Crear con `categoryId: null` | `201` · `ML` · `mlConfidence > 0` |
| Crear con `categoryId` explícito | `201` · `MANUAL` (no se llama al ML) |
| Crear con `amount: 0` o `-5` | `400` |
| `PATCH` con `categoryId` | `categorySource: "MANUAL"` y `mlConfidence: null` |
| `DELETE` | `204`, y luego `404` |

### 3.3 Aislamiento entre usuarios

Registra un segundo usuario, obtén su token y pide la transacción del primero:

```bash
curl -s -o /dev/null -w '%{http_code}\n' -X DELETE $API/api/transactions/$ID -H "Authorization: Bearer $TOKEN_USUARIO_2"
```

**Esperado:** `404` (no `403`: no se revela que existe).

### 3.4 CORS

```bash
# Origen permitido -> 200 con Access-Control-Allow-Origin
curl -s -i -X OPTIONS $API/api/transactions \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: authorization,content-type" | grep -i -E 'HTTP/|access-control'

# Origen no permitido -> 403
curl -s -o /dev/null -w '%{http_code}\n' -X OPTIONS $API/api/transactions \
  -H "Origin: http://evil.example" -H "Access-Control-Request-Method: POST"
```

### 3.5 Presupuestos y reportes

```bash
# Crear límite de 100 € para Restaurantes en septiembre de 2026
REST=$(curl -s $API/api/categories -H "$AUTH" | jq '.[] | select(.name=="Restaurantes") | .id')
curl -s -X PUT $API/api/budgets -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"categoryId\": $REST, \"month\": \"2026-09-01\", \"limitAmount\": 100}" | jq

curl -s "$API/api/budgets?month=2026-09-01" -H "$AUTH" | jq        # incluye "spent"
curl -s "$API/api/reports/monthly?year=2026" -H "$AUTH" | jq       # 12 meses (con ceros)
curl -s "$API/api/reports/by-category?from=2026-09-01&to=2026-09-30" -H "$AUTH" | jq
```

**Esperado:** repetir el `PUT` con otro límite **actualiza** el presupuesto (no crea un duplicado); `spent` refleja los gastos
de esa categoría en el mes.

---

## 4. Resiliencia: ML caído

Es la propiedad más importante del diseño: **un fallo del ML nunca debe convertirse en un 500.**

```bash
docker compose stop ml-service

curl -s -i -X POST $API/api/transactions -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"amount":30,"type":"EXPENSE","description":"Taxi al aeropuerto","occurredOn":"2026-09-20","categoryId":null}'
```

**Esperado:**

| Campo | Valor |
|---|---|
| Estado HTTP | `201 Created` |
| `category.name` | `Transporte` (regla `taxi`) |
| `categorySource` | `RULE` |
| `mlConfidence` | `null` |

Prueba también una descripción sin palabras clave conocidas (por ejemplo `"Pago varios xyz"`): debe asignar **`Otros`** con `RULE`.
Y una descripción por regla distinta, como `"Cena en restaurante"` → `Restaurantes`.

Recupera el ML:

```bash
docker compose start ml-service
```

> ⏱️ **Circuit breaker.** Tras varios fallos seguidos (mínimo 5 llamadas, ≥ 50 % de error) el backend abre el circuito y
> deja de llamar al ML durante **30 s**. Si haces una transacción justo después de `start ml-service`, puede salir otra vez
> como `RULE`. Espera ~30 s y repite: volverá a `ML`. Es el comportamiento esperado, no un fallo.

### Otras variantes de fallo

| Escenario | Cómo provocarlo | Esperado |
|---|---|---|
| ML lento (> 800 ms) | difícil de reproducir a mano; lo cubre `MlClientTest` (WireMock con 2 s de retraso) | `201` + `RULE` |
| ML sin modelo | sección 2.5 | `201` + `RULE` (Spring descarta la respuesta `source: RULE` del ML y usa sus propias reglas) |
| API key errónea entre servicios | cambia `INTERNAL_API_KEY` solo en un servicio y reinicia | `201` + `RULE` (el ML responde 403) |

---

## 5. Frontend (React)

Abre http://localhost:5173.

### Flujo completo

1. **Registro / login.** Crea una cuenta (contraseña ≥ 8 caracteres). Debes acabar en *Resumen*.
2. **Transacciones.** Crea una con descripción `Cena en restaurante`, importe `42,50`, categoría **Automática**.
   - ✅ Aparece un aviso *"Guardada en [Restaurantes] ML 9x %"* y una fila con el badge de la categoría
     (punto de color + nombre) y una etiqueta de origen: `ML xx %`, `Regla` o `Manual`.
3. **Cambiar categoría.** En la fila, usa *Cambiar categoría…* → el badge pasa a `Manual`.
   *Reclasificar* vuelve a pedirla al ML.
4. **Resumen (dashboard).** ✅ Las tarjetas de Gastos y Balance y el gráfico por categoría reflejan la transacción.
5. **Presupuestos.** Fija `100` € para *Restaurantes* en el mes actual. ✅ La barra de progreso muestra lo gastado
   (42,50 € de 100 €) y se vuelve ámbar a partir del 80 % y roja al pasarse.

### Sesión y seguridad en el navegador

| Prueba | Cómo | Esperado |
|---|---|---|
| Persistencia | Recarga con **F5** | Sigues dentro (el token está en `localStorage`, clave `sft.token`) |
| Cierre de sesión | *Cerrar sesión* | Vuelves a `/login`; `sft.token` desaparece |
| Ruta protegida | Cerrada la sesión, abre `/transactions` | Redirige a `/login` y, tras entrar, vuelve a `/transactions` |
| Token caducado | DevTools → Application → Local Storage → altera `sft.token`, o arranca el backend con `JWT_EXPIRATION_MINUTES=1` y espera | Siguiente petición → `401` → redirige a `/login` |
| ML caído | `docker compose stop ml-service` y crea una transacción | Se guarda igualmente, con etiqueta **Regla** |
| Red | DevTools → Network | Ninguna petición va al puerto 8000: el navegador solo habla con el 8080 |

---

## 6. Pruebas automatizadas

| Capa | Comando | Qué cubre |
|---|---|---|
| Backend | `cd backend-java && mvn test` | `TransactionServiceTest` (Mockito): manual, ML, ML caído, categoría desconocida, ML sin modelo, baja confianza · `MlClientTest` (**WireMock**): cabecera `X-Internal-Api-Key`, timeout de 800 ms, 500, 403, conexión rechazada, cuerpo vacío · `MlFallbackServiceTest`: reglas |
| ML | `cd ml-service-python && pip install -r requirements-dev.txt && pytest` | `/health` sin clave; `/predict` 403 sin clave o con clave errónea; predicción real; sin modelo → `Otros/0.0/RULE`; modelo roto → 200; servicio sin clave configurada → 403 |
| Frontend | `cd frontend-react && npm install && npm run typecheck && npm run build` | Comprobación de tipos y build de producción |

**Sin instalar Java ni Python** (solo Docker):

```bash
# Backend
docker run --rm -v "$PWD/backend-java":/app -v "$HOME/.m2":/root/.m2 -w /app \
  maven:3.9-eclipse-temurin-21 mvn -B test

# ML service
docker run --rm -v "$PWD/ml-service-python":/app -w /app python:3.12-slim \
  sh -c "pip install -q -r requirements-dev.txt && pytest"
```

> ℹ️ El proyecto **no incluye Maven Wrapper** (`./mvnw`). Si lo prefieres: `cd backend-java && mvn wrapper:wrapper`.
> Tampoco incluye aún tests de frontend (`npm run test`/Vitest): están en el [roadmap](README.md#-roadmap).

---

## 7. Checklist final

- [ ] `docker compose ps` → 4 servicios `healthy`
- [ ] Flyway: V1 y V2 aplicadas; 12 categorías de sistema
- [ ] ML: 200 con clave, **403** sin ella o con clave incorrecta
- [ ] ML sin modelo → `200 {"category":"Otros","confidence":0.0,"source":"RULE"}`
- [ ] Backend: registro, login y `401` sin token
- [ ] `POST /api/transactions` con `categoryId: null` → `201`, `ML`, `mlConfidence > 0`
- [ ] `PATCH` con categoría → `MANUAL`
- [ ] Con el ML parado → `201`, `RULE`, `Transporte` para "Taxi al aeropuerto"
- [ ] CORS: `localhost:5173` permitido, otros orígenes rechazados
- [ ] Frontend: flujo completo, F5 mantiene la sesión, `401` redirige a login
- [ ] `mvn test` y `pytest` en verde

## 8. Problemas frecuentes

| Síntoma | Causa probable |
|---|---|
| `docker compose up` falla con *Define JWT_SECRET* / *Define INTERNAL_API_KEY* | Falta el fichero `.env` (`cp .env.example .env`) |
| El backend se reinicia con *JWT_SECRET debe tener al menos 32 caracteres* | Secreto demasiado corto |
| Todas las transacciones salen como `RULE` aunque el ML esté arriba | `INTERNAL_API_KEY` distinta entre servicios (el ML responde 403), circuito abierto (espera 30 s), o el ML arrancó sin modelo (`model_loaded: false`) |
| El frontend carga pero el login falla con *No se pudo conectar con el servidor* | Backend aún arrancando, o `VITE_API_URL` no apunta a `http://localhost:8080` |
| Error de CORS en el navegador | Frontend servido desde un origen distinto de `http://localhost:5173`; ajusta `CORS_ALLOWED_ORIGINS` |
| Puerto 5432 / 8080 / 5173 ocupado | Para el servicio local que lo usa o cambia el mapeo en `docker-compose.yml` |
| Cambios en `.env` que no se aplican | `docker compose up -d --force-recreate` |
| Empezar de cero (borra la BD) | `docker compose down -v && docker compose up --build` |
