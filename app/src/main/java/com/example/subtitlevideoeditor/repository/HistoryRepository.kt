package com.example.subtitlevideoeditor.repository

import android.content.Context
import com.example.subtitlevideoeditor.data.AppDatabase
import com.example.subtitlevideoeditor.data.HistoryRecord
import kotlinx.coroutines.flow.Flow

class HistoryRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val historyDao = db.historyRecordDao()

    fun getAllHistory(): Flow<List<HistoryRecord>> = historyDao.getAllHistory()

    suspend fun getHistoryById(id: Long): HistoryRecord? = historyDao.getHistoryById(id)

    suspend fun insertHistory(record: HistoryRecord): Long = historyDao.insertHistory(record)

    suspend fun updateHistory(record: HistoryRecord) = historyDao.updateHistory(record)

    suspend fun deleteHistory(record: HistoryRecord) = historyDao.deleteHistory(record)

    suspend fun deleteAllHistory() = historyDao.deleteAllHistory()
}
