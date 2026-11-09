package com.pushpyns.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.pushpyns.domain.model.ThemeMode

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF0061A4),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD1E4FF),
        onPrimaryContainer = Color(0xFF001D36),
        secondary = Color(0xFF00687A),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFB4EBFF),
        onSecondaryContainer = Color(0xFF001F27),
        tertiary = Color(0xFF4559A9),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFDCE1FF),
        onTertiaryContainer = Color(0xFF001551),
        background = Color(0xFFF6F8FC),
        onBackground = Color(0xFF191C20),
        surface = Color(0xFFF6F8FC),
        onSurface = Color(0xFF191C20),
        surfaceVariant = Color(0xFFDEE3EB),
        onSurfaceVariant = Color(0xFF42474E),
        outline = Color(0xFF72787F),
        error = Color(0xFFBA1A1A),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF9ECAFF),
        onPrimary = Color(0xFF003258),
        primaryContainer = Color(0xFF00497D),
        onPrimaryContainer = Color(0xFFD1E4FF),
        secondary = Color(0xFF5CD6F5),
        onSecondary = Color(0xFF003640),
        secondaryContainer = Color(0xFF004E5C),
        onSecondaryContainer = Color(0xFFB4EBFF),
        tertiary = Color(0xFFB7C4FF),
        onTertiary = Color(0xFF0E2878),
        tertiaryContainer = Color(0xFF2C4090),
        onTertiaryContainer = Color(0xFFDCE1FF),
        background = Color(0xFF101418),
        onBackground = Color(0xFFE1E2E8),
        surface = Color(0xFF101418),
        onSurface = Color(0xFFE1E2E8),
        surfaceVariant = Color(0xFF42474E),
        onSurfaceVariant = Color(0xFFC2C7CE),
        outline = Color(0xFF8C9198),
        error = Color(0xFFFFB4AB),
    )

@Composable
fun PushpynsTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, dynamicColor: Boolean = false, content: @Composable () -> Unit) {
    val dark =
        when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    val dynamicAvailable = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colors =
        when {
            dynamicAvailable && dark -> dynamicDarkColorScheme(LocalContext.current)
            dynamicAvailable && !dark -> dynamicLightColorScheme(LocalContext.current)
            dark -> DarkColors
            else -> LightColors
        }
    MaterialTheme(colorScheme = colors, content = content)
}
