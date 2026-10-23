# pkgwatch

Watches the npm packages your projects depend on and flags unusual changes in their daily downloads.

Point it at a `package.json`. A sudden collapse can mean a dependency was abandoned or deprecated, a spike can mean a hijacked or typosquatted release, and missing data means the stats feed itself broke. Each finding becomes a stored incident with a short explanation and a signed webhook alert.

Counts come from the public npm downloads API (https://api.npmjs.org). Two modes feed Kafka: live polling of your dependencies, and a replay of a year of history for a few popular packages (used for demos and tests; generated data is used if the API is unreachable).

## Flow

1. `LivePoller` reads the watchlist (a `package.json` plus any extra names), backfills recent days, then refreshes every few hours. `DownloadReplayer` is the demo alternative. Both publish one reading per package per day to `package.downloads`, keyed by package name. Sequence numbers are day numbers, so refetching a day is harmless: duplicates are skipped downstream.
2. `UsageListener` consumes the readings. Malformed or invalid messages go straight to `package.downloads.dlq`; other failures are retried with exponential backoff first.
3. `IngestionService` skips duplicates, runs the detectors, stores incidents in PostgreSQL, asks the model for a summary (a template is used if the model is unavailable) and sends a signed webhook.
4. `TriageAgent` can read metrics, open incidents and dead letters through a small tool loop and report on them. It can only acknowledge LOW and MEDIUM incidents.

## Run

```
docker compose up -d
LIVE_ENABLED=true WATCH_PACKAGE_JSON=/path/to/package.json mvn spring-boot:run
```

Use `REPLAY_ENABLED=true` instead of the live variables for the demo replay. Extra names can be added with `WATCH_PACKAGES=react,zod`.

Optional environment variables: `GEMINI_API_KEY`, `ALERT_WEBHOOK_URL`, `ALERT_WEBHOOK_SECRET`, `AGENT_ENABLED`.

| Endpoint | Purpose |
| --- | --- |
| `GET /api/incidents?openOnly=true` | incidents |
| `POST /api/incidents/{id}/ack` | acknowledge |
| `POST /api/agent/triage` | run the triage agent |
| `GET /actuator/health` | pipeline health |
| `GET /actuator/prometheus` | metrics |

Prometheus runs on port 9090 and Grafana on port 3000 (admin / admin) once the compose stack is up.

## Test

```
mvn test
```
