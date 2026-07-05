# Quality Gates

Run before considering any phase "done". Each must pass.

## Build & tests
- [x] `JAVA_HOME=<JDK 21/23> ./gradlew build` — compiles + runs all tests, BUILD SUCCESSFUL
      (host JDK 25 is rejected by the Gradle 8.10.2 launcher — use JDK 21 or 23)
      <!-- Ran with JDK 23.0.2 (/Users/.../openjdk-23.0.2). BUILD SUCCESSFUL in 1s, all tests pass. -->

- [x] No test skipped or `@Disabled` without a `// ponytail:` note saying why
      <!-- Verified: grep for @Disabled/@Ignore/assumeTrue/@EnabledIf across all 7 test files (src/test/kotlin) found zero skipped tests. Nothing to annotate. -->


## App actually runs (the real e2e gate)
- [x] `docker compose up --build -d` from clean state comes up healthy
- [x] `curl -sf localhost:8080/actuator/health` returns `{"status":"UP"}`
- [x] `curl -sf localhost:8080/` returns 200 and the dashboard HTML (not a stack trace)
- [x] `docker compose down -v` — cleans up
      <!-- Verified 2026-07-05: `down -v` from clean state, then `up --build -d` (db healthy, app started).
           Health returned {"status":"UP"}; `/` returned HTTP 200 with the PUBG Stats dashboard HTML.
           `down -v` removed containers/network/volume; `compose ps -a` shows nothing left. All 4 pass. -->


## Config hygiene
- [x] Every env var read by application.yml is documented in `.env.example`
      <!-- Verified 2026-07-05: application.yml reads 6 vars (SPRING_DATASOURCE_URL/USERNAME/PASSWORD,
           PUBG_API_KEY, PUBG_SHARD, PUBG_PLAYERS). All 6 are documented in .env.example. Nothing missing. -->

- [x] App boots with a blank PUBG_API_KEY (falls back to seed data, no crash)
      <!-- Verified 2026-07-05: docker-compose `app` sets no PUBG_API_KEY (application.yml default is blank).
           Fresh `up --build -d`: health {"status":"UP"}, app logged "PUBG_API_KEY blank — using sample seed data",
           `printenv PUBG_API_KEY` unset in container, dashboard rendered seeded rows (you/shroud/TGLTN/cuhLee).
           No crash. Also covered by 3 DataSeederTest unit tests. `down -v` cleaned up. -->


## Docs
- [x] README covers: what it does, .env setup, `docker compose up --build`, how coaching heuristics work
      <!-- Verified 2026-07-05: README already covered the first three. Coaching section was stale
           (said tips "land in a later phase") — rewrote it to document the now-live ProBaselineService
           (per-metric medians) + CoachService (below-baseline gaps → ranked worded tips, surfaced on the
           dashboard). All four items now accurate. -->
