package com.pubgstats

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DataJpaTest
@AutoConfigureTestDatabase
class PlayerStatsRepositoryTest {

    @Autowired
    lateinit var repository: PlayerStatsRepository

    private fun stats(name: String, isPro: Boolean) = PlayerStats(
        playerName = name,
        kd = 2.0,
        avgSurvivalTimeSeconds = 900,
        headshotRatio = 0.2,
        winRatio = 0.1,
        avgDamage = 250.0,
        isPro = isPro,
        fetchedAt = Instant.now(),
    )

    @Test
    fun `findByPlayerName returns the matching player and null otherwise`() {
        repository.save(stats("you", isPro = false))

        assertEquals("you", repository.findByPlayerName("you")?.playerName)
        assertNull(repository.findByPlayerName("nobody"))
    }

    @Test
    fun `findByIsProTrue returns only pros`() {
        repository.save(stats("you", isPro = false))
        repository.save(stats("proA", isPro = true))
        repository.save(stats("proB", isPro = true))

        val pros = repository.findByIsProTrue()
        assertEquals(setOf("proA", "proB"), pros.map { it.playerName }.toSet())
    }
}
