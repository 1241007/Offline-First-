package com.offline_First.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = Cyan80,
    tertiary = Navy80
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = Cyan40,
    tertiary = Navy40
)

@Composable
fun OfflineFirstTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}