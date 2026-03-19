package com.example.subtitlevideoeditor.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.subtitlevideoeditor.data.*
import com.example.subtitlevideoeditor.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MainViewModel = viewModel(),
    onAddToClipboard: (SubtitleItem) -> Unit
) {
    val searchResults by viewModel.searchResults.collectAsState()
    val searchFilter by viewModel.searchFilter.collectAsState()
    val videos by viewModel.videos.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }
    var keyword by remember { mutableStateOf(searchFilter.keyword) }

    LaunchedEffect(searchFilter) {
        keyword = searchFilter.keyword
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("字幕搜索") },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "筛选")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { 
                    keyword = it
                    viewModel.updateSearchFilter(searchFilter.copy(keyword = it))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("输入搜索关键词...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (keyword.isNotEmpty()) {
                        IconButton(onClick = {
                            keyword = ""
                            viewModel.updateSearchFilter(searchFilter.copy(keyword = ""))
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.performSearch() })
            )

            if (videos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                        Text("请先添加视频", style = MaterialTheme.typography.titleLarge)
                    }
                }
            } else if (searchResults.isEmpty() && keyword.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("输入关键词开始搜索", style = MaterialTheme.typography.titleLarge)
                    }
                }
            } else if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.NotInterested,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("未找到匹配的字幕", style = MaterialTheme.typography.titleLarge)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults) { subtitle ->
                    SubtitleSearchResultItem(
                        subtitle = subtitle,
                        onAddToClipboard = { onAddToClipboard(subtitle) }
                    )
                }
            }
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            currentFilter = searchFilter,
            onDismiss = { showFilterDialog = false },
            onFilterChanged = { 
                viewModel.updateSearchFilter(it)
                showFilterDialog = false
            }
        )
    }
}

@Composable
fun SubtitleSearchResultItem(
    subtitle: SubtitleItem,
    onAddToClipboard: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    subtitle.videoName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    formatTime(subtitle.startTimeMs),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                subtitle.content,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onAddToClipboard) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("添加到剪辑区")
                }
            }
        }
    }
}

@Composable
fun FilterDialog(
    currentFilter: SearchFilter,
    onDismiss: () -> Unit,
    onFilterChanged: (SearchFilter) -> Unit
) {
    var searchType by remember { mutableStateOf(currentFilter.searchType) }
    var matchType by remember { mutableStateOf(currentFilter.matchType) }
    var durationFilter by remember { mutableStateOf(currentFilter.durationFilter) }
    var sortType by remember { mutableStateOf(currentFilter.sortType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("筛选条件") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp) {
                Column {
                    Text("搜索类型", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    SearchType.values().forEach { type ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { searchType = type }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = searchType == type,
                                onClick = { searchType = type }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when (type) {
                                    SearchType.CONTENT -> "字幕内容"
                                    SearchType.VIDEO_NAME -> "视频名称"
                                    SearchType.BOTH -> "内容和视频"
                                }
                            )
                        }
                    }
                }

                Column {
                    Text("匹配方式", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    MatchType.values().forEach { type ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { matchType = type }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = matchType == type,
                                onClick = { matchType = type }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when (type) {
                                    MatchType.ANY -> "任一匹配"
                                    MatchType.ALL -> "全部匹配"
                                }
                            )
                        }
                    }
                }

                Column {
                    Text("时长筛选", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    DurationFilter.values().forEach { type ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { durationFilter = type }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = durationFilter == type,
                                onClick = { durationFilter = type }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when (type) {
                                    DurationFilter.ALL -> "全部"
                                    DurationFilter.SHORT -> "短（<3秒）"
                                    DurationFilter.MEDIUM -> "中（3-10秒）"
                                    DurationFilter.LONG -> "长（>10秒）"
                                }
                            )
                        }
                    }
                }

                Column {
                    Text("排序方式", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    SortType.values().forEach { type ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { sortType = type }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = sortType == type,
                                onClick = { sortType = type }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when (type) {
                                    SortType.TIME -> "按时间"
                                    SortType.VIDEO -> "按视频"
                                    SortType.RELEVANCE -> "按匹配度"
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onFilterChanged(
                    currentFilter.copy(
                        searchType = searchType,
                        matchType = matchType,
                        durationFilter = durationFilter,
                        sortType = sortType
                    )
                )
            }) {
                Text("应用")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun formatTime(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
}
