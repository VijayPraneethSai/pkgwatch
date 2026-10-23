# pkgwatch

Watches daily download counts of open-source packages and flags spikes, drifts and gaps.

Download history is replayed from the public npm downloads API (https://api.npmjs.org) into Kafka. If the API is unreachable, generated data is used instead.

## Flow

1. `DownloadReplayer` publishes one reading per package per day to `package.downloads`, keyed by package name.
2. `UsageListener` consumes the readings. Malformed or invalid messages go straight to `package.downloads.dlq`; other failures are retried with exponential backoff first.
3. `IngestionService` skips duplicates, runs the detectors, stores incidents in PostgreSQL, asks the model for a summary (a template is used if the model is unavailable) and sends a signed webhook.
4. `TriageAgent` can read metrics, open incidents and dead letters through a small tool loop and report on them. It can only acknowledge LOW and MEDIUM incidents.

## Run

```
docker compose up -d
REPLAY_ENABLED=true mvn spring-boot:run
```

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
