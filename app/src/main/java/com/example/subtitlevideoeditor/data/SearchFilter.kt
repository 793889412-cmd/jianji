package com.example.subtitlevideoeditor.data

enum class SearchType {
    CONTENT,
    VIDEO_NAME,
    BOTH
}

enum class MatchType {
    ANY,
    ALL
}

enum class DurationFilter {
    ALL,
    SHORT,
    MEDIUM,
    LONG
}

enum class SortType {
    TIME,
    VIDEO,
    RELEVANCE
}

data class SearchFilter(
    val keyword: String = "",
    val searchType: SearchType = SearchType.BOTH,
    val matchType: MatchType = MatchType.ANY,
    val durationFilter: DurationFilter = DurationFilter.ALL,
    val sortType: SortType = SortType.TIME
)
