# Pokemon GO Trade Tracker

React PWA (Netlify) + Spring Boot API (Render) + PostgreSQL (Neon). Design details are in `docs/architecture.md`.

```
backend/    Spring Boot 3, Java 17, Flyway, Dockerfile
frontend/   React + Vite PWA
netlify.toml, render.yaml, docker-compose.yml
```

## Run locally

```bash
docker compose up -d                 # local Postgres
cd backend && mvn test               # run the tests first
mvn spring-boot:run                  # API on http://localhost:8080, API key "dev-key"

cd ../frontend
cp .env.example .env.local
npm install && npm run dev           # UI on http://localhost:5173
```

## Deploy (all free tiers)

Do these in order, because each step needs a value from the one before.

### 1. Neon (database)
1. Create a project and a database.
2. Open the connection details and turn **Connection pooling off** (use the direct host, without `-pooler`). Flyway runs migrations on startup and works best on a direct connection.
3. Convert the connection string for JDBC. Neon gives `postgresql://USER:PASSWORD@HOST/DB?sslmode=require`. You need three separate values:
   - `SPRING_DATASOURCE_URL` = `jdbc:postgresql://HOST/DB?sslmode=require`
   - `SPRING_DATASOURCE_USERNAME` = `USER`
   - `SPRING_DATASOURCE_PASSWORD` = `PASSWORD`

### 2. Render (API)
Render has no native Java runtime, so the service builds from `backend/Dockerfile`.
1. Push this repo to GitHub.
2. New > Web Service > pick the repo. Language/Runtime: **Docker**. Dockerfile path `./backend/Dockerfile`, Docker build context `./backend`. Instance type: Free. (Or use New > Blueprint, which reads `render.yaml`.)
3. Health check path: `/api/v1/health`.
4. Environment variables:
   - `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` from step 1
   - `APP_API_KEY` = a long random string you make up (for example `openssl rand -hex 24`)
   - `APP_ALLOWED_ORIGIN` = your Netlify URL with no trailing slash, e.g. `https://your-site.netlify.app` (you can put a placeholder first and fix it after step 3)
   - `APP_TIMEZONE` = `Asia/Kolkata` (optional, this is the default)
5. Deploy, then open `https://<your-service>.onrender.com/api/v1/health`. You should see `{"status":"UP"}`. The first build takes several minutes.

### 3. Netlify (UI)
1. Add new site > Import from Git > pick the repo. `netlify.toml` already sets base `frontend`, build `npm run build`, publish `dist`.
2. Environment variables:
   - `VITE_API_BASE_URL` = `https://<your-service>.onrender.com` (no trailing slash, no `/api/v1`)
   - `VITE_API_KEY` = the same value as `APP_API_KEY`
3. Deploy. Then go back to Render and set `APP_ALLOWED_ORIGIN` to the final Netlify URL, and let Render redeploy.

Vite bakes `VITE_*` values in at build time, so after changing either one on Netlify, trigger a new deploy.

## Check it works
```bash
API=https://<your-service>.onrender.com
KEY=<your APP_API_KEY>

curl $API/api/v1/health                                   # {"status":"UP"}
curl -i $API/api/v1/deals                                 # 401 without the key
curl -H "X-API-Key: $KEY" $API/api/v1/dashboard/today     # JSON with empty lists
```
Then open the Netlify site, tap **Add deal**, save one, and confirm it shows on **Today**.

## Things to expect on free tiers
- Render sleeps when idle. The first request can take up to a minute; the UI shows a "Waking the server" banner after 3 seconds.
- Neon scales to zero, so the first query after a pause adds a short delay.
- `VITE_API_KEY` ends up in the JavaScript bundle, so anyone who opens dev tools can read it. It keeps casual visitors out, nothing more. Move to real login (architecture section 14) if the data matters.
- If the UI shows "Cannot reach the server", the usual causes are a wrong `VITE_API_BASE_URL`, a trailing slash or typo in `APP_ALLOWED_ORIGIN`, or the API still waking up.
- If the API shows "The API key was rejected", `VITE_API_KEY` and `APP_API_KEY` differ. Redeploy Netlify after fixing.
