package com.pubgstats

import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProBaselineServiceTest {

    private val repository = mock(PlayerStatsRepository::class.java)
    private val service = ProBaselineService(repository)

    private fun pro(kd: Double, survival: Int, hs: Double, win: Double, dmg: Double) =
        PlayerStats(null, "pro", kd, survival, hs, win, dmg, isPro = true, fetchedAt = Instant.EPOCH)

    @Test
    fun `single pro is its own baseline`() {
        whenever(repository.findByIsProTrue()).thenReturn(listOf(pro(4.0, 1500, 0.3, 0.2, 500.0)))

        val baseline = service.compute()!!

        assertEquals(4.0, baseline.kd)
        assertEquals(1500.0, baseline.avgSurvivalTimeSeconds)
        assertEquals(0.3, baseline.headshotRatio)
        assertEquals(0.2, baseline.winRatio)
        assertEquals(500.0, baseline.avgDamage)
    }

    @Test
    fun `odd count uses the middle pro per metric`() {
        whenever(repository.findByIsProTrue()).thenReturn(
            listOf(
                pro(1.0, 1000, 0.1, 0.1, 300.0),
                pro(5.0, 1400, 0.5, 0.3, 700.0),
                pro(3.0, 1200, 0.3, 0.2, 500.0),
            )
        )

        val baseline = service.compute()!!

        assertEquals(3.0, baseline.kd)
        assertEquals(1200.0, baseline.avgSurvivalTimeSeconds)
        assertEquals(0.3, baseline.headshotRatio)
        assertEquals(0.2, baseline.winRatio)
        assertEquals(500.0, baseline.avgDamage)
    }

    @Test
    fun `even count averages the two middle pros per metric`() {
        whenever(repository.findByIsProTrue()).thenReturn(
            listOf(
                pro(1.0, 1000, 0.1, 0.1, 300.0),
                pro(2.0, 1200, 0.2, 0.2, 400.0),
                pro(4.0, 1400, 0.4, 0.4, 600.0),
                pro(5.0, 1600, 0.5, 0.5, 700.0),
            )
        )

        val baseline = service.compute()!!

        assertEquals(3.0, baseline.kd)
        assertEquals(1300.0, baseline.avgSurvivalTimeSeconds)
        assertEquals(0.3, baseline.headshotRatio, 1e-9)
        assertEquals(0.3, baseline.winRatio, 1e-9)
        assertEquals(500.0, baseline.avgDamage)
    }

    @Test
    fun `empty pro list yields no baseline`() {
        whenever(repository.findByIsProTrue()).thenReturn(emptyList())

        assertNull(service.compute())
    }
}
