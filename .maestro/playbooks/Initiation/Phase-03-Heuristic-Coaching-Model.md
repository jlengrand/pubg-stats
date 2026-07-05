# Phase 03: Heuristic Coaching Model

This phase turns raw comparisons into actionable advice. It computes the median of each pro metric, compares the user's stats against those medians, and produces a ranked list of improvement suggestions ("your headshot ratio is 40% below the pro median — focus on aim training"). No ML — just transparent, tunable heuristics that ship today and can be swapped for a model later.

## Tasks

- [x] Implement the pro-baseline calculator:
  - `ProBaselineService.kt` that loads all `isPro = true` stats and computes the median (and optionally mean) for each metric: K/D, avg survival time, headshot ratio, win ratio, avg damage
  - Return a small `ProBaseline` data class holding one baseline value per metric
  - Add a self-check (`@Test` or `assert`-based) confirming the median is correct for a known small set
  - Note: `median` lives in the companion object so it's testable without a Spring context. `compute()` returns `null` for an empty pro list. Tests must run with JDK 17 (`JAVA_HOME=.../temurin-17.0.2`) — the default JDK 25 breaks the Kotlin compiler.

- [x] Implement the heuristic coach:
  - `CoachService.kt` that takes the user's `PlayerStats` and a `ProBaseline` and produces a list of `Suggestion` items
  - Each `Suggestion` has: metric name, user value, pro baseline value, percentage gap, a severity/priority score, and a human-readable tip
  - Define per-metric tips in a simple map/config (e.g. low headshot ratio → aim training, low survival time → play more cautiously, land safer)
  - Rank suggestions by how far below baseline the user is (biggest gaps first); skip metrics where the user meets or beats the baseline
  - Mark the tip mapping with a `// ponytail:` comment noting these thresholds are hand-tuned heuristics, upgrade path is a trained model
  - Note: metrics defined in a `METRICS` list in the companion object (label + tip + value accessors); `coach()` returns `emptyList()` for a null baseline, skips metrics where user ≥ baseline, and ranks by `gap` (fraction below baseline) descending. `CoachServiceTest.kt` is a later task. Compiles clean with JDK 17.

- [x] Surface coaching on the dashboard:
  - Update `DashboardController` to build the `ProBaseline`, run `CoachService`, and add the ranked suggestions to the model
  - Update `dashboard.html` with a "Coaching" section rendering each suggestion as a card: metric, your value vs pro baseline, the gap as a colored indicator (red for large gap), and the tip
  - Keep the existing stats table and pro comparison; add the coaching section above or beside it
  - Note: controller injects `ProBaselineService` + `CoachService`, guards null `you`. Coaching `<section>` renders above the Pros table, hidden when there are no suggestions. Gap indicator is a colored left border + `-NN%` badge: red ≥30%, amber ≥15%, green below (classes `gap-high/mid/low` in `style.css`). Compiles clean with JDK 17.

- [x] Write tests for the coaching logic:
  - `ProBaselineServiceTest.kt` covering median computation with even/odd counts and single-pro edge cases
  - `CoachServiceTest.kt` covering: suggestions ranked by gap, metrics at/above baseline excluded, correct tip selected per metric, empty-pro-list handled gracefully
  - Note: both tests use Mockito (`ProBaselineServiceTest` mocks the repo to drive `compute()`; `CoachService` needs no mocks). `ProBaselineServiceTest` verifies median per-metric for single/odd/even pro counts and `null` on empty. Median even/odd of the raw helper stays in the pre-existing `ProBaselineServiceMedianTest`. All 3 pass under JDK 17.

- [x] Run the test suite and verify the coaching output:
  - Run `./gradlew test` and fix any failures
  - Run `docker compose up --build` and confirm the dashboard shows a ranked, readable list of improvement suggestions driven by the real (or seeded) data
  - Note: `./gradlew test` (JDK 17) BUILD SUCCESSFUL — 23 tests across 7 classes pass, no failures, no fixes needed. `docker compose up --build -d` came up healthy; `GET /` returned 200 and the Coaching section rendered a gap-ranked list from seeded data: K/D −69%, Win Ratio −67%, Avg Damage −56%, Headshot Ratio −42%, Avg Survival Time −29% (descending, each with your-vs-pro values + tip). Stack torn down with `docker compose down`.
