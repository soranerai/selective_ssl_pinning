package dev.soranerai.netprivacy.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Dark = darkColorScheme(primary = Color(0xFF43C978), secondary = Color(0xFF4AA8E8), tertiary = Color(0xFF28BCC8), background = Color.Black, surface = Color.Black)
private val Light = lightColorScheme(primary = Color(0xFF168A4A), secondary = Color(0xFF256FA6), tertiary = Color(0xFF087F8C), background = Color(0xFFF4F7F9))

@Composable fun NetPrivacyTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) Dark else Light
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect { (view.context as Activity).window.apply { statusBarColor = colors.background.toArgb(); navigationBarColor = colors.surface.toArgb() }; WindowCompat.getInsetsController((view.context as Activity).window, view).apply { isAppearanceLightStatusBars = !darkTheme; isAppearanceLightNavigationBars = !darkTheme } }
    MaterialTheme(colorScheme = colors, content = content)
}
