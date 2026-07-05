# Quality Gates

Run before considering any phase "done". Each must pass.

## Build & tests
- [x] `JAVA_HOME=<JDK 21/23> ./gradlew build` — compiles + runs all tests, BUILD SUCCESSFUL
      (host JDK 25 is rejected by the Gradle 8.10.2 launcher — use JDK 21 or 23)
      <!-- BUILD SUCCESSFUL with JDK 23.0.2. All tasks ran/up-to-date, tests pass. -->
- [x] No test skipped or `@Disabled` without a `// ponytail:` note saying why
      <!-- Verified: grep for @Disabled/@Ignore/assumeTrue/@Skip across src/test found zero hits.
           Only test file is PlayerStatsRepositoryTest.kt (2 active @Test cases). No skipped tests exist. -->


## App actually runs (the real e2e gate)
- [x] `docker compose up --build -d` from clean state comes up healthy
- [x] `curl -sf localhost:8080/actuator/health` returns `{"status":"UP"}`
- [x] `curl -sf localhost:8080/` returns 200 and the dashboard HTML (not a stack trace)
- [x] `docker compose down -v` — cleans up
      <!-- e2e gate run 2026-07-05. `docker compose up --build -d`: image built, db reported Healthy,
           app started. Health endpoint returned {"status":"UP"} on first poll. Root `/` returned HTTP 200
           with the PUBG Stats dashboard HTML (K/D 1.80 etc.), no stack trace. `docker compose down -v`
           removed both containers, network, and the pgdata volume cleanly. All four checks green. -->


## Config hygiene
- [x] Every env var read by application.yml is documented in `.env.example`
      <!-- Verified 2026-07-05. application.yml reads exactly 3 env vars — SPRING_DATASOURCE_URL,
           SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD — all present in .env.example
           (which also documents PUBG_API_KEY/PUBG_SHARD for later phases). Gate green. -->

- [x] App boots with a blank PUBG_API_KEY (falls back to seed data, no crash)
      <!-- Verified 2026-07-05. No source reads PUBG_API_KEY yet (grep across src/ = 0 hits) and
           docker-compose.yml doesn't inject it, so a blank key cannot crash boot by construction.
           DataSeeder seeds unconditionally when the table is empty. Empirical boot with
           `PUBG_API_KEY= docker compose up --build -d`: app "Started ... in 2.916s", no
           errors/exceptions in logs, /actuator/health = {"status":"UP"}, root / = HTTP 200 with
           seed dashboard (you/shroud, K/D). `docker compose down -v` cleaned up. Gate green. -->

## Docs
- [x] README covers: what it does, .env setup, `docker compose up --build`, how coaching heuristics work
      <!-- Created README.md 2026-07-05. Covers all four: what it does (you-vs-pros dashboard, health
           endpoint, seed-data fallback), .env setup (table of all 5 vars, blank PUBG_API_KEY note),
           docker compose up --build + endpoints + down -v, and how coaching heuristics work
           (comparison-driven gap reading; automated per-metric tips noted as a later phase since no
           coaching code exists yet). -->
