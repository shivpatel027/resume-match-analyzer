# AI Resume–Job Match Analyzer

A full-stack web app that scores how well a resume fits a job description using an LLM, lists the
skills the job asks for that the resume is missing, and suggests targeted rewrites of resume bullets.
If the AI service is down (or no API key is set), it falls back to keyword matching so users still get a score.

**Stack:** Java 21, Spring Boot 3, Spring Security + JWT, Spring Data JPA, PostgreSQL, Apache PDFBox,
Caffeine cache, React 18 (Vite), Docker, GitHub Actions, Azure App Service.

## How it works

```
React (Vite) ──HTTP/JSON──▶ Spring Boot REST API ──▶ PostgreSQL (users, analysis history)
                                 │
              JWT filter ── rate limiter ── PDFBox parser
                                 │
                  Caffeine cache ─▶ Anthropic Messages API
                                 └─ (on failure) keyword matcher
```

1. The user uploads a PDF and pastes a job description.
2. `ResumeParser` checks the file really is a PDF (magic bytes, not just the extension) and extracts text with PDFBox.
3. `AnalysisService` hashes the normalized resume + job text (SHA-256) and calls `ClaudeMatchAnalyzer`, which is
   `@Cacheable` on that hash, so re-running an identical comparison never hits the paid API twice.
4. The model is asked for strict JSON (score, summary, matched/missing skills, bullet rewrites). `ClaudeResponseParser`
   validates it; anything malformed counts as a failure.
5. Any failure (no key, timeout, HTTP error, bad JSON) falls back to `KeywordMatchAnalyzer`, a whole-word matcher
   over ~40 skills with aliases (it won't match "Java" inside "JavaScript"). The result is labelled so the UI can say so.
6. The result is saved to the user's history in PostgreSQL.

`POST /api/analyses` is rate limited per user with an in-memory token bucket (default 5 per minute, returns `429`
with `Retry-After`).

## API

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | – | `{name, email, password}` → `{token, user}` |
| POST | `/api/auth/login` | – | `{email, password}` → `{token, user}` |
| GET | `/api/auth/me` | JWT | Current user |
| POST | `/api/analyses` | JWT | multipart: `resume` (PDF), `jobDescription` → full analysis |
| GET | `/api/analyses` | JWT | Your 50 most recent analyses |
| GET | `/api/analyses/{id}` | JWT | One analysis (only your own) |
| DELETE | `/api/analyses/{id}` | JWT | Delete one |
| GET | `/actuator/health` | – | Health check for Azure |

Errors use RFC 7807 problem JSON (`{"status": 400, "detail": "..."}`).

## Run locally

### Everything in Docker (easiest)

```bash
cp .env.example .env          # add your ANTHROPIC_API_KEY, or leave it empty to try the fallback
docker compose up --build
```

Open http://localhost:8080.

### Dev mode (hot reload)

```bash
docker compose up db                              # just PostgreSQL

cd backend
export ANTHROPIC_API_KEY=sk-ant-...               # optional
./mvnw spring-boot:run                            # API on :8080

cd ../frontend
npm install
npm run dev                                       # UI on :5173, proxies /api to :8080
```

### Tests

```bash
cd backend && ./mvnw test
```

Unit tests cover the keyword matcher, LLM response parsing, the AI-to-fallback switch, the rate limiter and JWT
handling; integration tests boot the app on in-memory H2 and check auth flows with MockMvc.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | local Postgres | Database connection |
| `JWT_SECRET` | dev-only value | HMAC key, at least 32 characters. **Set this in production.** |
| `JWT_EXPIRATION` | `PT2H` | Token lifetime (ISO-8601 duration) |
| `ANTHROPIC_API_KEY` | empty | Empty = always use keyword fallback |
| `ANTHROPIC_MODEL` | `claude-haiku-5-5` | Model used for analysis |
| `RATE_LIMIT_CAPACITY`, `RATE_LIMIT_REFILL_PERIOD` | `5`, `PT1M` | Analyses allowed per user per period |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Only needed if the UI is served from another origin |

## Deploy to Azure

The `Dockerfile` builds the React app, bundles it into the Spring Boot jar, and produces one image. GitHub Actions
runs the tests on every push and PR; on `main` it builds the image, pushes it to Azure Container Registry, and
deploys it to an App Service for Containers.

One-time setup with the Azure CLI:

```bash
RG=resume-match-rg
LOC=eastus
ACR=resumematch$RANDOM          # must be globally unique
APP=resume-match-<yourname>     # must be globally unique
PG=resume-match-pg-<yourname>

az group create -n $RG -l $LOC
az acr create -g $RG -n $ACR --sku Basic

az postgres flexible-server create -g $RG -n $PG \
  --tier Burstable --sku-name Standard_B1ms \
  --admin-user rmaadmin --admin-password '<strong-password>' \
  --public-access 0.0.0.0 --database-name resume_analyzer

az appservice plan create -g $RG -n resume-match-plan --is-linux --sku B1
az webapp create -g $RG -p resume-match-plan -n $APP \
  --container-image-name mcr.microsoft.com/appsvc/staticsite:latest

# Let the web app pull from ACR with its managed identity
PRINCIPAL=$(az webapp identity assign -g $RG -n $APP --query principalId -o tsv)
az role assignment create --assignee $PRINCIPAL --role AcrPull \
  --scope $(az acr show -n $ACR --query id -o tsv)
az webapp config set -g $RG -n $APP --generic-configurations '{"acrUseManagedIdentityCreds": true}'

az webapp config appsettings set -g $RG -n $APP --settings \
  WEBSITES_PORT=8080 \
  DB_URL="jdbc:postgresql://$PG.postgres.database.azure.com:5432/resume_analyzer?sslmode=require" \
  DB_USERNAME=rmaadmin DB_PASSWORD='<strong-password>' \
  JWT_SECRET="$(openssl rand -base64 48)" \
  ANTHROPIC_API_KEY='<your-key>'

# Credentials for GitHub Actions
az ad sp create-for-rbac --name resume-match-github --role contributor \
  --scopes $(az group show -n $RG --query id -o tsv) --json-auth
```

In the GitHub repo settings:

- **Secrets:** `AZURE_CREDENTIALS` = the JSON printed by the last command.
- **Variables:** `ACR_NAME` = `$ACR`, `AZURE_WEBAPP_NAME` = `$APP`.

Push to `main` and the pipeline deploys. In the App Service, set the health check path to `/actuator/health`.

## Design notes

- **Fallback, not failure.** The LLM is the slow, paid, occasionally-down dependency, so every failure mode is
  converted into one exception type and handled in one place.
- **Prompt injection.** Resume and job text are wrapped in tags and the system prompt tells the model to treat them as
  data. The model is also told never to invent experience in rewrites.
- **Ownership checks.** Analyses are always looked up by `id` *and* the caller's email, so guessing another id returns 404.
- **Single instance assumptions.** The cache and rate limiter are in-memory. Scaling out would move both to Redis.

## Credits

Started from [ayush26git/ai-resume-analyzer](https://github.com/ayush26git/ai-resume-analyzer) (PDFBox parsing,
basic keyword matching and registration), then rebuilt with the LLM integration, Spring Security filter chain,
persistence of history, caching, rate limiting, React UI, tests, Docker and CI/CD.
