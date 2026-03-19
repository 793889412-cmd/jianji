package com.example.subtitlevideoeditor.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryRecordDao {
    @Query("SELECT * FROM history_records ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<HistoryRecord>>

    @Query("SELECT * FROM history_records WHERE id = :id")
    suspend fun getHistoryById(id: Long): HistoryRecord?

    @Insert
    suspend fun insertHistory(record: HistoryRecord): Long

    @Update
    suspend fun updateHistory(record: HistoryRecord)

    @Delete
    suspend fun deleteHistory(record: HistoryRecord)

    @Query("DELETE FROM history_records")
    suspend fun deleteAllHistory()
}
