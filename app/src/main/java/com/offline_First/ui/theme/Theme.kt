package com.offline_First.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EduNovaPrimaryDark,
    onPrimary = Color(0xFF123638),
    primaryContainer = EduNovaPrimaryContainerDark,
    onPrimaryContainer = Color(0xFFDDF2EE),
    secondary = EduNovaSecondaryDark,
    onSecondary = Color(0xFF163638),
    secondaryContainer = EduNovaSecondaryContainerDark,
    onSecondaryContainer = Color(0xFFDDF2EE),
    tertiary = EduNovaAccentDark,
    onTertiary = Color(0xFF183A38),
    tertiaryContainer = Color(0xFF275B58),
    onTertiaryContainer = Color(0xFFDDF2EE),
    background = EduNovaBackgroundDark,
    onBackground = EduNovaTextPrimaryDark,
    surface = EduNovaSurfaceDark,
    onSurface = EduNovaTextPrimaryDark,
    surfaceVariant = EduNovaSurfaceVariantDark,
    onSurfaceVariant = EduNovaTextSecondaryDark,
    outline = EduNovaBorderDark,
    error = EduNovaErrorDark,
    onError = Color(0xFF4A1718)
)

private val LightColorScheme = lightColorScheme(
    primary = EduNovaPrimary,
    onPrimary = Color.White,
    primaryContainer = EduNovaPrimaryContainer,
    onPrimaryContainer = EduNovaTextPrimary,
    secondary = EduNovaSecondary,
    onSecondary = Color.White,
    secondaryContainer = EduNovaSecondaryContainer,
    onSecondaryContainer = EduNovaTextPrimary,
    tertiary = EduNovaAccent,
    onTertiary = EduNovaTextPrimary,
    tertiaryContainer = EduNovaPrimaryContainer,
    onTertiaryContainer = EduNovaTextPrimary,
    background = EduNovaBackground,
    onBackground = EduNovaTextPrimary,
    surface = EduNovaSurface,
    onSurface = EduNovaTextPrimary,
    surfaceVariant = EduNovaSurfaceVariant,
    onSurfaceVariant = EduNovaTextSecondary,
    outline = EduNovaBorder,
    error = EduNovaError,
    onError = Color.White
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