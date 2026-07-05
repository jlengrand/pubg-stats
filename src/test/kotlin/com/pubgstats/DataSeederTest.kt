package com.pubgstats

import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import kotlin.test.Test

class DataSeederTest {

    private val repository = mock(PlayerStatsRepository::class.java)
    private val ingestion = mock(StatsIngestionService::class.java)

    @Test
    fun `blank API key seeds sample data instead of calling the API`() {
        whenever(repository.count()).thenReturn(0)
        val seeder = DataSeeder(repository, ingestion, PubgApiProperties(apiKey = ""))

        seeder.run(null)

        verify(ingestion, never()).ingestAll()
        verify(repository).saveAll(any<Iterable<PlayerStats>>())
    }

    @Test
    fun `blank API key does not re-seed when data already exists`() {
        whenever(repository.count()).thenReturn(4)
        val seeder = DataSeeder(repository, ingestion, PubgApiProperties(apiKey = ""))

        seeder.run(null)

        verify(ingestion, never()).ingestAll()
        verify(repository, never()).saveAll(any<Iterable<PlayerStats>>())
    }

    @Test
    fun `set API key ingests real stats and skips seeding`() {
        val seeder = DataSeeder(repository, ingestion, PubgApiProperties(apiKey = "secret"))

        seeder.run(null)

        verify(ingestion).ingestAll()
        verify(repository, never()).saveAll(any<Iterable<PlayerStats>>())
    }
}
