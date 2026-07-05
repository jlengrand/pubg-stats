package com.pubgstats

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

/**
 * Cache-aware ingestion: for each configured player, reuse the newest cached row while it is
 * within the TTL, otherwise fetch fresh stats from the PUBG API and persist a new row.
 */
@Service
class StatsIngestionService(
    private val client: PubgApiClient,
    private val repository: PlayerStatsRepository,
    private val props: PubgApiProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Ingest all configured players. First entry is the user (isPro=false); the rest are pros. */
    fun ingestAll(): List<PlayerStats> =
        props.players.mapIndexedNotNull { index, name -> ingest(name, isPro = index != 0) }

    /**
     * Ingest all players for a user-triggered refresh. Returns a human-readable warning to show as a
     * dashboard banner, or null on full success. The API client swallows per-player failures, so a
     * failure surfaces as fewer results than configured players (no cached row to fall back on).
     */
    fun refresh(): String? {
        if (props.apiKey.isBlank()) {
            return "No PUBG API key configured — showing any cached data. Set PUBG_API_KEY in your .env and restart."
        }
        val missing = props.players.size - ingestAll().size
        return if (missing > 0) {
            "$missing of ${props.players.size} player(s) could not be refreshed (unknown handle, rate limit, or network) — showing cached data where available."
        } else {
            null
        }
    }

    /** Reuse the freshest cached row if within TTL, else fetch, map and save. Returns null if unavailable. */
    fun ingest(name: String, isPro: Boolean): PlayerStats? {
        val ttl = Duration.ofHours(props.cacheTtlHours)
        val cached = repository.findFirstByPlayerNameOrderByFetchedAtDesc(name)
        // ponytail: row-per-fetch timestamp cache — no eviction, no dedupe. Freshness is keyed by
        // player name + fetchedAt: the newest row within cacheTtlHours wins, a stale one triggers a
        // new fetch that appends another row. History is a feature, not a leak.
        if (cached != null && Duration.between(cached.fetchedAt, Instant.now()) < ttl) {
            log.info("Cache hit for '{}' (fetched {}), skipping API call", name, cached.fetchedAt)
            return cached
        }

        val summary = client.fetchPlayerSummary(name) ?: run {
            log.warn("No stats available for '{}' — keeping any existing cached row", name)
            return cached
        }
        return repository.save(
            PlayerStats(
                playerName = summary.playerName,
                kd = summary.kd,
                avgSurvivalTimeSeconds = summary.avgSurvivalTimeSeconds,
                headshotRatio = summary.headshotRatio,
                winRatio = summary.winRatio,
                avgDamage = summary.avgDamage,
                isPro = isPro,
                fetchedAt = Instant.now(),
            )
        )
    }
}
