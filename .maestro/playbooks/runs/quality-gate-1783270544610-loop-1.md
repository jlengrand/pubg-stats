# Quality Gates

Run before considering any phase "done". Each must pass.

## Build & tests
- [x] `JAVA_HOME=<JDK 21/23> ./gradlew build` — compiles + runs all tests, BUILD SUCCESSFUL
      (host JDK 25 is rejected by the Gradle 8.10.2 launcher — use JDK 21 or 23)
      Verified with JDK 23.0.2: `BUILD SUCCESSFUL in 1s`, all tests pass.
- [x] No test skipped or `@Disabled` without a `// ponytail:` note saying why
      Verified: grep for `@Disabled`/`@Ignore`/`assumeTrue`/`assumeFalse` across all 8
      test files and the Gradle config finds zero matches — nothing to justify.

## App actually runs (the real e2e gate)
- [x] `docker compose up --build -d` from clean state comes up healthy
      Verified: image built, db reached Healthy, app started. Docker 29.5.3 / Compose v5.1.4.
- [x] `curl -sf localhost:8080/actuator/health` returns `{"status":"UP"}`
      Verified: returned `{"status":"UP"}` after ~app boot.
- [x] `curl -sf localhost:8080/` returns 200 and the dashboard HTML (not a stack trace)
      Verified: HTTP 200, 4775 bytes, `<title>PUBG Stats</title>` dashboard (blank API key → seed data).
- [x] `docker compose down -v` — cleans up
      Verified: containers/network/volume all removed, exit 0, `docker compose ps` empty.

## Config hygiene
- [x] Every env var read by application.yml is documented in `.env.example`
      Verified: all 7 vars (`SPRING_DATASOURCE_URL/USERNAME/PASSWORD`, `PUBG_API_KEY`,
      `PUBG_SHARD`, `PUBG_REFRESH_CRON`, `PUBG_PLAYERS`) present in `.env.example`.
- [x] App boots with a blank PUBG_API_KEY (falls back to seed data, no crash)
      Verified: `docker compose up` (no PUBG_API_KEY set → blank) booted clean, health UP,
      `/` returned HTTP 200 with seed players (you/shroud/TGLTN/cuhLee). App log:
      `DataSeeder : PUBG_API_KEY blank — using sample seed data`. No exceptions. DataSeeder
      logic is also covered by DataSeederTest (blank-key seeds, no re-seed, set-key ingests).

## Docs
- [x] README covers: what it does, .env setup, `docker compose up --build`, how coaching heuristics work
      Verified: README has "What it does", ".env setup" (env-var table), "Run it"
      (`docker compose up --build`), and "How coaching heuristics work" (ProBaselineService
      median baseline → CoachService gap-ranked suggestions). Added the one missing env var
      `PUBG_REFRESH_CRON` to the .env table so all 7 documented vars are covered.