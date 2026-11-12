package com.tecsup.tcweek.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta VYNK: la misma que la web de estudiantes y el panel de administración.
val VynkNavy = Color(0xFF0B2A5B)       // azul marino Tecsup
val VynkNavy2 = Color(0xFF123A78)
val VynkBlue = Color(0xFF1557D0)       // acciones
val VynkBlueSoft = Color(0xFFE8F0FD)
val VynkSky = Color(0xFF22B5E8)        // la K del logo
val VynkTeal = Color(0xFF0F9A76)
val VynkBackground = Color(0xFFF2F5F9)
val VynkSurface = Color.White
val VynkSurface2 = Color(0xFFF7F9FC)
val VynkLine = Color(0xFFDDE4EE)
val VynkLineStrong = Color(0xFFC6D1DF)
val VynkText = Color(0xFF14213D)
val VynkMuted = Color(0xFF55657E)
val VynkSubtle = Color(0xFF7D8AA0)

val VynkOk = Color(0xFF12805C)
val VynkOkSoft = Color(0xFFE5F5EE)
val VynkWarn = Color(0xFFA35F00)
val VynkWarnSoft = Color(0xFFFFF3DC)
val VynkError = Color(0xFFC0322B)
val VynkErrorSoft = Color(0xFFFDECEA)

/** Color propio de cada una de las cinco áreas de Tecsup. */
data class ColorArea(val fuerte: Color, val suave: Color, val texto: Color = Color.White)

private val AreaGeneral = ColorArea(VynkBlue, VynkBlueSoft)

fun colorArea(nombre: String?): ColorArea {
    val t = java.text.Normalizer.normalize(nombre.orEmpty(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase()
    return when {
        t.isBlank() -> AreaGeneral
        "digital" in t -> ColorArea(Color(0xFFDC2626), Color(0xFFFDE3E3))                          // TD · rojo
        "electr" in t -> ColorArea(Color(0xFF1D6FE0), Color(0xFFE0EDFD))                           // Electricidad · azul
        "mecanica" in t -> ColorArea(Color(0xFFF5B700), Color(0xFFFFF4CC), Color(0xFF3A2A00))      // Mecánica · amarillo (texto oscuro)
        "quimic" in t || "mineria" in t -> ColorArea(Color(0xFF12A150), Color(0xFFDDF5E7))         // Química · verde
        "gestion" in t || "produccion" in t -> ColorArea(Color(0xFF7C3AED), Color(0xFFEDE5FD))    // DPG (por confirmar) · morado
        else -> AreaGeneral
    }
}

/** Las cinco áreas en el orden de la franja de colores. */
val AreasTecsup = listOf(
    "Gestión y Producción", "Tecnología Digital", "Minería y Procesos Químicos",
    "Electricidad y Electrónica", "Mecánica"
)
