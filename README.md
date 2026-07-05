# PUBG Stats

A self-hosted dashboard that puts your PUBG stats next to the pros' so you can
see where you're bleeding rounds. It's a Spring Boot (Kotlin) app backed by
Postgres, rendering a server-side Thymeleaf dashboard.

With a `PUBG_API_KEY` set, it ingests real stats for the handles in
`PUBG_PLAYERS` on startup (cached, so repeat boots don't re-hit the API). With a
blank key it falls back to seed data so it runs end-to-end with zero external
calls.

## What it does

- Stores per-player stats: K/D, avg survival time, headshot %, win %, avg damage.
- Renders a dashboard at `/` showing **you** (first handle in `PUBG_PLAYERS`) vs
  a table of **pro** players (the rest).
- Ingests stats from the PUBG API on startup, caching each row for
  `pubg.cache-ttl-hours` (default 24h); a **Refresh stats** button (`POST /refresh`)
  re-ingests but still respects that cache TTL.
- Exposes a health check at `/actuator/health`.

## .env setup

Copy the example and adjust if needed:

```bash
cp .env.example .env
```

| Variable | Purpose | Default |
|----------|---------|---------|
| `SPRING_DATASOURCE_URL` | Postgres JDBC URL | `jdbc:postgresql://localhost:5432/pubgstats` |
| `SPRING_DATASOURCE_USERNAME` | DB user | `pubgstats` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | `pubgstats` |
| `PUBG_API_KEY` | PUBG API key — blank falls back to seed data | *(blank)* |
| `PUBG_SHARD` | PUBG platform shard, e.g. `steam` | `steam` |
| `PUBG_PLAYERS` | Comma-separated handles; first is **you**, the rest are pros | `shroud,chocoTaco,TGLTN,WackyJacky101,Pio` |

All the above are read by `application.yml`. A blank `PUBG_API_KEY` is fine —
the app falls back to seed data and won't crash.

## Run it

```bash
docker compose up --build
```

Then:

- Dashboard: http://localhost:8080/
- Health: http://localhost:8080/actuator/health → `{"status":"UP"}`

Tear down (including the DB volume):

```bash
docker compose down -v
```

## How coaching heuristics work

Coaching is comparison-driven: the dashboard shows your row against pro rows on
the same five metrics (K/D, avg survival, headshot %, win %, avg damage), so the
biggest gaps read straight off the page — e.g. a K/D of 1.8 against a pro median
near 5–6, or a 22% headshot rate against ~40%, points at aim and engagement
discipline before it points at anything else.

The seed data encodes the heuristic baseline: pros survive longer *and* deal
more damage, which is the pattern to chase — staying alive isn't enough if the
damage isn't there. Automated per-metric coaching tips (turning each gap into a
worded recommendation) build on this comparison and land in a later phase.
