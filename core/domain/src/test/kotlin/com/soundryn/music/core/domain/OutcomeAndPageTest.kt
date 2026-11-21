package com.soundryn.music.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OutcomeAndPageTest {

    @Test
    fun map_transforms_success() {
        val outcome = Outcome.Success(2).map { it * 2 }
        assertTrue(outcome is Outcome.Success)
        assertEquals(4, (outcome as Outcome.Success).value)
    }

    @Test
    fun map_keeps_failure_error() {
        val failure: Outcome<Int> = Outcome.Failure(DomainError.NotFound)
        val mapped = failure.map { it * 2 }
        assertTrue(mapped is Outcome.Failure)
        assertEquals(DomainError.NotFound, (mapped as Outcome.Failure).error)
    }

    @Test
    fun getOrNull_is_null_on_failure() {
        val failure: Outcome<Int> = Outcome.Failure(DomainError.Network)
        assertNull(failure.getOrNull())

        val success: Outcome<Int> = Outcome.Success(42)
        assertEquals(42, success.getOrNull())
    }

    @Test
    fun pageRequest_limit_below_one_becomes_one() {
        val reqZero = PageRequest.of(null, 0)
        assertEquals(1, reqZero.limit)

        val reqNegative = PageRequest.of("cursor", -5)
        assertEquals(1, reqNegative.limit)
    }

    @Test
    fun pageRequest_limit_above_200_becomes_200() {
        val req999 = PageRequest.of(null, 999)
        assertEquals(200, req999.limit)

        val req201 = PageRequest.of("cursor", 201)
        assertEquals(200, req201.limit)
    }
}
