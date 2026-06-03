package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = NeonBlue,
    tertiary = GoldColor,
    background = DarkBg,
    surface = DarkCard,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFF1F5F9)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for Solo Leveling
    dynamicColor: Boolean = false, // Use our handcrafted game colors exclusively
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
