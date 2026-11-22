package com.tecsup.tcweek.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.ActividadProxima
import com.tecsup.tcweek.data.AvisoPendiente
import com.tecsup.tcweek.data.CambiosAgenda
import com.tecsup.tcweek.data.ProximasApi
import com.tecsup.tcweek.data.TICK_PROXIMAS_MS
import com.tecsup.tcweek.data.debeRecargarProximas
import com.tecsup.tcweek.data.empiezaPronto
import com.tecsup.tcweek.data.textoFalta
import com.tecsup.tcweek.data.vigentes
import com.tecsup.tcweek.ui.comun.Pildora
import com.tecsup.tcweek.ui.comun.presionable
import com.tecsup.tcweek.ui.comun.sombraSuave
import com.tecsup.tcweek.ui.theme.VynkBlue
import com.tecsup.tcweek.ui.theme.VynkBlueSoft
import com.tecsup.tcweek.ui.theme.VynkLine
import com.tecsup.tcweek.ui.theme.VynkMuted
import com.tecsup.tcweek.ui.theme.VynkNavy
import com.tecsup.tcweek.ui.theme.VynkSky
import com.tecsup.tcweek.ui.theme.VynkWarn
import com.tecsup.tcweek.ui.theme.VynkWarnSoft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * US-72 T03: tarjeta «Próximamente» del Inicio: la actividad que empieza antes (con cuánto falta) y las dos siguientes.
 *
 * T02 (carga programada): pide la lista al entrar y cada 5 minutos, o antes si la primera ya empezó o llegó una
 * inscripción, cancelación o cambio. Entre recargas, el «Empieza en N min» se actualiza cada 30 s con el reloj del
 * celular, sin gastar datos. Al salir del Inicio la carga se detiene sola.
 */
@Composable
fun TarjetaProximamente(token: String, omitir: String? = null) {
    var lista by remember { mutableStateOf<List<ActividadProxima>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var reintento by remember { mutableIntStateOf(0) }
    val cambios by CambiosAgenda.version.collectAsState()

    LaunchedEffect(token, cambios, reintento) {
        var ultimaCarga = 0L
        while (true) {
            ahora = System.currentTimeMillis()
            if (debeRecargarProximas(lista.orEmpty(), ultimaCarga, ahora)) {
                try {
                    lista = ProximasApi.cargar(token, 4)
                    error = null
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Se conserva lo último que se mostró; si nunca cargó, se muestra el error.
                    if (lista == null) error = e.message ?: "No se pudieron cargar las próximas actividades."
                }
                ultimaCarga = ahora
            }
            delay(TICK_PROXIMAS_MS)
        }
    }

    // La que ya se muestra en «Mi próxima actividad» no se repite aquí.
    val visibles = vigentes(lista.orEmpty(), ahora).filter { it.actividad.id != omitir }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.AccessTime, contentDescription = null, tint = VynkSky, modifier = Modifier.size(18.dp))
            Text("Próximamente", style = MaterialTheme.typography.titleMedium, color = VynkNavy)
        }
        when {
            lista == null && error == null -> Text("Buscando lo que empieza pronto…", style = MaterialTheme.typography.bodySmall, color = VynkMuted)
            error != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(error.orEmpty(), style = MaterialTheme.typography.bodySmall, color = VynkMuted, modifier = Modifier.weight(1f))
                TextButton(onClick = { reintento++ }) { Text("Reintentar") }
            }
            visibles.isEmpty() -> Text("No hay actividades por empezar por ahora.", style = MaterialTheme.typography.bodySmall, color = VynkMuted)
            else -> {
                Principal(visibles.first(), ahora)
                visibles.drop(1).take(2).forEach { Siguiente(it, ahora) }
            }
        }
    }
}

/** La que empieza antes, en grande. Tocarla abre su detalle (y desde ahí, la inscripción). */
@Composable
private fun Principal(p: ActividadProxima, ahora: Long) {
    val a = p.actividad
    val pronto = empiezaPronto(p.empiezaEn, ahora)
    val falta = textoFalta(p.empiezaEn, ahora)
    Card(
        colors = CardDefaults.cardColors(containerColor = VynkNavy),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().sombraSuave(RoundedCornerShape(16.dp), 10.dp).presionable { AvisoPendiente.abrir(a.id) }
            .semantics { contentDescription = "Próxima actividad: ${a.nombre}. $falta" }
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pildora(falta, if (pronto) VynkWarnSoft else Color.White.copy(alpha = 0.15f), if (pronto) VynkWarn else Color.White)
                Spacer(Modifier.weight(1f))
                if (p.inscrito) Pildora("Inscrito", VynkBlueSoft, VynkBlue)
            }
            Text(a.nombre, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Dato(Icons.Filled.Schedule, rangoHoras(a.horaInicio, a.horaFin))
                if (a.ubicacion.isNotBlank()) Dato(Icons.Filled.Place, a.ubicacion)
            }
        }
    }
}

@Composable
private fun Dato(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icono, contentDescription = null, tint = VynkSky, modifier = Modifier.size(15.dp))
        Text(texto, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Las siguientes, en una línea cada una. */
@Composable
private fun Siguiente(p: ActividadProxima, ahora: Long) {
    val a = p.actividad
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().sombraSuave(RoundedCornerShape(16.dp), 4.dp).presionable { AvisoPendiente.abrir(a.id) }
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(a.horaInicio.ifBlank { "--:--" }, style = MaterialTheme.typography.labelLarge, color = VynkBlue)
            Column(Modifier.weight(1f)) {
                Text(a.nombre, style = MaterialTheme.typography.bodyMedium, color = VynkNavy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(textoFalta(p.empiezaEn, ahora), style = MaterialTheme.typography.labelSmall, color = VynkMuted)
            }
            if (p.inscrito) Pildora("Inscrito", VynkBlueSoft, VynkBlue)
        }
    }
}
