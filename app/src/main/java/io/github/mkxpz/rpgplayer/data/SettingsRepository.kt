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
            virtualGamepadKeyRun = prefs[Keys.virtualGamepadKeyRun] ?: android.view.KeyEvent.KEYCODE_SHIFT_LEFT,
            physicalGamepadMappingEnabled = prefs[Keys.physicalGamepadMappingEnabled] ?: true,
            physicalGamepadBackAsB = prefs[Keys.physicalGamepadBackAsB] ?: true,
            physicalGamepadKeyA = prefs[Keys.physicalGamepadKeyA] ?: android.view.KeyEvent.KEYCODE_Z,
            physicalGamepadKeyB = prefs[Keys.physicalGamepadKeyB] ?: android.view.KeyEvent.KEYCODE_X,
            physicalGamepadKeyX = prefs[Keys.physicalGamepadKeyX] ?: android.view.KeyEvent.KEYCODE_A,
            physicalGamepadKeyY = prefs[Keys.physicalGamepadKeyY] ?: android.view.KeyEvent.KEYCODE_S,
            physicalGamepadKeyL1 = prefs[Keys.physicalGamepadKeyL1] ?: android.view.KeyEvent.KEYCODE_Q,
            physicalGamepadKeyR1 = prefs[Keys.physicalGamepadKeyR1] ?: android.view.KeyEvent.KEYCODE_W,
            physicalGamepadKeyL2 = prefs[Keys.physicalGamepadKeyL2] ?: android.view.KeyEvent.KEYCODE_PAGE_UP,
            physicalGamepadKeyR2 = prefs[Keys.physicalGamepadKeyR2] ?: android.view.KeyEvent.KEYCODE_PAGE_DOWN,
            physicalGamepadKeyStart = prefs[Keys.physicalGamepadKeyStart] ?: android.view.KeyEvent.KEYCODE_ENTER,
            physicalGamepadKeySelect = prefs[Keys.physicalGamepadKeySelect] ?: android.view.KeyEvent.KEYCODE_ESCAPE,
            physicalGamepadKeyRun = prefs[Keys.physicalGamepadKeyRun] ?: android.view.KeyEvent.KEYCODE_SHIFT_LEFT,
            fixedFramerate = prefs[Keys.fixedFramerate] ?: 0,
            smoothScaling = prefs[Keys.smoothScaling] ?: false,
            keepAspectRatio = prefs[Keys.keepAspectRatio] ?: true,
            soundFontPath = prefs[Keys.soundFontPath].orEmpty(),
            vxRtpPath = prefs[Keys.vxRtpPath].orEmpty(),
            vxAceRtpPath = prefs[Keys.vxAceRtpPath].orEmpty(),
            rubyClassicCompatibilityEnabled = prefs[Keys.rubyClassicCompatibilityEnabled] ?: true,
            winApiCompatibilityEnabled = prefs[Keys.winApiCompatibilityEnabled] ?: true,
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
    suspend fun setPhysicalGamepadMappingEnabled(value: Boolean) = edit { it[Keys.physicalGamepadMappingEnabled] = value }
    suspend fun setPhysicalGamepadBackAsB(value: Boolean) = edit { it[Keys.physicalGamepadBackAsB] = value }
    suspend fun setPhysicalGamepadKey(button: PhysicalGamepadButton, keyCode: Int) = edit {
        it[Keys.physicalGamepadKey(button)] = keyCode
    }
    suspend fun setFixedFramerate(value: Int) = edit { it[Keys.fixedFramerate] = value.coerceIn(0, 240) }
    suspend fun setSmoothScaling(value: Boolean) = edit { it[Keys.smoothScaling] = value }
    suspend fun setKeepAspectRatio(value: Boolean) = edit { it[Keys.keepAspectRatio] = value }
    suspend fun setSoundFontPath(value: String) = edit { it[Keys.soundFontPath] = value.trim() }
    suspend fun setVxRtpPath(value: String) = edit { it[Keys.vxRtpPath] = value.trim() }
    suspend fun setVxAceRtpPath(value: String) = edit { it[Keys.vxAceRtpPath] = value.trim() }
    suspend fun setRubyClassicCompatibilityEnabled(value: Boolean) = edit { it[Keys.rubyClassicCompatibilityEnabled] = value }
    suspend fun setWinApiCompatibilityEnabled(value: Boolean) = edit { it[Keys.winApiCompatibilityEnabled] = value }
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
        val virtualGamepadKeyRun = intPreferencesKey("virtual_gamepad_key_run")
        val physicalGamepadMappingEnabled = booleanPreferencesKey("physical_gamepad_mapping_enabled")
        val physicalGamepadBackAsB = booleanPreferencesKey("physical_gamepad_back_as_b")
        val physicalGamepadKeyA = intPreferencesKey("physical_gamepad_key_a")
        val physicalGamepadKeyB = intPreferencesKey("physical_gamepad_key_b")
        val physicalGamepadKeyX = intPreferencesKey("physical_gamepad_key_x")
        val physicalGamepadKeyY = intPreferencesKey("physical_gamepad_key_y")
        val physicalGamepadKeyL1 = intPreferencesKey("physical_gamepad_key_l1")
        val physicalGamepadKeyR1 = intPreferencesKey("physical_gamepad_key_r1")
        val physicalGamepadKeyL2 = intPreferencesKey("physical_gamepad_key_l2")
        val physicalGamepadKeyR2 = intPreferencesKey("physical_gamepad_key_r2")
        val physicalGamepadKeyStart = intPreferencesKey("physical_gamepad_key_start")
        val physicalGamepadKeySelect = intPreferencesKey("physical_gamepad_key_select")
        val physicalGamepadKeyRun = intPreferencesKey("physical_gamepad_key_run")
        val fixedFramerate = intPreferencesKey("fixed_framerate")
        val smoothScaling = booleanPreferencesKey("smooth_scaling")
        val keepAspectRatio = booleanPreferencesKey("keep_aspect_ratio")
        val soundFontPath = stringPreferencesKey("sound_font_path")
        val vxRtpPath = stringPreferencesKey("vx_rtp_path")
        val vxAceRtpPath = stringPreferencesKey("vx_ace_rtp_path")
        val rubyClassicCompatibilityEnabled = booleanPreferencesKey("ruby_classic_compatibility_enabled")
        val winApiCompatibilityEnabled = booleanPreferencesKey("winapi_compatibility_enabled")
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
            VirtualGamepadButton.RUN -> virtualGamepadKeyRun
        }

        fun physicalGamepadKey(button: PhysicalGamepadButton) = when (button) {
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
    }
}
