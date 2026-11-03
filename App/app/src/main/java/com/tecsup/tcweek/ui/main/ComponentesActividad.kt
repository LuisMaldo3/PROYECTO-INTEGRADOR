package com.tecsup.tcweek.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Piezas compartidas por Catálogo y Agenda. */

internal val ColorMarino = com.tecsup.tcweek.ui.theme.VynkNavy
internal val ColorGris = Color(0xFF55657E)
internal val ColorBorde = com.tecsup.tcweek.ui.theme.VynkLine
internal val ColorAzul = Color(0xFF1557D0)

/** Categoría con los colores del cartel del T&C Week: azul para lo cultural y verde azulado para los concursos. */
@Composable
fun EtiquetaCategoria(estilo: EstiloCategoria) {
    val fondo: Color
    val texto: Color
    val icono: ImageVector
    when (estilo.clase) {
        ClaseCategoria.CULTURAL -> {
            fondo = Color(0xFFF3ECFF); texto = Color(0xFF5B34C7); icono = Icons.Filled.TheaterComedy
        }
        ClaseCategoria.CONCURSO -> {
            fondo = Color(0xFFFFF4E0); texto = Color(0xFF8A4F00); icono = Icons.Filled.EmojiEvents
        }
        ClaseCategoria.OTRA -> {
            fondo = Color(0xFFDDE4EE); texto = Color(0xFF14213D); icono = Icons.Filled.Star
        }
    }
    Surface(color = fondo, shape = RoundedCornerShape(50)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icono, contentDescription = null, tint = texto, modifier = Modifier.size(14.dp))
            Text(estilo.nombre, color = texto, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Título de cada día: «Lunes 19 de octubre · 3 actividades». */
@Composable
fun EncabezadoDia(titulo: String, detalle: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, color = ColorMarino)
            Text(detalle, style = MaterialTheme.typography.bodySmall, color = ColorGris)
        }
        HorizontalDivider(modifier = Modifier.padding(top = 6.dp), color = ColorBorde)
    }
}

/** Una línea de datos con su ícono (hora, lugar, organizador). */
@Composable
fun DatoConIcono(icono: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icono, contentDescription = null, tint = ColorGris, modifier = Modifier.size(16.dp))
        Text(texto, style = MaterialTheme.typography.bodySmall, color = ColorMarino)
    }
}

/** Color del texto de los cupos según el tono. */
fun TonoCupos.color(): Color = when (this) {
    TonoCupos.OK -> Color(0xFF12805C)
    TonoCupos.POCO -> Color(0xFF9A5B00)
    TonoCupos.AGOTADO -> Color(0xFFC0322B)
    TonoCupos.NEUTRO -> ColorGris
}
