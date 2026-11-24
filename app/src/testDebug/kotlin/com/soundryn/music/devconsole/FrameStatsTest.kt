package com.soundryn.music.devconsole

import org.junit.Assert.assertEquals
import org.junit.Test

class FrameStatsTest {

    @Test
    fun statsCountFramesOverDeadline() {
        val totals = listOf(5f, 6f, 20f, 7f)
        val deadlines = listOf(8.3f, 8.3f, 8.3f, 8.3f)
        val stats = frameStats(totals, deadlines)
        assertEquals(4, stats.frames)
        assertEquals(1, stats.overDeadline)
        assertEquals(20f, stats.worstMs, 0.0001f)
    }

    @Test
    fun statsPercentile95IsNearestRank() {
        val totals = (1..20).map { it.toFloat() }
        val deadlines = List(20) { 100f }
        val stats = frameStats(totals, deadlines)
        assertEquals(19f, stats.p95Ms, 0.0001f)
    }

    @Test
    fun statsEmptyIsZero() {
        val stats = frameStats(emptyList(), emptyList())
        assertEquals(0, stats.frames)
        assertEquals(0, stats.overDeadline)
        assertEquals(0f, stats.worstMs, 0.0001f)
        assertEquals(0f, stats.p95Ms, 0.0001f)
    }
}
