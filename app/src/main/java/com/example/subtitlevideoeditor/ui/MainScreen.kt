package com.example.subtitlevideoeditor.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.subtitlevideoeditor.data.HistoryRecord
import com.example.subtitlevideoeditor.data.SubtitleItem
import com.example.subtitlevideoeditor.data.VideoFile
import com.example.subtitlevideoeditor.ui.screens.*
import com.example.subtitlevideoeditor.utils.ExportResult
import com.example.subtitlevideoeditor.utils.VideoExporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportTitle by remember { mutableStateOf("") }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            showPermissionDialog = true
        }
    }

    val manageStorageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    LaunchedEffect(Unit) {
        checkAndRequestPermissions(
            context = context,
            storagePermissionLauncher = { storagePermissionLauncher.launch(it) },
            manageStorageLauncher = { manageStorageLauncher.launch(it) },
            showPermissionDialog = { showPermissionDialog = true }
        )
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentScreen = currentScreen,
                onScreenSelected = { screen ->
                    currentScreen = screen
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onVideoSelected = { video ->
                        viewModel.selectVideo(video)
                    }
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    viewModel = viewModel,
                    onAddToClipboard = { subtitle ->
                        val video = viewModel.videos.value.find { it.id == subtitle.videoId }
                        video?.let {
                            viewModel.addClipSegment(subtitle, it.uri)
                        }
                    }
                )
            }
            composable(Screen.Editor.route) {
                EditorScreen(
                    viewModel = viewModel,
                    onExport = {
                        if (viewModel.clipSegments.value.isNotEmpty()) {
                            exportTitle = viewModel.clipSegments.value.firstOrNull()?.subtitleContent?.take(30) ?: ""
                            showExportDialog = true
                        }
                    }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    viewModel = viewModel,
                    onLoadHistory = { record ->
                        viewModel.loadHistoryToClips(record)
                        currentScreen = Screen.Editor
                        navController.navigate(Screen.Editor.route)
                    }
                )
            }
        }
    }

    if (showPermissionDialog) {
        PermissionDialog(
            onDismiss = { showPermissionDialog = false },
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
                showPermissionDialog = false
            }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            title = exportTitle,
            onTitleChanged = { exportTitle = it },
            onDismiss = { showExportDialog = false },
            onConfirm = { title ->
                scope.launch {
                    viewModel.setExporting(true)
                    viewModel.setExportProgress(0f)
                    
                    val exporter = VideoExporter(context) { progress ->
                        viewModel.setExportProgress(progress)
                    }
                    
                    val result = exporter.exportVideo(
                        segments = viewModel.clipSegments.value,
                        title = title
                    )
                    
                    viewModel.setExporting(false)
                    
                    when (result) {
                        is ExportResult.Success -> {
                            viewModel.saveHistory(
                                title = title.ifEmpty { exportTitle },
                                outputPath = result.outputPath,
                                outputUri = result.outputUri,
                                duration = result.duration
                            )
                            viewModel.clearClipSegments()
                        }
                        is ExportResult.Error -> {
                        }
                    }
                    
                    showExportDialog = false
                }
            }
        )
    }
}

@Composable
fun BottomNavigationBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentScreen == Screen.Home,
            onClick = { onScreenSelected(Screen.Home) },
            icon = { Icon(Icons.Default.Folder, contentDescription = "文件") },
            label = { Text("文件") }
        )
        NavigationBarItem(
            selected = currentScreen == Screen.Search,
            onClick = { onScreenSelected(Screen.Search) },
            icon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
            label = { Text("搜索") }
        )
        NavigationBarItem(
            selected = currentScreen == Screen.Editor,
            onClick = { onScreenSelected(Screen.Editor) },
            icon = { Icon(Icons.Default.VideoSettings, contentDescription = "剪辑") },
            label = { Text("剪辑") }
        )
        NavigationBarItem(
            selected = currentScreen == Screen.History,
            onClick = { onScreenSelected(Screen.History) },
            icon = { Icon(Icons.Default.History, contentDescription = "历史") },
            label = { Text("历史") }
        )
    }
}

@Composable
fun PermissionDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("需要存储权限") },
        text = { Text("应用需要存储权限来访问视频和字幕文件，以及保存导出的视频。") },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("去设置")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun ExportDialog(
    title: String,
    onTitleChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出视频") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChanged,
                label = { Text("视频标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title) }) {
                Text("导出")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun checkAndRequestPermissions(
    context: Context,
    storagePermissionLauncher: (String) -> Unit,
    manageStorageLauncher: (Intent) -> Unit,
    showPermissionDialog: () -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (!Environment.isExternalStorageManager()) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                manageStorageLauncher(intent)
            } catch (e: Exception) {
                showPermissionDialog()
            }
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        storagePermissionLauncher(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}
