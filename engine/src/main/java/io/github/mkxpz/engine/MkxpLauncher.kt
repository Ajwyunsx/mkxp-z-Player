package io.github.mkxpz.engine

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Process
import android.view.KeyEvent
import org.easyrpg.player.player.EasyRpgPlayerActivity
import java.io.File

object MkxpLauncher {
    const val EXTRA_GAME_ID = "io.github.mkxpz.engine.GAME_ID"
    const val EXTRA_GAME_PATH = "io.github.mkxpz.engine.GAME_PATH"
    const val EXTRA_CONFIG_PATH = "io.github.mkxpz.engine.CONFIG_PATH"
    const val EXTRA_DEBUG = "io.github.mkxpz.engine.DEBUG"
    const val EXTRA_HTML5_3D_MODE = "io.github.mkxpz.engine.HTML5_3D_MODE"
    const val EXTRA_HTML5_3D_PLUGINS = "io.github.mkxpz.engine.HTML5_3D_PLUGINS"
    const val EXTRA_VIRTUAL_GAMEPAD = "io.github.mkxpz.engine.VIRTUAL_GAMEPAD"
    const val EXTRA_GAMEPAD_OPACITY = "io.github.mkxpz.engine.GAMEPAD_OPACITY"
    const val EXTRA_GAMEPAD_SCALE = "io.github.mkxpz.engine.GAMEPAD_SCALE"
    const val EXTRA_GAMEPAD_DIAGONAL_MOVEMENT = "io.github.mkxpz.engine.GAMEPAD_DIAGONAL_MOVEMENT"
    const val EXTRA_GAMEPAD_KEY_A = "io.github.mkxpz.engine.GAMEPAD_KEY_A"
    const val EXTRA_GAMEPAD_KEY_B = "io.github.mkxpz.engine.GAMEPAD_KEY_B"
    const val EXTRA_GAMEPAD_KEY_C = "io.github.mkxpz.engine.GAMEPAD_KEY_C"
    const val EXTRA_GAMEPAD_KEY_X = "io.github.mkxpz.engine.GAMEPAD_KEY_X"
    const val EXTRA_GAMEPAD_KEY_Y = "io.github.mkxpz.engine.GAMEPAD_KEY_Y"
    const val EXTRA_GAMEPAD_KEY_Z = "io.github.mkxpz.engine.GAMEPAD_KEY_Z"
    const val EXTRA_GAMEPAD_KEY_L = "io.github.mkxpz.engine.GAMEPAD_KEY_L"
    const val EXTRA_GAMEPAD_KEY_R = "io.github.mkxpz.engine.GAMEPAD_KEY_R"
    const val EXTRA_GAMEPAD_KEY_CTRL = "io.github.mkxpz.engine.GAMEPAD_KEY_CTRL"
    const val EXTRA_GAMEPAD_KEY_ALT = "io.github.mkxpz.engine.GAMEPAD_KEY_ALT"
    const val EXTRA_GAMEPAD_KEY_SHIFT = "io.github.mkxpz.engine.GAMEPAD_KEY_SHIFT"
    const val EXTRA_GAMEPAD_KEY_RUN = "io.github.mkxpz.engine.GAMEPAD_KEY_RUN"
    const val EXTRA_GAMEPAD_LAYOUT = "io.github.mkxpz.engine.GAMEPAD_LAYOUT"
    const val EXTRA_PHYSICAL_GAMEPAD_MAPPING = "io.github.mkxpz.engine.PHYSICAL_GAMEPAD_MAPPING"
    const val EXTRA_PHYSICAL_GAMEPAD_BACK_AS_B = "io.github.mkxpz.engine.PHYSICAL_GAMEPAD_BACK_AS_B"
    const val EXTRA_PHYSICAL_GAMEPAD_KEY_A = "io.github.mkxpz.engine.PHYSICAL_GAMEPAD_KEY_A"
    const val EXTRA_PHYSICAL_GAMEPAD_KEY_B = "io.github.mkxpz.engine.PHYSICAL_GAMEPAD_KEY_B"
    const val EXTRA_PHYSICAL_GAMEPAD_KEY_X = "io.github.mkxpz.engine.PHYSICAL_GAMEPAD_KEY_X"
    const val EXTRA_PHYSICAL_GAMEPAD_KEY_Y = "io.github.mkxpz.engine.PHYSICAL_GAMEPAD_KEY_Y"
    const val EXTRA_PHYSICAL_GAMEPAD_KEY_L1 = "io.github.mkxpz.engine.PHYSICAL_GAMEPAD_KEY_L1"
    const val EXTRA_PHYSICAL_GAMEPAD_KEY_R1 = "io.github.mkxpz.engine.PHYSICAL_GAMEPAD_KEY_R1"
    const val EXTRA_PHYSICAL_GAMEPAD_KEY_L2 = "io.github.mkxpz.engine.PHYSICAL_GAMEPAD_KEY_L2"
    const val EXTRA_PHYSICAL_GAMEPAD_KEY_R2 = "io.github.mkxpz.engine.PHYSICAL_GAMEPAD_KEY_R2"
    const val EXTRA_PHYSICAL_GAMEPAD_KEY_START = "io.github.mkxpz.engine.PHYSICAL_GAMEPAD_KEY_START"
    const val EXTRA_PHYSICAL_GAMEPAD_KEY_SELECT = "io.github.mkxpz.engine.PHYSICAL_GAMEPAD_KEY_SELECT"
    const val EXTRA_PHYSICAL_GAMEPAD_KEY_RUN = "io.github.mkxpz.engine.PHYSICAL_GAMEPAD_KEY_RUN"
    const val EXTRA_EASYRPG_RTP_PATH = "io.github.mkxpz.engine.EASYRPG_RTP_PATH"
    const val EXTRA_MODIFIER_CONFIG_PATH = "io.github.mkxpz.engine.MODIFIER_CONFIG_PATH"

    fun launch(
        context: Context,
        gameId: String,
        gamePath: String,
        configPath: String,
        debug: Boolean,
        virtualGamepad: Boolean,
        virtualGamepadOpacity: Int = 30,
        virtualGamepadScale: Int = 100,
        virtualGamepadDiagonalMovement: Boolean = false,
        virtualGamepadKeyA: Int = KeyEvent.KEYCODE_Z,
        virtualGamepadKeyB: Int = KeyEvent.KEYCODE_X,
        virtualGamepadKeyC: Int = KeyEvent.KEYCODE_C,
        virtualGamepadKeyX: Int = KeyEvent.KEYCODE_A,
        virtualGamepadKeyY: Int = KeyEvent.KEYCODE_S,
        virtualGamepadKeyZ: Int = KeyEvent.KEYCODE_D,
        virtualGamepadKeyL: Int = KeyEvent.KEYCODE_Q,
        virtualGamepadKeyR: Int = KeyEvent.KEYCODE_W,
        virtualGamepadKeyCtrl: Int = KeyEvent.KEYCODE_CTRL_LEFT,
        virtualGamepadKeyAlt: Int = KeyEvent.KEYCODE_ALT_LEFT,
        virtualGamepadKeyShift: Int = KeyEvent.KEYCODE_SHIFT_LEFT,
        virtualGamepadKeyRun: Int = KeyEvent.KEYCODE_SHIFT_LEFT,
        physicalGamepadMappingEnabled: Boolean = true,
        physicalGamepadBackAsB: Boolean = true,
        physicalGamepadKeyA: Int = KeyEvent.KEYCODE_Z,
        physicalGamepadKeyB: Int = KeyEvent.KEYCODE_X,
        physicalGamepadKeyX: Int = KeyEvent.KEYCODE_A,
        physicalGamepadKeyY: Int = KeyEvent.KEYCODE_S,
        physicalGamepadKeyL1: Int = KeyEvent.KEYCODE_Q,
        physicalGamepadKeyR1: Int = KeyEvent.KEYCODE_W,
        physicalGamepadKeyL2: Int = KeyEvent.KEYCODE_PAGE_UP,
        physicalGamepadKeyR2: Int = KeyEvent.KEYCODE_PAGE_DOWN,
        physicalGamepadKeyStart: Int = KeyEvent.KEYCODE_ENTER,
        physicalGamepadKeySelect: Int = KeyEvent.KEYCODE_ESCAPE,
        physicalGamepadKeyRun: Int = KeyEvent.KEYCODE_SHIFT_LEFT,
    ) {
        resetGameProcessIfNeeded(context)
        val modifierConfig = modifierConfigForMkxp(context, gameId, configPath)
        ModifierConfig.load(context, gameId, modifierConfig.absolutePath).writeToFile(modifierConfig)
        val target = if (hasFullMkxpRuntime(context)) SdlGameActivity::class.java else GameActivity::class.java
        val intent = Intent(context, target)
            .putExtra(EXTRA_GAME_ID, gameId)
            .putExtra(EXTRA_GAME_PATH, gamePath)
            .putExtra(EXTRA_CONFIG_PATH, configPath)
            .putExtra(EXTRA_MODIFIER_CONFIG_PATH, modifierConfig.absolutePath)
            .putExtra(EXTRA_DEBUG, debug)
            .putExtra(EXTRA_VIRTUAL_GAMEPAD, virtualGamepad)
            .putExtra(EXTRA_GAMEPAD_OPACITY, virtualGamepadOpacity)
            .putExtra(EXTRA_GAMEPAD_SCALE, virtualGamepadScale)
            .putExtra(EXTRA_GAMEPAD_DIAGONAL_MOVEMENT, virtualGamepadDiagonalMovement)
            .putExtra(EXTRA_GAMEPAD_KEY_A, virtualGamepadKeyA)
            .putExtra(EXTRA_GAMEPAD_KEY_B, virtualGamepadKeyB)
            .putExtra(EXTRA_GAMEPAD_KEY_C, virtualGamepadKeyC)
            .putExtra(EXTRA_GAMEPAD_KEY_X, virtualGamepadKeyX)
            .putExtra(EXTRA_GAMEPAD_KEY_Y, virtualGamepadKeyY)
            .putExtra(EXTRA_GAMEPAD_KEY_Z, virtualGamepadKeyZ)
            .putExtra(EXTRA_GAMEPAD_KEY_L, virtualGamepadKeyL)
            .putExtra(EXTRA_GAMEPAD_KEY_R, virtualGamepadKeyR)
            .putExtra(EXTRA_GAMEPAD_KEY_CTRL, virtualGamepadKeyCtrl)
            .putExtra(EXTRA_GAMEPAD_KEY_ALT, virtualGamepadKeyAlt)
            .putExtra(EXTRA_GAMEPAD_KEY_SHIFT, virtualGamepadKeyShift)
            .putExtra(EXTRA_GAMEPAD_KEY_RUN, virtualGamepadKeyRun)
            .putExtra(EXTRA_GAMEPAD_LAYOUT, savedGamepadLayout(context))
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_MAPPING, physicalGamepadMappingEnabled)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_BACK_AS_B, physicalGamepadBackAsB)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_A, physicalGamepadKeyA)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_B, physicalGamepadKeyB)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_X, physicalGamepadKeyX)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_Y, physicalGamepadKeyY)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_L1, physicalGamepadKeyL1)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_R1, physicalGamepadKeyR1)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_L2, physicalGamepadKeyL2)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_R2, physicalGamepadKeyR2)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_START, physicalGamepadKeyStart)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_SELECT, physicalGamepadKeySelect)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_RUN, physicalGamepadKeyRun)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
    }

    fun launchHtml5(
        context: Context,
        gameId: String,
        gamePath: String,
        debug: Boolean,
        virtualGamepad: Boolean,
        html5ThreeDMode: Boolean = false,
        html5ThreeDPlugins: List<String> = emptyList(),
        virtualGamepadOpacity: Int = 30,
        virtualGamepadScale: Int = 100,
        virtualGamepadDiagonalMovement: Boolean = false,
        virtualGamepadKeyA: Int = KeyEvent.KEYCODE_Z,
        virtualGamepadKeyB: Int = KeyEvent.KEYCODE_X,
        virtualGamepadKeyC: Int = KeyEvent.KEYCODE_C,
        virtualGamepadKeyX: Int = KeyEvent.KEYCODE_A,
        virtualGamepadKeyY: Int = KeyEvent.KEYCODE_S,
        virtualGamepadKeyZ: Int = KeyEvent.KEYCODE_D,
        virtualGamepadKeyL: Int = KeyEvent.KEYCODE_Q,
        virtualGamepadKeyR: Int = KeyEvent.KEYCODE_W,
        virtualGamepadKeyCtrl: Int = KeyEvent.KEYCODE_CTRL_LEFT,
        virtualGamepadKeyAlt: Int = KeyEvent.KEYCODE_ALT_LEFT,
        virtualGamepadKeyShift: Int = KeyEvent.KEYCODE_SHIFT_LEFT,
        virtualGamepadKeyRun: Int = KeyEvent.KEYCODE_SHIFT_LEFT,
        physicalGamepadMappingEnabled: Boolean = true,
        physicalGamepadBackAsB: Boolean = true,
        physicalGamepadKeyA: Int = KeyEvent.KEYCODE_Z,
        physicalGamepadKeyB: Int = KeyEvent.KEYCODE_X,
        physicalGamepadKeyX: Int = KeyEvent.KEYCODE_A,
        physicalGamepadKeyY: Int = KeyEvent.KEYCODE_S,
        physicalGamepadKeyL1: Int = KeyEvent.KEYCODE_Q,
        physicalGamepadKeyR1: Int = KeyEvent.KEYCODE_W,
        physicalGamepadKeyL2: Int = KeyEvent.KEYCODE_PAGE_UP,
        physicalGamepadKeyR2: Int = KeyEvent.KEYCODE_PAGE_DOWN,
        physicalGamepadKeyStart: Int = KeyEvent.KEYCODE_ENTER,
        physicalGamepadKeySelect: Int = KeyEvent.KEYCODE_ESCAPE,
        physicalGamepadKeyRun: Int = KeyEvent.KEYCODE_SHIFT_LEFT,
    ) {
        resetGameProcessIfNeeded(context)
        val modifierConfig = modifierConfigForGame(context, gameId)
        ModifierConfig.load(context, gameId, modifierConfig.absolutePath).writeToFile(modifierConfig)
        val intent = Intent(context, WebGameActivity::class.java)
            .putExtra(EXTRA_GAME_ID, gameId)
            .putExtra(EXTRA_GAME_PATH, gamePath)
            .putExtra(EXTRA_MODIFIER_CONFIG_PATH, modifierConfig.absolutePath)
            .putExtra(EXTRA_DEBUG, debug)
            .putExtra(EXTRA_HTML5_3D_MODE, html5ThreeDMode)
            .putStringArrayListExtra(EXTRA_HTML5_3D_PLUGINS, ArrayList(html5ThreeDPlugins))
            .putExtra(EXTRA_VIRTUAL_GAMEPAD, virtualGamepad)
            .putExtra(EXTRA_GAMEPAD_OPACITY, virtualGamepadOpacity)
            .putExtra(EXTRA_GAMEPAD_SCALE, virtualGamepadScale)
            .putExtra(EXTRA_GAMEPAD_DIAGONAL_MOVEMENT, virtualGamepadDiagonalMovement)
            .putExtra(EXTRA_GAMEPAD_KEY_A, virtualGamepadKeyA)
            .putExtra(EXTRA_GAMEPAD_KEY_B, virtualGamepadKeyB)
            .putExtra(EXTRA_GAMEPAD_KEY_C, virtualGamepadKeyC)
            .putExtra(EXTRA_GAMEPAD_KEY_X, virtualGamepadKeyX)
            .putExtra(EXTRA_GAMEPAD_KEY_Y, virtualGamepadKeyY)
            .putExtra(EXTRA_GAMEPAD_KEY_Z, virtualGamepadKeyZ)
            .putExtra(EXTRA_GAMEPAD_KEY_L, virtualGamepadKeyL)
            .putExtra(EXTRA_GAMEPAD_KEY_R, virtualGamepadKeyR)
            .putExtra(EXTRA_GAMEPAD_KEY_CTRL, virtualGamepadKeyCtrl)
            .putExtra(EXTRA_GAMEPAD_KEY_ALT, virtualGamepadKeyAlt)
            .putExtra(EXTRA_GAMEPAD_KEY_SHIFT, virtualGamepadKeyShift)
            .putExtra(EXTRA_GAMEPAD_KEY_RUN, virtualGamepadKeyRun)
            .putExtra(EXTRA_GAMEPAD_LAYOUT, savedGamepadLayout(context))
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_MAPPING, physicalGamepadMappingEnabled)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_BACK_AS_B, physicalGamepadBackAsB)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_A, physicalGamepadKeyA)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_B, physicalGamepadKeyB)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_X, physicalGamepadKeyX)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_Y, physicalGamepadKeyY)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_L1, physicalGamepadKeyL1)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_R1, physicalGamepadKeyR1)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_L2, physicalGamepadKeyL2)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_R2, physicalGamepadKeyR2)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_START, physicalGamepadKeyStart)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_SELECT, physicalGamepadKeySelect)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_RUN, physicalGamepadKeyRun)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
    }

    fun launchEasyRpg(
        context: Context,
        gameId: String,
        gamePath: String,
        debug: Boolean,
        virtualGamepad: Boolean,
        virtualGamepadOpacity: Int = 30,
        virtualGamepadScale: Int = 100,
        virtualGamepadDiagonalMovement: Boolean = false,
        virtualGamepadKeyA: Int = KeyEvent.KEYCODE_Z,
        virtualGamepadKeyB: Int = KeyEvent.KEYCODE_X,
        virtualGamepadKeyC: Int = KeyEvent.KEYCODE_C,
        virtualGamepadKeyX: Int = KeyEvent.KEYCODE_A,
        virtualGamepadKeyY: Int = KeyEvent.KEYCODE_S,
        virtualGamepadKeyZ: Int = KeyEvent.KEYCODE_D,
        virtualGamepadKeyL: Int = KeyEvent.KEYCODE_Q,
        virtualGamepadKeyR: Int = KeyEvent.KEYCODE_W,
        virtualGamepadKeyCtrl: Int = KeyEvent.KEYCODE_CTRL_LEFT,
        virtualGamepadKeyAlt: Int = KeyEvent.KEYCODE_ALT_LEFT,
        virtualGamepadKeyShift: Int = KeyEvent.KEYCODE_SHIFT_LEFT,
        virtualGamepadKeyRun: Int = KeyEvent.KEYCODE_SHIFT_LEFT,
        physicalGamepadMappingEnabled: Boolean = true,
        physicalGamepadBackAsB: Boolean = true,
        physicalGamepadKeyA: Int = KeyEvent.KEYCODE_Z,
        physicalGamepadKeyB: Int = KeyEvent.KEYCODE_X,
        physicalGamepadKeyX: Int = KeyEvent.KEYCODE_A,
        physicalGamepadKeyY: Int = KeyEvent.KEYCODE_S,
        physicalGamepadKeyL1: Int = KeyEvent.KEYCODE_Q,
        physicalGamepadKeyR1: Int = KeyEvent.KEYCODE_W,
        physicalGamepadKeyL2: Int = KeyEvent.KEYCODE_PAGE_UP,
        physicalGamepadKeyR2: Int = KeyEvent.KEYCODE_PAGE_DOWN,
        physicalGamepadKeyStart: Int = KeyEvent.KEYCODE_ENTER,
        physicalGamepadKeySelect: Int = KeyEvent.KEYCODE_ESCAPE,
        physicalGamepadKeyRun: Int = KeyEvent.KEYCODE_SHIFT_LEFT,
    ) {
        resetGameProcessIfNeeded(context)
        val easyRpgRoot = File(context.filesDir, "easyrpg")
        val configDir = File(easyRpgRoot, "config/$gameId").apply { mkdirs() }
        val saveDir = File(easyRpgRoot, "saves/$gameId").apply { mkdirs() }
        val modifierConfig = File(configDir, "modifier.properties")
        ModifierConfig.load(context, gameId, modifierConfig.absolutePath).writeToFile(modifierConfig)
        val logFile = File(easyRpgRoot, "logs/$gameId/easyrpg-player.log").also {
            it.parentFile?.mkdirs()
        }
        val rtpDir = File(easyRpgRoot, "rtp")

        val args = buildList {
            add("--project-path")
            add(gamePath)
            add("--save-path")
            add(saveDir.absolutePath)
            add("--config-path")
            add(configDir.absolutePath)
            add("--log-file")
            add(logFile.absolutePath)
            if (debug) {
                add("--test-play")
            }
        }.toTypedArray()

        val intent = Intent(context, EasyRpgPlayerActivity::class.java)
            .putExtra(EXTRA_GAME_ID, gameId)
            .putExtra(EXTRA_GAME_PATH, gamePath)
            .putExtra(EXTRA_CONFIG_PATH, File(configDir, "easyrpg-player.ini").absolutePath)
            .putExtra(EXTRA_MODIFIER_CONFIG_PATH, modifierConfig.absolutePath)
            .putExtra(EXTRA_DEBUG, debug)
            .putExtra(EXTRA_EASYRPG_RTP_PATH, rtpDir.absolutePath)
            .putExtra(EasyRpgPlayerActivity.TAG_PROJECT_PATH, gamePath)
            .putExtra(EasyRpgPlayerActivity.TAG_SAVE_PATH, saveDir.absolutePath)
            .putExtra(EasyRpgPlayerActivity.TAG_LOG_FILE, logFile.absolutePath)
            .putExtra(EasyRpgPlayerActivity.TAG_COMMAND_LINE, args)
            .putExtra(EasyRpgPlayerActivity.TAG_STANDALONE, true)
            .putExtra(EXTRA_VIRTUAL_GAMEPAD, virtualGamepad)
            .putExtra(EXTRA_GAMEPAD_OPACITY, virtualGamepadOpacity)
            .putExtra(EXTRA_GAMEPAD_SCALE, virtualGamepadScale)
            .putExtra(EXTRA_GAMEPAD_DIAGONAL_MOVEMENT, virtualGamepadDiagonalMovement)
            .putExtra(EXTRA_GAMEPAD_KEY_A, virtualGamepadKeyA)
            .putExtra(EXTRA_GAMEPAD_KEY_B, virtualGamepadKeyB)
            .putExtra(EXTRA_GAMEPAD_KEY_C, virtualGamepadKeyC)
            .putExtra(EXTRA_GAMEPAD_KEY_X, virtualGamepadKeyX)
            .putExtra(EXTRA_GAMEPAD_KEY_Y, virtualGamepadKeyY)
            .putExtra(EXTRA_GAMEPAD_KEY_Z, virtualGamepadKeyZ)
            .putExtra(EXTRA_GAMEPAD_KEY_L, virtualGamepadKeyL)
            .putExtra(EXTRA_GAMEPAD_KEY_R, virtualGamepadKeyR)
            .putExtra(EXTRA_GAMEPAD_KEY_CTRL, virtualGamepadKeyCtrl)
            .putExtra(EXTRA_GAMEPAD_KEY_ALT, virtualGamepadKeyAlt)
            .putExtra(EXTRA_GAMEPAD_KEY_SHIFT, virtualGamepadKeyShift)
            .putExtra(EXTRA_GAMEPAD_KEY_RUN, virtualGamepadKeyRun)
            .putExtra(EXTRA_GAMEPAD_LAYOUT, savedGamepadLayout(context))
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_MAPPING, physicalGamepadMappingEnabled)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_BACK_AS_B, physicalGamepadBackAsB)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_A, physicalGamepadKeyA)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_B, physicalGamepadKeyB)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_X, physicalGamepadKeyX)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_Y, physicalGamepadKeyY)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_L1, physicalGamepadKeyL1)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_R1, physicalGamepadKeyR1)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_L2, physicalGamepadKeyL2)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_R2, physicalGamepadKeyR2)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_START, physicalGamepadKeyStart)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_SELECT, physicalGamepadKeySelect)
            .putExtra(EXTRA_PHYSICAL_GAMEPAD_KEY_RUN, physicalGamepadKeyRun)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
    }

    fun saveGamepadLayout(context: Context, layout: String) {
        context.getSharedPreferences(GAMEPAD_LAYOUT_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(GAMEPAD_LAYOUT_KEY, layout)
            .apply()
    }

    fun savedGamepadLayout(context: Context): String =
        context.getSharedPreferences(GAMEPAD_LAYOUT_PREFS, Context.MODE_PRIVATE)
            .getString(GAMEPAD_LAYOUT_KEY, "")
            .orEmpty()

    private fun modifierConfigForMkxp(context: Context, gameId: String, configPath: String): File {
        val parent = File(configPath).parentFile
        return File(parent ?: File(context.filesDir, "modifiers/$gameId").apply { mkdirs() }, "modifier.properties")
    }

    private fun modifierConfigForGame(context: Context, gameId: String): File =
        File(context.filesDir, "modifiers/$gameId/modifier.properties").apply {
            parentFile?.mkdirs()
        }

    private fun resetGameProcessIfNeeded(context: Context) {
        val packageName = context.packageName
        val gameProcessName = "$packageName:game"
        if (Application.getProcessName() == gameProcessName) {
            return
        }
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
        manager.runningAppProcesses
            ?.filter { process -> process.uid == Process.myUid() && process.processName == gameProcessName }
            ?.forEach { process -> Process.killProcess(process.pid) }
    }

    fun hasFullMkxpRuntime(context: Context): Boolean {
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        return File(nativeDir, "libSDL2.so").isFile &&
            File(nativeDir, "libSDL2_image.so").isFile &&
            File(nativeDir, "libSDL2_ttf.so").isFile &&
            File(nativeDir, "libSDL2_sound.so").isFile &&
            File(nativeDir, "libopenal.so").isFile &&
            File(nativeDir, "libruby.so").isFile &&
            File(nativeDir, "libmkxp-z.so").isFile
    }

    fun hasEasyRpgRuntime(context: Context): Boolean {
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        return File(nativeDir, "libSDL2.so").isFile &&
            File(nativeDir, "libeasyrpg_android.so").isFile
    }

    private const val GAMEPAD_LAYOUT_PREFS = "mkxp_gamepad_layout"
    private const val GAMEPAD_LAYOUT_KEY = "layout"
}
