package com.tecsup.tcweek.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Paleta VYNK propia: la app siempre usa los colores de la marca, sin tonos dinámicos del teléfono.
private val LightColorScheme = lightColorScheme(
    primary = VynkBlue,
    onPrimary = Color.White,
    primaryContainer = VynkBlueSoft,
    onPrimaryContainer = VynkNavy,
    secondary = VynkNavy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3EAF5),
    onSecondaryContainer = VynkNavy,
    tertiary = VynkSky,
    onTertiary = Color.White,
    background = VynkBackground,
    onBackground = VynkText,
    surface = VynkSurface,
    onSurface = VynkText,
    surfaceVariant = VynkSurface2,
    onSurfaceVariant = VynkMuted,
    surfaceContainer = VynkSurface,
    surfaceContainerLow = VynkSurface,
    surfaceContainerHigh = VynkSurface2,
    outline = VynkLineStrong,
    outlineVariant = VynkLine,
    error = VynkError,
    errorContainer = VynkErrorSoft
)

private val VynkShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun TCWeekTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = VynkShapes,
        content = content
    )
}
