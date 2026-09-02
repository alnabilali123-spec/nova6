package com.novatube.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.novatube.app.data.prefs.ThemeMode

private val DarkColors = darkColorScheme(
    primary = BrandRed,
    onPrimary = Neutral50,
    primaryContainer = BrandRedDark,
    onPrimaryContainer = Neutral50,
    secondary = BrandTeal,
    onSecondary = Neutral900,
    tertiary = BrandPurple,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Neutral800,
    onSurfaceVariant = Neutral200,
    outline = Neutral700,
    error = Error,
    onError = Neutral50
)

private val LightColors = lightColorScheme(
    primary = BrandRed,
    onPrimary = Neutral50,
    primaryContainer = BrandRedLight,
    onPrimaryContainer = Neutral900,
    secondary = BrandTeal,
    onSecondary = Neutral900,
    tertiary = BrandPurple,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Neutral100,
    onSurfaceVariant = OnSurfaceLight,
    outline = Neutral200,
    error = Error,
    onError = Neutral50
)

@Composable
fun NovaTubeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (dark) DarkColors else LightColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
