package com.assessment.minilogbook.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AppGreenLight,
    onPrimary = Color.Black,
    secondary = AppGreen,
    background = DarkBackground,
    surface = DarkSurface,
    // TopAppBar background in dark mode: keep it dark surface, not green
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    error = GlucoseRedDark
)

private val LightColorScheme = lightColorScheme(
    primary = AppGreen,
    onPrimary = AppWhite,          // White letters on green TopAppBar
    secondary = AppDarkGreen,
    background = AppWhite,
    surface = AppWhite,
    // TopAppBar in light mode: green background with white letters (handled via onPrimary)
    surfaceVariant = Color(0xFFF1F1F1),
    onSurfaceVariant = Color(0xFF3A3A3A),
    onBackground = Color.Black,
    onSurface = Color.Black,
    error = GlucoseRed
)

@Composable
fun MiniLogbookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Set to false to keep our specific brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}