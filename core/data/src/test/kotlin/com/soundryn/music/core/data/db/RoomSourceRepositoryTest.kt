package com.soundryn.music.core.data.db

import com.soundryn.music.core.domain.DomainError
import com.soundryn.music.core.domain.Outcome
import com.soundryn.music.core.domain.Source
import com.soundryn.music.core.domain.SourceId
import com.soundryn.music.core.domain.SourceKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomSourceRepositoryTest {

    private lateinit var database: SoundrynDatabase
    private lateinit var repository: RoomSourceRepository

    @Before
    fun setup() {
        database = createTestDatabase()
        repository = RoomSourceRepository(database.sourceDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun save_then_observeSources_returns_source() = runTest {
        val source = Source(
            id = SourceId("local"),
            kind = SourceKind.LOCAL,
            displayName = "Local Phone",
            enabled = true,
            addonId = null
        )
        val outcome = repository.save(source)
        assertTrue(outcome is Outcome.Success)

        val sources = repository.observeSources().first()
        assertEquals(1, sources.size)
        assertEquals(source, sources[0])
    }

    @Test
    fun setEnabled_missing_returns_not_found() = runTest {
        val outcome = repository.setEnabled(SourceId("missing"), true)
        assertTrue(outcome is Outcome.Failure)
        assertEquals(DomainError.NotFound, (outcome as Outcome.Failure).error)
    }

    @Test
    fun remove_missing_is_success() = runTest {
        val outcome = repository.remove(SourceId("missing"))
        assertTrue(outcome is Outcome.Success)
    }

    @Test
    fun unknown_kind_row_is_skipped() = runTest {
        val validSource = Source(
            id = SourceId("local"),
            kind = SourceKind.LOCAL,
            displayName = "Local Phone",
            enabled = true,
            addonId = null
        )
        repository.save(validSource)

        val unknownEntity = SourceEntity(
            id = "future-src",
            kind = "FUTURE_KIND",
            displayName = "Future Service",
            enabled = true,
            addonId = null
        )
        database.sourceDao().upsert(unknownEntity)

        val sources = repository.observeSources().first()
        assertEquals(1, sources.size)
        assertEquals(validSource, sources[0])
    }
}
