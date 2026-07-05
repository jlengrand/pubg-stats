package com.pubgstats

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals

class PubgApiClientTest {

    // Two game modes so the test also exercises aggregation across modes.
    private val fixture = """
        {
          "data": {
            "attributes": {
              "gameModeStats": {
                "squad": {
                  "kills": 100, "wins": 10, "losses": 90, "roundsPlayed": 100,
                  "headshotKills": 25, "damageDealt": 20000.0, "timeSurvived": 90000.0
                },
                "duo": {
                  "kills": 50, "wins": 5, "losses": 45, "roundsPlayed": 50,
                  "headshotKills": 10, "damageDealt": 10000.0, "timeSurvived": 45000.0
                }
              }
            }
          }
        }
    """.trimIndent()

    @Test
    fun `maps lifetime JSON into DTO with correct derived metrics`() {
        val json = ObjectMapper().readTree(fixture)

        val summary = PubgApiClient.mapLifetimeStats("you", json)

        // kills=150 wins=15 losses=135 rounds=150 headshots=35 damage=30000 time=135000
        assertEquals("you", summary.playerName)
        assertEquals(150.0 / 135.0, summary.kd)                 // kills / losses
        assertEquals(15.0 / 150.0, summary.winRatio)            // wins / rounds
        assertEquals(35.0 / 150.0, summary.headshotRatio)       // headshots / kills
        assertEquals(30000.0 / 150.0, summary.avgDamage)        // damage / rounds
        assertEquals(900, summary.avgSurvivalTimeSeconds)       // (135000 / 150) as Int
    }

    @Test
    fun `guards against divide-by-zero on an empty stat block`() {
        val json = ObjectMapper().readTree("""{"data":{"attributes":{"gameModeStats":{}}}}""")

        val summary = PubgApiClient.mapLifetimeStats("empty", json)

        assertEquals(0.0, summary.kd)
        assertEquals(0.0, summary.winRatio)
        assertEquals(0.0, summary.headshotRatio)
        assertEquals(0.0, summary.avgDamage)
        assertEquals(0, summary.avgSurvivalTimeSeconds)
    }
}
