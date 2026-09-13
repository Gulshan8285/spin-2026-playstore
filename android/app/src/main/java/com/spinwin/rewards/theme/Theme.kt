package com.spinwin.rewards.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    secondary = ElectricCyan,
    tertiary = Gold,
    background = BgPrimary,
    surface = BgSecondary,
    surfaceVariant = SurfaceElevated,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onTertiary = BgPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = CoralRed
)

private val LightColorScheme = lightColorScheme(
    primary = NeonPurple,
    secondary = ElectricCyan,
    tertiary = Gold,
    background = LightBgPrimary,
    surface = LightBgSurface,
    surfaceVariant = SurfaceElevated,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onTertiary = BgPrimary,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    error = CoralRed
)

@Composable
fun SpinWinRewardsTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = BgPrimary.toArgb()
                window.navigationBarColor = BgPrimary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalThemeIsDark provides true
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = SpinWinTypography,
            content = content
        )
    }
}
