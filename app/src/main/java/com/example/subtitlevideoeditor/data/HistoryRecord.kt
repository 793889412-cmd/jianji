package com.example.subtitlevideoeditor.data

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_records")
data class HistoryRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val outputPath: String,
    val outputUri: String,
    val createdAt: Long = System.currentTimeMillis(),
    val segmentsJson: String,
    val duration: Long = 0L
)
