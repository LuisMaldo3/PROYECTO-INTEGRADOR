package com.tecsup.tcweek.ui.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.AvisoInicio
import com.tecsup.tcweek.data.DestacadaInicio
import com.tecsup.tcweek.data.ProximaInicio
import com.tecsup.tcweek.data.textoFalta
import com.tecsup.tcweek.ui.comun.DegradadoVynk
import com.tecsup.tcweek.ui.comun.Esqueleto
import com.tecsup.tcweek.ui.comun.PuntoVivo
import com.tecsup.tcweek.ui.comun.TituloSeccion
import com.tecsup.tcweek.ui.comun.aparecer
import com.tecsup.tcweek.ui.comun.presionable
import com.tecsup.tcweek.ui.comun.sombraSuave
import com.tecsup.tcweek.ui.theme.VynkBlue
import com.tecsup.tcweek.ui.theme.VynkMuted
import com.tecsup.tcweek.ui.theme.VynkNavy
import com.tecsup.tcweek.ui.theme.VynkOk
import com.tecsup.tcweek.ui.theme.VynkSky
import com.tecsup.tcweek.ui.theme.VynkSubtle
import com.tecsup.tcweek.ui.theme.VynkWarn

private val ANCHO_TARJETA = 248.dp

@Composable
private fun CarruselEsqueleto() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(2) { Esqueleto(Modifier.width(ANCHO_TARJETA).height(210.dp), RoundedCornerShape(20.dp)) }
    }
}

/** US-81 T4: mis próximas actividades en tarjetas con portada, con cuánto falta y el estado del equipo. */
@Composable
fun SeccionMisProximas(proximas: List<ProximaInicio>?, ahora: Long, onAbrir: (String) -> Unit, onAgenda: () -> Unit, onExplorar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TituloSeccion("Tus próximas actividades", "Ver agenda", onAgenda)
        when {
            proximas == null -> CarruselEsqueleto()
            proximas.isEmpty() -> Row(
                Modifier.fillMaxWidth().sombraSuave(RoundedCornerShape(18.dp), 4.dp).clip(RoundedCornerShape(18.dp))
                    .background(Color.White).presionable(onClick = onExplorar).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(DegradadoVynk), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Search, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("No tienes más actividades por ahora", color = VynkNavy, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium)
                    Text("Explora el catálogo: lo que te interesa aparece primero.", color = VynkMuted,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 4.dp)) {
                itemsIndexed(proximas, key = { _, p -> p.actividad.id }) { i, p ->
                    TarjetaProximaInicio(p, ahora, Modifier.aparecer(i)) { onAbrir(p.actividad.id) }
                }
            }
        }
    }
}

@Composable
private fun TarjetaProximaInicio(p: ProximaInicio, ahora: Long, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val a = p.actividad
    val forma = RoundedCornerShape(20.dp)
    val enCurso = p.enCurso(ahora)
    Column(
        modifier.width(ANCHO_TARJETA).sombraSuave(forma, 6.dp).clip(forma).background(Color.White).presionable(onClick = onClick)
            .semantics { contentDescription = "Próxima: ${a.nombre}, ${p.cuando}" }
    ) {
        Box {
            ImagenPortadaCatalogo(a.portadaUrl, a.nombre, portadaTematica(a), alto = 112.dp)
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color(0xCC0B2A5B))))
            Row(
                Modifier.align(Alignment.BottomStart).padding(10.dp).clip(RoundedCornerShape(50))
                    .background(if (enCurso) VynkOk else Color.White).padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (enCurso) PuntoVivo(Color.White)
                Text(if (enCurso) "Ahora" else textoFalta(p.empiezaEn, ahora), color = if (enCurso) Color.White else VynkNavy,
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(a.nombre, color = VynkNavy, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(p.cuando.replaceFirstChar { it.uppercase() } + " – " + a.horaFin, color = VynkMuted, style = MaterialTheme.typography.bodySmall)
            if (a.ubicacion.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.Place, null, tint = VynkBlue, modifier = Modifier.size(14.dp))
                Text(a.ubicacion, color = VynkNavy, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (p.equipo.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.Groups, null, tint = if (p.equipoConfirmado) VynkOk else VynkWarn, modifier = Modifier.size(14.dp))
                Text(p.equipo + if (p.equipoConfirmado) " · confirmado" else " · falta confirmar",
                    color = if (p.equipoConfirmado) VynkOk else VynkWarn, style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** US-81-T07: actividades y competencias destacadas; la portada publicada o la ilustración de su tema. */
@Composable
fun SeccionDestacadas(destacadas: List<DestacadaInicio>?, onAbrir: (String) -> Unit, onVerTodo: () -> Unit) {
    if (destacadas != null && destacadas.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TituloSeccion("Destacadas", "Ver todo", onVerTodo)
        if (destacadas == null) CarruselEsqueleto()
        else LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 4.dp)) {
            itemsIndexed(destacadas, key = { _, d -> d.actividad.id }) { i, d ->
                TarjetaDestacada(d, Modifier.aparecer(i)) { onAbrir(d.actividad.id) }
            }
        }
    }
}

@Composable
private fun TarjetaDestacada(d: DestacadaInicio, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val a = d.actividad
    val forma = RoundedCornerShape(22.dp)
    Box(
        modifier.width(272.dp).height(190.dp).sombraSuave(forma, 8.dp).clip(forma).presionable(onClick = onClick)
            .semantics { contentDescription = "Destacada: ${a.nombre}, ${d.cuando}" }
    ) {
        Box(Modifier.fillMaxSize()) { ImagenPortadaCatalogo(a.portadaUrl, a.nombre, portadaTematica(a), alto = 190.dp) }
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(0.25f to Color.Transparent, 1f to Color(0xE60B2A5B))))
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val (icono, fondo) = when (d.destacadaPor) {
                "Competencia" -> Icons.Filled.EmojiEvents to Color(0xFF7C3AED)
                "Para ti" -> Icons.Filled.AutoAwesome to VynkBlue
                else -> Icons.Filled.Campaign to VynkNavy
            }
            Row(Modifier.clip(RoundedCornerShape(50)).background(fondo).padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icono, null, tint = Color.White, modifier = Modifier.size(13.dp))
                Text(d.destacadaPor, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            if (d.paraTi && d.destacadaPor != "Para ti") InsigniaParaTi()
        }
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(d.cuando.replaceFirstChar { it.uppercase() }, color = VynkSky, style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold)
            Text(a.nombre, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (a.ubicacion.isNotBlank()) Text(a.ubicacion, color = Color(0xFFD6E4F7), style = MaterialTheme.typography.bodySmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** US-81-T06: avisos recientes con su imagen; al tocar uno se lee completo. */
@Composable
fun SeccionAvisos(avisos: List<AvisoInicio>?) {
    var abierto by remember { mutableStateOf<AvisoInicio?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TituloSeccion("Avisos de Tecsup")
        when {
            avisos == null -> CarruselEsqueleto()
            avisos.isEmpty() -> Text("No hay avisos por ahora. Cuando administración publique uno, aparecerá aquí.",
                color = VynkMuted, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(16.dp))
            else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 4.dp)) {
                itemsIndexed(avisos, key = { _, v -> v.id.ifBlank { v.titulo } }) { i, aviso ->
                    TarjetaAviso(aviso, Modifier.aparecer(i)) { abierto = aviso }
                }
            }
        }
    }
    abierto?.let { aviso -> DialogoAviso(aviso) { abierto = null } }
}

@Composable
private fun ImagenAviso(aviso: AvisoInicio, alto: androidx.compose.ui.unit.Dp) {
    val respaldo: @Composable () -> Unit = {
        Box(Modifier.fillMaxWidth().height(alto).background(DegradadoVynk), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Campaign, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(38.dp))
        }
    }
    if (aviso.imagenUrl.isBlank()) respaldo() else FotoAviso(aviso.imagenUrl, alto, respaldo)
}

@Composable
private fun TarjetaAviso(aviso: AvisoInicio, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val forma = RoundedCornerShape(20.dp)
    Column(
        modifier.width(ANCHO_TARJETA).sombraSuave(forma, 6.dp).clip(forma).background(Color.White).presionable(onClick = onClick)
            .semantics { contentDescription = "Aviso: ${aviso.titulo}" }
    ) {
        ImagenAviso(aviso, 118.dp)
        Column(Modifier.padding(14.dp).heightIn(min = 86.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MetaAviso(aviso.publicadoEn)
            Text(aviso.titulo, color = VynkNavy, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(aviso.mensaje, color = VynkMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DialogoAviso(aviso: AvisoInicio, onCerrar: () -> Unit) {
    val contexto = LocalContext.current
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(aviso.titulo, color = VynkNavy, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (aviso.imagenUrl.isNotBlank()) Box(Modifier.clip(RoundedCornerShape(14.dp))) { FotoAviso(aviso.imagenUrl, 170.dp) }
                MetaAviso(aviso.publicadoEn)
                Text(aviso.mensaje, color = VynkNavy, style = MaterialTheme.typography.bodyMedium)
                if (aviso.creditoUrl.startsWith("https://commons.wikimedia.org/"))
                    Text("Foto: ${aviso.autor.ifBlank { "Wikimedia Commons" }}", color = VynkSubtle, style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.clickable { contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(aviso.creditoUrl))) })
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } },
        containerColor = Color.White, shape = RoundedCornerShape(20.dp)
    )
}

