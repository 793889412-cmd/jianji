package com.example.subtitlevideoeditor.data

data class SubtitleItem(
    val id: Long = System.currentTimeMillis(),
    val index: Int = 0,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L,
    val content: String = "",
    val videoId: Long = 0L,
    val videoName: String = ""
) {
    val durationMs: Long
        get() = endTimeMs - startTimeMs

    val durationSeconds: Double
        get() = durationMs / 1000.0
}
