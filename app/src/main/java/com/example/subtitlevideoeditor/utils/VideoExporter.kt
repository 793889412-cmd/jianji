package com.example.subtitlevideoeditor.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.ReturnCode
import com.example.subtitlevideoeditor.data.ClipSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class VideoExporter(
    private val context: Context,
    private val onProgressUpdate: (Float) -> Unit
) {

    suspend fun exportVideo(
        segments: List<ClipSegment>,
        title: String? = null
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            if (segments.isEmpty()) {
                return@withContext ExportResult.Error("没有视频片段")
            }

            val outputDir = getOutputDirectory()
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val outputFileName = generateFileName(segments, title)
            val outputFile = File(outputDir, outputFileName)

            if (segments.size == 1) {
                val segment = segments.first()
                val result = cutSingleVideo(segment, outputFile)
                if (result.isSuccess) {
                    ExportResult.Success(
                        outputPath = outputFile.absolutePath,
                        outputUri = Uri.fromFile(outputFile).toString(),
                        duration = segments.sumOf { it.endTimeMs - it.startTimeMs }
                    )
                } else {
                    ExportResult.Error(result.message ?: "导出失败")
                }
            } else {
                val result = mergeMultipleVideos(segments, outputFile)
                if (result.isSuccess) {
                    ExportResult.Success(
                        outputPath = outputFile.absolutePath,
                        outputUri = Uri.fromFile(outputFile).toString(),
                        duration = segments.sumOf { it.endTimeMs - it.startTimeMs }
                    )
                } else {
                    ExportResult.Error(result.message ?: "导出失败")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ExportResult.Error(e.message ?: "导出失败")
        }
    }

    private fun getOutputDirectory(): File {
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val appDir = File(moviesDir, "智能字幕剪辑工具")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        return appDir
    }

    private fun generateFileName(segments: List<ClipSegment>, title: String?): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val safeTitle = if (!title.isNullOrEmpty()) {
            title.replace(Regex("[^a-zA-Z0-9\u4e00-\u9fa5]"), "_").take(30)
        } else {
            val firstSubtitle = segments.firstOrNull()?.subtitleContent?.take(20) ?: "剪辑"
            firstSubtitle.replace(Regex("[^a-zA-Z0-9\u4e00-\u9fa5]"), "_")
        }
        return "${safeTitle}_${timestamp}.mp4"
    }

    private fun cutSingleVideo(segment: ClipSegment, outputFile: File): FFmpegResult {
        val inputPath = FFmpegKitConfig.getSafParameter(context, segment.videoUri, "r")
        val startTime = formatTimeForFFmpeg(segment.startTimeMs)
        val duration = formatTimeForFFmpeg(segment.endTimeMs - segment.startTimeMs)

        val command = "-y -ss $startTime -i $inputPath -t $duration -c:v libx264 -c:a aac -strict experimental ${outputFile.absolutePath}"

        val session = FFmpegKit.execute(command)
        
        return if (ReturnCode.isSuccess(session.returnCode)) {
            FFmpegResult(true, null)
        } else {
            FFmpegResult(false, session.output)
        }
    }

    private fun mergeMultipleVideos(segments: List<ClipSegment>, outputFile: File): FFmpegResult {
        val tempDir = File(context.cacheDir, "video_temp")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }

        val clipFiles = mutableListOf<File>()
        try {
            segments.forEachIndexed { index, segment ->
                onProgressUpdate(index.toFloat() / (segments.size + 1))
                
                val clipFile = File(tempDir, "clip_$index.mp4")
                val result = cutSingleVideo(segment, clipFile)
                if (!result.isSuccess) {
                    return FFmpegResult(false, result.message)
                }
                clipFiles.add(clipFile)
            }

            onProgressUpdate(segments.size.toFloat() / (segments.size + 1))

            val concatFile = File(tempDir, "concat.txt")
            concatFile.writeText(clipFiles.joinToString("\n") { "file '${it.absolutePath}'" })

            val command = "-y -f concat -safe 0 -i ${concatFile.absolutePath} -c:v libx264 -c:a aac -strict experimental ${outputFile.absolutePath}"
            val session = FFmpegKit.execute(command)

            onProgressUpdate(1f)

            return if (ReturnCode.isSuccess(session.returnCode)) {
                FFmpegResult(true, null)
            } else {
                FFmpegResult(false, session.output)
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun formatTimeForFFmpeg(ms: Long): String {
        val seconds = ms / 1000.0
        return String.format("%.3f", seconds)
    }

    private data class FFmpegResult(
        val isSuccess: Boolean,
        val message: String?
    )
}

sealed class ExportResult {
    data class Success(
        val outputPath: String,
        val outputUri: String,
        val duration: Long
    ) : ExportResult()

    data class Error(val message: String) : ExportResult()
}
