package com.pubgstats

import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class StatsIngestionServiceTest {

    private val client = mock(PubgApiClient::class.java)
    private val repository = mock(PlayerStatsRepository::class.java)
    private val props = PubgApiProperties(cacheTtlHours = 24, players = listOf("you", "proA"))
    private val service = StatsIngestionService(client, repository, props)

    private fun row(name: String, fetchedAt: Instant) = PlayerStats(
        playerName = name, kd = 2.0, avgSurvivalTimeSeconds = 900,
        headshotRatio = 0.2, winRatio = 0.1, avgDamage = 250.0,
        isPro = false, fetchedAt = fetchedAt,
    )

    @Test
    fun `fresh cached row is reused without calling the client`() {
        val fresh = row("you", Instant.now().minus(1, ChronoUnit.HOURS))
        `when`(repository.findFirstByPlayerNameOrderByFetchedAtDesc("you")).thenReturn(fresh)

        val result = service.ingest("you", isPro = false)

        assertSame(fresh, result)
        verify(client, never()).fetchPlayerSummary("you")
        verify(repository, never()).save(any())
    }

    @Test
    fun `stale cached row triggers a fresh fetch and save`() {
        val stale = row("you", Instant.now().minus(48, ChronoUnit.HOURS))
        `when`(repository.findFirstByPlayerNameOrderByFetchedAtDesc("you")).thenReturn(stale)
        `when`(client.fetchPlayerSummary("you")).thenReturn(
            PlayerStatsSummary("you", kd = 3.0, avgSurvivalTimeSeconds = 800,
                headshotRatio = 0.3, winRatio = 0.15, avgDamage = 300.0)
        )
        `when`(repository.save(any())).thenAnswer { it.getArgument(0) }

        val result = service.ingest("you", isPro = false)

        assertEquals(3.0, result?.kd)
        verify(client, times(1)).fetchPlayerSummary("you")
        verify(repository, times(1)).save(any())
    }

    @Test
    fun `missing cache triggers a fetch`() {
        `when`(repository.findFirstByPlayerNameOrderByFetchedAtDesc("proA")).thenReturn(null)
        `when`(client.fetchPlayerSummary("proA")).thenReturn(
            PlayerStatsSummary("proA", kd = 5.0, avgSurvivalTimeSeconds = 1000,
                headshotRatio = 0.4, winRatio = 0.25, avgDamage = 400.0)
        )
        `when`(repository.save(any())).thenAnswer { it.getArgument(0) }

        val result = service.ingest("proA", isPro = true)

        assertEquals(5.0, result?.kd)
        assertEquals(true, result?.isPro)
        verify(client, times(1)).fetchPlayerSummary("proA")
    }

    @Test
    fun `ingestAll tags first player as user and rest as pros`() {
        `when`(repository.findFirstByPlayerNameOrderByFetchedAtDesc(any())).thenReturn(null)
        `when`(client.fetchPlayerSummary(any())).thenAnswer {
            val name = it.getArgument<String>(0)
            PlayerStatsSummary(name, kd = 1.0, avgSurvivalTimeSeconds = 1,
                headshotRatio = 0.0, winRatio = 0.0, avgDamage = 0.0)
        }
        `when`(repository.save(any())).thenAnswer { it.getArgument(0) }

        val results = service.ingestAll()

        assertEquals(listOf("you" to false, "proA" to true), results.map { it.playerName to it.isPro })
    }

    @Test
    fun `refresh warns when no api key is configured`() {
        val service = StatsIngestionService(client, repository, props.copy(apiKey = ""))

        val warning = service.refresh()

        assertTrue(warning!!.contains("API key"))
        verify(client, never()).fetchPlayerSummary(any())
    }

    @Test
    fun `refresh warns when a player cannot be refreshed and has no cache`() {
        val service = StatsIngestionService(client, repository, props.copy(apiKey = "k"))
        `when`(repository.findFirstByPlayerNameOrderByFetchedAtDesc(any())).thenReturn(null)
        // "you" fetches fine; "proA" fails (client swallows the error and returns null).
        `when`(client.fetchPlayerSummary("you")).thenReturn(
            PlayerStatsSummary("you", 1.0, 1, 0.0, 0.0, 0.0)
        )
        `when`(client.fetchPlayerSummary("proA")).thenReturn(null)
        `when`(repository.save(any())).thenAnswer { it.getArgument(0) }

        val warning = service.refresh()

        assertTrue(warning!!.contains("1 of 2"))
    }

    @Test
    fun `refresh returns null when all players succeed`() {
        val service = StatsIngestionService(client, repository, props.copy(apiKey = "k"))
        `when`(repository.findFirstByPlayerNameOrderByFetchedAtDesc(any())).thenReturn(null)
        `when`(client.fetchPlayerSummary(any())).thenAnswer {
            PlayerStatsSummary(it.getArgument(0), 1.0, 1, 0.0, 0.0, 0.0)
        }
        `when`(repository.save(any())).thenAnswer { it.getArgument(0) }

        assertNull(service.refresh())
    }
}
