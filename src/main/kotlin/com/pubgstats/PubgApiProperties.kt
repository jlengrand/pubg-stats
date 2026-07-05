package com.pubgstats

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "pubg")
data class PubgApiProperties(
    val apiKey: String = "",
    val shard: String = "steam",
    val baseUrl: String = "https://api.pubg.com",
    val cacheTtlHours: Long = 24,
    /** Cron for the background refresh job. Default: daily at 03:00. */
    val refreshCron: String = "0 0 3 * * *",
    /** User handle first, then pro handles. */
    val players: List<String> = emptyList(),
)
