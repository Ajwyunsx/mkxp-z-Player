package io.github.mkxpz.rpgplayer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.github.mkxpz.rpgplayer.data.LauncherSettings
import io.github.mkxpz.rpgplayer.data.ThemeColorSource
import io.github.mkxpz.rpgplayer.data.ThemeModeSetting

@Composable
fun MkxpPlayerTheme(
    settings: LauncherSettings,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (settings.themeMode) {
        ThemeModeSetting.SYSTEM -> isSystemInDarkTheme()
        ThemeModeSetting.LIGHT -> false
        ThemeModeSetting.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme = when {
        settings.colorSource == ThemeColorSource.DYNAMIC_SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> manualColorScheme(settings.manualSeedColor, darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content,
    )
}

fun supportsDynamicColor(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
    sdkInt >= Build.VERSION_CODES.S

private fun manualColorScheme(seedArgb: Long, darkTheme: Boolean): ColorScheme {
    val primary = Color(seedArgb)
    return if (darkTheme) {
        darkColorScheme(
            primary = primary.lighten(0.28f),
            onPrimary = Color(0xFF102027),
            secondary = Color(0xFF80CBC4),
            tertiary = Color(0xFFE0B568),
            background = Color(0xFF101416),
            surface = Color(0xFF151A1D),
            surfaceVariant = Color(0xFF273136),
            onBackground = Color(0xFFE7ECEF),
            onSurface = Color(0xFFE7ECEF),
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            secondary = Color(0xFF3D6F6A),
            tertiary = Color(0xFF855D16),
            background = Color(0xFFFBFCFD),
            surface = Color.White,
            surfaceVariant = Color(0xFFE7EEF2),
            onBackground = Color(0xFF151A1D),
            onSurface = Color(0xFF151A1D),
        )
    }
}

private fun Color.lighten(amount: Float): Color {
    return Color(
        red = red + (1f - red) * amount,
        green = green + (1f - green) * amount,
        blue = blue + (1f - blue) * amount,
        alpha = alpha,
    )
}
