package com.pubgstats

import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import kotlin.test.Test

class ScheduledStatsRefreshTest {

    private val ingestion = mock(StatsIngestionService::class.java)
    private val job = ScheduledStatsRefresh(ingestion)

    @Test
    fun `scheduled job delegates to ingestion refresh`() {
        `when`(ingestion.refresh()).thenReturn(null)

        job.refresh()

        verify(ingestion, times(1)).refresh()
    }
}
