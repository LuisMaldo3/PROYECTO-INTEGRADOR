package com.tecsup.tcweek.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta VYNK propia: evita tonos dinámicos del dispositivo ajenos a la marca.
private val LightColorScheme = lightColorScheme(
    primary = VynkBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBFF),
    onPrimaryContainer = VynkNavy,
    secondary = VynkTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9F5F0),
    onSecondaryContainer = Color(0xFF045B58),
    tertiary = VynkSky,
    background = VynkBackground,
    onBackground = VynkText,
    surface = VynkSurface,
    onSurface = VynkText,
    surfaceVariant = Color(0xFFEAF0F8),
    onSurfaceVariant = VynkMuted,
    outline = Color(0xFFCAD7E8),
    error = Color(0xFFB42332)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF80B9FF),
    onPrimary = VynkNavy,
    secondary = Color(0xFF67D2C9),
    background = Color(0xFF0D182B),
    onBackground = Color(0xFFF3F6FC),
    surface = Color(0xFF17263F),
    onSurface = Color(0xFFF3F6FC)
)

@Composable
fun TCWeekTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
