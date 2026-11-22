package com.soundryn.music.core.data.db

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import org.robolectric.RuntimeEnvironment

fun createTestDatabase(): SoundrynDatabase {
    val context: Context = RuntimeEnvironment.getApplication()
    return Room.inMemoryDatabaseBuilder(context, SoundrynDatabase::class.java)
        .setDriver(AndroidSQLiteDriver())
        .build()
}
