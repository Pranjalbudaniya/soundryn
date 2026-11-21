package com.soundryn.music.core.data.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources ORDER BY displayName COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources WHERE id = :id")
    suspend fun getById(id: String): SourceEntity?

    @Upsert
    suspend fun upsert(entity: SourceEntity)

    @Query("UPDATE sources SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean): Int

    @Query("DELETE FROM sources WHERE id = :id")
    suspend fun delete(id: String): Int
}
