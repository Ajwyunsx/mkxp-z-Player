package io.github.mkxpz.rpgplayer.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.mkxpz.rpgplayer.data.AppContainer
import io.github.mkxpz.rpgplayer.data.GameEntry
import io.github.mkxpz.rpgplayer.data.LauncherSettings
import io.github.mkxpz.rpgplayer.data.PreferredImportMode
import io.github.mkxpz.rpgplayer.data.ThemeColorSource
import io.github.mkxpz.rpgplayer.data.ThemeModeSetting
import io.github.mkxpz.rpgplayer.data.VirtualGamepadButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val games: List<GameEntry> = emptyList(),
    val settings: LauncherSettings = LauncherSettings(),
    val busyMessage: String? = null,
    val userMessage: String? = null,
)

class MainViewModel(private val container: AppContainer) : ViewModel() {
    private val busyMessage = MutableStateFlow<String?>(null)
    private val userMessage = MutableStateFlow<String?>(null)

    val uiState = combine(
        container.gameRepository.games,
        container.settingsRepository.settings,
        busyMessage,
        userMessage,
    ) { games, settings, busy, message ->
        MainUiState(
            games = games,
            settings = settings,
            busyMessage = busy,
            userMessage = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    fun importGameTree(uri: Uri) {
        viewModelScope.launch {
            runBusy("正在复制游戏...") {
                val entry = container.importService.importTree(uri)
                userMessage.value = "已添加 ${entry.title}"
            }
        }
    }

    fun addDirectPath(path: String) {
        viewModelScope.launch {
            runBusy("正在校验路径...") {
                val entry = container.importService.addDirectPath(path)
                userMessage.value = "已添加 ${entry.title}"
            }
        }
    }

    fun launch(entry: GameEntry) {
        viewModelScope.launch {
            runBusy("正在准备 mkxp-z 配置...") {
                container.launchController.launch(entry)
            }
        }
    }

    fun deleteGame(entry: GameEntry, removeFiles: Boolean) {
        viewModelScope.launch {
            runBusy("正在删除...") {
                container.gameRepository.delete(entry, removeFiles)
                userMessage.value = "已删除 ${entry.title}"
            }
        }
    }

    fun consumeUserMessage() {
        userMessage.value = null
    }

    fun setThemeMode(value: ThemeModeSetting) = updateSetting { setThemeMode(value) }
    fun setColorSource(value: ThemeColorSource) = updateSetting { setColorSource(value) }
    fun setManualSeedColor(value: Long) = updateSetting { setManualSeedColor(value) }
    fun setPreferredImportMode(value: PreferredImportMode) = updateSetting { setPreferredImportMode(value) }
    fun setVirtualGamepadEnabled(value: Boolean) = updateSetting { setVirtualGamepadEnabled(value) }
    fun setVirtualGamepadOpacity(value: Int) = updateSetting { setVirtualGamepadOpacity(value) }
    fun setVirtualGamepadScale(value: Int) = updateSetting { setVirtualGamepadScale(value) }
    fun setVirtualGamepadDiagonalMovement(value: Boolean) = updateSetting { setVirtualGamepadDiagonalMovement(value) }
    fun setVirtualGamepadKey(button: VirtualGamepadButton, keyCode: Int) =
        updateSetting { setVirtualGamepadKey(button, keyCode) }
    fun setFixedFramerate(value: Int) = updateSetting { setFixedFramerate(value) }
    fun setSmoothScaling(value: Boolean) = updateSetting { setSmoothScaling(value) }
    fun setKeepAspectRatio(value: Boolean) = updateSetting { setKeepAspectRatio(value) }
    fun setSoundFontPath(value: String) = updateSetting { setSoundFontPath(value) }
    fun setDebugLaunch(value: Boolean) = updateSetting { setDebugLaunch(value) }

    private fun updateSetting(block: suspend io.github.mkxpz.rpgplayer.data.SettingsRepository.() -> Unit) {
        viewModelScope.launch {
            container.settingsRepository.block()
        }
    }

    private suspend fun runBusy(message: String, block: suspend () -> Unit) {
        busyMessage.value = message
        try {
            block()
        } catch (error: Throwable) {
            userMessage.value = error.message ?: error::class.java.simpleName
        } finally {
            busyMessage.value = null
        }
    }
}

class MainViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(container) as T
    }
}
