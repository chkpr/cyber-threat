# cyber-threat — backend

Cyber threat intelligence watch tool. Spring Boot backend: scheduled collectors
pull from public sources, dedup + score them, route critical items, and serve a
daily digest to the Angular front.

## Requirements

- Java 21
- PostgreSQL running locally (or set `DB_URL` / `DB_USER` / `DB_PASSWORD`)

Create the database:

```sql
CREATE DATABASE cyberthreat;
CREATE USER cyberthreat WITH PASSWORD 'cyberthreat';
GRANT ALL PRIVILEGES ON DATABASE cyberthreat TO cyberthreat;
```

## Run

```bash
./mvnw spring-boot:run
```

On startup the collectors run once (`app.collect.run-on-startup: true`), so the
digest has data immediately. Afterwards they run every hour.

## API

- `GET /api/digest` — stats, critical alerts, and the scored digest.
- `POST /api/items/{id}/action` — body `{ "action": "READ_LATER" | "ARCHIVE" | "IGNORE" }`.

Quick check:

```bash
curl http://localhost:8080/api/digest | jq
```

## Structure

```
config/    RestClient + CORS
item/      Item entity, repository, service, controller, DTOs
collect/   Collector interface, RawItem, scheduler, CISA KEV + Hacker News
process/   dedup + ingestion, scoring / critical routing
```

## Adding a source

Implement `Collector`, annotate with `@Component`. The scheduler picks it up
automatically — nothing else to change.

## Not wired yet

- LLM summaries (`SummaryService` via Spring AI) — `Item.summary` stays null.
- `Source` as an entity with learnable weights (currently a string + in-memory map).
- RSS + GitHub Releases collectors (Rome dependency is already present).
```
