package com.pubgstats

import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

/** Internal summary mapped from PUBG lifetime stats, ready to persist as [PlayerStats]. */
data class PlayerStatsSummary(
    val playerName: String,
    val kd: Double,
    val avgSurvivalTimeSeconds: Int,
    val headshotRatio: Double,
    val winRatio: Double,
    val avgDamage: Double,
)

@Service
class PubgApiClient(props: PubgApiProperties) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val shard = props.shard
    private val restClient = RestClient.builder()
        .baseUrl(props.baseUrl)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${props.apiKey}")
        .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.api+json")
        .build()

    /** Resolve a player handle to its PUBG account id, or null if unknown / rate limited. */
    fun resolvePlayerId(name: String): String? = guarded(name) {
        restClient.get()
            .uri { it.path("/shards/{shard}/players").queryParam("filter[playerNames]", name).build(shard) }
            .retrieve()
            .body(JsonNode::class.java)
            ?.path("data")?.firstOrNull()
            ?.path("id")?.asText()?.takeIf { it.isNotBlank() }
    }

    /** Fetch raw lifetime stats JSON for an account id, or null on failure. */
    fun fetchLifetimeStats(accountId: String): JsonNode? = guarded(accountId) {
        restClient.get()
            .uri("/shards/{shard}/players/{id}/seasons/lifetime", shard, accountId)
            .retrieve()
            .body(JsonNode::class.java)
    }

    /** Resolve, fetch and map a handle into a [PlayerStatsSummary], or null if unavailable. */
    fun fetchPlayerSummary(name: String): PlayerStatsSummary? {
        val id = resolvePlayerId(name) ?: return null
        val json = fetchLifetimeStats(id) ?: return null
        return mapLifetimeStats(name, json)
    }

    private fun <T> guarded(name: String, call: () -> T?): T? = try {
        call()
    } catch (e: HttpClientErrorException.NotFound) {
        log.warn("PUBG API 404 for '{}' — unknown player, skipping", name)
        null
    } catch (e: HttpClientErrorException.TooManyRequests) {
        log.warn("PUBG API 429 rate limited while handling '{}' — skipping", name)
        null
    } catch (e: RestClientException) {
        // Bad/missing API key (401/403), network errors, malformed responses — never fatal.
        log.warn("PUBG API call failed for '{}': {}", name, e.message)
        null
    }

    companion object {
        /** Pure mapping of a lifetime-stats response into the internal DTO. Aggregates all game modes. */
        fun mapLifetimeStats(name: String, json: JsonNode): PlayerStatsSummary {
            var kills = 0L; var wins = 0L; var losses = 0L; var rounds = 0L
            var headshotKills = 0L; var damage = 0.0; var timeSurvived = 0.0
            for (mode in json.path("data").path("attributes").path("gameModeStats")) {
                kills += mode.path("kills").asLong()
                wins += mode.path("wins").asLong()
                losses += mode.path("losses").asLong()
                rounds += mode.path("roundsPlayed").asLong()
                headshotKills += mode.path("headshotKills").asLong()
                damage += mode.path("damageDealt").asDouble()
                timeSurvived += mode.path("timeSurvived").asDouble()
            }
            return PlayerStatsSummary(
                playerName = name,
                // K/D is kills per loss (games not won), the standard PUBG measure.
                kd = if (losses > 0) kills.toDouble() / losses else kills.toDouble(),
                avgSurvivalTimeSeconds = if (rounds > 0) (timeSurvived / rounds).toInt() else 0,
                headshotRatio = if (kills > 0) headshotKills.toDouble() / kills else 0.0,
                winRatio = if (rounds > 0) wins.toDouble() / rounds else 0.0,
                avgDamage = if (rounds > 0) damage / rounds else 0.0,
            )
        }
    }
}
