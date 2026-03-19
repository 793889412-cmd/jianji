package com.example.subtitlevideoeditor.ui

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Editor : Screen("editor")
    object History : Screen("history")
    object VideoPreview : Screen("video_preview")
}
