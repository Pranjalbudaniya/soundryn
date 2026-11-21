package com.soundryn.music.core.data.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver

@Database(entities = [SourceEntity::class], version = 1, exportSchema = true)
abstract class SoundrynDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
}

fun buildSoundrynDatabase(context: Context): SoundrynDatabase {
    return Room.databaseBuilder(
        context = context,
        klass = SoundrynDatabase::class.java,
        name = "soundryn.db"
    )
        .setDriver(AndroidSQLiteDriver())
        .build()
}
