package com.example.subtitlevideoeditor.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.subtitlevideoeditor.data.ClipSegment
import com.example.subtitlevideoeditor.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: MainViewModel = viewModel(),
    onExport: () -> Unit
) {
    val clipSegments by viewModel.clipSegments.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()

    var showDuplicateDialog by remember { mutableStateOf(false) }
    var duplicateSegments by remember { mutableStateOf<List<ClipSegment>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("待剪辑区") },
                actions = {
                    if (clipSegments.isNotEmpty()) {
                        IconButton(onClick = {
                            duplicateSegments = viewModel.checkDuplicates()
                            if (duplicateSegments.isNotEmpty()) {
                                showDuplicateDialog = true
                            }
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "检测重复")
                        }
                        IconButton(onClick = { viewModel.clearClipSegments() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "清空")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (clipSegments.isNotEmpty() && !isExporting) {
                FloatingActionButton(onClick = onExport) {
                    Icon(Icons.Default.VideoFile, contentDescription = "导出视频")
                }
            }
        }
    ) { paddingValues ->
        if (isExporting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { exportProgress },
                            modifier = Modifier.size(64.dp)
                        )
                        Text("正在导出视频...", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${(exportProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
            }
        } else if (clipSegments.isEmpty()) {
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
                        Icons.Default.VideoSettings,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("待剪辑区为空", style = MaterialTheme.typography.titleLarge)
                    Text("从搜索结果添加字幕片段", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${clipSegments.size} 个片段")
                        val totalDuration = clipSegments.sumOf { it.endTimeMs - it.startTimeMs }
                        Text("总时长: ${formatDuration(totalDuration)}")
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(clipSegments) { index, segment ->
                        ClipSegmentItem(
                            segment = segment,
                            index = index,
                            onRemove = { viewModel.removeClipSegment(segment.id) },
                            onMoveUp = if (index > 0) {
                                { viewModel.reorderClipSegments(index, index - 1) }
                            } else null,
                            onMoveDown = if (index < clipSegments.size - 1) {
                                { viewModel.reorderClipSegments(index, index + 1) }
                            } else null
                        )
                    }
                }
            }
        }
    }

    if (showDuplicateDialog && duplicateSegments.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            title = { Text("检测到重复片段") },
            text = {
                Text("发现 ${duplicateSegments.size} 个重复的字幕片段")
            },
            confirmButton = {
                TextButton(onClick = { showDuplicateDialog = false }) {
                    Text("知道了")
                }
            }
        )
    }
}

@Composable
fun ClipSegmentItem(
    segment: ClipSegment,
    index: Int,
    onRemove: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "#${index + 1}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onMoveUp != null) {
                        IconButton(
                            onClick = onMoveUp,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "上移")
                        }
                    }
                    if (onMoveDown != null) {
                        IconButton(
                            onClick = onMoveDown,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "下移")
                        }
                    }
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            }
            Text(
                segment.videoName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "${formatTime(segment.startTimeMs)} - ${formatTime(segment.endTimeMs)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                segment.subtitleContent,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format("%d小时%d分钟%d秒", hours, minutes % 60, seconds % 60)
    } else if (minutes > 0) {
        String.format("%d分钟%d秒", minutes, seconds % 60)
    } else {
        String.format("%d秒", seconds)
    }
}
