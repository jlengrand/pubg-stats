package com.pubgstats

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.time.Instant

// ponytail: no PUBG_API_KEY → sample seed data so the app runs with zero external calls.
// With a key set, we skip the fixtures and pull real stats via StatsIngestionService.
@Component
class DataSeeder(
    private val repository: PlayerStatsRepository,
    private val ingestion: StatsIngestionService,
    private val props: PubgApiProperties,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments?) {
        if (props.apiKey.isNotBlank()) {
            log.info("PUBG_API_KEY set — ingesting real stats for {} player(s)", props.players.size)
            ingestion.ingestAll()
            return
        }
        log.info("PUBG_API_KEY blank — using sample seed data")
        if (repository.count() > 0) return
        val now = Instant.now()
        repository.saveAll(
            listOf(
                PlayerStats(
                    playerName = "you",
                    kd = 1.8,
                    avgSurvivalTimeSeconds = 840,
                    headshotRatio = 0.22,
                    winRatio = 0.06,
                    avgDamage = 210.0,
                    isPro = false,
                    fetchedAt = now,
                ),
                PlayerStats(
                    playerName = "shroud",
                    kd = 6.4,
                    avgSurvivalTimeSeconds = 1180,
                    headshotRatio = 0.41,
                    winRatio = 0.18,
                    avgDamage = 480.0,
                    isPro = true,
                    fetchedAt = now,
                ),
                PlayerStats(
                    playerName = "TGLTN",
                    kd = 5.9,
                    avgSurvivalTimeSeconds = 1240,
                    headshotRatio = 0.38,
                    winRatio = 0.21,
                    avgDamage = 510.0,
                    isPro = true,
                    fetchedAt = now,
                ),
                PlayerStats(
                    playerName = "cuhLee",
                    kd = 5.2,
                    avgSurvivalTimeSeconds = 1090,
                    headshotRatio = 0.35,
                    winRatio = 0.15,
                    avgDamage = 445.0,
                    isPro = true,
                    fetchedAt = now,
                ),
            )
        )
    }
}
