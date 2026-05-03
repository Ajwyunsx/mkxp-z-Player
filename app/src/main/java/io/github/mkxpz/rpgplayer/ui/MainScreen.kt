package io.github.mkxpz.rpgplayer.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.Settings as AndroidSettings
import android.view.KeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.File
import java.util.Locale
import io.github.mkxpz.rpgplayer.data.GameEntry
import io.github.mkxpz.rpgplayer.data.LauncherSettings
import io.github.mkxpz.rpgplayer.data.PhysicalGamepadButton
import io.github.mkxpz.rpgplayer.data.PreferredImportMode
import io.github.mkxpz.rpgplayer.data.RpgMakerEngine
import io.github.mkxpz.rpgplayer.data.StoredImportMode
import io.github.mkxpz.rpgplayer.data.ThemeColorSource
import io.github.mkxpz.rpgplayer.data.ThemeModeSetting
import io.github.mkxpz.rpgplayer.data.VirtualGamepadButton
import io.github.mkxpz.rpgplayer.domain.StandaloneApkOptions
import io.github.mkxpz.rpgplayer.ui.theme.supportsDynamicColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    snackbarHostState: SnackbarHostState,
    onImportTree: (Uri) -> Unit,
    onImportVxRtpTree: (Uri) -> Unit,
    onImportVxAceRtpTree: (Uri) -> Unit,
    onDownloadVxRtp: () -> Unit,
    onDownloadVxAceRtp: () -> Unit,
    onAddDirectPath: (String) -> Unit,
    onLaunch: (GameEntry) -> Unit,
    onDelete: (GameEntry, Boolean) -> Unit,
    onUpdateGameMetadata: (GameEntry, String, Uri?, Boolean) -> Unit,
    onExportStandaloneApk: (GameEntry, StandaloneApkOptions) -> Unit,
    onThemeModeChange: (ThemeModeSetting) -> Unit,
    onColorSourceChange: (ThemeColorSource) -> Unit,
    onManualSeedColorChange: (Long) -> Unit,
    onPreferredImportModeChange: (PreferredImportMode) -> Unit,
    onVirtualGamepadChange: (Boolean) -> Unit,
    onVirtualGamepadOpacityChange: (Int) -> Unit,
    onVirtualGamepadDiagonalMovementChange: (Boolean) -> Unit,
    onVirtualGamepadKeyChange: (VirtualGamepadButton, Int) -> Unit,
    onPhysicalGamepadMappingChange: (Boolean) -> Unit,
    onPhysicalGamepadBackAsBChange: (Boolean) -> Unit,
    onPhysicalGamepadKeyChange: (PhysicalGamepadButton, Int) -> Unit,
    onFixedFramerateChange: (Int) -> Unit,
    onSmoothScalingChange: (Boolean) -> Unit,
    onKeepAspectRatioChange: (Boolean) -> Unit,
    onSoundFontPathChange: (String) -> Unit,
    onVxRtpPathChange: (String) -> Unit,
    onVxAceRtpPathChange: (String) -> Unit,
    onRubyClassicCompatibilityChange: (Boolean) -> Unit,
    onWinApiCompatibilityChange: (Boolean) -> Unit,
    onDebugLaunchChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) onImportTree(uri)
    }
    val vxRtpLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) onImportVxRtpTree(uri)
    }
    val vxAceRtpLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) onImportVxAceRtpTree(uri)
    }
    val allFilesAccessLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDirectPathDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<GameEntry?>(null) }
    var editTarget by remember { mutableStateOf<GameEntry?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editIconUri by remember { mutableStateOf<Uri?>(null) }
    var clearEditIcon by remember { mutableStateOf(false) }
    var exportTarget by remember { mutableStateOf<GameEntry?>(null) }
    var exportAppName by remember { mutableStateOf("") }
    var exportPackageName by remember { mutableStateOf("") }
    var exportVersionCode by remember { mutableStateOf("1") }
    var exportVersionName by remember { mutableStateOf("1.0.0") }
    var exportIconUri by remember { mutableStateOf<Uri?>(null) }
    val iconLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            editIconUri = uri
            clearEditIcon = false
        }
    }
    val exportIconLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) exportIconUri = uri
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (selectedTab == 0) "mkxp-z 游戏库" else "播放器设置",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "添加游戏")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.SportsEsports, contentDescription = null) },
                    label = { Text("游戏") },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("设置") },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (selectedTab) {
                0 -> GamesScreen(
                    games = uiState.games,
                    onLaunch = onLaunch,
                    onDelete = { deleteTarget = it },
                    onExportStandaloneApk = { entry ->
                        exportTarget = entry
                        exportAppName = entry.title
                        exportPackageName = defaultStandalonePackageName(entry)
                        exportVersionCode = "1"
                        exportVersionName = "1.0.0"
                        exportIconUri = entry.customIconPath
                            ?.takeIf { File(it).isFile }
                            ?.let { Uri.fromFile(File(it)) }
                    },
                    onEdit = { entry ->
                        editTarget = entry
                        editTitle = entry.title
                        editIconUri = null
                        clearEditIcon = false
                    },
                    bottomPadding = 88.dp,
                )
                else -> SettingsScreen(
                    settings = uiState.settings,
                    allFilesAccessGranted = isAllFilesAccessGranted(),
                    onRequestAllFilesAccess = {
                        val appIntent = Intent(
                            AndroidSettings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            "package:${context.packageName}".toUri(),
                        )
                        val fallbackIntent = Intent(AndroidSettings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        try {
                            allFilesAccessLauncher.launch(appIntent)
                        } catch (_: ActivityNotFoundException) {
                            allFilesAccessLauncher.launch(fallbackIntent)
                        }
                    },
                    onThemeModeChange = onThemeModeChange,
                    onColorSourceChange = onColorSourceChange,
                    onManualSeedColorChange = onManualSeedColorChange,
                    onPreferredImportModeChange = onPreferredImportModeChange,
                    onVirtualGamepadChange = onVirtualGamepadChange,
                    onVirtualGamepadOpacityChange = onVirtualGamepadOpacityChange,
                    onVirtualGamepadDiagonalMovementChange = onVirtualGamepadDiagonalMovementChange,
                    onVirtualGamepadKeyChange = onVirtualGamepadKeyChange,
                    onPhysicalGamepadMappingChange = onPhysicalGamepadMappingChange,
                    onPhysicalGamepadBackAsBChange = onPhysicalGamepadBackAsBChange,
                    onPhysicalGamepadKeyChange = onPhysicalGamepadKeyChange,
                    onFixedFramerateChange = onFixedFramerateChange,
                    onSmoothScalingChange = onSmoothScalingChange,
                    onKeepAspectRatioChange = onKeepAspectRatioChange,
                    onSoundFontPathChange = onSoundFontPathChange,
                    onVxRtpPathChange = onVxRtpPathChange,
                    onVxAceRtpPathChange = onVxAceRtpPathChange,
                    onDownloadVxRtp = onDownloadVxRtp,
                    onDownloadVxAceRtp = onDownloadVxAceRtp,
                    onChooseVxRtpFolder = { vxRtpLauncher.launch(null) },
                    onChooseVxAceRtpFolder = { vxAceRtpLauncher.launch(null) },
                    onRubyClassicCompatibilityChange = onRubyClassicCompatibilityChange,
                    onWinApiCompatibilityChange = onWinApiCompatibilityChange,
                    onDebugLaunchChange = onDebugLaunchChange,
                )
            }

            uiState.busyMessage?.let { message ->
                BusyOverlay(message = message)
            }
        }
    }

    if (showAddDialog) {
        AddGameDialog(
            preferredMode = uiState.settings.preferredImportMode,
            onDismiss = { showAddDialog = false },
            onCopyImport = {
                showAddDialog = false
                treeLauncher.launch(null)
            },
            onDirectPath = {
                showAddDialog = false
                showDirectPathDialog = true
            },
        )
    }

    if (showDirectPathDialog) {
        DirectPathDialog(
            allFilesAccessGranted = isAllFilesAccessGranted(),
            onDismiss = { showDirectPathDialog = false },
            onSubmit = {
                showDirectPathDialog = false
                onAddDirectPath(it)
            },
        )
    }

    deleteTarget?.let { entry ->
        DeleteDialog(
            entry = entry,
            onDismiss = { deleteTarget = null },
            onConfirm = { removeFiles ->
                deleteTarget = null
                onDelete(entry, removeFiles)
            },
        )
    }

    editTarget?.let { entry ->
        EditGameDialog(
            entry = entry,
            title = editTitle,
            selectedIconUri = editIconUri,
            clearIcon = clearEditIcon,
            onTitleChange = { editTitle = it },
            onChooseIcon = { iconLauncher.launch("image/*") },
            onClearIcon = {
                editIconUri = null
                clearEditIcon = true
            },
            onDismiss = { editTarget = null },
            onConfirm = {
                editTarget = null
                onUpdateGameMetadata(entry, editTitle, editIconUri, clearEditIcon)
            },
        )
    }

    exportTarget?.let { entry ->
        StandaloneApkDialog(
            entry = entry,
            appName = exportAppName,
            packageName = exportPackageName,
            versionCode = exportVersionCode,
            versionName = exportVersionName,
            iconUri = exportIconUri,
            onAppNameChange = { exportAppName = it },
            onPackageNameChange = { exportPackageName = it },
            onVersionCodeChange = { exportVersionCode = it.filter(Char::isDigit).take(9) },
            onVersionNameChange = { exportVersionName = it },
            onChooseIcon = { exportIconLauncher.launch("image/*") },
            onClearIcon = { exportIconUri = null },
            onDismiss = { exportTarget = null },
            onConfirm = {
                exportTarget = null
                onExportStandaloneApk(
                    entry,
                    StandaloneApkOptions(
                        appName = exportAppName.trim(),
                        packageName = exportPackageName.trim(),
                        versionCode = exportVersionCode.toIntOrNull() ?: 1,
                        versionName = exportVersionName.trim(),
                        iconUri = exportIconUri?.toString(),
                    ),
                )
            },
        )
    }
}

@Composable
private fun GamesScreen(
    games: List<GameEntry>,
    onLaunch: (GameEntry) -> Unit,
    onDelete: (GameEntry) -> Unit,
    onExportStandaloneApk: (GameEntry) -> Unit,
    onEdit: (GameEntry) -> Unit,
    bottomPadding: Dp,
) {
    if (games.isEmpty()) {
        EmptyGamesState()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(games, key = { it.id }) { entry ->
            GameCard(
                entry = entry,
                onLaunch = { onLaunch(entry) },
                onDelete = { onDelete(entry) },
                onExportStandaloneApk = { onExportStandaloneApk(entry) },
                onEdit = { onEdit(entry) },
            )
        }
    }
}

@Composable
private fun EmptyGamesState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "还没有游戏",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "点击右下角按钮添加 RPG Maker XP、VX、VX Ace、MV 或 MZ 游戏文件夹。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GameCard(
    entry: GameEntry,
    onLaunch: () -> Unit,
    onDelete: () -> Unit,
    onExportStandaloneApk: () -> Unit,
    onEdit: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GameIcon(entry = entry, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${entry.engine.displayName} · ${entry.importMode.label()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = onExportStandaloneApk) {
                    Icon(Icons.Default.Storage, contentDescription = "导出直装 APK")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (entry.hasEncryptedArchive) {
                    AssistChip(onClick = {}, label = { Text("加密包") })
                }
                AssistChip(onClick = {}, label = { Text("启动 ${entry.launchCount} 次") })
                if (entry.hasWinApiUsage) {
                    AssistChip(onClick = {}, label = { Text("WinAPI") })
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = onLaunch) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("运行")
                }
            }
        }
    }
}

@Composable
private fun GameIcon(
    entry: GameEntry,
    size: Dp,
    selectedIconUri: Uri? = null,
    clearIcon: Boolean = false,
) {
    val context = LocalContext.current
    val selectedBitmap = remember(selectedIconUri) {
        selectedIconUri?.let { uri ->
            runCatching {
                context.contentResolver.openInputStream(uri).use { input ->
                    input?.let { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                }
            }.getOrNull()
        }
    }
    val iconPath = if (clearIcon) null else entry.customIconPath
    val iconModified = iconPath?.let { File(it).lastModified() } ?: 0L
    val fileBitmap = remember(iconPath, iconModified) {
        iconPath
            ?.takeIf { File(it).isFile }
            ?.let { path -> runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() }
    }
    val bitmap = selectedBitmap ?: fileBitmap

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = entry.engine.shortLabel(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun StandaloneApkDialog(
    entry: GameEntry,
    appName: String,
    packageName: String,
    versionCode: String,
    versionName: String,
    iconUri: Uri?,
    onAppNameChange: (String) -> Unit,
    onPackageNameChange: (String) -> Unit,
    onVersionCodeChange: (String) -> Unit,
    onVersionNameChange: (String) -> Unit,
    onChooseIcon: () -> Unit,
    onClearIcon: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val parsedVersionCode = versionCode.toIntOrNull()
    val packageValid = isValidStandalonePackageName(packageName)
    val canConfirm = appName.isNotBlank() &&
        packageValid &&
        parsedVersionCode != null &&
        parsedVersionCode > 0 &&
        versionName.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Standalone APK") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StandaloneExportIcon(iconUri = iconUri, size = 56.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onChooseIcon) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Icon")
                            }
                            TextButton(onClick = onClearIcon) {
                                Text("Clear")
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = appName,
                    onValueChange = onAppNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("App name") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = packageName,
                    onValueChange = onPackageNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Package name") },
                    supportingText = { Text("Use a package different from the player, e.g. com.example.game") },
                    isError = packageName.isNotBlank() && !packageValid,
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = versionCode,
                        onValueChange = onVersionCodeChange,
                        modifier = Modifier.weight(1f),
                        label = { Text("Version code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = versionCode.isNotBlank() && (parsedVersionCode == null || parsedVersionCode <= 0),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = versionName,
                        onValueChange = onVersionNameChange,
                        modifier = Modifier.weight(1f),
                        label = { Text("Version name") },
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = canConfirm) {
                Text("Build and install")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/*
@Composable
private fun StandaloneApkDialog(
    entry: GameEntry,
    appName: String,
    packageName: String,
    versionCode: String,
    versionName: String,
    iconUri: Uri?,
    onAppNameChange: (String) -> Unit,
    onPackageNameChange: (String) -> Unit,
    onVersionCodeChange: (String) -> Unit,
    onVersionNameChange: (String) -> Unit,
    onChooseIcon: () -> Unit,
    onClearIcon: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val parsedVersionCode = versionCode.toIntOrNull()
    val packageValid = isValidStandalonePackageName(packageName)
    val canConfirm = appName.isNotBlank() &&
        packageValid &&
        parsedVersionCode != null &&
        parsedVersionCode > 0 &&
        versionName.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("直装 APK 设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StandaloneExportIcon(iconUri = iconUri, size = 56.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onChooseIcon) {
                                Icon(ImageIcon, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("选择图标")
                            }
                            TextButton(onClick = onClearIcon) {
                                Text("清除")
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = appName,
                    onValueChange = onAppNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("应用名称") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = packageName,
                    onValueChange = onPackageNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("包名") },
                    supportingText = { Text("必须和播放器不同，例如 com.example.game") },
                    isError = packageName.isNotBlank() && !packageValid,
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = versionCode,
                        onValueChange = onVersionCodeChange,
                        modifier = Modifier.weight(1f),
                        label = { Text("版本号") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = versionCode.isNotBlank() && (parsedVersionCode == null || parsedVersionCode <= 0),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = versionName,
                        onValueChange = onVersionNameChange,
                        modifier = Modifier.weight(1f),
                        label = { Text("版本名") },
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = canConfirm,
            ) {
                Text("生成并安装")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

*/
@Composable
private fun StandaloneExportIcon(iconUri: Uri?, size: Dp) {
    val context = LocalContext.current
    val selectedBitmap = remember(iconUri) {
        iconUri?.let { uri ->
            runCatching {
                context.contentResolver.openInputStream(uri).use { input ->
                    input?.let { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                }
            }.getOrNull()
        }
    }

    if (selectedBitmap != null) {
        Image(
            bitmap = selectedBitmap,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "V",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun defaultStandalonePackageName(entry: GameEntry): String {
    val suffix = entry.id
        .filter { it.isLetterOrDigit() }
        .lowercase(Locale.US)
        .take(24)
        .ifBlank { "game" }
    return "io.github.mkxpz.game.g$suffix"
}

private fun isValidStandalonePackageName(value: String): Boolean =
    standalonePackageRegex.matches(value)

private val standalonePackageRegex = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")

@Composable
private fun SettingsScreen(
    settings: LauncherSettings,
    allFilesAccessGranted: Boolean,
    onRequestAllFilesAccess: () -> Unit,
    onThemeModeChange: (ThemeModeSetting) -> Unit,
    onColorSourceChange: (ThemeColorSource) -> Unit,
    onManualSeedColorChange: (Long) -> Unit,
    onPreferredImportModeChange: (PreferredImportMode) -> Unit,
    onVirtualGamepadChange: (Boolean) -> Unit,
    onVirtualGamepadOpacityChange: (Int) -> Unit,
    onVirtualGamepadDiagonalMovementChange: (Boolean) -> Unit,
    onVirtualGamepadKeyChange: (VirtualGamepadButton, Int) -> Unit,
    onPhysicalGamepadMappingChange: (Boolean) -> Unit,
    onPhysicalGamepadBackAsBChange: (Boolean) -> Unit,
    onPhysicalGamepadKeyChange: (PhysicalGamepadButton, Int) -> Unit,
    onFixedFramerateChange: (Int) -> Unit,
    onSmoothScalingChange: (Boolean) -> Unit,
    onKeepAspectRatioChange: (Boolean) -> Unit,
    onSoundFontPathChange: (String) -> Unit,
    onVxRtpPathChange: (String) -> Unit,
    onVxAceRtpPathChange: (String) -> Unit,
    onDownloadVxRtp: () -> Unit,
    onDownloadVxAceRtp: () -> Unit,
    onChooseVxRtpFolder: () -> Unit,
    onChooseVxAceRtpFolder: () -> Unit,
    onRubyClassicCompatibilityChange: (Boolean) -> Unit,
    onWinApiCompatibilityChange: (Boolean) -> Unit,
    onDebugLaunchChange: (Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SettingsGroup(title = "主题", icon = Icons.Default.Palette) {
                ChoiceRow(
                    options = ThemeModeSetting.entries.toList(),
                    selected = settings.themeMode,
                    label = { it.label() },
                    onSelected = onThemeModeChange,
                )
                Spacer(Modifier.height(12.dp))
                ChoiceRow(
                    options = ThemeColorSource.entries.toList(),
                    selected = settings.colorSource,
                    label = { source: ThemeColorSource ->
                        when (source) {
                            ThemeColorSource.DYNAMIC_SYSTEM -> if (supportsDynamicColor()) "系统动态色" else "动态色(12+)"
                            ThemeColorSource.MANUAL -> "手动颜色"
                        }
                    },
                    onSelected = onColorSourceChange,
                )
                Spacer(Modifier.height(12.dp))
                ColorSwatches(
                    selected = settings.manualSeedColor,
                    onSelected = onManualSeedColorChange,
                )
            }
        }

        item {
            SettingsGroup(title = "添加游戏", icon = Icons.Default.Storage) {
                ChoiceRow(
                    options = PreferredImportMode.entries.toList(),
                    selected = settings.preferredImportMode,
                    label = { if (it == PreferredImportMode.COPY_TO_LIBRARY) "复制到应用" else "外部路径" },
                    onSelected = onPreferredImportModeChange,
                )
                Spacer(Modifier.height(8.dp))
                ListItem(
                    headlineContent = { Text("全部文件访问") },
                    supportingContent = {
                        Text(if (allFilesAccessGranted) "已授权，可使用外部路径直连" else "未授权，建议使用复制导入")
                    },
                    trailingContent = {
                        OutlinedButton(onClick = onRequestAllFilesAccess) {
                            Text(if (allFilesAccessGranted) "管理" else "授权")
                        }
                    },
                )
            }
        }

        item {
            SettingsGroup(title = "运行参数", icon = Icons.Default.Tune) {
                SwitchRow("虚拟手柄", "为触屏设备显示按键覆盖层", settings.virtualGamepadEnabled, onVirtualGamepadChange)
                VirtualGamepadConfigSection(
                    settings = settings,
                    onOpacityChange = onVirtualGamepadOpacityChange,
                    onDiagonalMovementChange = onVirtualGamepadDiagonalMovementChange,
                    onKeyChange = onVirtualGamepadKeyChange,
                )
                Spacer(Modifier.height(8.dp))
                SwitchRow(
                    "实体手柄映射",
                    "把手柄按键转换成播放器键位，避免 B 键触发系统返回",
                    settings.physicalGamepadMappingEnabled,
                    onPhysicalGamepadMappingChange,
                )
                SwitchRow(
                    "手柄 Back 当作 B",
                    "部分手柄会把 B 发成系统返回键；开启后按取消键处理",
                    settings.physicalGamepadBackAsB,
                    onPhysicalGamepadBackAsBChange,
                )
                PhysicalGamepadConfigSection(
                    settings = settings,
                    onKeyChange = onPhysicalGamepadKeyChange,
                )
                Spacer(Modifier.height(8.dp))
                SwitchRow("保持宽高比", "避免拉伸游戏画面", settings.keepAspectRatio, onKeepAspectRatioChange)
                SwitchRow("平滑缩放", "放大时启用线性插值", settings.smoothScaling, onSmoothScalingChange)
                SwitchRow("调试启动", "生成配置时启用 FPS 输出", settings.debugLaunch, onDebugLaunchChange)
                SwitchRow(
                    "Ruby 经典兼容",
                    "修复 VX/VX Ace 旧脚本的顶层变量和 Ruby 1.9 差异",
                    settings.rubyClassicCompatibilityEnabled,
                    onRubyClassicCompatibilityChange,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (settings.fixedFramerate == 0) "固定帧率：关闭" else "固定帧率：${settings.fixedFramerate} FPS",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                androidx.compose.material3.Slider(
                    value = settings.fixedFramerate.toFloat(),
                    onValueChange = { onFixedFramerateChange(it.toInt()) },
                    valueRange = 0f..120f,
                    steps = 119,
                )
                SwitchRow(
                    "WinAPI 兼容",
                    "检测到 Ruby Win32API/WinAPI 脚本时自动加载兼容层",
                    settings.winApiCompatibilityEnabled,
                    onWinApiCompatibilityChange,
                )
                OutlinedTextField(
                    value = settings.soundFontPath,
                    onValueChange = onSoundFontPathChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("SoundFont 路径") },
                    singleLine = true,
                    placeholder = { Text("例如 /storage/emulated/0/GMGSx.sf2") },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "VX RTP",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = if (settings.vxRtpPath.isBlank()) {
                        "未设置；依赖 VX RTP 的游戏可能缺少图像或音频。"
                    } else {
                        "已设置；启动 VX 游戏时会自动加入 mkxp-z RTP 搜索路径。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = onDownloadVxRtp) {
                        Text("自动下载")
                    }
                    OutlinedButton(onClick = onChooseVxRtpFolder) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("导入 VX RTP")
                    }
                    if (settings.vxRtpPath.isNotBlank()) {
                        TextButton(onClick = { onVxRtpPathChange("") }) {
                            Text("清除")
                        }
                    }
                }
                OutlinedTextField(
                    value = settings.vxRtpPath,
                    onValueChange = onVxRtpPathChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("VX RTP 路径") },
                    singleLine = true,
                    placeholder = { Text("选择 RPGVX 文件夹，或输入已解压 RTP 路径") },
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "VX Ace RTP",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = if (settings.vxAceRtpPath.isBlank()) {
                        "未设置；依赖 VX Ace RTP 的加密包可能没有声音或缺少素材。"
                    } else {
                        "已设置；启动 VX Ace 游戏时会自动加入 mkxp-z RTP 搜索路径。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = onDownloadVxAceRtp) {
                        Text("下载 VX Ace RTP")
                    }
                    OutlinedButton(onClick = onChooseVxAceRtpFolder) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("导入 VX Ace RTP")
                    }
                    if (settings.vxAceRtpPath.isNotBlank()) {
                        TextButton(onClick = { onVxAceRtpPathChange("") }) {
                            Text("清除")
                        }
                    }
                }
                OutlinedTextField(
                    value = settings.vxAceRtpPath,
                    onValueChange = onVxAceRtpPathChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("VX Ace RTP 路径") },
                    singleLine = true,
                    placeholder = { Text("选择 RPGVXAce 文件夹，或输入已解压 RTP 路径") },
                )
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun <T> ChoiceRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelected(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun VirtualGamepadConfigSection(
    settings: LauncherSettings,
    onOpacityChange: (Int) -> Unit,
    onDiagonalMovementChange: (Boolean) -> Unit,
    onKeyChange: (VirtualGamepadButton, Int) -> Unit,
) {
    var showKeyDialogFor by remember { mutableStateOf<VirtualGamepadButton?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 2.dp, bottom = 6.dp),
    ) {
        Text(
            text = "透明度：${settings.virtualGamepadOpacity}%",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        androidx.compose.material3.Slider(
            value = settings.virtualGamepadOpacity.toFloat(),
            onValueChange = { onOpacityChange(it.toInt()) },
            valueRange = 5f..100f,
            steps = 94,
            enabled = settings.virtualGamepadEnabled,
        )
        SwitchRow(
            title = "方向键斜向移动",
            subtitle = "允许上左、上右、下左、下右输入",
            checked = settings.virtualGamepadDiagonalMovement,
            onCheckedChange = onDiagonalMovementChange,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "按键映射",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(6.dp))
        virtualGamepadButtons.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { button ->
                    OutlinedButton(
                        onClick = { showKeyDialogFor = button },
                        enabled = settings.virtualGamepadEnabled,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "${button.displayLabel()}: ${keyLabel(settings.keyCodeFor(button))}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    showKeyDialogFor?.let { button ->
        KeyPickerDialog(
            title = "${button.displayLabel()} 映射",
            selectedKeyCode = settings.keyCodeFor(button),
            onDismiss = { showKeyDialogFor = null },
            onSelected = { keyCode ->
                showKeyDialogFor = null
                onKeyChange(button, keyCode)
            },
        )
    }
}

@Composable
private fun PhysicalGamepadConfigSection(
    settings: LauncherSettings,
    onKeyChange: (PhysicalGamepadButton, Int) -> Unit,
) {
    var showKeyDialogFor by remember { mutableStateOf<PhysicalGamepadButton?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 2.dp, bottom = 6.dp),
    ) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = "实体手柄按键",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(6.dp))
        physicalGamepadButtons.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { button ->
                    OutlinedButton(
                        onClick = { showKeyDialogFor = button },
                        enabled = settings.physicalGamepadMappingEnabled,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "${button.displayLabel()}: ${keyLabel(settings.keyCodeFor(button))}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    showKeyDialogFor?.let { button ->
        KeyPickerDialog(
            title = "${button.displayLabel()} 映射",
            selectedKeyCode = settings.keyCodeFor(button),
            onDismiss = { showKeyDialogFor = null },
            onSelected = { keyCode ->
                showKeyDialogFor = null
                onKeyChange(button, keyCode)
            },
        )
    }
}

@Composable
private fun KeyPickerDialog(
    title: String,
    selectedKeyCode: Int,
    onDismiss: () -> Unit,
    onSelected: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(commonKeyCodes, key = { it }) { keyCode ->
                    FilterChip(
                        selected = keyCode == selectedKeyCode,
                        onClick = { onSelected(keyCode) },
                        label = { Text(keyLabel(keyCode)) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun ColorSwatches(
    selected: Long,
    onSelected: (Long) -> Unit,
) {
    val colors = listOf(
        0xFF2F5C8A,
        0xFF006C4C,
        0xFF8A4F00,
        0xFF9A405B,
        0xFF526600,
        0xFF5E5D72,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(if (selected == color) 36.dp else 30.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .clickable { onSelected(color) },
            )
        }
    }
}

@Composable
private fun AddGameDialog(
    preferredMode: PreferredImportMode,
    onDismiss: () -> Unit,
    onCopyImport: () -> Unit,
    onDirectPath: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加游戏") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("选择 RPG Maker 游戏文件夹。XP/VX/VX Ace 使用 mkxp-z；MV/MZ 使用 WebView。复制导入兼容性最好；外部路径需要全部文件访问权限。")
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            if (preferredMode == PreferredImportMode.COPY_TO_LIBRARY) "默认：复制到应用游戏库" else "默认：外部路径直连",
                        )
                    },
                )
            }
        },
        confirmButton = {
            Button(onClick = if (preferredMode == PreferredImportMode.COPY_TO_LIBRARY) onCopyImport else onDirectPath) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(if (preferredMode == PreferredImportMode.COPY_TO_LIBRARY) "选择文件夹" else "输入路径")
            }
        },
        dismissButton = {
            TextButton(onClick = if (preferredMode == PreferredImportMode.COPY_TO_LIBRARY) onDirectPath else onCopyImport) {
                Text(if (preferredMode == PreferredImportMode.COPY_TO_LIBRARY) "外部路径" else "复制导入")
            }
        },
    )
}

@Composable
private fun DirectPathDialog(
    allFilesAccessGranted: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var path by remember { mutableStateOf("/storage/emulated/0/") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("外部路径直连") },
        text = {
            Column {
                Text(if (allFilesAccessGranted) "输入包含 Game.ini 或 MV/MZ index.html 的游戏目录。" else "当前没有全部文件访问权限，路径可能无法读取。")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("游戏路径") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(path) }, enabled = path.isNotBlank()) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun DeleteDialog(
    entry: GameEntry,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit,
) {
    val canRemoveFiles = entry.importMode == StoredImportMode.COPIED_TO_LIBRARY
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除 ${entry.title}") },
        text = {
            Text(if (canRemoveFiles) "可以只从列表移除，也可以同时删除已复制到应用内的游戏文件。" else "外部路径游戏只会从列表移除，不会删除原文件。")
        },
        confirmButton = {
            Button(onClick = { onConfirm(canRemoveFiles) }) {
                Text(if (canRemoveFiles) "删除文件" else "移除")
            }
        },
        dismissButton = {
            TextButton(onClick = { if (canRemoveFiles) onConfirm(false) else onDismiss() }) {
                Text(if (canRemoveFiles) "仅移除" else "取消")
            }
        },
    )
}

@Composable
private fun EditGameDialog(
    entry: GameEntry,
    title: String,
    selectedIconUri: Uri?,
    clearIcon: Boolean,
    onTitleChange: (String) -> Unit,
    onChooseIcon: () -> Unit,
    onClearIcon: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑游戏") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GameIcon(
                        entry = entry,
                        size = 64.dp,
                        selectedIconUri = selectedIconUri,
                        clearIcon = clearIcon,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onChooseIcon) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("选择图标")
                        }
                        if (entry.customIconPath != null || selectedIconUri != null) {
                            TextButton(onClick = onClearIcon) {
                                Text("恢复默认图标")
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("游戏名称") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = entry.installedPath,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("游戏路径") },
                    enabled = false,
                    maxLines = 2,
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = title.trim().isNotBlank()) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun BusyOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(message)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(modifier = Modifier.width(180.dp))
        }
    }
}

private fun RpgMakerEngine.shortLabel(): String = when (this) {
    RpgMakerEngine.XP -> "XP"
    RpgMakerEngine.VX -> "VX"
    RpgMakerEngine.VX_ACE -> "VA"
    RpgMakerEngine.MV -> "MV"
    RpgMakerEngine.MZ -> "MZ"
    RpgMakerEngine.RPG_2000_2003 -> "2K"
    RpgMakerEngine.UNKNOWN -> "RG"
}

private fun StoredImportMode.label(): String = when (this) {
    StoredImportMode.COPIED_TO_LIBRARY -> "应用库"
    StoredImportMode.DIRECT_EXTERNAL_PATH -> "外部路径"
}

private fun ThemeModeSetting.label(): String = when (this) {
    ThemeModeSetting.SYSTEM -> "跟随系统"
    ThemeModeSetting.LIGHT -> "浅色"
    ThemeModeSetting.DARK -> "深色"
}

private val virtualGamepadButtons = listOf(
    VirtualGamepadButton.A,
    VirtualGamepadButton.B,
    VirtualGamepadButton.C,
    VirtualGamepadButton.X,
    VirtualGamepadButton.Y,
    VirtualGamepadButton.Z,
    VirtualGamepadButton.L,
    VirtualGamepadButton.R,
    VirtualGamepadButton.CTRL,
    VirtualGamepadButton.ALT,
    VirtualGamepadButton.SHIFT,
    VirtualGamepadButton.RUN,
)

private val physicalGamepadButtons = listOf(
    PhysicalGamepadButton.A,
    PhysicalGamepadButton.B,
    PhysicalGamepadButton.X,
    PhysicalGamepadButton.Y,
    PhysicalGamepadButton.L1,
    PhysicalGamepadButton.R1,
    PhysicalGamepadButton.L2,
    PhysicalGamepadButton.R2,
    PhysicalGamepadButton.START,
    PhysicalGamepadButton.SELECT,
    PhysicalGamepadButton.RUN,
)

private val commonKeyCodes = listOf(
    KeyEvent.KEYCODE_Z,
    KeyEvent.KEYCODE_X,
    KeyEvent.KEYCODE_C,
    KeyEvent.KEYCODE_A,
    KeyEvent.KEYCODE_S,
    KeyEvent.KEYCODE_D,
    KeyEvent.KEYCODE_Q,
    KeyEvent.KEYCODE_W,
    KeyEvent.KEYCODE_ENTER,
    KeyEvent.KEYCODE_SPACE,
    KeyEvent.KEYCODE_ESCAPE,
    KeyEvent.KEYCODE_MENU,
    KeyEvent.KEYCODE_CTRL_LEFT,
    KeyEvent.KEYCODE_ALT_LEFT,
    KeyEvent.KEYCODE_SHIFT_LEFT,
    KeyEvent.KEYCODE_PAGE_UP,
    KeyEvent.KEYCODE_PAGE_DOWN,
    KeyEvent.KEYCODE_TAB,
)

private fun VirtualGamepadButton.displayLabel(): String = when (this) {
    VirtualGamepadButton.A -> "A"
    VirtualGamepadButton.B -> "B"
    VirtualGamepadButton.C -> "C"
    VirtualGamepadButton.X -> "X"
    VirtualGamepadButton.Y -> "Y"
    VirtualGamepadButton.Z -> "Z"
    VirtualGamepadButton.L -> "L"
    VirtualGamepadButton.R -> "R"
    VirtualGamepadButton.CTRL -> "CTRL"
    VirtualGamepadButton.ALT -> "ALT"
    VirtualGamepadButton.SHIFT -> "SHIFT"
    VirtualGamepadButton.RUN -> "RUN"
}

private fun PhysicalGamepadButton.displayLabel(): String = when (this) {
    PhysicalGamepadButton.A -> "A"
    PhysicalGamepadButton.B -> "B"
    PhysicalGamepadButton.X -> "X"
    PhysicalGamepadButton.Y -> "Y"
    PhysicalGamepadButton.L1 -> "L1"
    PhysicalGamepadButton.R1 -> "R1"
    PhysicalGamepadButton.L2 -> "L2"
    PhysicalGamepadButton.R2 -> "R2"
    PhysicalGamepadButton.START -> "START"
    PhysicalGamepadButton.SELECT -> "SELECT"
    PhysicalGamepadButton.RUN -> "RUN"
}

private fun LauncherSettings.keyCodeFor(button: VirtualGamepadButton): Int = when (button) {
    VirtualGamepadButton.A -> virtualGamepadKeyA
    VirtualGamepadButton.B -> virtualGamepadKeyB
    VirtualGamepadButton.C -> virtualGamepadKeyC
    VirtualGamepadButton.X -> virtualGamepadKeyX
    VirtualGamepadButton.Y -> virtualGamepadKeyY
    VirtualGamepadButton.Z -> virtualGamepadKeyZ
    VirtualGamepadButton.L -> virtualGamepadKeyL
    VirtualGamepadButton.R -> virtualGamepadKeyR
    VirtualGamepadButton.CTRL -> virtualGamepadKeyCtrl
    VirtualGamepadButton.ALT -> virtualGamepadKeyAlt
    VirtualGamepadButton.SHIFT -> virtualGamepadKeyShift
    VirtualGamepadButton.RUN -> virtualGamepadKeyRun
}

private fun LauncherSettings.keyCodeFor(button: PhysicalGamepadButton): Int = when (button) {
    PhysicalGamepadButton.A -> physicalGamepadKeyA
    PhysicalGamepadButton.B -> physicalGamepadKeyB
    PhysicalGamepadButton.X -> physicalGamepadKeyX
    PhysicalGamepadButton.Y -> physicalGamepadKeyY
    PhysicalGamepadButton.L1 -> physicalGamepadKeyL1
    PhysicalGamepadButton.R1 -> physicalGamepadKeyR1
    PhysicalGamepadButton.L2 -> physicalGamepadKeyL2
    PhysicalGamepadButton.R2 -> physicalGamepadKeyR2
    PhysicalGamepadButton.START -> physicalGamepadKeyStart
    PhysicalGamepadButton.SELECT -> physicalGamepadKeySelect
    PhysicalGamepadButton.RUN -> physicalGamepadKeyRun
}

private fun keyLabel(keyCode: Int): String =
    KeyEvent.keyCodeToString(keyCode)
        .removePrefix("KEYCODE_")
        .removeSuffix("_LEFT")
        .removeSuffix("_RIGHT")

private fun isAllFilesAccessGranted(): Boolean {
    return Environment.isExternalStorageManager()
}
