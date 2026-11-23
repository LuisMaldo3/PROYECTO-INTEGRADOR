package com.tecsup.tcweek.ui.agenda

import com.tecsup.tcweek.ui.catalog.estiloCategoria
import com.tecsup.tcweek.ui.catalog.nombreDia
import com.tecsup.tcweek.ui.home.ActividadDeAviso
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.remote.AlternativaAsistencia
import com.tecsup.tcweek.data.remote.AlternativasAsistenciaApi
import com.tecsup.tcweek.domain.CambiosAgenda
import com.tecsup.tcweek.data.remote.MotivoInasistencia
import com.tecsup.tcweek.data.remote.RespuestaAlternativas
import com.tecsup.tcweek.data.remote.ResultadoCambio
import com.tecsup.tcweek.data.remote.origenAlternativas
import com.tecsup.tcweek.ui.components.EsqueletoTarjetas
import com.tecsup.tcweek.ui.components.PuntoVivo
import com.tecsup.tcweek.ui.components.aparecer
import com.tecsup.tcweek.ui.components.presionable
import com.tecsup.tcweek.ui.components.sombraSuave
import com.tecsup.tcweek.ui.theme.VynkBackground
import com.tecsup.tcweek.ui.theme.VynkBlue
import com.tecsup.tcweek.ui.theme.VynkBlueSoft
import com.tecsup.tcweek.ui.theme.VynkError
import com.tecsup.tcweek.ui.theme.VynkLine
import com.tecsup.tcweek.ui.theme.VynkMuted
import com.tecsup.tcweek.ui.theme.VynkNavy
import com.tecsup.tcweek.ui.theme.VynkOk
import com.tecsup.tcweek.ui.theme.VynkOkSoft
import com.tecsup.tcweek.ui.theme.VynkSky
import com.tecsup.tcweek.ui.theme.VynkWarn
import com.tecsup.tcweek.ui.theme.VynkWarnSoft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Botón «No puedo asistir» (US-79 T2) para el detalle y la agenda. */
@Composable
fun BotonNoPuedoAsistir(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, VynkWarn.copy(alpha = 0.45f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = VynkWarn),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Icon(Icons.Filled.EventBusy, contentDescription = null, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text("No puedo asistir", fontWeight = FontWeight.SemiBold)
    }
}

/**
 * US-79 T3: alternativas propuestas cuando no puedo asistir a [actividadId]. Se elige el motivo (no puedo a esa
 * hora / otro), se ven las alternativas con su porqué y se puede abrir el detalle o cambiarse en un toque
 * (primero se inscribe en la nueva y luego cancela la anterior).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaNoPuedoAsistir(actividadId: String, nombre: String, token: String, onCerrar: () -> Unit, onCambiado: (String) -> Unit) {
    val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var motivo by remember { mutableStateOf(MotivoInasistencia.HORARIO) }
    var recarga by remember { mutableIntStateOf(0) }
    var datos by remember { mutableStateOf<RespuestaAlternativas?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var detalleId by remember { mutableStateOf<String?>(null) }
    var confirmar by remember { mutableStateOf<AlternativaAsistencia?>(null) }
    var cambiando by remember { mutableStateOf(false) }
    var errorCambio by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(motivo, recarga) {
        cargando = true
        error = null
        try { datos = AlternativasAsistenciaApi.cargar(token, actividadId, motivo) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "No se pudieron buscar alternativas." }
        finally { cargando = false }
    }

    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = estadoHoja, containerColor = VynkBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(VynkWarnSoft), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.EventBusy, contentDescription = null, tint = VynkWarn, modifier = Modifier.size(24.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("No puedo asistir", color = VynkNavy, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                    Text(nombre + (datos?.let { d -> if (d.fecha.isNotBlank()) " · ${nombreDia(d.fecha)} ${d.horaInicio}" else "" } ?: ""),
                        color = VynkMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Text("¿Por qué no puedes ir?", color = VynkNavy, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MotivoInasistencia.entries.forEach { m -> ChipMotivo(m.texto, motivo == m, enabled = !cargando) { motivo = m } }
            }
            val d = datos
            if (d != null && !cargando) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                if (d.conIa) PuntoVivo(VynkSky) else Icon(Icons.Filled.AutoAwesome, null, tint = VynkMuted, modifier = Modifier.size(14.dp))
                Text(origenAlternativas(d.origen), color = if (d.conIa) VynkBlue else VynkMuted,
                    fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
            }
            when {
                cargando -> {
                    Text("Buscando alternativas que encajen en tu agenda…", color = VynkMuted, style = MaterialTheme.typography.bodySmall)
                    EsqueletoTarjetas(2)
                }
                error != null -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(error.orEmpty(), color = VynkError, style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { recarga++ }, shape = RoundedCornerShape(12.dp)) { Text("Reintentar") }
                }
                d != null && d.alternativas.isEmpty() -> Surface(color = Color.White, shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, VynkLine)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("No encontramos alternativas", color = VynkNavy, fontWeight = FontWeight.Bold)
                        Text(d.aviso.ifBlank { "Prueba con «Otro motivo» o revisa el catálogo más tarde." }, color = VynkMuted,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                d != null -> {
                    d.alternativas.forEachIndexed { i, alt ->
                        TarjetaAlternativa(alt, puedeCambiar = d.puedeCambiar, modifier = Modifier.aparecer(i),
                            onDetalle = { detalleId = alt.actividad.id },
                            onCambiar = { if (alt.inscripcionDirecta) { errorCambio = null; confirmar = alt } else detalleId = alt.actividad.id })
                    }
                    if (d.aviso.isNotBlank()) Text(d.aviso, color = if (d.puedeCambiar) VynkMuted else VynkWarn,
                        style = MaterialTheme.typography.bodySmall, fontWeight = if (d.puedeCambiar) FontWeight.Normal else FontWeight.SemiBold)
                }
            }
        }
    }

    detalleId?.let { id -> ActividadDeAviso(id, token, alCerrar = { detalleId = null }) }
    confirmar?.let { alt ->
        AlertDialog(
            onDismissRequest = { if (!cambiando) confirmar = null },
            icon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = VynkBlue) },
            title = { Text("¿Te cambias?", fontWeight = FontWeight.ExtraBold, color = VynkNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Te inscribimos en «${alt.actividad.nombre}» (${nombreDia(alt.actividad.fecha)}, ${alt.actividad.horaInicio}) " +
                        "y cancelamos tu inscripción en «$nombre».", color = VynkNavy)
                    Text("Si la nueva inscripción falla, no tocamos la anterior.", color = VynkMuted, style = MaterialTheme.typography.bodySmall)
                    errorCambio?.let { Text(it, color = VynkError, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                Button(enabled = !cambiando, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = VynkBlue),
                    onClick = {
                        cambiando = true
                        scope.launch {
                            when (val r = AlternativasAsistenciaApi.cambiar(token, alt.actividad.id, actividadId)) {
                                ResultadoCambio.Cambiado -> {
                                    CambiosAgenda.avisar()
                                    confirmar = null
                                    onCambiado("Listo: te cambiaste a «${alt.actividad.nombre}» y liberamos tu cupo en «$nombre».")
                                }
                                is ResultadoCambio.NoInscrito -> errorCambio = r.mensaje + " Tu inscripción en «$nombre» sigue igual."
                                is ResultadoCambio.SinCancelar -> {
                                    CambiosAgenda.avisar()
                                    confirmar = null
                                    onCambiado("Te inscribiste en «${alt.actividad.nombre}», pero no pudimos cancelar «$nombre»: " +
                                        "${r.mensaje} Cancélala desde su detalle.")
                                }
                            }
                            cambiando = false
                        }
                    }) {
                    if (cambiando) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Cambiarme", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmar = null }, enabled = !cambiando) { Text("Ahora no") } }
        )
    }
}

@Composable
private fun ChipMotivo(texto: String, activo: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val fondo by animateColorAsState(if (activo) VynkNavy else Color.White, label = "fondo")
    val color by animateColorAsState(if (activo) Color.White else VynkNavy, label = "texto")
    Surface(color = fondo, shape = RoundedCornerShape(50), border = if (activo) null else BorderStroke(1.dp, VynkLine),
        modifier = Modifier.presionable(enabled = enabled, onClick = onClick)) {
        Text(texto, color = color, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaAlternativa(
    alt: AlternativaAsistencia, puedeCambiar: Boolean, modifier: Modifier = Modifier,
    onDetalle: () -> Unit, onCambiar: () -> Unit
) {
    val a = alt.actividad
    val forma = RoundedCornerShape(20.dp)
    Column(
        modifier.fillMaxWidth().sombraSuave(forma, 6.dp).clip(forma).background(Color.White)
            .border(1.dp, VynkLine.copy(alpha = 0.7f), forma).presionable(onClick = onDetalle).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(estiloCategoria(id = a.categoriaId).nombre.uppercase(), color = VynkBlue, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall)
        Text(a.nombre, color = VynkNavy, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(Icons.Filled.Event, null, tint = VynkBlue, modifier = Modifier.size(15.dp))
            Text("${nombreDia(a.fecha)} · ${a.horaInicio} – ${a.horaFin}", color = VynkNavy, style = MaterialTheme.typography.bodySmall)
        }
        if (a.ubicacion.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(Icons.Filled.Place, null, tint = VynkBlue, modifier = Modifier.size(15.dp))
            Text(a.ubicacion, color = VynkNavy, style = MaterialTheme.typography.bodySmall)
        }
        if (alt.etiquetas.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            alt.etiquetas.forEach { e ->
                val (fondo, color) = when {
                    e.startsWith("Misma") -> VynkBlueSoft to VynkBlue
                    e.startsWith("Queda") -> VynkWarnSoft to VynkWarn
                    e == "Hoy" || e.startsWith("Hoy") || e.startsWith("Mañana") -> VynkOkSoft to VynkOk
                    else -> Color(0xFFEFF2F7) to VynkMuted
                }
                Text(e, color = color, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(fondo).padding(horizontal = 10.dp, vertical = 5.dp))
            }
        }
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(VynkBlueSoft, Color(0xFFF1F7FF)))).padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.AutoAwesome, null, tint = VynkBlue, modifier = Modifier.size(16.dp))
                Text(alt.motivo, color = VynkNavy, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onDetalle, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(13.dp),
                border = BorderStroke(1.dp, VynkLine)) { Text("Ver detalle", color = VynkNavy, fontWeight = FontWeight.SemiBold) }
            if (puedeCambiar) Button(onClick = onCambiar, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VynkBlue)) {
                Icon(Icons.Filled.SwapHoriz, null, Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text(if (alt.porEquipos) "Con mi equipo" else "Cambiarme", fontWeight = FontWeight.Bold)
            }
        }
    }
}
