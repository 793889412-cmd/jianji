package com.example.subtitlevideoeditor.data

import android.net.Uri

data class ClipSegment(
    val id: Long = System.currentTimeMillis(),
    val videoId: Long,
    val videoUri: Uri,
    val videoName: String,
    val subtitleId: Long,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val subtitleContent: String,
    val order: Int = 0
)
