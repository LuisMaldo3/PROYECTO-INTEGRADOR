package com.tecsup.tcweek.ui.comun

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.tcweek.ui.theme.Archivo
import com.tecsup.tcweek.ui.theme.AreasTecsup
import com.tecsup.tcweek.ui.theme.VynkLine
import com.tecsup.tcweek.ui.theme.VynkMuted
import com.tecsup.tcweek.ui.theme.VynkNavy
import com.tecsup.tcweek.ui.theme.VynkSky
import com.tecsup.tcweek.ui.theme.colorArea

/** «VYN» + «K» celeste, el mismo logo que en la web. */
@Composable
fun VynkLogo(tamano: TextUnit = 24.sp, color: Color = Color.White, modifier: Modifier = Modifier) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = color)) { append("VYN") }
            withStyle(SpanStyle(color = VynkSky)) { append("K") }
        },
        fontFamily = Archivo,
        fontWeight = FontWeight.ExtraBold,
        fontSize = tamano,
        letterSpacing = 0.5.sp,
        modifier = modifier
    )
}

/** Franja con el color de las cinco áreas de Tecsup. */
@Composable
fun FranjaAreas(modifier: Modifier = Modifier, alto: Int = 5) {
    Row(modifier.fillMaxWidth()) {
        AreasTecsup.forEach { area ->
            Box(Modifier.weight(1f).height(alto.dp).background(colorArea(area).fuerte))
        }
    }
}

/** Franja de áreas con su nombre debajo (pantalla de acceso). */
@Composable
fun FranjaAreasConNombre(colorTexto: Color = Color.White.copy(alpha = .9f)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        AreasTecsup.forEach { area ->
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.fillMaxWidth().height(4.dp).background(colorArea(area).fuerte, RoundedCornerShape(2.dp)))
                Text(area, color = colorTexto, fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Título de sección con acción opcional a la derecha. */
@Composable
fun TituloSeccion(titulo: String, accion: String? = null, onAccion: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, color = VynkNavy, modifier = Modifier.weight(1f))
        if (accion != null) TextButton(onClick = onAccion) { Text(accion, style = MaterialTheme.typography.labelLarge) }
    }
}

/** Tarjeta blanca con borde suave, base de todas las listas. */
@Composable
fun Tarjeta(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, VynkLine),
        content = contenido
    )
}

/** Etiqueta pequeña tipo píldora. */
@Composable
fun Pildora(texto: String, fondo: Color, color: Color, modifier: Modifier = Modifier) {
    Surface(color = fondo, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(texto, color = color, style = MaterialTheme.typography.labelSmall, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp))
    }
}

/** Etiqueta de área: punto de color + nombre. */
@Composable
fun EtiquetaArea(area: String) {
    val c = colorArea(area)
    Surface(color = c.suave, shape = RoundedCornerShape(50)) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.width(7.dp).height(7.dp).background(c.fuerte, RoundedCornerShape(50)))
            Text(area, color = VynkNavy, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Texto secundario estándar. */
@Composable
fun TextoSuave(texto: String, modifier: Modifier = Modifier) {
    Text(texto, style = MaterialTheme.typography.bodySmall, color = VynkMuted, modifier = modifier)
}
