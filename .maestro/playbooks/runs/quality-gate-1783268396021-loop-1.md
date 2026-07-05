# Quality Gates

Run before considering any phase "done". Each must pass.

## Build & tests
- [x] `JAVA_HOME=<JDK 21/23> ./gradlew build` — compiles + runs all tests, BUILD SUCCESSFUL
      (host JDK 25 is rejected by the Gradle 8.10.2 launcher — use JDK 21 or 23)
      <!-- Verified with JDK 23.0.2: BUILD SUCCESSFUL, all tests pass. -->
- [x] No test skipped or `@Disabled` without a `// ponytail:` note saying why
      <!-- Verified: grep for @Disabled/@Ignore/assumeTrue in src returns nothing. All 8 @Test methods across 3 Kotlin test files are active. -->


## App actually runs (the real e2e gate)
- [x] `docker compose up --build -d` from clean state comes up healthy
- [x] `curl -sf localhost:8080/actuator/health` returns `{"status":"UP"}`
- [x] `curl -sf localhost:8080/` returns 200 and the dashboard HTML (not a stack trace)
- [x] `docker compose down -v` — cleans up
      <!-- Verified 2026-07-05: down -v from clean → up --build -d, db healthy then app started (~57s build+up).
           /actuator/health → {"status":"UP"}. / → 200 serving dashboard HTML (PUBG Stats page, not a stack trace).
           down -v removed containers, network, and init_pgdata volume; `docker compose ps -a` empty afterwards. -->


## Config hygiene
- [x] Every env var read by application.yml is documented in `.env.example`
      <!-- Verified: application.yml reads SPRING_DATASOURCE_{URL,USERNAME,PASSWORD}, PUBG_API_KEY,
           PUBG_SHARD, PUBG_PLAYERS. PUBG_PLAYERS was missing from .env.example — added. -->

- [x] App boots with a blank PUBG_API_KEY (falls back to seed data, no crash)
      <!-- Verified with JDK 23.0.2: added DataSeederTest covering the DataSeeder branch —
           blank apiKey seeds sample data (never calls ingestAll), does not re-seed when rows
           already exist, and a set key ingests real stats instead. BUILD SUCCESSFUL, all tests pass. -->


## Docs
- [x] README covers: what it does, .env setup, `docker compose up --build`, how coaching heuristics work
      <!-- Verified 2026-07-05: README has all four sections. De-staled the "later phase" language now
           that ingestion is live, documented PUBG_PLAYERS in the env table, and described the startup
           ingestion + 24h cache TTL + POST /refresh button. .env.example already documents PUBG_PLAYERS. -->