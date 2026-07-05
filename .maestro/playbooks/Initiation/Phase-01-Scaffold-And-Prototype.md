# Phase 01: Scaffold and Working Prototype

This phase stands up the entire stack end-to-end with sample data so `docker compose up` renders a working stats page in the browser. No PUBG API key or user input is needed — we use a hardcoded sample profile so the app runs on its own. This proves the Kotlin + Spring Boot + PostgreSQL + Thymeleaf + Docker Compose plumbing works before we wire in the real API or any coaching logic.

## Tasks

- [x] Generate the Gradle + Kotlin Spring Boot project skeleton at the repo root:
  - `build.gradle.kts` using the Spring Boot plugin, Kotlin JVM plugin, and dependencies: `spring-boot-starter-web`, `spring-boot-starter-thymeleaf`, `spring-boot-starter-data-jpa`, `org.postgresql:postgresql`, and `spring-boot-starter-actuator`
  - `settings.gradle.kts` with `rootProject.name = "pubg-stats"`
  - Gradle wrapper files (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties` pinned to a recent Gradle 8.x)
  - `.gitignore` covering `build/`, `.gradle/`, `.env`, and IDE folders
  - Main class `src/main/kotlin/com/pubgstats/PubgStatsApplication.kt` annotated with `@SpringBootApplication`
  - _Notes: Spring Boot 3.3.5 + Kotlin 1.9.25, Java toolchain 21 (host runs JDK 25 which the Gradle launcher rejects, so wrapper/build use JDK 21). Wrapper pinned to Gradle 8.10.2. Verified with `./gradlew compileKotlin` — BUILD SUCCESSFUL._

- [x] Configure the application for Postgres and local dev:
  - `src/main/resources/application.yml` reading DB connection from env vars (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`) with sensible localhost defaults, `jpa.hibernate.ddl-auto: update`, and `jpa.open-in-view: false`
  - A `.env.example` file documenting the DB vars plus a placeholder `PUBG_API_KEY=` and `PUBG_SHARD=steam` for later phases
  - _Notes: `application.yml` uses `${VAR:default}` syntax for localhost defaults (db name/user/pass all `pubgstats`). `.env.example` documents the three DB vars plus `PUBG_API_KEY=` and `PUBG_SHARD=steam`. Build unaffected — `./gradlew compileKotlin` still BUILD SUCCESSFUL._

- [x] Create the core domain entity and repository for a player's season stats:
  - `PlayerStats.kt` JPA `@Entity` with fields: `id`, `playerName`, `kd` (double), `avgSurvivalTimeSeconds` (int), `headshotRatio` (double), `winRatio` (double), `avgDamage` (double), `isPro` (boolean), `fetchedAt` (Instant)
  - `PlayerStatsRepository.kt` extending `JpaRepository`, with `findByPlayerName` and `findByIsProTrue`
  - _Notes: Both in package `com.pubgstats`. Entity uses `GenerationType.IDENTITY` for `id`. Added `com.h2database:h2` as `testRuntimeOnly` plus a `@DataJpaTest` (`PlayerStatsRepositoryTest`) verifying both query methods. Run gradle with JAVA_HOME=JDK 23 (host default JDK 25 is rejected by the Gradle 8.10.2 launcher; no JDK 21 installed, toolchain still targets 21). `./gradlew test` — BUILD SUCCESSFUL, 5 tests green._

- [x] Seed hardcoded sample data so the app runs with zero external calls:
  - A `DataSeeder.kt` `@Component` implementing `ApplicationRunner` that, when the `player_stats` table is empty, inserts one sample "you" profile (`isPro = false`) and three sample pro profiles (`isPro = true`) with realistic numbers
  - Mark this seeder with a `// ponytail:` comment noting it is dev/sample seed data to be gated or removed once real ingestion lands in Phase 02
  - _Notes: `DataSeeder.kt` in package `com.pubgstats`, guards on `repository.count() > 0` for idempotency. Inserts "you" (K/D 1.8, non-pro) plus pros shroud/TGLTN/cuhLee with realistic K/D, survival, headshot, win, damage values. Has the `// ponytail:` gate/remove comment. `./gradlew compileKotlin` — BUILD SUCCESSFUL (JAVA_HOME=JDK 23)._

- [x] Build the Thymeleaf dashboard page and controller:
  - `DashboardController.kt` `@Controller` mapping `GET /` that loads the non-pro player and the pro players, adds them to the model, and returns view `dashboard`
  - `src/main/resources/templates/dashboard.html` showing the user's stats in a card and a table of the pro players' stats side by side
  - `src/main/resources/static/css/style.css` with clean, minimal styling (readable table, cards, sensible spacing)
  - _Notes: `DashboardController` loads `findByPlayerName("you")` + `findByIsProTrue()` into the model. `dashboard.html` renders a stats card for "you" (guarded with `th:if`) and a pro comparison table, using Thymeleaf `#numbers` helpers for K/D (2dp), headshot/win as percent, damage rounded. Dark minimal CSS with CSS-grid stat tiles and a bordered table. No dedicated controller test added — glue is trivial and end-to-end rendering is covered by the final verify task. `./gradlew compileKotlin test` (JAVA_HOME=JDK 23) — BUILD SUCCESSFUL, 5 tests green._

- [x] Add Docker Compose deployment:
  - `Dockerfile` doing a multi-stage build (Gradle build stage producing the boot jar, slim JRE runtime stage running the jar)
  - `docker-compose.yml` with a `db` service (`postgres:16` with a named volume and healthcheck) and an `app` service that builds from the Dockerfile, depends on `db` being healthy, passes the datasource env vars, and maps port `8080:8080`
  - `.dockerignore` excluding `build/`, `.gradle/`, and `.git/`
  - _Notes: `Dockerfile` build stage uses `gradle:8.10.2-jdk21` (matches wrapper/toolchain), runs `gradle bootJar --no-daemon`; runtime stage is `eclipse-temurin:21-jre-jammy` running `app.jar`, `EXPOSE 8080`. `docker-compose.yml`: `db` = `postgres:16` with named `pgdata` volume + `pg_isready` healthcheck; `app` builds from Dockerfile, `depends_on db: service_healthy`, passes `SPRING_DATASOURCE_*` (URL host `db`), maps `8080:8080`. No `version:` key (obsolete in Compose v2). `.dockerignore` excludes `build/ .gradle/ .git/`. Validated with `docker compose config -q` — OK. Full `up --build` is the next verify task._

- [x] Verify the prototype runs end to end:
  - Run `docker compose up --build` and confirm both services start and the app connects to Postgres
  - Confirm `http://localhost:8080/` renders the dashboard with the seeded user stats and pro comparison table
  - Confirm `http://localhost:8080/actuator/health` returns `UP`
  - Fix any build, connection, or template errors until the page renders cleanly
  - _Notes: `docker compose up --build -d` — image built clean (BUILD SUCCESSFUL, bootJar), `db` reached healthy, `app` started and connected to Postgres. `GET /actuator/health` → `{"status":"UP"}`. `GET /` → 200, HTML renders `<title>PUBG Stats</title>`, the `you` stats card, and a Pros table with all three seeded pros (shroud, TGLTN, cuhLee). No build/connection/template fixes needed. Torn back down with `docker compose down`. (Docker 29.5.3, Compose v5.1.4 on host.)_
