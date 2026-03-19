package com.example.subtitlevideoeditor.data

import android.net.Uri

data class VideoFile(
    val id: Long = System.currentTimeMillis(),
    val uri: Uri,
    val name: String,
    val path: String,
    val duration: Long = 0L,
    val size: Long = 0L,
    val subtitles: List<SubtitleItem> = emptyList()
)
