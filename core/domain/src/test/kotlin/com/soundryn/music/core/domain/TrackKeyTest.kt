package com.soundryn.music.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackKeyTest {

    @Test
    fun encode_simple_ids_joins_with_slash() {
        val outcome = TrackKey.of("local", "42")
        assertTrue(outcome is Outcome.Success)
        val key = (outcome as Outcome.Success).value
        assertEquals("local/42", key.encode())
    }

    @Test
    fun encode_escapes_slash_and_percent_in_item() {
        val outcome = TrackKey.of("local", "a/b%c")
        assertTrue(outcome is Outcome.Success)
        val key = (outcome as Outcome.Success).value
        assertEquals("local/a%2Fb%25c", key.encode())
    }

    @Test
    fun parse_restores_escaped_item() {
        val outcome = TrackKey.parse("local/a%2Fb%25c")
        assertTrue(outcome is Outcome.Success)
        val key = (outcome as Outcome.Success).value
        assertEquals(SourceId("local"), key.sourceId)
        assertEquals("a/b%c", key.itemId)
    }

    @Test
    fun round_trip_keeps_unicode_and_spaces() {
        val item = "Привет мир/💿"
        val keyOutcome = TrackKey.of("local", item)
        assertTrue(keyOutcome is Outcome.Success)
        val key = (keyOutcome as Outcome.Success).value
        val encoded = key.encode()
        val parsedOutcome = TrackKey.parse(encoded)
        assertTrue(parsedOutcome is Outcome.Success)
        val parsedKey = (parsedOutcome as Outcome.Success).value
        assertEquals(item, parsedKey.itemId)
        assertEquals(key, parsedKey)
    }

    @Test
    fun of_blank_source_returns_parse_failure() {
        val outcome = TrackKey.of("", "1")
        assertTrue(outcome is Outcome.Failure)
        assertTrue((outcome as Outcome.Failure).error is DomainError.Parse)
    }

    @Test
    fun of_uppercase_source_returns_parse_failure() {
        val outcome = TrackKey.of("Local", "1")
        assertTrue(outcome is Outcome.Failure)
        assertTrue((outcome as Outcome.Failure).error is DomainError.Parse)
    }

    @Test
    fun of_source_with_slash_returns_parse_failure() {
        val outcome = TrackKey.of("lo/cal", "1")
        assertTrue(outcome is Outcome.Failure)
        assertTrue((outcome as Outcome.Failure).error is DomainError.Parse)
    }

    @Test
    fun parse_rejects_missing_separator() {
        val outcome = TrackKey.parse("local42")
        assertTrue(outcome is Outcome.Failure)
        assertTrue((outcome as Outcome.Failure).error is DomainError.Parse)
    }

    @Test
    fun parse_rejects_empty_parts() {
        val outcome1 = TrackKey.parse("/42")
        assertTrue(outcome1 is Outcome.Failure)
        assertTrue((outcome1 as Outcome.Failure).error is DomainError.Parse)

        val outcome2 = TrackKey.parse("local/")
        assertTrue(outcome2 is Outcome.Failure)
        assertTrue((outcome2 as Outcome.Failure).error is DomainError.Parse)
    }

    @Test
    fun parse_rejects_bad_percent_sequence() {
        val outcome1 = TrackKey.parse("local/%zz")
        assertTrue(outcome1 is Outcome.Failure)
        assertTrue((outcome1 as Outcome.Failure).error is DomainError.Parse)

        val outcome2 = TrackKey.parse("local/50%")
        assertTrue(outcome2 is Outcome.Failure)
        assertTrue((outcome2 as Outcome.Failure).error is DomainError.Parse)
    }
}
