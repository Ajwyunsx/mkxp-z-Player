package io.github.mkxpz.rpgplayer.domain

import android.content.Context
import io.github.mkxpz.engine.MkxpLauncher
import io.github.mkxpz.rpgplayer.data.GameEntry
import io.github.mkxpz.rpgplayer.data.GameRepository
import io.github.mkxpz.rpgplayer.data.SettingsRepository
import io.github.mkxpz.rpgplayer.data.isHtml5
import kotlinx.coroutines.flow.first

class GameLaunchController(
    private val context: Context,
    private val repository: GameRepository,
    private val settingsRepository: SettingsRepository,
    private val configWriter: MkxpConfigWriter,
    private val html5CompatibilityDetector: Html5CompatibilityDetector = Html5CompatibilityDetector(),
) {
    suspend fun launch(entry: GameEntry) {
        val settings = settingsRepository.settings.first()
        val config = configWriter.writeConfig(entry, settings, java.io.File(entry.configPath))
        repository.markLaunched(entry)
        if (entry.engine.isHtml5()) {
            val html5Compatibility = html5CompatibilityDetector.detect(java.io.File(entry.installedPath))
            MkxpLauncher.launchHtml5(
                context = context,
                gameId = entry.id,
                gamePath = entry.installedPath,
                debug = settings.debugLaunch,
                html5ThreeDMode = html5Compatibility.hasThreeDPlugins,
                html5ThreeDPlugins = html5Compatibility.threeDPluginNames,
                virtualGamepad = settings.virtualGamepadEnabled,
                virtualGamepadOpacity = settings.virtualGamepadOpacity,
                virtualGamepadScale = settings.virtualGamepadScale,
                virtualGamepadDiagonalMovement = settings.virtualGamepadDiagonalMovement,
                virtualGamepadKeyA = settings.virtualGamepadKeyA,
                virtualGamepadKeyB = settings.virtualGamepadKeyB,
                virtualGamepadKeyC = settings.virtualGamepadKeyC,
                virtualGamepadKeyX = settings.virtualGamepadKeyX,
                virtualGamepadKeyY = settings.virtualGamepadKeyY,
                virtualGamepadKeyZ = settings.virtualGamepadKeyZ,
                virtualGamepadKeyL = settings.virtualGamepadKeyL,
                virtualGamepadKeyR = settings.virtualGamepadKeyR,
                virtualGamepadKeyCtrl = settings.virtualGamepadKeyCtrl,
                virtualGamepadKeyAlt = settings.virtualGamepadKeyAlt,
                virtualGamepadKeyShift = settings.virtualGamepadKeyShift,
            )
            return
        }
        MkxpLauncher.launch(
            context = context,
            gameId = entry.id,
            gamePath = entry.installedPath,
            configPath = config.absolutePath,
            debug = settings.debugLaunch,
            virtualGamepad = settings.virtualGamepadEnabled,
            virtualGamepadOpacity = settings.virtualGamepadOpacity,
            virtualGamepadScale = settings.virtualGamepadScale,
            virtualGamepadDiagonalMovement = settings.virtualGamepadDiagonalMovement,
            virtualGamepadKeyA = settings.virtualGamepadKeyA,
            virtualGamepadKeyB = settings.virtualGamepadKeyB,
            virtualGamepadKeyC = settings.virtualGamepadKeyC,
            virtualGamepadKeyX = settings.virtualGamepadKeyX,
            virtualGamepadKeyY = settings.virtualGamepadKeyY,
            virtualGamepadKeyZ = settings.virtualGamepadKeyZ,
            virtualGamepadKeyL = settings.virtualGamepadKeyL,
            virtualGamepadKeyR = settings.virtualGamepadKeyR,
            virtualGamepadKeyCtrl = settings.virtualGamepadKeyCtrl,
            virtualGamepadKeyAlt = settings.virtualGamepadKeyAlt,
            virtualGamepadKeyShift = settings.virtualGamepadKeyShift,
        )
    }
}
