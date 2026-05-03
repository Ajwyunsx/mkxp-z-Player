package io.github.mkxpz.rpgplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mkxpz.rpgplayer.ui.MainScreen
import io.github.mkxpz.rpgplayer.ui.MainViewModel
import io.github.mkxpz.rpgplayer.ui.MainViewModelFactory
import io.github.mkxpz.rpgplayer.ui.theme.MkxpPlayerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as MkxpPlayerApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(uiState.userMessage) {
                uiState.userMessage?.let {
                    snackbarHostState.showSnackbar(it)
                    viewModel.consumeUserMessage()
                }
            }

            MkxpPlayerTheme(settings = uiState.settings) {
                MainScreen(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    onImportTree = viewModel::importGameTree,
                    onImportVxRtpTree = viewModel::importVxRtpTree,
                    onImportVxAceRtpTree = viewModel::importVxAceRtpTree,
                    onDownloadVxRtp = viewModel::downloadVxRtp,
                    onDownloadVxAceRtp = viewModel::downloadVxAceRtp,
                    onAddDirectPath = viewModel::addDirectPath,
                    onLaunch = viewModel::launch,
                    onDelete = viewModel::deleteGame,
                    onUpdateGameMetadata = viewModel::updateGameMetadata,
                    onExportStandaloneApk = viewModel::exportStandaloneApk,
                    onThemeModeChange = viewModel::setThemeMode,
                    onColorSourceChange = viewModel::setColorSource,
                    onManualSeedColorChange = viewModel::setManualSeedColor,
                    onPreferredImportModeChange = viewModel::setPreferredImportMode,
                    onVirtualGamepadChange = viewModel::setVirtualGamepadEnabled,
                    onVirtualGamepadOpacityChange = viewModel::setVirtualGamepadOpacity,
                    onVirtualGamepadDiagonalMovementChange = viewModel::setVirtualGamepadDiagonalMovement,
                    onVirtualGamepadKeyChange = viewModel::setVirtualGamepadKey,
                    onPhysicalGamepadMappingChange = viewModel::setPhysicalGamepadMappingEnabled,
                    onPhysicalGamepadBackAsBChange = viewModel::setPhysicalGamepadBackAsB,
                    onPhysicalGamepadKeyChange = viewModel::setPhysicalGamepadKey,
                    onFixedFramerateChange = viewModel::setFixedFramerate,
                    onSmoothScalingChange = viewModel::setSmoothScaling,
                    onKeepAspectRatioChange = viewModel::setKeepAspectRatio,
                    onSoundFontPathChange = viewModel::setSoundFontPath,
                    onVxRtpPathChange = viewModel::setVxRtpPath,
                    onVxAceRtpPathChange = viewModel::setVxAceRtpPath,
                    onRubyClassicCompatibilityChange = viewModel::setRubyClassicCompatibilityEnabled,
                    onWinApiCompatibilityChange = viewModel::setWinApiCompatibilityEnabled,
                    onDebugLaunchChange = viewModel::setDebugLaunch,
                )
            }
        }
    }
}
