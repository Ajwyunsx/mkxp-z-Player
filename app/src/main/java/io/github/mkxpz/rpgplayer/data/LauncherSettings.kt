package io.github.mkxpz.rpgplayer.data

import android.view.KeyEvent

data class LauncherSettings(
    val themeMode: ThemeModeSetting = ThemeModeSetting.SYSTEM,
    val colorSource: ThemeColorSource = ThemeColorSource.DYNAMIC_SYSTEM,
    val manualSeedColor: Long = 0xFF2F5C8A,
    val preferredImportMode: PreferredImportMode = PreferredImportMode.COPY_TO_LIBRARY,
    val virtualGamepadEnabled: Boolean = true,
    val virtualGamepadOpacity: Int = 30,
    val virtualGamepadScale: Int = 100,
    val virtualGamepadDiagonalMovement: Boolean = false,
    val virtualGamepadKeyA: Int = KeyEvent.KEYCODE_Z,
    val virtualGamepadKeyB: Int = KeyEvent.KEYCODE_X,
    val virtualGamepadKeyC: Int = KeyEvent.KEYCODE_C,
    val virtualGamepadKeyX: Int = KeyEvent.KEYCODE_A,
    val virtualGamepadKeyY: Int = KeyEvent.KEYCODE_S,
    val virtualGamepadKeyZ: Int = KeyEvent.KEYCODE_D,
    val virtualGamepadKeyL: Int = KeyEvent.KEYCODE_Q,
    val virtualGamepadKeyR: Int = KeyEvent.KEYCODE_W,
    val virtualGamepadKeyCtrl: Int = KeyEvent.KEYCODE_CTRL_LEFT,
    val virtualGamepadKeyAlt: Int = KeyEvent.KEYCODE_ALT_LEFT,
    val virtualGamepadKeyShift: Int = KeyEvent.KEYCODE_SHIFT_LEFT,
    val virtualGamepadKeyRun: Int = KeyEvent.KEYCODE_SHIFT_LEFT,
    val physicalGamepadMappingEnabled: Boolean = true,
    val physicalGamepadBackAsB: Boolean = true,
    val physicalGamepadKeyA: Int = KeyEvent.KEYCODE_Z,
    val physicalGamepadKeyB: Int = KeyEvent.KEYCODE_X,
    val physicalGamepadKeyX: Int = KeyEvent.KEYCODE_A,
    val physicalGamepadKeyY: Int = KeyEvent.KEYCODE_S,
    val physicalGamepadKeyL1: Int = KeyEvent.KEYCODE_Q,
    val physicalGamepadKeyR1: Int = KeyEvent.KEYCODE_W,
    val physicalGamepadKeyL2: Int = KeyEvent.KEYCODE_PAGE_UP,
    val physicalGamepadKeyR2: Int = KeyEvent.KEYCODE_PAGE_DOWN,
    val physicalGamepadKeyStart: Int = KeyEvent.KEYCODE_ENTER,
    val physicalGamepadKeySelect: Int = KeyEvent.KEYCODE_ESCAPE,
    val physicalGamepadKeyRun: Int = KeyEvent.KEYCODE_SHIFT_LEFT,
    val fixedFramerate: Int = 0,
    val smoothScaling: Boolean = false,
    val keepAspectRatio: Boolean = true,
    val soundFontPath: String = "",
    val vxRtpPath: String = "",
    val vxAceRtpPath: String = "",
    val rubyClassicCompatibilityEnabled: Boolean = true,
    val winApiCompatibilityEnabled: Boolean = true,
    val debugLaunch: Boolean = false,
)

enum class ThemeModeSetting {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class ThemeColorSource {
    DYNAMIC_SYSTEM,
    MANUAL,
}

enum class PreferredImportMode {
    COPY_TO_LIBRARY,
    DIRECT_EXTERNAL_PATH,
}

enum class VirtualGamepadButton {
    A,
    B,
    C,
    X,
    Y,
    Z,
    L,
    R,
    CTRL,
    ALT,
    SHIFT,
    RUN,
}

enum class PhysicalGamepadButton {
    A,
    B,
    X,
    Y,
    L1,
    R1,
    L2,
    R2,
    START,
    SELECT,
    RUN,
}
