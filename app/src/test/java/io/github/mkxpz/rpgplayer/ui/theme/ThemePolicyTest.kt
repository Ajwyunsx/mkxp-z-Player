package io.github.mkxpz.rpgplayer.ui.theme

import android.os.Build
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class ThemePolicyTest {
    @Test
    fun dynamicColorRequiresAndroid12OrNewer() {
        assertFalse(supportsDynamicColor(Build.VERSION_CODES.R))
        assertTrue(supportsDynamicColor(Build.VERSION_CODES.S))
    }
}
