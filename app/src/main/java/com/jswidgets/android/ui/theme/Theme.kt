package com.jswidgets.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
    primary = Color(0xFF81D4FA),
    primaryVariant = Color(0xFF0288D1),
    secondary = Color(0xFF03DAC6)
)

private val LightColorPalette = lightColors(
    primary = Color(0xFF03A9F4),
    primaryVariant = Color(0xFF0288D1),
    secondary = Color(0xFF03DAC6)
)

@Composable
fun JSWidgetsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        content = content
    )
} 