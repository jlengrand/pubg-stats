package com.pubgstats

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoachServiceTest {

    private val service = CoachService()

    private fun stats(kd: Double, survival: Int, hs: Double, win: Double, dmg: Double) =
        PlayerStats(null, "you", kd, survival, hs, win, dmg, isPro = false, fetchedAt = Instant.EPOCH)

    private val baseline = ProBaseline(
        kd = 4.0,
        avgSurvivalTimeSeconds = 1500.0,
        headshotRatio = 0.4,
        winRatio = 0.2,
        avgDamage = 500.0,
    )

    @Test
    fun `suggestions are ranked by gap, biggest first`() {
        // Below on K/D (~50%), headshot (~25%), avg damage (~10%).
        val suggestions = service.coach(stats(2.0, 1500, 0.3, 0.2, 450.0), baseline)

        assertEquals(listOf("K/D", "Headshot Ratio", "Avg Damage"), suggestions.map { it.metric })
        val gaps = suggestions.map { it.gap }
        assertTrue(gaps.zipWithNext().all { (a, b) -> a >= b }, "gaps should be descending")
        assertEquals(50, suggestions.first().priority)
    }

    @Test
    fun `metrics at or above baseline are excluded`() {
        // Meets/beats everything except headshot ratio.
        val suggestions = service.coach(stats(5.0, 1600, 0.3, 0.25, 600.0), baseline)

        assertEquals(listOf("Headshot Ratio"), suggestions.map { it.metric })
    }

    @Test
    fun `each metric carries its own tip`() {
        val suggestions = service.coach(stats(1.0, 1000, 0.1, 0.1, 300.0), baseline)

        val tipByMetric = suggestions.associate { it.metric to it.tip }
        assertTrue(tipByMetric.getValue("Headshot Ratio").contains("aim training"))
        assertTrue(tipByMetric.getValue("Avg Survival Time").contains("cautiously"))
        assertTrue(tipByMetric.getValue("Win Ratio").contains("placement"))
    }

    @Test
    fun `null baseline yields no suggestions`() {
        assertEquals(emptyList(), service.coach(stats(1.0, 1000, 0.1, 0.1, 300.0), null))
    }
}
