# cyber-threat (vibe-coded with Claude)

Cyber threat intelligence tool built with Angular and Spring Boot.

Scheduled collectors pull from public sources (CISA KEV, Hacker News), deduplicate
and score the items, route actively-exploited vulnerabilities as critical alerts,
and serve a daily digest to an Angular front end.

## Repository layout

- `cyth-backend/` — Spring Boot API: collectors, scoring, digest endpoints
- `cyth-frontend/` — Angular app: digest page consuming the API

## Requirements

- Java 21
- Node.js + Angular CLI
- PostgreSQL running locally

## Database setup

Create the database (choose a real password, then provide it to the backend
through the `DB_PASSWORD` environment variable):

    CREATE DATABASE cyberthreat;
    CREATE USER cyberthreat WITH PASSWORD '<your-password>';
    GRANT ALL PRIVILEGES ON DATABASE cyberthreat TO cyberthreat;
    ALTER SCHEMA public OWNER TO cyberthreat;

On PostgreSQL 15+, the schema-ownership line is required so the app user can
create tables. If local connections fail with an "Ident authentication" error,
set the `127.0.0.1/32` and `::1/128` lines in `pg_hba.conf` to `md5` and reload
PostgreSQL.

## Running the backend

From `cyth-backend/`:

    ./gradlew bootRun

On startup the collectors run once (`app.collect.run-on-startup: true`), so the
digest has data immediately. Afterwards they run every hour.

## Running the frontend

From `cyth-frontend/`:

    ng serve

Then open http://localhost:4200. The backend must be running on port 8080.

## API

- `GET /api/digest` — stats, critical alerts, and the scored digest.
- `POST /api/items/{id}/action` — body `{ "action": "READ_LATER" | "ARCHIVE" | "IGNORE" }`.

Quick check:

    curl http://localhost:8080/api/digest | jq

## Backend structure

- `config/` — RestClient, CORS, global exception handler
- `item/` — Item entity, repository, service, controller, DTOs
- `collect/` — Collector interface, RawItem, scheduler, CISA KEV + Hacker News collectors
- `process/` — deduplication + ingestion, scoring and critical routing

Adding a source: implement `Collector`, annotate it with `@Component`. The
scheduler discovers it automatically — nothing else to change.

## Frontend structure

- `core/models/` — TypeScript mirrors of the API DTOs
- `core/services/` — `DigestService` (HTTP calls to the backend)
- `features/digest/` — digest page: stats, critical alerts, item cards + actions

## Roadmap

- LLM summaries (`SummaryService` via Spring AI + Ollama) — `Item.summary` is null for now.
- `Source` as a persisted entity with learnable weights (currently a string + in-memory
  map), so user actions (read later / archive / ignore) adjust scoring over time.
- RSS + GitHub Releases collectors (the Rome dependency is already present).
- Authentication before any deployment beyond localhost.
