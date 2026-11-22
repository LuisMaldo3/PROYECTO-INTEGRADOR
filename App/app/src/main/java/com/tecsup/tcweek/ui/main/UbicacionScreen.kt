package com.tecsup.tcweek.ui.main

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.ActividadCatalogo
import com.tecsup.tcweek.data.AuthRepository
import com.tecsup.tcweek.data.CatalogoApi
import com.tecsup.tcweek.data.ProgramacionApi
import com.tecsup.tcweek.data.UbicacionActividad
import com.tecsup.tcweek.ui.comun.Tarjeta
import com.tecsup.tcweek.ui.theme.VynkBackground
import com.tecsup.tcweek.ui.theme.VynkBlue
import com.tecsup.tcweek.ui.theme.VynkBlueSoft
import com.tecsup.tcweek.ui.theme.VynkLine
import com.tecsup.tcweek.ui.theme.VynkMuted
import com.tecsup.tcweek.ui.theme.VynkNavy
import com.tecsup.tcweek.ui.theme.VynkWarn
import com.tecsup.tcweek.ui.theme.VynkWarnSoft
import kotlinx.coroutines.CancellationException

/**
 * US-71 T03: dónde es una actividad. Muestra el lugar con su sede, pabellón, aula, dirección y referencia (solo lo
 * que la organización cargó), el botón «Abrir en el mapa» si el lugar tiene coordenadas reales, y las otras
 * actividades del mismo día en ese lugar.
 */
@Composable
fun UbicacionScreen(actividadId: String, onVolver: () -> Unit) {
    val context = LocalContext.current
    val token = remember { AuthRepository(context).currentSession()?.llave.orEmpty() }
    var actividad by remember { mutableStateOf<ActividadCatalogo?>(null) }
    var mismoLugar by remember { mutableStateOf<List<ActividadCatalogo>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }

    LaunchedEffect(actividadId, recarga) {
        error = null
        try {
            val a = CatalogoApi.detalle(token, actividadId)
            actividad = a
            // Lo demás que pasa ese día en el mismo lugar (si falla, la ubicación se muestra igual).
            mismoLugar = try {
                ProgramacionApi.cargar(token, a.fecha).actividades.map { it.actividad }
                    .filter { it.id != a.id && it.ubicacion == a.ubicacion && a.ubicacionDetalle.confirmada }
            } catch (e: CancellationException) { throw e } catch (e: Exception) { emptyList() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "No se pudo abrir la ubicación."
        }
    }

    Column(Modifier.fillMaxSize().background(VynkBackground)) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
            Text("Ubicación", style = MaterialTheme.typography.headlineSmall, color = VynkNavy)
        }
        val a = actividad
        when {
            error != null -> EstadoVacio(Icons.Filled.ErrorOutline, "No pudimos abrir la ubicación", error.orEmpty(), esError = true) {
                OutlinedButton(onClick = { recarga++ }) { Text("Reintentar") }
            }
            a == null -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VynkBlue)
            }
            else -> FichaUbicacion(a, mismoLugar)
        }
    }
}

@Composable
private fun FichaUbicacion(a: ActividadCatalogo, mismoLugar: List<ActividadCatalogo>) {
    val context = LocalContext.current
    val u = a.ubicacionDetalle
    var sinMapa by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Tarjeta {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(VynkBlueSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Place, contentDescription = null, tint = VynkBlue)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(u.lugar.ifBlank { a.ubicacion.ifBlank { UbicacionActividad.POR_CONFIRMAR } },
                            style = MaterialTheme.typography.titleLarge, color = VynkNavy)
                        Text("Lugar de la actividad", style = MaterialTheme.typography.bodySmall, color = VynkMuted)
                    }
                }
                if (!u.confirmada) Text(
                    "La organización todavía no confirmó el lugar. Te avisaremos si cambia.",
                    style = MaterialTheme.typography.bodySmall, color = VynkWarn,
                    modifier = Modifier.fillMaxWidth().background(VynkWarnSoft).padding(10.dp)
                )
                u.filas().forEach { (etiqueta, valor) ->
                    HorizontalDivider(color = VynkLine)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(etiqueta, style = MaterialTheme.typography.labelLarge, color = VynkMuted, modifier = Modifier.weight(0.35f))
                        Text(valor, style = MaterialTheme.typography.bodyMedium, color = VynkNavy, modifier = Modifier.weight(0.65f))
                    }
                }
                val enlace = u.enlaceMapa()
                if (enlace != null) Button(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(enlace)))
                        } catch (e: ActivityNotFoundException) {
                            sinMapa = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VynkNavy), modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Abrir en el mapa")
                }
                else if (u.confirmada) Text(
                    "Este lugar no tiene un punto en el mapa: guíate por la referencia.",
                    style = MaterialTheme.typography.bodySmall, color = VynkMuted
                )
                if (sinMapa) Text("No hay una aplicación de mapas instalada.", style = MaterialTheme.typography.bodySmall, color = VynkWarn)
            }
        }

        Tarjeta {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(a.nombre, style = MaterialTheme.typography.titleMedium, color = VynkNavy)
                DatoConIcono(Icons.Filled.Schedule, "${nombreDia(a.fecha)} · ${rangoHoras(a.horaInicio, a.horaFin)}")
            }
        }

        if (mismoLugar.isNotEmpty()) {
            Text("También ese día en este lugar", style = MaterialTheme.typography.titleSmall, color = VynkNavy)
            mismoLugar.forEach { otra ->
                Tarjeta {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(rangoHoras(otra.horaInicio, otra.horaFin), style = MaterialTheme.typography.labelLarge, color = VynkBlue)
                        Text(otra.nombre, style = MaterialTheme.typography.bodyMedium, color = VynkNavy)
                    }
                }
            }
        }
    }
}
