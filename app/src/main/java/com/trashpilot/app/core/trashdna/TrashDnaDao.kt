package com.trashpilot.app.core.trashdna

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface TrashDnaDao {
    @Insert
    suspend fun insert(session: TrashDnaSessionEntity)

    @Query("SELECT * FROM trash_dna_sessions ORDER BY timestampMillis DESC, id DESC")
    suspend fun loadAll(): List<TrashDnaSessionEntity>

    @Query("DELETE FROM trash_dna_sessions")
    suspend fun clearAll()

    @Insert
    suspend fun insertAll(sessions: List<TrashDnaSessionEntity>)

    @Transaction
    suspend fun replaceAll(sessions: List<TrashDnaSessionEntity>) {
        clearAll()
        insertAll(sessions)
    }
}
