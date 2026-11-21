package com.soundryn.music.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextRulesTest {

    @Test
    fun sortKey_strips_leading_the() {
        assertEquals("beatles", sortKeyOf("The Beatles"))
    }

    @Test
    fun sortKey_strips_a_and_an() {
        assertEquals("tribe called quest", sortKeyOf("A Tribe Called Quest"))
        assertEquals("honest mistake", sortKeyOf("An Honest Mistake"))
    }

    @Test
    fun sortKey_keeps_article_only_title() {
        assertEquals("the", sortKeyOf("The"))
        assertEquals("a", sortKeyOf("A"))
    }

    @Test
    fun sortKey_removes_diacritics_and_lowercases() {
        assertEquals("edith piaf", sortKeyOf("Édith Piaf"))
    }

    @Test
    fun sortKey_trims_and_collapses_spaces() {
        assertEquals("daft punk", sortKeyOf("  Daft   Punk "))
    }

    @Test
    fun sortKey_keeps_non_latin() {
        assertEquals("кино", sortKeyOf("Кино"))
    }

    @Test
    fun sortKey_empty_stays_empty() {
        assertEquals("", sortKeyOf(""))
        assertEquals("", sortKeyOf("   "))
    }

    @Test
    fun duration_null_and_negative_are_null() {
        assertNull(formatDurationMs(null))
        assertNull(formatDurationMs(-1L))
    }

    @Test
    fun duration_floors_to_seconds() {
        assertEquals("0:59", formatDurationMs(59_999L))
    }

    @Test
    fun duration_formats_minutes_and_hours() {
        assertEquals("0:00", formatDurationMs(0L))
        assertEquals("1:01", formatDurationMs(61_000L))
        assertEquals("1:00:00", formatDurationMs(3_600_000L))
        assertEquals("1:02:03", formatDurationMs(3_723_000L))
    }
}
