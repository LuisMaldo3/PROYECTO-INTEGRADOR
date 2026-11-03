package com.tecsup.tcweek.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tecsup.tcweek.R

/** Archivo para títulos y Public Sans para el texto, igual que en la web. */
val Archivo = FontFamily(
    Font(R.font.archivo_semibold, FontWeight.SemiBold),
    Font(R.font.archivo_bold, FontWeight.Bold),
    Font(R.font.archivo_extrabold, FontWeight.ExtraBold),
    Font(R.font.archivo_extrabold, FontWeight.Black)
)

val PublicSans = FontFamily(
    Font(R.font.public_sans_regular, FontWeight.Normal),
    Font(R.font.public_sans_medium, FontWeight.Medium),
    Font(R.font.public_sans_semibold, FontWeight.SemiBold),
    Font(R.font.public_sans_bold, FontWeight.Bold),
    Font(R.font.public_sans_bold, FontWeight.ExtraBold)
)

private fun titulo(size: Int, line: Int, weight: FontWeight = FontWeight.Bold) =
    TextStyle(fontFamily = Archivo, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = 0.sp)

private fun texto(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = PublicSans, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = 0.sp)

val Typography = Typography(
    displaySmall = titulo(32, 38, FontWeight.ExtraBold),
    headlineLarge = titulo(28, 34, FontWeight.ExtraBold),
    headlineMedium = titulo(25, 31),
    headlineSmall = titulo(22, 28),
    titleLarge = titulo(19, 25),
    titleMedium = titulo(16, 22),
    titleSmall = titulo(14, 20, FontWeight.SemiBold),
    bodyLarge = texto(16, 24),
    bodyMedium = texto(14, 21),
    bodySmall = texto(13, 19),
    labelLarge = texto(14, 20, FontWeight.SemiBold),
    labelMedium = texto(12, 16, FontWeight.SemiBold),
    labelSmall = texto(11, 15, FontWeight.SemiBold)
)
