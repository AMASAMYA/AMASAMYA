package com.example.amasamya.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = VibrantCyan,
    secondary = LightGrey,
    tertiary = NeonGreen,
    background = DeepSpace,
    surface = GlassySurface,
    error = NeonRed,
    onPrimary = PureWhite,
    onSecondary = DeepSpace,
    onBackground = PureWhite,
    onSurface = PureWhite,
    onError = PureWhite
)

@Composable
fun AMASAMYATheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
