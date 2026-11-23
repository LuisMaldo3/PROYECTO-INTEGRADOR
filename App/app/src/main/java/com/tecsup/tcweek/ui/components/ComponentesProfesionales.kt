package com.tecsup.tcweek.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.ui.theme.VynkBlue
import com.tecsup.tcweek.ui.theme.VynkBlueSoft
import com.tecsup.tcweek.ui.theme.VynkLine
import com.tecsup.tcweek.ui.theme.VynkMuted
import com.tecsup.tcweek.ui.theme.VynkNavy
import com.tecsup.tcweek.ui.theme.VynkWarn
import com.tecsup.tcweek.ui.theme.VynkWarnSoft

/*
 * Piezas visuales compartidas por las pantallas: acceso rápido con indicador y cabecera de sección.
 * Mantienen el mismo lenguaje en toda la app: cuadro blanco, borde fino, ícono dentro de un recuadro tenue del color
 * de la función y textos en azul marino.
 */

/** Indicador de un acceso rápido: un número o una palabra corta. [alerta] lo pinta en ámbar (algo requiere atención). */
data class IndicadorAcceso(val texto: String, val alerta: Boolean = false)

/**
 * Cuadro de acceso rápido de color: fondo con degradado del color de la función, ícono blanco y solo el nombre.
 * La [descripcion] no se muestra (menos texto en pantalla) pero se lee con el lector de pantalla.
 */
@Composable
fun AccesoRapido(
    titulo: String,
    descripcion: String,
    icono: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    indicador: IndicadorAcceso? = null,
    onClick: () -> Unit
) {
    val forma = RoundedCornerShape(20.dp)
    val lectura = buildString {
        append(titulo); append(". "); append(descripcion)
        if (indicador != null) { append(". "); append(indicador.texto) }
    }
    Box(
        modifier
            .height(104.dp)
            .sombraSuave(forma, 6.dp)
            .clip(forma)
            .background(Brush.linearGradient(listOf(color, oscurecer(color))))
            .presionable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = lectura }
    ) {
        // Círculo decorativo que da profundidad al cuadro.
        Box(
            Modifier.align(Alignment.BottomEnd).padding(end = 0.dp).size(86.dp)
                .clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.10f))
        )
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(23.dp))
            }
            Text(titulo, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
        }
        if (indicador != null) InsigniaIndicador(indicador, Modifier.align(Alignment.TopEnd).padding(10.dp), sobreColor = true)
    }
}

/** El mismo color, más oscuro: para el final de los degradados. */
fun oscurecer(color: Color, factor: Float = 0.78f): Color =
    Color(color.red * factor, color.green * factor, color.blue * factor, color.alpha)

/** Píldora del indicador: azul tenue normalmente, ámbar cuando es una alerta. */
@Composable
fun InsigniaIndicador(indicador: IndicadorAcceso, modifier: Modifier = Modifier, sobreColor: Boolean = false) {
    val fondo = when {
        indicador.alerta -> Color(0xFFFFD48A)
        sobreColor -> Color.White
        else -> VynkBlueSoft
    }
    val texto = when {
        indicador.alerta -> Color(0xFF5A3500)
        sobreColor -> VynkNavy
        else -> VynkBlue
    }
    Box(modifier.clip(RoundedCornerShape(50)).background(fondo).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(indicador.texto, color = texto, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

/** Ícono en un recuadro de esquinas redondeadas con el color de la función en tono suave. */
@Composable
fun IconoRecuadro(icono: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
    }
}

/** Cabecera de una sección dentro de una tarjeta: ícono, título y una línea de ayuda. */
@Composable
fun CabeceraSeccion(icono: ImageVector, titulo: String, descripcion: String? = null, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(VynkBlueSoft), contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = null, tint = VynkBlue, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, color = VynkNavy)
            if (descripcion != null) Text(descripcion, style = MaterialTheme.typography.bodySmall, color = VynkMuted)
        }
    }
}

/**
 * Cabecera de color de cada pantalla: degradado del [color] de la sección, ícono, título y una línea corta.
 * [acciones] va a la derecha (botones de ícono) y [extra] debajo (cifras, filtros rápidos, botones).
 */
@Composable
fun CabeceraColor(
    titulo: String,
    subtitulo: String?,
    icono: ImageVector,
    color: Color = VynkNavy,
    modifier: Modifier = Modifier,
    acciones: @Composable RowScope.() -> Unit = {},
    extra: (@Composable () -> Unit)? = null
) {
    Box(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
            .background(Brush.linearGradient(listOf(color, oscurecer(color, 0.62f))))
    ) {
        Box(Modifier.align(Alignment.TopEnd).padding(top = 0.dp).size(150.dp).clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.07f)))
        Box(Modifier.align(Alignment.BottomStart).padding(start = 40.dp).size(70.dp).clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.06f)))
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 16.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center) {
                    Icon(icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(25.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(titulo, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!subtitulo.isNullOrBlank()) Text(subtitulo, style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.82f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically, content = acciones)
            }
            if (extra != null) Box(Modifier.padding(end = 8.dp)) { extra() }
        }
    }
}

/** Cifra blanca sobre una cabecera de color: número grande y etiqueta corta. */
@Composable
fun CifraCabecera(valor: String, etiqueta: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.14f))
        .padding(horizontal = 12.dp, vertical = 9.dp)) {
        Text(valor, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Text(etiqueta, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

/** Chip blanco translúcido para elegir una opción sobre una cabecera de color. */
@Composable
fun ChipCabecera(texto: String, elegido: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50))
            .background(if (elegido) Color.White else Color.White.copy(alpha = 0.16f))
            .presionable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(texto, color = if (elegido) VynkNavy else Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}
