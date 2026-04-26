package io.github.mkxpz.engine

import android.content.Context
import android.content.Intent
import android.view.KeyEvent
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
    const val EXTRA_GAMEPAD_LAYOUT = "io.github.mkxpz.engine.GAMEPAD_LAYOUT"

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
    ) {
        val target = if (hasFullMkxpRuntime(context)) SdlGameActivity::class.java else GameActivity::class.java
        val intent = Intent(context, target)
            .putExtra(EXTRA_GAME_ID, gameId)
            .putExtra(EXTRA_GAME_PATH, gamePath)
            .putExtra(EXTRA_CONFIG_PATH, configPath)
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
            .putExtra(EXTRA_GAMEPAD_LAYOUT, savedGamepadLayout(context))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
    ) {
        val intent = Intent(context, WebGameActivity::class.java)
            .putExtra(EXTRA_GAME_ID, gameId)
            .putExtra(EXTRA_GAME_PATH, gamePath)
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
            .putExtra(EXTRA_GAMEPAD_LAYOUT, savedGamepadLayout(context))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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

    private const val GAMEPAD_LAYOUT_PREFS = "mkxp_gamepad_layout"
    private const val GAMEPAD_LAYOUT_KEY = "layout"
}
