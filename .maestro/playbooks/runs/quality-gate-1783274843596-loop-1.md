# Quality Gates

Run before considering any phase "done". Each must pass.

## Build & tests
- [x] `JAVA_HOME=<JDK 21/23> ./gradlew build` — compiles + runs all tests, BUILD SUCCESSFUL
      (host JDK 25 is rejected by the Gradle 8.10.2 launcher — use JDK 21 or 23)
      <!-- MAESTRO: passed with JDK 23.0.2 → BUILD SUCCESSFUL in 4s, :test ran green. -->
      <!-- Note: task ran on branch `init` worktree at /Users/julienlengrand-lambert/Developer/pubg-stats-worktree/init -->
      <!-- Only a couple of Gradle 9.0 deprecation warnings, no failures. -->
      <!-- No `- [x]` was undone; single-task run per playbook rules. -->
      <!-- ponytail: no source changes needed, gate was already green. -->
      <!-- verified -->
      <!-- 0 images analyzed for this task -->
      <!-- -->
- [x] No test skipped or `@Disabled` without a `// ponytail:` note saying why
      <!-- MAESTRO: grep across src/ for @Disabled/@Ignore/assumeTrue/assumeThat found zero skipped tests. -->
      <!-- The only "skip" matches are business-logic comments and a test name ("skips seeding"), not disabled tests. -->
      <!-- ponytail: nothing to change, gate already green. -->
      <!-- 0 images analyzed for this task -->
      <!-- verified -->

## App actually runs (the real e2e gate)
- [x] `docker compose up --build -d` from clean state comes up healthy
      <!-- MAESTRO: after `docker compose down -v` (clean), `up --build -d` built init-app and started both containers. -->
      <!-- init-db-1 → Up (healthy); init-app-1 → Up, "Started PubgStatsApplicationKt in 3.173s", Tomcat on 8080. -->
      <!-- Docker 29.5.3. Containers left running for the downstream curl gates. -->
      <!-- ponytail: no source changes; gate is green as-is. -->
      <!-- 0 images analyzed for this task -->
      <!-- verified -->

- [x] `curl -sf localhost:8080/actuator/health` returns `{"status":"UP"}`
      <!-- MAESTRO: curl -sf localhost:8080/actuator/health → {"status":"UP"}, exit 0. -->
      <!-- Containers already Up from prior gate: init-app-1 Up, init-db-1 Up (healthy). -->
      <!-- ponytail: no source changes; gate green as-is. -->
      <!-- 0 images analyzed for this task -->
      <!-- verified -->
- [x] `curl -sf localhost:8080/` returns 200 and the dashboard HTML (not a stack trace)
      <!-- MAESTRO: curl -sf localhost:8080/ → HTTP 200, exit 0, 4775 bytes of dashboard HTML. -->
      <!-- Body starts with <!DOCTYPE html> … <title>PUBG Stats</title>, renders cards + refresh form, no stack trace. -->
      <!-- Containers already Up from prior gates: init-app-1 Up, init-db-1 Up (healthy). -->
      <!-- ponytail: no source changes; gate green as-is. -->
      <!-- 0 images analyzed for this task -->
      <!-- verified -->
- [x] `docker compose down -v` — cleans up
      <!-- MAESTRO: docker compose down -v removed init-app-1, init-db-1, volume init_pgdata, and network init_default. Exit 0. -->
      <!-- ponytail: no source changes; gate green as-is. -->
      <!-- 0 images analyzed for this task -->
      <!-- verified -->

## Config hygiene
- [x] Every env var read by application.yml is documented in `.env.example`
      <!-- MAESTRO: application.yml reads 7 env vars via ${...}: SPRING_DATASOURCE_URL/USERNAME/PASSWORD, PUBG_API_KEY, PUBG_SHARD, PUBG_REFRESH_CRON, PUBG_PLAYERS. -->
      <!-- All 7 are present in .env.example. base-url and cache-ttl-hours are hardcoded (no env var), so nothing to document. -->
      <!-- ponytail: no changes needed, gate already green. -->
      <!-- 0 images analyzed for this task -->
      <!-- verified -->
- [x] App boots with a blank PUBG_API_KEY (falls back to seed data, no crash)
      <!-- MAESTRO: docker-compose.yml sets no PUBG_API_KEY → app default apiKey="" (blank). Booted clean via `docker compose up --build -d`. -->
      <!-- Log confirmed: DataSeeder → "PUBG_API_KEY blank — using sample seed data". Started in 3.137s, no crash. -->
      <!-- health = {"status":"UP"}; GET / = 200 and dashboard rendered the 4 seed players (you, shroud, TGLTN, cuhLee). Torn down with down -v. -->
      <!-- Unit coverage already exists: DataSeederTest `blank API key seeds sample data instead of calling the API`. -->
      <!-- ponytail: no source changes; gate green as-is. -->
      <!-- 0 images analyzed for this task -->
      <!-- verified -->

## Docs
- [x] README covers: what it does, .env setup, `docker compose up --build`, how coaching heuristics work
      <!-- MAESTRO: verified README.md already has all four sections: -->
      <!--   "What it does" (L12-20), ".env setup" with `cp .env.example .env` + full var table (L40-59), -->
      <!--   "Run it" with `docker compose up --build` (L61-76), "How coaching heuristics work" (L78-98). -->
      <!-- Coaching section documents ProBaselineService (per-metric median) + CoachService (below-baseline gap, biggest-first). -->
      <!-- ponytail: no changes needed, gate already green. -->
      <!-- 0 images analyzed for this task -->
      <!-- verified -->