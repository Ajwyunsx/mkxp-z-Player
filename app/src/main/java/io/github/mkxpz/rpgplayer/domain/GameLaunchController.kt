package io.github.mkxpz.rpgplayer.domain

import android.content.Context
import io.github.mkxpz.engine.MkxpLauncher
import io.github.mkxpz.rpgplayer.data.GameEntry
import io.github.mkxpz.rpgplayer.data.GameRepository
import io.github.mkxpz.rpgplayer.data.SettingsRepository
import io.github.mkxpz.rpgplayer.data.isEasyRpg
import io.github.mkxpz.rpgplayer.data.isHtml5
import io.github.mkxpz.rpgplayer.easyrpg.EasyRpgGameActivity
import kotlinx.coroutines.flow.first

class GameLaunchController(
    private val context: Context,
    private val repository: GameRepository,
    private val settingsRepository: SettingsRepository,
    private val html5CompatibilityDetector: Html5CompatibilityDetector = Html5CompatibilityDetector(),
) {
    suspend fun launch(entry: GameEntry) {
        val settings = settingsRepository.settings.first()
        repository.markLaunched(entry)
        if (entry.engine.isEasyRpg()) {
            if (MkxpLauncher.hasEasyRpgRuntime(context)) {
                MkxpLauncher.launchEasyRpg(
                    context = context,
                    gameId = entry.id,
                    gamePath = entry.installedPath,
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
                    virtualGamepadKeyRun = settings.virtualGamepadKeyRun,
                    physicalGamepadMappingEnabled = settings.physicalGamepadMappingEnabled,
                    physicalGamepadBackAsB = settings.physicalGamepadBackAsB,
                    physicalGamepadKeyA = settings.physicalGamepadKeyA,
                    physicalGamepadKeyB = settings.physicalGamepadKeyB,
                    physicalGamepadKeyX = settings.physicalGamepadKeyX,
                    physicalGamepadKeyY = settings.physicalGamepadKeyY,
                    physicalGamepadKeyL1 = settings.physicalGamepadKeyL1,
                    physicalGamepadKeyR1 = settings.physicalGamepadKeyR1,
                    physicalGamepadKeyL2 = settings.physicalGamepadKeyL2,
                    physicalGamepadKeyR2 = settings.physicalGamepadKeyR2,
                    physicalGamepadKeyStart = settings.physicalGamepadKeyStart,
                    physicalGamepadKeySelect = settings.physicalGamepadKeySelect,
                    physicalGamepadKeyRun = settings.physicalGamepadKeyRun,
                )
            } else {
                EasyRpgGameActivity.launch(
                    context = context,
                    gameId = entry.id,
                    title = entry.title,
                    gamePath = entry.installedPath,
                    reason = "未发现内置 EasyRPG 原生库 libeasyrpg_android.so。源码已固定到 third_party/easyrpg-player，打包原生库后会自动切换为内置核心。",
                )
            }
            return
        }
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
                virtualGamepadKeyRun = settings.virtualGamepadKeyRun,
                physicalGamepadMappingEnabled = settings.physicalGamepadMappingEnabled,
                physicalGamepadBackAsB = settings.physicalGamepadBackAsB,
                physicalGamepadKeyA = settings.physicalGamepadKeyA,
                physicalGamepadKeyB = settings.physicalGamepadKeyB,
                physicalGamepadKeyX = settings.physicalGamepadKeyX,
                physicalGamepadKeyY = settings.physicalGamepadKeyY,
                physicalGamepadKeyL1 = settings.physicalGamepadKeyL1,
                physicalGamepadKeyR1 = settings.physicalGamepadKeyR1,
                physicalGamepadKeyL2 = settings.physicalGamepadKeyL2,
                physicalGamepadKeyR2 = settings.physicalGamepadKeyR2,
                physicalGamepadKeyStart = settings.physicalGamepadKeyStart,
                physicalGamepadKeySelect = settings.physicalGamepadKeySelect,
                physicalGamepadKeyRun = settings.physicalGamepadKeyRun,
            )
            return
        }
        val config = repository.writeConfig(entry)
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
            virtualGamepadKeyRun = settings.virtualGamepadKeyRun,
            physicalGamepadMappingEnabled = settings.physicalGamepadMappingEnabled,
            physicalGamepadBackAsB = settings.physicalGamepadBackAsB,
            physicalGamepadKeyA = settings.physicalGamepadKeyA,
            physicalGamepadKeyB = settings.physicalGamepadKeyB,
            physicalGamepadKeyX = settings.physicalGamepadKeyX,
            physicalGamepadKeyY = settings.physicalGamepadKeyY,
            physicalGamepadKeyL1 = settings.physicalGamepadKeyL1,
            physicalGamepadKeyR1 = settings.physicalGamepadKeyR1,
            physicalGamepadKeyL2 = settings.physicalGamepadKeyL2,
            physicalGamepadKeyR2 = settings.physicalGamepadKeyR2,
            physicalGamepadKeyStart = settings.physicalGamepadKeyStart,
            physicalGamepadKeySelect = settings.physicalGamepadKeySelect,
            physicalGamepadKeyRun = settings.physicalGamepadKeyRun,
        )
    }
}
