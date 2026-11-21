package com.soundryn.music.core.data.db

// Recipe B: Robolectric with Room's Android driver and Room.inMemoryDatabaseBuilder
//
// Dependencies (core/data/build.gradle.kts):
//   testImplementation(libs.robolectric)
//   testImplementation(libs.kotlinx.coroutines.test)
//   testImplementation(libs.junit)
//
// Setup:
//   @RunWith(RobolectricTestRunner::class)
//   @Config(sdk = [34])
//   val context: Context = RuntimeEnvironment.getApplication()
//   Room.inMemoryDatabaseBuilder(context, SoundrynDatabase::class.java)
//       .setDriver(AndroidSQLiteDriver())
//       .build()

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SourceDaoTest {

    private lateinit var database: SoundrynDatabase
    private lateinit var dao: SourceDao

    @Before
    fun setup() {
        val context: Context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, SoundrynDatabase::class.java)
            .setDriver(AndroidSQLiteDriver())
            .build()
        dao = database.sourceDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun upsert_then_observeAll_returns_row() = runTest {
        val entity = SourceEntity("local", "LOCAL", "Phone", true, null)
        dao.upsert(entity)
        val all = dao.observeAll().first()
        assertEquals(listOf(entity), all)
    }

    @Test
    fun upsert_same_id_replaces_row() = runTest {
        val first = SourceEntity("local", "LOCAL", "First Name", true, null)
        val second = SourceEntity("local", "LOCAL", "Second Name", true, null)
        dao.upsert(first)
        dao.upsert(second)
        val all = dao.observeAll().first()
        assertEquals(listOf(second), all)
    }

    @Test
    fun setEnabled_changes_only_that_row() = runTest {
        val s1 = SourceEntity("local", "LOCAL", "Phone", true, null)
        val s2 = SourceEntity("drive", "GOOGLE_DRIVE", "Drive", true, null)
        dao.upsert(s1)
        dao.upsert(s2)

        val updated = dao.setEnabled("local", false)
        assertEquals(1, updated)

        assertEquals(false, dao.getById("local")?.enabled)
        assertEquals(true, dao.getById("drive")?.enabled)
    }

    @Test
    fun setEnabled_missing_id_returns_zero() = runTest {
        val updated = dao.setEnabled("missing", true)
        assertEquals(0, updated)
    }

    @Test
    fun delete_removes_row() = runTest {
        val entity = SourceEntity("local", "LOCAL", "Phone", true, null)
        dao.upsert(entity)

        val deleted = dao.delete("local")
        assertEquals(1, deleted)
        assertNull(dao.getById("local"))
    }

    @Test
    fun getById_missing_returns_null() = runTest {
        assertNull(dao.getById("missing"))
    }

    @Test
    fun observeAll_orders_by_name_ignoring_case() = runTest {
        dao.upsert(SourceEntity("s1", "LOCAL", "Zeta", true, null))
        dao.upsert(SourceEntity("s2", "LOCAL", "alpha", true, null))
        dao.upsert(SourceEntity("s3", "LOCAL", "Beta", true, null))

        val names = dao.observeAll().first().map { it.displayName }
        assertEquals(listOf("alpha", "Beta", "Zeta"), names)
    }
}
