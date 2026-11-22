package com.tecsup.tcweek.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.ActividadCatalogo
import com.tecsup.tcweek.data.ActividadProgramada
import com.tecsup.tcweek.data.AuthRepository
import com.tecsup.tcweek.data.CambiosAgenda
import com.tecsup.tcweek.data.EstadoDia
import com.tecsup.tcweek.data.ProgramacionApi
import com.tecsup.tcweek.data.ProgramacionDia
import com.tecsup.tcweek.data.UbicacionActividad
import com.tecsup.tcweek.data.diaAnterior
import com.tecsup.tcweek.data.diaSiguiente
import com.tecsup.tcweek.data.etiquetaDia
import com.tecsup.tcweek.ui.comun.Pildora
import com.tecsup.tcweek.ui.theme.VynkBackground
import com.tecsup.tcweek.ui.theme.VynkBlue
import com.tecsup.tcweek.ui.theme.VynkBlueSoft
import com.tecsup.tcweek.ui.theme.VynkError
import com.tecsup.tcweek.ui.theme.VynkErrorSoft
import com.tecsup.tcweek.ui.theme.VynkLine
import com.tecsup.tcweek.ui.theme.VynkMuted
import com.tecsup.tcweek.ui.theme.VynkNavy
import com.tecsup.tcweek.ui.theme.VynkOk
import com.tecsup.tcweek.ui.theme.VynkOkSoft
import com.tecsup.tcweek.ui.theme.VynkSubtle
import com.tecsup.tcweek.ui.theme.VynkWarn
import com.tecsup.tcweek.ui.theme.VynkWarnSoft
import kotlinx.coroutines.CancellationException

/**
 * US-70: programación completa de un día. Arriba se elige el día (los que tienen actividades); abajo, las
 * actividades por hora, con portada, lugar, si está en curso o terminó y si el estudiante ya está inscrito.
 * Tocar una abre su detalle, desde donde se puede inscribir; tocar el lugar abre la pantalla de ubicación.
 */
@Composable
fun ProgramacionScreen(onVolver: () -> Unit, onVerUbicacion: (ActividadCatalogo) -> Unit) {
    val context = LocalContext.current
    val token = remember { AuthRepository(context).currentSession()?.llave.orEmpty() }
    // null = que el servidor elija el día (hoy o el próximo con actividades).
    var fecha by rememberSaveable { mutableStateOf<String?>(null) }
    var datos by remember { mutableStateOf<ProgramacionDia?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }
    var seleccionada by remember { mutableStateOf<ActividadCatalogo?>(null) }
    val cambiosAgenda by CambiosAgenda.version.collectAsState()

    // Cambiar de día cancela la carga anterior (LaunchedEffect se reinicia): nunca se muestra un día viejo.
    LaunchedEffect(fecha, recarga, cambiosAgenda) {
        cargando = true
        error = null
        try {
            val nuevo = ProgramacionApi.cargar(token, fecha)
            datos = nuevo
            if (fecha == null) fecha = nuevo.fecha
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar la programación."
        } finally {
            cargando = false
        }
    }

    val actual = datos
    Column(Modifier.fillMaxSize().background(VynkBackground)) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
            Column(Modifier.weight(1f)) {
                Text("Programación del día", style = MaterialTheme.typography.headlineSmall, color = VynkNavy)
                Text(
                    if (actual != null && fecha != null) nombreDia(fecha) else "Cargando…",
                    style = MaterialTheme.typography.bodySmall, color = VynkMuted
                )
            }
            IconButton(onClick = { recarga++ }, enabled = !cargando) {
                Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
            }
        }

        if (actual != null && actual.dias.isNotEmpty()) SelectorDias(actual, fecha.orEmpty()) { fecha = it }

        when {
            cargando && actual == null -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VynkBlue)
            }

            error != null -> EstadoVacio(
                icono = Icons.Filled.ErrorOutline, titulo = "No pudimos cargar la programación",
                texto = error.orEmpty(), esError = true
            ) { OutlinedButton(onClick = { recarga++ }) { Text("Reintentar") } }

            actual != null && actual.actividades.isEmpty() -> EstadoVacio(
                icono = Icons.Filled.EventBusy, titulo = "No hay actividades este día",
                texto = if (actual.dias.isEmpty()) "Todavía no se publicó la programación."
                else "Elige otro día de la lista para ver su programación."
            )

            actual != null -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        cantidad(actual.actividades.size, "actividad", "actividades") +
                            (if (cargando) " · actualizando…" else ""),
                        style = MaterialTheme.typography.labelMedium, color = VynkMuted
                    )
                }
                items(actual.actividades, key = { it.actividad.id }) { item ->
                    TarjetaProgramada(
                        item,
                        onAbrir = { if (item.sePuedeAbrir) seleccionada = item.actividad },
                        onUbicacion = { onVerUbicacion(item.actividad) }
                    )
                }
            }
        }
    }

    seleccionada?.let { a ->
        // Detalle con inscripción individual (US-70 T07). Al cerrar se recarga: el estado «Inscrito» puede cambiar.
        DialogoActividad(a, token, alCerrar = { seleccionada = null; recarga++ },
            onVerActividad = { seleccionada = it }, onVerUbicacion = { seleccionada = null; onVerUbicacion(it) })
    }
}

/** Fila de días con actividades, con flechas para pasar al anterior o al siguiente. */
@Composable
private fun SelectorDias(datos: ProgramacionDia, fecha: String, onElegir: (String) -> Unit) {
    val estado = rememberLazyListState()
    val indice = datos.dias.indexOfFirst { it.fecha == fecha }
    LaunchedEffect(indice) { if (indice >= 0) estado.animateScrollToItem(maxOf(0, indice - 1)) }
    val anterior = diaAnterior(datos.dias, fecha)
    val siguiente = diaSiguiente(datos.dias, fecha)
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { anterior?.let(onElegir) }, enabled = anterior != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Día anterior")
        }
        LazyRow(Modifier.weight(1f), state = estado, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(datos.dias, key = { it.fecha }) { dia ->
                FilterChip(
                    selected = dia.fecha == fecha,
                    onClick = { onElegir(dia.fecha) },
                    label = { Text("${etiquetaDia(dia.fecha, datos.hoy)} · ${dia.total}") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VynkNavy, selectedLabelColor = Color.White
                    )
                )
            }
        }
        IconButton(onClick = { siguiente?.let(onElegir) }, enabled = siguiente != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Día siguiente")
        }
    }
}

@Composable
private fun TarjetaProgramada(item: ActividadProgramada, onAbrir: () -> Unit, onUbicacion: () -> Unit) {
    val a = item.actividad
    val apagada = item.estadoDia == EstadoDia.CANCELADA || item.estadoDia == EstadoDia.FINALIZADA
    Card(
        colors = CardDefaults.cardColors(containerColor = if (apagada) Color(0xFFF7F9FC) else Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (item.estadoDia == EstadoDia.EN_CURSO) VynkOk else VynkLine),
        modifier = Modifier.fillMaxWidth().clickable(enabled = item.sePuedeAbrir, onClick = onAbrir)
            .semantics { contentDescription = "${a.nombre}, ${rangoHoras(a.horaInicio, a.horaFin)}, ${a.ubicacion}" }
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // US-70 T06: portada (o su ilustración de respaldo por área si no hay o no carga).
            Box(Modifier.size(width = 92.dp, height = 92.dp).clip(RoundedCornerShape(12.dp))) {
                if (a.portadaUrl.isNotBlank()) ImagenPortadaCatalogo(a.portadaUrl, a.nombre, portadaTematica(a), alto = 92.dp)
                else Image(painterResource(portadaTematica(a)), contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = VynkBlue, modifier = Modifier.size(15.dp))
                    Text(rangoHoras(a.horaInicio, a.horaFin), style = MaterialTheme.typography.labelLarge, color = VynkNavy)
                    Spacer(Modifier.weight(1f))
                    EtiquetaEstado(item)
                }
                Text(a.nombre, style = MaterialTheme.typography.titleSmall, color = if (apagada) VynkMuted else VynkNavy,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                // US-71 T02: el lugar en cada tarjeta; tocarlo abre la pantalla de ubicación.
                Row(
                    Modifier.clickable(onClick = onUbicacion).padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.Place, contentDescription = null, tint = VynkBlue, modifier = Modifier.size(15.dp))
                    Column {
                        Text(a.ubicacion.ifBlank { UbicacionActividad.POR_CONFIRMAR }, style = MaterialTheme.typography.bodySmall,
                            color = VynkBlue, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (a.ubicacionReferencia.isNotBlank()) Text(a.ubicacionReferencia,
                            style = MaterialTheme.typography.labelSmall, color = VynkSubtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (item.estadoDia == EstadoDia.PROXIMA && !item.inscrito) {
                    val cupos = estadoCupos(a.cupos)
                    Text(cupos.texto, style = MaterialTheme.typography.labelSmall, color = cupos.tono.color())
                }
            }
        }
    }
}

@Composable
private fun EtiquetaEstado(item: ActividadProgramada) {
    when {
        item.estadoDia == EstadoDia.CANCELADA -> Pildora("Cancelada", VynkErrorSoft, VynkError)
        item.estadoDia == EstadoDia.FINALIZADA -> Pildora("Finalizada", Color(0xFFEDEFF3), VynkMuted)
        item.estadoDia == EstadoDia.EN_CURSO -> Pildora("En curso", VynkOkSoft, VynkOk)
        item.inscrito -> Pildora("Inscrito", VynkBlueSoft, VynkBlue)
        item.actividad.tipoActividad == "competencia" -> Pildora("Competencia", VynkWarnSoft, VynkWarn)
    }
}
