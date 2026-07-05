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
