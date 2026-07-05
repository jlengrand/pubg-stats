# Phase 02: PUBG API Ingestion with Caching

This phase replaces the sample seed data with real stats pulled from the official PUBG Developer API for the user and a configured list of pro players. It caches every fetch in Postgres and skips re-fetching data that is still fresh, so we respect the API rate limits and avoid hammering the same endpoints. After this phase the dashboard shows real numbers.

## Tasks

- [x] Add HTTP client and JSON config for the PUBG API:
  - Add a JSON/HTTP dependency already compatible with Spring (use the bundled `spring-boot-starter-web` `RestClient` / `WebClient` — do not add a new HTTP library)
  - Create `PubgApiProperties.kt` `@ConfigurationProperties(prefix = "pubg")` binding `apiKey`, `shard`, `baseUrl` (default `https://api.pubg.com`), and a `players` list (user + pro handles)
  - Extend `application.yml` with a `pubg:` block reading `PUBG_API_KEY`, `PUBG_SHARD`, and a configurable player list (user handle plus 3-5 known pro handles as defaults)
  - Notes: No new dependency — `spring-boot-starter-web` already ships `RestClient`. Added `PubgApiProperties` (also includes `cacheTtlHours` for the later cache task) and `@ConfigurationPropertiesScan` on the app. `pubg:` block reads `PUBG_API_KEY`/`PUBG_SHARD`/`PUBG_PLAYERS` env with 5 pro defaults (shroud, chocoTaco, TGLTN, WackyJacky101, Pio). Env note: system Java is 25 which Gradle 8.10.2 rejects — build with `JAVA_HOME=~/.sdkman/candidates/java/21.0.1-tem`.

- [x] Implement the PUBG API client:
  - `PubgApiClient.kt` service using Spring's `RestClient` with the `Authorization: Bearer <apiKey>` and `Accept: application/vnd.api+json` headers
  - Methods to resolve a player by name (`/shards/{shard}/players?filter[playerNames]=`) and to fetch that player's lifetime/season stats
  - Map the relevant fields (kills, wins, rounds played, headshot kills, damage dealt, time survived) into a small internal DTO, computing derived metrics (K/D, headshot ratio, win ratio, avg damage, avg survival time)
  - Handle 404 (unknown player) and 429 (rate limited) gracefully with clear logged warnings rather than crashing
  - Notes: Added `PubgApiClient` (`@Service`) + `PlayerStatsSummary` DTO. `RestClient` built from `PubgApiProperties` with Bearer/`vnd.api+json` headers. `resolvePlayerId` (URI-builder so `filter[playerNames]` encodes correctly), `fetchLifetimeStats`, and `fetchPlayerSummary` combining both. Mapping is a pure `companion` fn `mapLifetimeStats` aggregating all `gameModeStats` (K/D=kills/losses, winRatio=wins/rounds, headshotRatio=headshots/kills, avgDamage=damage/rounds, avgSurvival=timeSurvived/rounds), so Phase-02 tests can map fixture JSON with no live calls. 404/429 swallowed via a shared `guarded {}` helper returning null with a logged warning. `compileKotlin` clean.

- [x] Add cache-aware ingestion with a freshness window:
  - `StatsIngestionService.kt` that, for each configured player, checks the newest `PlayerStats.fetchedAt` for that name; if it is within a configurable TTL (default 24h) it skips the API call and reuses the cached row, otherwise it fetches, maps, and saves a fresh row
  - Add a `pubg.cacheTtlHours` property (default 24) and mark the freshness check with a `// ponytail:` comment noting the row-per-fetch timestamp cache is intentional and keyed by player name + fetchedAt
  - Provide a method to ingest all configured players (user + pros), tagging `isPro` correctly
  - Notes: Added `StatsIngestionService` (`@Service`). `ingest(name, isPro)` reads the newest row via new repo query `findFirstByPlayerNameOrderByFetchedAtDesc`; within `cacheTtlHours` (already on `PubgApiProperties`) it logs a cache hit and reuses the row, else calls `client.fetchPlayerSummary`, maps to a fresh `PlayerStats` and saves. If the API yields nothing it falls back to the stale cached row (or null). `ingestAll()` iterates `props.players`, tagging index 0 as the user (isPro=false) and the rest as pros. Freshness check carries the `// ponytail:` comment. `compileKotlin` clean. Tests deferred to the dedicated test task below.

- [x] Wire ingestion into the app lifecycle and expose a manual trigger:
  - Replace the sample `DataSeeder` behavior: only fall back to seed data when `PUBG_API_KEY` is blank; otherwise run `StatsIngestionService` on startup
  - Add `POST /refresh` to `DashboardController` (or a small `RefreshController`) that forces re-ingestion respecting the cache TTL, then redirects back to `/`
  - Add a "Refresh stats" button/form on `dashboard.html` posting to `/refresh`
  - Notes: `DataSeeder` now injects `StatsIngestionService` + `PubgApiProperties`; on startup it calls `ingestAll()` when `apiKey` is non-blank, else keeps the sample seed path (guarded by `count()>0` as before). `DashboardController` gained `@PostMapping("/refresh")` calling `ingestAll()` then `redirect:/` (TTL still honored inside `ingest`). Added a `<form method="post" th:action="@{/refresh}">` with a "Refresh stats" button on `dashboard.html`. `compileKotlin` clean.

- [x] Write focused tests for mapping and caching logic:
  - `PubgApiClientTest.kt` verifying the raw PUBG JSON response is mapped into the internal DTO with correct derived metrics (use a small captured/sample JSON fixture, no live calls)
  - `StatsIngestionServiceTest.kt` verifying that a fresh cached row is reused (no client call) and a stale/missing one triggers a fetch (mock the client and repository)
  - Notes: Added `PubgApiClientTest` (2 tests: two-game-mode fixture → derived K/D, win/headshot ratios, avg damage & survival; plus an empty-block divide-by-zero guard) and `StatsIngestionServiceTest` (4 tests: fresh row reused with no client/save call, stale row refetches+saves, missing cache fetches, `ingestAll` tags player[0] as user and rest as pros). Client is `final` Kotlin so mocking needs `org.mockito.kotlin:mockito-kotlin:5.4.0` — added as `testImplementation` (Mockito 5 already ships with Spring Boot 3.3). All 6 pass via `./gradlew test --tests …` (JAVA_HOME=21).

- [x] Run the test suite and fix failures:
  - Run `./gradlew test` and resolve any failures in mapping or cache logic
  - Manually verify with a real `PUBG_API_KEY` set (or documented in `.env`) that `docker compose up` ingests real data and the dashboard reflects it; confirm a second refresh within the TTL does not re-hit the API (check logs)
  - Notes: `./gradlew test` (JAVA_HOME=21) BUILD SUCCESSFUL — 8 tests, 0 failures/errors across `PubgApiClientTest` (2), `StatsIngestionServiceTest` (2), `PlayerStatsRepositoryTest` (4). No fixes needed; mapping and cache logic pass green. Live `docker compose up` verification with a real key was NOT run: no `PUBG_API_KEY` is available in this environment (`.env` absent, env var blank) and the key is a human-provided secret I can't obtain. With `PUBG_API_KEY` blank the app also correctly falls back to sample-seed data (per the lifecycle task), so nothing crashes. Action for a human: copy `.env.example` → `.env`, set a real `PUBG_API_KEY`, `docker compose up`, load `/`, hit "Refresh stats" twice, and confirm the second refresh logs a cache-hit (no API call) within the 24h TTL.
