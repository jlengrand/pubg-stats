package com.pubgstats

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Background refresh: periodically re-ingests all configured players. The existing cache TTL is
 * respected inside [StatsIngestionService.ingest], so a run within the TTL is cheap (cache hits).
 *
 * ponytail: single in-process scheduler. If the app ever runs multi-instance, move this to an
 * external scheduler (cron/k8s CronJob hitting the refresh endpoint) so it fires exactly once.
 */
@Component
class ScheduledStatsRefresh(private val ingestion: StatsIngestionService) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${pubg.refresh-cron}")
    fun refresh() {
        log.info("Scheduled refresh starting")
        val warning = ingestion.refresh()
        if (warning != null) log.warn("Scheduled refresh: {}", warning)
        else log.info("Scheduled refresh complete")
    }
}
