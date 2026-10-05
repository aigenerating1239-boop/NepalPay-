package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = DarkCrimsonPrimary,
    onPrimary = Color.White,
    primaryContainer = NepalCrimsonDark,
    onPrimaryContainer = NepalCrimsonLight,
    secondary = DarkNavySecondary,
    onSecondary = Color(0xFF041E49),
    secondaryContainer = HimalayanNavyDark,
    onSecondaryContainer = HimalayanNavyLight,
    tertiary = MustangGold,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF452B00),
    onTertiaryContainer = MustangGoldLight,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

private val LightColorScheme = lightColorScheme(
    primary = NepalCrimson,
    onPrimary = Color.White,
    primaryContainer = NepalCrimsonLight,
    onPrimaryContainer = Color(0xFF40000A),
    secondary = HimalayanNavy,
    onSecondary = Color.White,
    secondaryContainer = HimalayanNavyLight,
    onSecondaryContainer = Color(0xFF001945),
    tertiary = MustangGold,
    onTertiary = Color.White,
    tertiaryContainer = MustangGoldLight,
    onTertiaryContainer = Color(0xFF3E1C00),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = RubyError,
    onError = Color.White
)

val NepalPayShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = NepalPayShapes,
        content = content
    )
}
