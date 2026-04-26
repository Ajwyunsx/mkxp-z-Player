package io.github.mkxpz.rpgplayer.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.launcherDataStore by preferencesDataStore(name = "launcher_settings")

class SettingsRepository(private val context: Context) {
    val settings: Flow<LauncherSettings> = context.launcherDataStore.data.map { prefs ->
        LauncherSettings(
            themeMode = prefs[Keys.themeMode]?.let(ThemeModeSetting::valueOf) ?: ThemeModeSetting.SYSTEM,
            colorSource = prefs[Keys.colorSource]?.let(ThemeColorSource::valueOf) ?: ThemeColorSource.DYNAMIC_SYSTEM,
            manualSeedColor = prefs[Keys.manualSeedColor] ?: 0xFF2F5C8A,
            preferredImportMode = prefs[Keys.preferredImportMode]?.let(PreferredImportMode::valueOf)
                ?: PreferredImportMode.COPY_TO_LIBRARY,
            virtualGamepadEnabled = prefs[Keys.virtualGamepadEnabled] ?: true,
            virtualGamepadOpacity = prefs[Keys.virtualGamepadOpacity] ?: 30,
            virtualGamepadScale = prefs[Keys.virtualGamepadScale] ?: 100,
            virtualGamepadDiagonalMovement = prefs[Keys.virtualGamepadDiagonalMovement] ?: false,
            virtualGamepadKeyA = prefs[Keys.virtualGamepadKeyA] ?: android.view.KeyEvent.KEYCODE_Z,
            virtualGamepadKeyB = prefs[Keys.virtualGamepadKeyB] ?: android.view.KeyEvent.KEYCODE_X,
            virtualGamepadKeyC = prefs[Keys.virtualGamepadKeyC] ?: android.view.KeyEvent.KEYCODE_C,
            virtualGamepadKeyX = prefs[Keys.virtualGamepadKeyX] ?: android.view.KeyEvent.KEYCODE_A,
            virtualGamepadKeyY = prefs[Keys.virtualGamepadKeyY] ?: android.view.KeyEvent.KEYCODE_S,
            virtualGamepadKeyZ = prefs[Keys.virtualGamepadKeyZ] ?: android.view.KeyEvent.KEYCODE_D,
            virtualGamepadKeyL = prefs[Keys.virtualGamepadKeyL] ?: android.view.KeyEvent.KEYCODE_Q,
            virtualGamepadKeyR = prefs[Keys.virtualGamepadKeyR] ?: android.view.KeyEvent.KEYCODE_W,
            virtualGamepadKeyCtrl = prefs[Keys.virtualGamepadKeyCtrl] ?: android.view.KeyEvent.KEYCODE_CTRL_LEFT,
            virtualGamepadKeyAlt = prefs[Keys.virtualGamepadKeyAlt] ?: android.view.KeyEvent.KEYCODE_ALT_LEFT,
            virtualGamepadKeyShift = prefs[Keys.virtualGamepadKeyShift] ?: android.view.KeyEvent.KEYCODE_SHIFT_LEFT,
            fixedFramerate = prefs[Keys.fixedFramerate] ?: 0,
            smoothScaling = prefs[Keys.smoothScaling] ?: false,
            keepAspectRatio = prefs[Keys.keepAspectRatio] ?: true,
            soundFontPath = prefs[Keys.soundFontPath].orEmpty(),
            debugLaunch = prefs[Keys.debugLaunch] ?: false,
        )
    }

    suspend fun setThemeMode(value: ThemeModeSetting) = edit { it[Keys.themeMode] = value.name }
    suspend fun setColorSource(value: ThemeColorSource) = edit { it[Keys.colorSource] = value.name }
    suspend fun setManualSeedColor(value: Long) = edit { it[Keys.manualSeedColor] = value }
    suspend fun setPreferredImportMode(value: PreferredImportMode) = edit { it[Keys.preferredImportMode] = value.name }
    suspend fun setVirtualGamepadEnabled(value: Boolean) = edit { it[Keys.virtualGamepadEnabled] = value }
    suspend fun setVirtualGamepadOpacity(value: Int) = edit { it[Keys.virtualGamepadOpacity] = value.coerceIn(5, 100) }
    suspend fun setVirtualGamepadScale(value: Int) = edit { it[Keys.virtualGamepadScale] = value.coerceIn(60, 160) }
    suspend fun setVirtualGamepadDiagonalMovement(value: Boolean) = edit { it[Keys.virtualGamepadDiagonalMovement] = value }
    suspend fun setVirtualGamepadKey(button: VirtualGamepadButton, keyCode: Int) = edit {
        it[Keys.virtualGamepadKey(button)] = keyCode
    }
    suspend fun setFixedFramerate(value: Int) = edit { it[Keys.fixedFramerate] = value.coerceIn(0, 240) }
    suspend fun setSmoothScaling(value: Boolean) = edit { it[Keys.smoothScaling] = value }
    suspend fun setKeepAspectRatio(value: Boolean) = edit { it[Keys.keepAspectRatio] = value }
    suspend fun setSoundFontPath(value: String) = edit { it[Keys.soundFontPath] = value.trim() }
    suspend fun setDebugLaunch(value: Boolean) = edit { it[Keys.debugLaunch] = value }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.launcherDataStore.edit(block)
    }

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val colorSource = stringPreferencesKey("color_source")
        val manualSeedColor = longPreferencesKey("manual_seed_color")
        val preferredImportMode = stringPreferencesKey("preferred_import_mode")
        val virtualGamepadEnabled = booleanPreferencesKey("virtual_gamepad_enabled")
        val virtualGamepadOpacity = intPreferencesKey("virtual_gamepad_opacity")
        val virtualGamepadScale = intPreferencesKey("virtual_gamepad_scale")
        val virtualGamepadDiagonalMovement = booleanPreferencesKey("virtual_gamepad_diagonal_movement")
        val virtualGamepadKeyA = intPreferencesKey("virtual_gamepad_key_a")
        val virtualGamepadKeyB = intPreferencesKey("virtual_gamepad_key_b")
        val virtualGamepadKeyC = intPreferencesKey("virtual_gamepad_key_c")
        val virtualGamepadKeyX = intPreferencesKey("virtual_gamepad_key_x")
        val virtualGamepadKeyY = intPreferencesKey("virtual_gamepad_key_y")
        val virtualGamepadKeyZ = intPreferencesKey("virtual_gamepad_key_z")
        val virtualGamepadKeyL = intPreferencesKey("virtual_gamepad_key_l")
        val virtualGamepadKeyR = intPreferencesKey("virtual_gamepad_key_r")
        val virtualGamepadKeyCtrl = intPreferencesKey("virtual_gamepad_key_ctrl")
        val virtualGamepadKeyAlt = intPreferencesKey("virtual_gamepad_key_alt")
        val virtualGamepadKeyShift = intPreferencesKey("virtual_gamepad_key_shift")
        val fixedFramerate = intPreferencesKey("fixed_framerate")
        val smoothScaling = booleanPreferencesKey("smooth_scaling")
        val keepAspectRatio = booleanPreferencesKey("keep_aspect_ratio")
        val soundFontPath = stringPreferencesKey("sound_font_path")
        val debugLaunch = booleanPreferencesKey("debug_launch")

        fun virtualGamepadKey(button: VirtualGamepadButton) = when (button) {
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
    }
}
