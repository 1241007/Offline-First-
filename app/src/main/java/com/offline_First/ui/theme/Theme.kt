package com.offline_First.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EduNovaPrimaryDark,
    onPrimary = Color(0xFF382343),
    primaryContainer = EduNovaPrimaryContainerDark,
    onPrimaryContainer = Color(0xFFF1DDF8),
    secondary = EduNovaSecondaryDark,
    onSecondary = Color(0xFF392C43),
    secondaryContainer = EduNovaSecondaryContainerDark,
    onSecondaryContainer = Color(0xFFEEDFF3),
    tertiary = EduNovaAccentDark,
    onTertiary = Color(0xFF163727),
    tertiaryContainer = Color(0xFF315342),
    onTertiaryContainer = Color(0xFFC4EBD2),
    background = EduNovaBackgroundDark,
    onBackground = EduNovaTextPrimaryDark,
    surface = EduNovaSurfaceDark,
    onSurface = EduNovaTextPrimaryDark,
    surfaceVariant = EduNovaSurfaceVariantDark,
    onSurfaceVariant = EduNovaTextSecondaryDark,
    outline = EduNovaBorderDark,
    error = EduNovaErrorDark,
    onError = Color(0xFF690005)
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
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCEFE3),
    onTertiaryContainer = Color(0xFF173A29),
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