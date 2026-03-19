package com.example.subtitlevideoeditor.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.subtitlevideoeditor.data.ClipSegment
import com.example.subtitlevideoeditor.data.HistoryRecord
import com.example.subtitlevideoeditor.data.SearchFilter
import com.example.subtitlevideoeditor.data.SubtitleItem
import com.example.subtitlevideoeditor.data.VideoFile
import com.example.subtitlevideoeditor.repository.HistoryRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val historyRepository = HistoryRepository(application)
    private val gson = Gson()

    private val _videos = MutableStateFlow<List<VideoFile>>(emptyList())
    val videos: StateFlow<List<VideoFile>> = _videos.asStateFlow()

    private val _currentVideo = MutableStateFlow<VideoFile?>(null)
    val currentVideo: StateFlow<VideoFile?> = _currentVideo.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SubtitleItem>>(emptyList())
    val searchResults: StateFlow<List<SubtitleItem>> = _searchResults.asStateFlow()

    private val _searchFilter = MutableStateFlow(SearchFilter())
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()

    private val _clipSegments = MutableStateFlow<List<ClipSegment>>(emptyList())
    val clipSegments: StateFlow<List<ClipSegment>> = _clipSegments.asStateFlow()

    private val _historyRecords = MutableStateFlow<List<HistoryRecord>>(emptyList())
    val historyRecords: StateFlow<List<HistoryRecord>> = _historyRecords.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

    init {
        loadHistory()
    }

    fun addVideo(video: VideoFile) {
        _videos.value = _videos.value + video
    }

    fun addVideos(newVideos: List<VideoFile>) {
        _videos.value = _videos.value + newVideos
    }

    fun removeVideo(videoId: Long) {
        _videos.value = _videos.value.filterNot { it.id == videoId }
        if (_currentVideo.value?.id == videoId) {
            _currentVideo.value = null
        }
    }

    fun clearVideos() {
        _videos.value = emptyList()
        _currentVideo.value = null
        _searchResults.value = emptyList()
    }

    fun selectVideo(video: VideoFile) {
        _currentVideo.value = video
    }

    fun updateSearchFilter(filter: SearchFilter) {
        _searchFilter.value = filter
        performSearch()
    }

    fun performSearch() {
        val filter = _searchFilter.value
        val allSubtitles = _videos.value.flatMap { it.subtitles }
        
        var results = allSubtitles

        if (filter.keyword.isNotEmpty()) {
            val keywords = filter.keyword.split(" ").filter { it.isNotEmpty() }
            results = results.filter { subtitle ->
                when (filter.searchType) {
                    com.example.subtitlevideoeditor.data.SearchType.CONTENT -> {
                        when (filter.matchType) {
                            com.example.subtitlevideoeditor.data.MatchType.ANY -> keywords.any { subtitle.content.contains(it, ignoreCase = true) }
                            com.example.subtitlevideoeditor.data.MatchType.ALL -> keywords.all { subtitle.content.contains(it, ignoreCase = true) }
                        }
                    }
                    com.example.subtitlevideoeditor.data.SearchType.VIDEO_NAME -> {
                        when (filter.matchType) {
                            com.example.subtitlevideoeditor.data.MatchType.ANY -> keywords.any { subtitle.videoName.contains(it, ignoreCase = true) }
                            com.example.subtitlevideoeditor.data.MatchType.ALL -> keywords.all { subtitle.videoName.contains(it, ignoreCase = true) }
                        }
                    }
                    com.example.subtitlevideoeditor.data.SearchType.BOTH -> {
                        when (filter.matchType) {
                            com.example.subtitlevideoeditor.data.MatchType.ANY -> keywords.any { 
                                subtitle.content.contains(it, ignoreCase = true) || subtitle.videoName.contains(it, ignoreCase = true) 
                            }
                            com.example.subtitlevideoeditor.data.MatchType.ALL -> keywords.all { 
                                subtitle.content.contains(it, ignoreCase = true) || subtitle.videoName.contains(it, ignoreCase = true) 
                            }
                        }
                    }
                }
            }
        }

        results = when (filter.durationFilter) {
            com.example.subtitlevideoeditor.data.DurationFilter.ALL -> results
            com.example.subtitlevideoeditor.data.DurationFilter.SHORT -> results.filter { it.durationMs < 3000 }
            com.example.subtitlevideoeditor.data.DurationFilter.MEDIUM -> results.filter { it.durationMs in 3000..10000 }
            com.example.subtitlevideoeditor.data.DurationFilter.LONG -> results.filter { it.durationMs > 10000 }
        }

        results = when (filter.sortType) {
            com.example.subtitlevideoeditor.data.SortType.TIME -> results.sortedBy { it.startTimeMs }
            com.example.subtitlevideoeditor.data.SortType.VIDEO -> results.sortedBy { it.videoName }
            com.example.subtitlevideoeditor.data.SortType.RELEVANCE -> {
                if (filter.keyword.isEmpty()) {
                    results.sortedBy { it.startTimeMs }
                } else {
                    results.sortedByDescending { subtitle ->
                        filter.keyword.split(" ").count { keyword ->
                            subtitle.content.contains(keyword, ignoreCase = true)
                        }
                    }
                }
            }
        }

        _searchResults.value = results
    }

    fun addClipSegment(subtitle: SubtitleItem, videoUri: Uri) {
        val order = _clipSegments.value.size
        val segment = ClipSegment(
            videoId = subtitle.videoId,
            videoUri = videoUri,
            videoName = subtitle.videoName,
            subtitleId = subtitle.id,
            startTimeMs = subtitle.startTimeMs,
            endTimeMs = subtitle.endTimeMs,
            subtitleContent = subtitle.content,
            order = order
        )
        _clipSegments.value = _clipSegments.value + segment
    }

    fun removeClipSegment(segmentId: Long) {
        _clipSegments.value = _clipSegments.value
            .filterNot { it.id == segmentId }
            .mapIndexed { index, segment ->
                segment.copy(order = index)
            }
    }

    fun clearClipSegments() {
        _clipSegments.value = emptyList()
    }

    fun reorderClipSegments(fromIndex: Int, toIndex: Int) {
        val currentList = _clipSegments.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _clipSegments.value = currentList.mapIndexed { index, segment ->
                segment.copy(order = index)
            }
        }
    }

    fun checkDuplicates(): List<ClipSegment> {
        val seen = mutableSetOf<Long>()
        val duplicates = mutableListOf<ClipSegment>()
        _clipSegments.value.forEach { segment ->
            if (seen.contains(segment.subtitleId)) {
                duplicates.add(segment)
            } else {
                seen.add(segment.subtitleId)
            }
        }
        return duplicates
    }

    fun loadHistory() {
        viewModelScope.launch {
            historyRepository.getAllHistory().collect { records ->
                _historyRecords.value = records
            }
        }
    }

    fun saveHistory(title: String, outputPath: String, outputUri: String, duration: Long) {
        viewModelScope.launch {
            val segmentsJson = gson.toJson(_clipSegments.value)
            val record = HistoryRecord(
                title = title,
                outputPath = outputPath,
                outputUri = outputUri,
                segmentsJson = segmentsJson,
                duration = duration
            )
            historyRepository.insertHistory(record)
        }
    }

    fun loadHistoryToClips(record: HistoryRecord) {
        val type = object : TypeToken<List<ClipSegment>>() {}.type
        val segments: List<ClipSegment> = gson.fromJson(record.segmentsJson, type)
        _clipSegments.value = segments
    }

    fun deleteHistory(record: HistoryRecord) {
        viewModelScope.launch {
            historyRepository.deleteHistory(record)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.deleteAllHistory()
        }
    }

    fun setExporting(exporting: Boolean) {
        _isExporting.value = exporting
    }

    fun setExportProgress(progress: Float) {
        _exportProgress.value = progress
    }
}
