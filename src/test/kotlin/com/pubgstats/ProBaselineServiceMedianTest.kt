package com.pubgstats

import kotlin.test.Test
import kotlin.test.assertEquals

/** Self-check for the median helper on a known small set. */
class ProBaselineServiceMedianTest {

    @Test
    fun `median of odd count picks the middle`() {
        assertEquals(3.0, ProBaselineService.median(listOf(5.0, 1.0, 3.0)))
    }

    @Test
    fun `median of even count averages the two middles`() {
        assertEquals(2.5, ProBaselineService.median(listOf(4.0, 1.0, 2.0, 3.0)))
    }
}
