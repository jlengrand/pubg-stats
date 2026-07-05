# Quality Gates

Run before considering any phase "done". Each must pass.

## Build & tests
- [x] `JAVA_HOME=<JDK 21/23> ./gradlew build` — compiles + runs all tests, BUILD SUCCESSFUL
      (host JDK 25 is rejected by the Gradle 8.10.2 launcher — use JDK 21 or 23)
      <!-- Verified with JDK 23.0.2: BUILD SUCCESSFUL, all tasks green. -->

- [x] No test skipped or `@Disabled` without a `// ponytail:` note saying why
      <!-- Verified: grep of src/test found zero @Disabled/@Ignore/enabled=false/assumeTrue.
           25 @Test methods across 8 files, all active. "skip" matches are business logic only. -->


## App actually runs (the real e2e gate)
- [x] `docker compose up --build -d` from clean state comes up healthy
- [x] `curl -sf localhost:8080/actuator/health` returns `{"status":"UP"}`
- [x] `curl -sf localhost:8080/` returns 200 and the dashboard HTML (not a stack trace)
- [x] `docker compose down -v` — cleans up
      <!-- Verified end-to-end from clean state. db became healthy, app UP within ~10s.
           / returned 200 with the PUBG Stats dashboard HTML (not a stack trace).
           down -v removed both containers, the network, and the pgdata volume — no leftovers.
           Note: needed a `docker system df`/prune first (host was out of disk); infra, not a code issue. -->


## Config hygiene
- [x] Every env var read by application.yml is documented in `.env.example`
      <!-- Verified: application.yml reads 7 env vars — SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME,
           SPRING_DATASOURCE_PASSWORD, PUBG_API_KEY, PUBG_SHARD, PUBG_REFRESH_CRON, PUBG_PLAYERS.
           All 7 are present in .env.example. No undocumented ${...} placeholders remain. -->

- [x] App boots with a blank PUBG_API_KEY (falls back to seed data, no crash)
      <!-- Verified. Blank key is the default (application.yml: ${PUBG_API_KEY:}), and docker-compose
           sets no PUBG_API_KEY for the app — so the docker gate above (comes up healthy, / returns 200)
           already booted with a blank key. Blank-key paths are also unit-tested and green (JDK 23):
           DataSeeder seeds sample data + never calls the API; StatsIngestionService.refresh returns a
           warning (no throw); ScheduledStatsRefresh delegates to refresh() without crashing. No code change. -->


## Docs
- [x] README covers: what it does, .env setup, `docker compose up --build`, how coaching heuristics work
      <!-- Verified: README has all four sections. "What it does", ".env setup" (table of all 7 env
           vars matching .env.example), "Run it" (docker compose up --build + down -v), and "How coaching
           heuristics work". Coaching section is accurate against code: ProBaselineService computes the
           median per metric (even counts averaged); CoachService emits gap-ranked Suggestions, skips
           metrics at/above baseline, hand-tuned tips with the ponytail: upgrade note. No doc change. -->
