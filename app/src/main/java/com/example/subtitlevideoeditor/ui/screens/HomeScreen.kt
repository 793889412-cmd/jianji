package com.example.subtitlevideoeditor.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.subtitlevideoeditor.data.VideoFile
import com.example.subtitlevideoeditor.parser.SubtitleParser
import com.example.subtitlevideoeditor.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel = viewModel(),
    onVideoSelected: (VideoFile) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val videos by viewModel.videos.collectAsState()
    var showPermissionDialog by remember { mutableStateOf(false) }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch(Dispatchers.IO {
                loadVideo(context, it, viewModel)
            }
        }
    }

    val pickFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch(Dispatchers.IO {
                loadFolder(context, it, viewModel)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
        } else {
            showPermissionDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文件管理") },
                actions = {
                    if (videos.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearVideos() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "清空")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
            ) {
                FloatingActionButton(
                    onClick = { pickVideoLauncher.launch("video/*") },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = "选择视频")
                }
                FloatingActionButton(
                    onClick = { pickFolderLauncher.launch(null) }
                ) {
                    Icon(Icons.Default.Folder, contentDescription = "选择文件夹")
                }
            }
        }
    ) { paddingValues ->
        if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("还没有添加视频", style = MaterialTheme.typography.titleLarge)
                    Text("点击右下角按钮添加视频或文件夹", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(videos) { video ->
                    VideoListItem(
                        video = video,
                        onClick = { onVideoSelected(video) },
                        onRemove = { viewModel.removeVideo(video.id) }
                    )
                }
            }
        }

        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text("需要存储权限") },
                text = { Text("需要存储权限以访问视频和字幕文件") },
                confirmButton = {
                    TextButton(onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                        showPermissionDialog = false
                    }) {
                        Text("设置")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
fun VideoListItem(
    video: VideoFile,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.VideoFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(video.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${video.subtitles.size} 条字幕",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "删除")
        }
    }
}

private suspend fun loadVideo(context: Context, uri: Uri, viewModel: MainViewModel) {
    val fileName = getFileName(context, uri) ?: "unknown"
    val videoId = System.currentTimeMillis()
    
    val videoFile = VideoFile(
        id = videoId,
        uri = uri,
        name = fileName,
        path = uri.toString()
    )
    
    val subtitles = findAndParseSubtitles(context, uri, fileName, videoId)
    viewModel.addVideo(videoFile.copy(subtitles = subtitles))
}

private suspend fun loadFolder(context: Context, uri: Uri, viewModel: MainViewModel) {
}

private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    result = it.getString(nameIndex)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result!!.lastIndexOf('/')
        if (cut != -1) {
            result = result!!.substring(cut + 1)
        }
    }
    return result
}

private fun findAndParseSubtitles(
    context: Context,
    videoUri: Uri,
    videoName: String,
    videoId: Long
): List<com.example.subtitlevideoeditor.data.SubtitleItem> {
    val subtitles = mutableListOf<com.example.subtitlevideoeditor.data.SubtitleItem>()
    val baseName = videoName.substringBeforeLast(".")
    
    val subtitleExtensions = listOf("srt", "vtt", "ass", "ssa")
    
    return subtitles
}
