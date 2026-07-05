# Quality Gates

Run before considering any phase "done". Each must pass.

## Build & tests
- [x] `JAVA_HOME=<JDK 21/23> ./gradlew build` — compiles + runs all tests, BUILD SUCCESSFUL
      (host JDK 25 is rejected by the Gradle 8.10.2 launcher — use JDK 21 or 23)
      <!-- Verified with JDK 23.0.2: BUILD SUCCESSFUL, all tests pass. -->
- [x] No test skipped or `@Disabled` without a `// ponytail:` note saying why
      <!-- Verified: grep across src/test found zero @Disabled/@Ignore/assumeTrue/skip;
           build.gradle.kts has no test excludes or onlyIf. Nothing to annotate. -->


## App actually runs (the real e2e gate)
- [x] `docker compose up --build -d` from clean state comes up healthy
- [x] `curl -sf localhost:8080/actuator/health` returns `{"status":"UP"}`
- [x] `curl -sf localhost:8080/` returns 200 and the dashboard HTML (not a stack trace)
- [x] `docker compose down -v` — cleans up
      <!-- Verified on Docker 29.5.3, blank PUBG_API_KEY (seed data): from `down -v` clean
           state, `up --build -d` came up (db healthy, app started); /actuator/health -> {"status":"UP"};
           GET / -> HTTP 200 with <title>PUBG Stats</title> dashboard (no stack trace);
           `down -v` removed containers, network, and pgdata volume — no leftovers. -->


## Config hygiene
- [x] Every env var read by application.yml is documented in `.env.example`
      <!-- Verified: all 7 env vars in application.yml — SPRING_DATASOURCE_{URL,USERNAME,PASSWORD},
           PUBG_API_KEY, PUBG_SHARD, PUBG_REFRESH_CRON, PUBG_PLAYERS — are present in .env.example.
           base-url/cache-ttl-hours are hardcoded, not env-driven. Nothing missing. -->

- [x] App boots with a blank PUBG_API_KEY (falls back to seed data, no crash)
      <!-- Verified via docker compose (no PUBG_API_KEY set → blank): app started in 3.0s,
           log "PUBG_API_KEY blank — using sample seed data", /actuator/health {"status":"UP"},
           GET / -> HTTP 200 rendering all 4 seed rows (you, shroud, TGLTN, cuhLee),
           zero exception/error/failed lines in app logs. DataSeederTest covers the branch. -->


## Docs
- [x] README covers: what it does, .env setup, `docker compose up --build`, how coaching heuristics work
      <!-- Verified: README has "What it does", ".env setup" (with env-var table), "Run it"
           (`docker compose up --build`), and "How coaching heuristics work" sections.
           Cross-checked against code: ProBaselineService computes per-metric medians;
           CoachService emits gap-ranked Suggestions (gap = (baseline−you)/baseline,
           sortedByDescending, at/above-baseline skipped) with the // ponytail: heuristics
           note. Descriptions match implementation. Doc-only gate, nothing to change. -->
