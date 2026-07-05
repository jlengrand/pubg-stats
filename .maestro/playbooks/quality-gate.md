# Quality Gates

Run before considering any phase "done". Each must pass.

## Build & tests
- [ ] `JAVA_HOME=<JDK 21/23> ./gradlew build` — compiles + runs all tests, BUILD SUCCESSFUL
      (host JDK 25 is rejected by the Gradle 8.10.2 launcher — use JDK 21 or 23)
- [ ] No test skipped or `@Disabled` without a `// ponytail:` note saying why

## App actually runs (the real e2e gate)
- [ ] `docker compose up --build -d` from clean state comes up healthy
- [ ] `curl -sf localhost:8080/actuator/health` returns `{"status":"UP"}`
- [ ] `curl -sf localhost:8080/` returns 200 and the dashboard HTML (not a stack trace)
- [ ] `docker compose down -v` — cleans up

## Config hygiene
- [ ] Every env var read by application.yml is documented in `.env.example`
- [ ] App boots with a blank PUBG_API_KEY (falls back to seed data, no crash)

## Docs
- [ ] README covers: what it does, .env setup, `docker compose up --build`, how coaching heuristics work