# 🌿 Guía de Git y GitHub

Cómo subir **Smart Finance Tracker** a GitHub y trabajar en él de forma profesional.

## 0. Antes de empezar

```bash
git --version                      # cualquier versión reciente
git config --global user.name  "Tu Nombre"
git config --global user.email "tu-email@ejemplo.com"
```

Usa el mismo email que tengas verificado en GitHub para que los commits se asocien a tu perfil.

## 1. Preparar el repositorio

Desde la raíz del proyecto (`smart-finance-tracker/`):

```bash
git init -b main
```

El proyecto ya trae dos ficheros que debes conservar:

| Fichero | Para qué sirve |
|---|---|
| `.gitignore` | Excluye `.env` (secretos), `node_modules/`, `target/`, `__pycache__/`, `*.joblib`, `pgdata/`, etc. |
| `.gitattributes` | Fuerza finales de línea LF en `*.sh` y `*.sql` (con CRLF de Windows, los scripts fallan en Linux/Docker) |

> ℹ️ `*.joblib` está ignorado a propósito: el modelo se entrena durante el build de la imagen del ML service.
> `ml-service-python/models/.gitkeep` mantiene la carpeta en el repo (el `Dockerfile` hace `COPY models`, y fallaría si no existiera).
> El dataset `app/data/seed_transactions.csv` **sí** debe versionarse.

### ⚠️ Comprueba que no subes secretos

```bash
git status --short                 # no debe aparecer ".env"
git check-ignore -v .env           # debe decir que lo ignora .gitignore
```

Si alguna vez subes un secreto por error, **borrarlo del historial no basta: rota la clave** (`JWT_SECRET`, `INTERNAL_API_KEY`, contraseñas).

## 2. Primer commit

```bash
git add .
git status                         # revisa la lista: nada de .env, node_modules, target...
git commit -m "feat: initial project structure (monorepo)"
```

Antes de dar el paso, conviene fijar el nombre del autor en `LICENSE` y `README.md` (busca `<TU NOMBRE>`, `<tu-usuario>`, `<tu-perfil>`).

Para que el script de humo conserve el permiso de ejecución (sobre todo si trabajas en Windows):

```bash
git update-index --chmod=+x scripts/smoke-test.sh
```

## 3. Crear el repositorio en GitHub

1. GitHub → **New repository**.
2. **Repository name:** `smart-finance-tracker`.
3. Visibilidad: *Public* (portfolio) o *Private*.
4. ⚠️ **No marques** *Add a README*, *.gitignore* ni *license*: el proyecto ya los tiene y generarían un conflicto en el primer push.
5. **Create repository**.

*Alternativa con GitHub CLI* (crea el repo, añade el remoto y sube en un solo paso):

```bash
gh auth login
gh repo create smart-finance-tracker --public --source=. --remote=origin --push
```

## 4. Vincular y subir

```bash
git remote add origin https://github.com/<tu-usuario>/smart-finance-tracker.git
git branch -M main
git push -u origin main
```

**Autenticación.** GitHub ya no acepta la contraseña de la cuenta por HTTPS. Elige una opción:

- **HTTPS + Personal Access Token:** al pedir contraseña, pega un token (Settings → Developer settings → Personal access tokens, con permiso `repo`). El *Git Credential Manager* lo recuerda.
- **SSH:** `ssh-keygen -t ed25519 -C "tu-email@ejemplo.com"`, añade la clave pública en Settings → SSH keys y usa
  `git remote add origin git@github.com:<tu-usuario>/smart-finance-tracker.git`.
- **GitHub CLI:** `gh auth login` configura las credenciales por ti.

Comprueba el remoto con `git remote -v`.

## 5. Flujo de trabajo (Git Flow simplificado)

`main` siempre debe estar en un estado que arranque con `docker compose up --build`. Todo cambio entra mediante una rama corta y un Pull Request.

```text
main ──●────────────●──────────●──▶   (siempre estable)
        \          / \        /
         feature/…●   fix/…──●
```

### Ramas

```bash
git checkout main && git pull
git checkout -b feature/ml-integration      # o: git switch -c feature/ml-integration
```

Convención de nombres: `feature/<tema>`, `fix/<tema>`, `docs/<tema>`, `test/<tema>`, `chore/<tema>`.

### Commits semánticos ([Conventional Commits](https://www.conventionalcommits.org/es/))

| Prefijo | Cuándo | Ejemplo |
|---|---|---|
| `feat:` | Funcionalidad nueva | `feat(backend): add ML fallback service` |
| `fix:` | Corrección de un bug | `fix(frontend): redirect to login on 401` |
| `docs:` | Solo documentación | `docs: add testing guide` |
| `test:` | Añadir o corregir tests | `test(ml): cover missing-model fallback` |
| `chore:` | Mantenimiento, dependencias, config | `chore: bump spring-boot to 3.3.6` |
| `refactor:` | Cambio interno sin alterar comportamiento | `refactor(ml): extract pipeline builder` |

Formato: `tipo(ámbito opcional): descripción en imperativo y en minúsculas`. El ámbito puede ser `backend`, `ml`, `frontend` o `infra`.
Un commit = un cambio con sentido; evita mezclar un `feat` del backend con uno del frontend en el mismo commit.

```bash
git add -p                                   # revisa y elige qué incluir
git commit -m "feat(backend): add MlClient with 800ms timeout"
git push -u origin feature/ml-integration    # la primera vez; después basta `git push`
```

### Pull Request

1. En GitHub aparecerá *Compare & pull request*, o desde la terminal: `gh pr create --fill`.
2. Describe **qué** cambia y **cómo lo probaste** (por ejemplo, qué secciones de [TESTING.md](TESTING.md) ejecutaste).
3. Revisa la pestaña *Files changed* (aunque seas el único desarrollador: simula el flujo y detecta secretos o ficheros sobrantes).
4. **Squash and merge** mantiene un historial limpio en `main`.
5. Limpia: `git checkout main && git pull && git branch -d feature/ml-integration`.

### Buenas prácticas

- Activa **branch protection** en `main` (Settings → Branches): exigir PR y, cuando exista CI, que pase antes de fusionar.
- No hagas commits directos a `main` tras el primero.
- No subas `.env`, modelos entrenados, `node_modules/` ni `target/`.
- Etiqueta versiones estables: `git tag -a v0.1.0 -m "MVP" && git push origin v0.1.0`.
- Si dos ramas tocan lo mismo, resuelve conflictos con `git pull --rebase origin main` y vuelve a ejecutar los tests.

## 6. Referencia rápida

```bash
git status                         # estado del árbol de trabajo
git log --oneline --graph -15      # historial compacto
git diff                           # cambios sin añadir
git restore <fichero>              # descartar cambios locales de un fichero
git stash / git stash pop          # guardar cambios temporalmente
git commit --amend                 # corregir el último commit (solo si NO lo has subido)
```
