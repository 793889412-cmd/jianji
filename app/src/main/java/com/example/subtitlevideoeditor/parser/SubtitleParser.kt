package com.example.subtitlevideoeditor.parser

import com.example.subtitlevideoeditor.data.SubtitleItem
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

object SubtitleParser {

    fun parseSrt(file: File, videoId: Long, videoName: String): List<SubtitleItem> {
        val subtitles = mutableListOf<SubtitleItem>()
        val inputStream = FileInputStream(file)
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        
        var index = 0
        var line: String?
        var state = 0
        var startTime = 0L
        var endTime = 0L
        var content = StringBuilder()

        while (reader.readLine().also { line = it } != null) {
            val trimmedLine = line!!.trim()
            
            when (state) {
                0 -> {
                    if (trimmedLine.isNotEmpty() && trimmedLine.all { it.isDigit() }) {
                        index = trimmedLine.toInt()
                        state = 1
                    }
                }
                1 -> {
                    if (trimmedLine.contains("-->")) {
                        val times = parseTimeRange(trimmedLine)
                        startTime = times.first
                        endTime = times.second
                        state = 2
                        content = StringBuilder()
                    }
                }
                2 -> {
                    if (trimmedLine.isEmpty()) {
                        if (content.isNotEmpty()) {
                            subtitles.add(
                                SubtitleItem(
                                    id = System.currentTimeMillis() + index,
                                    index = index,
                                    startTimeMs = startTime,
                                    endTimeMs = endTime,
                                    content = content.toString().trim(),
                                    videoId = videoId,
                                    videoName = videoName
                                )
                            )
                        }
                        state = 0
                    } else {
                        if (content.isNotEmpty()) {
                            content.append("\n")
                        }
                        content.append(trimmedLine)
                    }
                }
            }
        }
        
        if (state == 2 && content.isNotEmpty()) {
            subtitles.add(
                SubtitleItem(
                    id = System.currentTimeMillis() + index,
                    index = index,
                    startTimeMs = startTime,
                    endTimeMs = endTime,
                    content = content.toString().trim(),
                    videoId = videoId,
                    videoName = videoName
                )
            )
        }
        
        reader.close()
        inputStream.close()
        return subtitles
    }

    fun parseVtt(file: File, videoId: Long, videoName: String): List<SubtitleItem> {
        val subtitles = mutableListOf<SubtitleItem>()
        val inputStream = FileInputStream(file)
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        
        var index = 0
        var line: String?
        var state = 0
        var startTime = 0L
        var endTime = 0L
        var content = StringBuilder()

        while (reader.readLine().also { line = it } != null) {
            val trimmedLine = line!!.trim()
            
            if (trimmedLine.startsWith("WEBVTT") || trimmedLine.startsWith("X-TIMESTAMP")) {
                continue
            }
            
            when (state) {
                0 -> {
                    if (trimmedLine.contains("-->")) {
                        val times = parseVttTimeRange(trimmedLine)
                        startTime = times.first
                        endTime = times.second
                        index++
                        state = 1
                        content = StringBuilder()
                    } else if (trimmedLine.isNotEmpty() && trimmedLine.all { it.isDigit() }) {
                        state = 0
                    }
                }
                1 -> {
                    if (trimmedLine.isEmpty()) {
                        if (content.isNotEmpty()) {
                            subtitles.add(
                                SubtitleItem(
                                    id = System.currentTimeMillis() + index,
                                    index = index,
                                    startTimeMs = startTime,
                                    endTimeMs = endTime,
                                    content = content.toString().trim(),
                                    videoId = videoId,
                                    videoName = videoName
                                )
                            )
                        }
                        state = 0
                    } else {
                        if (!trimmedLine.startsWith("NOTE") && !trimmedLine.startsWith("STYLE")) {
                            if (content.isNotEmpty()) {
                                content.append("\n")
                            }
                            content.append(trimmedLine)
                        }
                    }
                }
            }
        }
        
        if (state == 1 && content.isNotEmpty()) {
            subtitles.add(
                SubtitleItem(
                    id = System.currentTimeMillis() + index,
                    index = index,
                    startTimeMs = startTime,
                    endTimeMs = endTime,
                    content = content.toString().trim(),
                    videoId = videoId,
                    videoName = videoName
                )
            )
        }
        
        reader.close()
        inputStream.close()
        return subtitles
    }

    fun parseAss(file: File, videoId: Long, videoName: String): List<SubtitleItem> {
        val subtitles = mutableListOf<SubtitleItem>()
        val inputStream = FileInputStream(file)
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        
        var index = 0
        var line: String?
        var inEvents = false

        while (reader.readLine().also { line = it } != null) {
            val trimmedLine = line!!.trim()
            
            if (trimmedLine.startsWith("[Events]")) {
                inEvents = true
                continue
            }
            
            if (inEvents && trimmedLine.startsWith("Dialogue:")) {
                val parts = trimmedLine.substringAfter("Dialogue:").split(",", limit = 10)
                if (parts.size >= 10) {
                    val startTime = parseAssTime(parts[1].trim())
                    val endTime = parseAssTime(parts[2].trim())
                    val content = removeAssFormatting(parts[9].trim())
                    
                    if (content.isNotEmpty()) {
                        index++
                        subtitles.add(
                            SubtitleItem(
                                id = System.currentTimeMillis() + index,
                                index = index,
                                startTimeMs = startTime,
                                endTimeMs = endTime,
                                content = content,
                                videoId = videoId,
                                videoName = videoName
                            )
                        )
                    }
                }
            }
        }
        
        reader.close()
        inputStream.close()
        return subtitles
    }

    private fun parseTimeRange(line: String): Pair<Long, Long> {
        val pattern = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})\\s*-->\\s*(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})")
        val matcher = pattern.matcher(line)
        if (matcher.find()) {
            val startH = matcher.group(1)?.toInt() ?: 0
            val startM = matcher.group(2)?.toInt() ?: 0
            val startS = matcher.group(3)?.toInt() ?: 0
            val startMs = matcher.group(4)?.toInt() ?: 0
            
            val endH = matcher.group(5)?.toInt() ?: 0
            val endM = matcher.group(6)?.toInt() ?: 0
            val endS = matcher.group(7)?.toInt() ?: 0
            val endMs = matcher.group(8)?.toInt() ?: 0
            
            val startTime = (startH * 3600 + startM * 60 + startS) * 1000L + startMs
            val endTime = (endH * 3600 + endM * 60 + endS) * 1000L + endMs
            
            return Pair(startTime, endTime)
        }
        return Pair(0L, 0L)
    }

    private fun parseVttTimeRange(line: String): Pair<Long, Long> {
        val pattern = Pattern.compile("(?:(\\d{2}):)?(\\d{2}):(\\d{2})[.,](\\d{3})\\s*-->\\s*(?:(\\d{2}):)?(\\d{2}):(\\d{2})[.,](\\d{3})")
        val matcher = pattern.matcher(line)
        if (matcher.find()) {
            val startH = matcher.group(1)?.toInt() ?: 0
            val startM = matcher.group(2)?.toInt() ?: 0
            val startS = matcher.group(3)?.toInt() ?: 0
            val startMs = matcher.group(4)?.toInt() ?: 0
            
            val endH = matcher.group(5)?.toInt() ?: 0
            val endM = matcher.group(6)?.toInt() ?: 0
            val endS = matcher.group(7)?.toInt() ?: 0
            val endMs = matcher.group(8)?.toInt() ?: 0
            
            val startTime = (startH * 3600 + startM * 60 + startS) * 1000L + startMs
            val endTime = (endH * 3600 + endM * 60 + endS) * 1000L + endMs
            
            return Pair(startTime, endTime)
        }
        return Pair(0L, 0L)
    }

    private fun parseAssTime(timeStr: String): Long {
        val parts = timeStr.split(":")
        if (parts.size >= 3) {
            val hours = parts[0].toIntOrNull() ?: 0
            val minutes = parts[1].toIntOrNull() ?: 0
            val secondsPart = parts[2]
            val seconds = secondsPart.split(".")[0].toIntOrNull() ?: 0
            val centiseconds = secondsPart.split(".")[1].toIntOrNull() ?: 0
            
            return (hours * 3600 + minutes * 60 + seconds) * 1000L + centiseconds * 10L
        }
        return 0L
    }

    private fun removeAssFormatting(text: String): String {
        return text.replace(Regex("\\{[^}]*\\}"), "").trim()
    }

    fun parse(file: File, videoId: Long, videoName: String): List<SubtitleItem> {
        val extension = file.extension.lowercase()
        return when (extension) {
            "srt" -> parseSrt(file, videoId, videoName)
            "vtt" -> parseVtt(file, videoId, videoName)
            "ass", "ssa" -> parseAss(file, videoId, videoName)
            else -> emptyList()
        }
    }
}
