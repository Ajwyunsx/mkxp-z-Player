package io.github.mkxpz.rpgplayer.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings as AndroidSettings
import android.view.KeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
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
import androidx.compose.material.icons.filled.FolderOpen
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mkxpz.rpgplayer.data.GameEntry
import io.github.mkxpz.rpgplayer.data.LauncherSettings
import io.github.mkxpz.rpgplayer.data.PreferredImportMode
import io.github.mkxpz.rpgplayer.data.RpgMakerEngine
import io.github.mkxpz.rpgplayer.data.StoredImportMode
import io.github.mkxpz.rpgplayer.data.ThemeColorSource
import io.github.mkxpz.rpgplayer.data.ThemeModeSetting
import io.github.mkxpz.rpgplayer.data.VirtualGamepadButton
import io.github.mkxpz.rpgplayer.ui.theme.supportsDynamicColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    snackbarHostState: SnackbarHostState,
    onImportTree: (Uri) -> Unit,
    onAddDirectPath: (String) -> Unit,
    onLaunch: (GameEntry) -> Unit,
    onDelete: (GameEntry, Boolean) -> Unit,
    onThemeModeChange: (ThemeModeSetting) -> Unit,
    onColorSourceChange: (ThemeColorSource) -> Unit,
    onManualSeedColorChange: (Long) -> Unit,
    onPreferredImportModeChange: (PreferredImportMode) -> Unit,
    onVirtualGamepadChange: (Boolean) -> Unit,
    onVirtualGamepadOpacityChange: (Int) -> Unit,
    onVirtualGamepadScaleChange: (Int) -> Unit,
    onVirtualGamepadDiagonalMovementChange: (Boolean) -> Unit,
    onVirtualGamepadKeyChange: (VirtualGamepadButton, Int) -> Unit,
    onFixedFramerateChange: (Int) -> Unit,
    onSmoothScalingChange: (Boolean) -> Unit,
    onKeepAspectRatioChange: (Boolean) -> Unit,
    onSoundFontPathChange: (String) -> Unit,
    onDebugLaunchChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) onImportTree(uri)
    }
    val allFilesAccessLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDirectPathDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<GameEntry?>(null) }

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
                    onVirtualGamepadScaleChange = onVirtualGamepadScaleChange,
                    onVirtualGamepadDiagonalMovementChange = onVirtualGamepadDiagonalMovementChange,
                    onVirtualGamepadKeyChange = onVirtualGamepadKeyChange,
                    onFixedFramerateChange = onFixedFramerateChange,
                    onSmoothScalingChange = onSmoothScalingChange,
                    onKeepAspectRatioChange = onKeepAspectRatioChange,
                    onSoundFontPathChange = onSoundFontPathChange,
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
}

@Composable
private fun GamesScreen(
    games: List<GameEntry>,
    onLaunch: (GameEntry) -> Unit,
    onDelete: (GameEntry) -> Unit,
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
                Box(
                    modifier = Modifier
                        .size(44.dp)
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
    onVirtualGamepadScaleChange: (Int) -> Unit,
    onVirtualGamepadDiagonalMovementChange: (Boolean) -> Unit,
    onVirtualGamepadKeyChange: (VirtualGamepadButton, Int) -> Unit,
    onFixedFramerateChange: (Int) -> Unit,
    onSmoothScalingChange: (Boolean) -> Unit,
    onKeepAspectRatioChange: (Boolean) -> Unit,
    onSoundFontPathChange: (String) -> Unit,
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
                    onScaleChange = onVirtualGamepadScaleChange,
                    onDiagonalMovementChange = onVirtualGamepadDiagonalMovementChange,
                    onKeyChange = onVirtualGamepadKeyChange,
                )
                Spacer(Modifier.height(8.dp))
                SwitchRow("保持宽高比", "避免拉伸游戏画面", settings.keepAspectRatio, onKeepAspectRatioChange)
                SwitchRow("平滑缩放", "放大时启用线性插值", settings.smoothScaling, onSmoothScalingChange)
                SwitchRow("调试启动", "生成配置时启用 FPS 输出", settings.debugLaunch, onDebugLaunchChange)
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
                OutlinedTextField(
                    value = settings.soundFontPath,
                    onValueChange = onSoundFontPathChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("SoundFont 路径") },
                    singleLine = true,
                    placeholder = { Text("例如 /storage/emulated/0/GMGSx.sf2") },
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
    onScaleChange: (Int) -> Unit,
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
        Text(
            text = "大小：${settings.virtualGamepadScale}%",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        androidx.compose.material3.Slider(
            value = settings.virtualGamepadScale.toFloat(),
            onValueChange = { onScaleChange(it.toInt()) },
            valueRange = 60f..160f,
            steps = 99,
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
}

private fun keyLabel(keyCode: Int): String =
    KeyEvent.keyCodeToString(keyCode)
        .removePrefix("KEYCODE_")
        .removeSuffix("_LEFT")
        .removeSuffix("_RIGHT")

private fun isAllFilesAccessGranted(): Boolean {
    return Environment.isExternalStorageManager()
}
