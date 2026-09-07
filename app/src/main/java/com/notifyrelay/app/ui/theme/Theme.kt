package com.notifyrelay.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF0F6B5C)
private val GreenDark = Color(0xFF0B3D36)
private val Surface = Color(0xFFF4F7F6)
private val Card = Color(0xFFFFFFFF)
private val OnSurface = Color(0xFF1B2B28)

private val Colors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5EDE8),
    onPrimaryContainer = GreenDark,
    secondary = Color(0xFF3F5F58),
    background = Surface,
    surface = Card,
    onBackground = OnSurface,
    onSurface = OnSurface,
    error = Color(0xFFB3261E),
)

@Composable
fun NotifyRelayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        content = content,
    )
}
