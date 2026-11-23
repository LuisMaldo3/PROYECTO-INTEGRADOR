package com.tecsup.tcweek.ui.home

import com.tecsup.tcweek.ui.catalog.nombreDia
import com.tecsup.tcweek.ui.catalog.rangoHoras
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.domain.CambiosAgenda
import com.tecsup.tcweek.data.remote.MiProximaActividad
import com.tecsup.tcweek.data.remote.MiProximaApi
import com.tecsup.tcweek.data.remote.TICK_PROXIMAS_MS
import com.tecsup.tcweek.domain.UbicacionActividad
import com.tecsup.tcweek.data.remote.debeRecargarMiProxima
import com.tecsup.tcweek.data.remote.empiezaPronto
import com.tecsup.tcweek.data.remote.estadoMiProxima
import com.tecsup.tcweek.ui.components.Esqueleto
import com.tecsup.tcweek.ui.components.IconoDegradado
import com.tecsup.tcweek.ui.components.PuntoVivo
import com.tecsup.tcweek.ui.components.presionable
import com.tecsup.tcweek.ui.components.sombraSuave
import com.tecsup.tcweek.ui.theme.VynkBlue
import com.tecsup.tcweek.ui.theme.VynkBlueSoft
import com.tecsup.tcweek.ui.theme.VynkLine
import com.tecsup.tcweek.ui.theme.VynkMuted
import com.tecsup.tcweek.ui.theme.VynkNavy
import com.tecsup.tcweek.ui.theme.VynkOk
import com.tecsup.tcweek.ui.theme.VynkOkSoft
import com.tecsup.tcweek.ui.theme.VynkWarn
import com.tecsup.tcweek.ui.theme.VynkWarnSoft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * US-74 T02: acceso rápido «Mi próxima actividad». Responde en un vistazo dónde tiene que estar el estudiante: el
 * lugar en grande, la referencia para llegar, cuánto falta (o «Ahora mismo» si ya empezó) y qué viene después.
 * «Cómo llegar» abre la pantalla de ubicación; tocar la tarjeta abre el detalle.
 *
 * T03 (carga programada): se pide al entrar, cada 5 minutos, al terminar la actividad (pasa sola a la siguiente) y
 * cuando cambia la agenda (inscripción, cancelación o cambio de horario/lugar). El estado se recalcula cada 30 s.
 */
@Composable
fun TarjetaMiProxima(
    token: String,
    onVerUbicacion: (String) -> Unit,
    onAbrir: (String) -> Unit,
    onExplorar: () -> Unit,
    modifier: Modifier = Modifier,
    onCargada: (String?) -> Unit = {}
) {
    var datos by remember { mutableStateOf<MiProximaActividad?>(null) }
    var cargado by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var reintento by remember { mutableIntStateOf(0) }
    val cambios by CambiosAgenda.version.collectAsState()

    LaunchedEffect(token, cambios, reintento) {
        var ultimaCarga = 0L
        while (true) {
            ahora = System.currentTimeMillis()
            if (debeRecargarMiProxima(datos, ultimaCarga, ahora)) {
                try {
                    datos = MiProximaApi.cargar(token)
                    error = null
                    onCargada(datos?.actividad?.id)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (!cargado) error = e.message ?: "No se pudo consultar tu próxima actividad."
                }
                cargado = true
                ultimaCarga = ahora
            }
            delay(TICK_PROXIMAS_MS)
        }
    }

    val forma = RoundedCornerShape(22.dp)
    Box(modifier.fillMaxWidth().sombraSuave(forma, 14.dp).clip(forma).background(Color.White)) {
        AnimatedContent(
            targetState = Triple(cargado, error, datos?.actividad?.id),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "miProxima"
        ) { (listo, falla, _) ->
            val actual = datos
            when {
                !listo -> Cargando()
                falla != null -> Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(falla, style = MaterialTheme.typography.bodySmall, color = VynkMuted, modifier = Modifier.weight(1f))
                    TextButton(onClick = { reintento++ }) { Text("Reintentar") }
                }
                actual == null -> SinProxima(onExplorar)
                else -> ConProxima(actual, ahora, onVerUbicacion, onAbrir)
            }
        }
    }
}

@Composable
private fun Cargando() {
    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Esqueleto(Modifier.fillMaxWidth(0.4f).height(12.dp))
        Esqueleto(Modifier.fillMaxWidth(0.8f).height(26.dp))
        Esqueleto(Modifier.fillMaxWidth(0.6f).height(14.dp))
    }
}

@Composable
private fun SinProxima(onExplorar: () -> Unit) {
    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconoDegradado(Icons.Filled.EventAvailable, VynkBlue)
        Column(Modifier.weight(1f)) {
            Text("Mi próxima actividad", style = MaterialTheme.typography.labelMedium, color = VynkMuted)
            Text("Aún no tienes actividades por delante", style = MaterialTheme.typography.titleSmall, color = VynkNavy)
        }
        TextButton(onClick = onExplorar) { Text("Explorar") }
    }
}

@Composable
private fun ConProxima(p: MiProximaActividad, ahora: Long, onVerUbicacion: (String) -> Unit, onAbrir: (String) -> Unit) {
    val a = p.actividad
    val enCurso = p.enCurso(ahora)
    val pronto = empiezaPronto(p.empiezaEn, ahora)
    val estado = estadoMiProxima(p, ahora)
    val lugar = a.ubicacionDetalle.lugar.ifBlank { a.ubicacion.ifBlank { UbicacionActividad.POR_CONFIRMAR } }
    Column(
        Modifier.presionable { onAbrir(a.id) }.padding(18.dp)
            .semantics { contentDescription = "Mi próxima actividad: ${a.nombre}, en $lugar. $estado" },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("MI PRÓXIMA ACTIVIDAD", style = MaterialTheme.typography.labelSmall, color = VynkBlue)
            Spacer(Modifier.weight(1f))
            val (fondo, color) = when {
                enCurso -> VynkOkSoft to VynkOk
                pronto -> VynkWarnSoft to VynkWarn
                else -> VynkBlueSoft to VynkBlue
            }
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(fondo).padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (enCurso) PuntoVivo(VynkOk)
                Text(estado, style = MaterialTheme.typography.labelMedium, color = color)
            }
        }
        Text(a.nombre, style = MaterialTheme.typography.titleLarge, color = VynkNavy, maxLines = 2, overflow = TextOverflow.Ellipsis)

        // El lugar es lo importante: grande y con su referencia.
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(VynkBlueSoft.copy(alpha = 0.6f)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconoDegradado(Icons.Filled.Place, VynkBlue, tamano = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(lugar, style = MaterialTheme.typography.titleMedium, color = VynkNavy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val detalle = listOf(a.ubicacionDetalle.pabellon, a.ubicacionDetalle.aula, a.ubicacionReferencia)
                    .filter { it.isNotBlank() }.joinToString(" · ")
                Text(detalle.ifBlank { "${rangoHoras(a.horaInicio, a.horaFin)} · ${nombreDia(a.fecha)}" },
                    style = MaterialTheme.typography.bodySmall, color = VynkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Schedule, contentDescription = null, tint = VynkMuted, modifier = Modifier.size(16.dp))
            Text("  ${rangoHoras(a.horaInicio, a.horaFin)}", style = MaterialTheme.typography.bodySmall, color = VynkNavy,
                modifier = Modifier.weight(1f))
            Button(
                onClick = { onVerUbicacion(a.id) },
                colors = ButtonDefaults.buttonColors(containerColor = VynkNavy),
                shape = RoundedCornerShape(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Filled.DirectionsWalk, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(" Cómo llegar", style = MaterialTheme.typography.labelLarge)
            }
        }

        p.despues?.let { d ->
            HorizontalDivider(color = VynkLine)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = VynkMuted, modifier = Modifier.size(15.dp))
                Text(
                    "Después: ${d.nombre} · ${d.horaInicio}" + if (d.ubicacion.isNotBlank()) " · ${d.ubicacion}" else "",
                    style = MaterialTheme.typography.bodySmall, color = VynkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
