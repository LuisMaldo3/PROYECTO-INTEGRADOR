package com.tecsup.tcweek.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.ActividadSugerida
import com.tecsup.tcweek.data.AgendaSugeridaApi
import com.tecsup.tcweek.data.RespuestaAgendaSugerida
import com.tecsup.tcweek.data.SesionGuardada
import kotlinx.coroutines.CancellationException

private val MarinoAgenda = Color(0xFF112752)
private val AzulAgenda = Color(0xFF0B63FF)
private val GrisAgenda = Color(0xFF5D6C84)

@Composable
fun AgendaSugeridaScreen(onVolver: () -> Unit) {
    val contexto = LocalContext.current
    val sesion = remember { SesionGuardada(contexto) }
    var respuesta by remember { mutableStateOf<RespuestaAgendaSugerida?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var recarga by remember { mutableIntStateOf(0) }

    LaunchedEffect(recarga) {
        cargando = true
        error = null
        try {
            val token = sesion.leer()?.llave ?: throw IllegalStateException("Inicia sesión para consultar tus sugerencias.")
            respuesta = AgendaSugeridaApi.cargar(token)
        } catch (e: CancellationException) { throw e }
          catch (e: Exception) { error = e.message ?: "No se pudo generar tu agenda." }
        finally { cargando = false }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF4F7FD))) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver a mi agenda", tint = MarinoAgenda) }
            Column(Modifier.weight(1f)) {
                Text("Agenda sugerida", fontWeight = FontWeight.ExtraBold, color = MarinoAgenda, style = MaterialTheme.typography.titleLarge)
                Text("Personalizada con tus horarios e intereses", color = GrisAgenda, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { recarga++ }, enabled = !cargando) { Icon(Icons.Filled.Refresh, "Actualizar sugerencias", tint = AzulAgenda) }
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = Color.Transparent) {
                    Box(Modifier.fillMaxWidth().background(
                        Brush.linearGradient(listOf(MarinoAgenda, Color(0xFF166ACC))), RoundedCornerShape(20.dp)
                    ).padding(22.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Icon(Icons.Filled.AutoAwesome, null, tint = Color(0xFFC5E1FF), modifier = Modifier.size(26.dp))
                            Text("Tu próximo plan comienza aquí", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                            Text("Te proponemos actividades reales disponibles sin cruzarlas con tus inscripciones.", color = Color(0xFFDDEBFF), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            if (cargando) item {
                Box(Modifier.fillMaxWidth().padding(34.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AzulAgenda) }
            }
            if (!cargando && error != null) item {
                EstadoVacio(Icons.Filled.Event, "No pudimos crear tu agenda", error.orEmpty(), esError = true) {
                    OutlinedButton(onClick = { recarga++ }) { Text("Reintentar") }
                }
            }
            if (!cargando && error == null) {
                val datos = respuesta
                if (datos != null) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                DatoResumen("${datos.actividades.size}", "sugeridas", Modifier.weight(1f))
                                DatoResumen("${datos.inscripciones}", "inscritas", Modifier.weight(1f))
                            }
                            Surface(shape = RoundedCornerShape(10.dp), color = if (datos.origen == "ia") Color(0xFFE9E7FF) else Color(0xFFE7EDF5)) {
                                Text(etiquetaOrigenAgenda(datos.origen), modifier = Modifier.padding(10.dp),
                                    fontWeight = FontWeight.Bold, color = if (datos.origen == "ia") Color(0xFF483A9A) else GrisAgenda, style = MaterialTheme.typography.labelMedium)
                            }
                            if (datos.aviso.isNotBlank()) Text(datos.aviso, color = GrisAgenda, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (datos.actividades.isEmpty()) item {
                        EstadoVacio(Icons.Filled.Event, "Sin sugerencias compatibles", "Cuando aparezcan nuevas actividades disponibles podrás crear tu propuesta sin cruces.")
                    }
                    items(datos.actividades, key = { it.id }) { actividad -> TarjetaSugerida(actividad) }
                    if (datos.actividades.isNotEmpty()) item {
                        Text("Estas son sugerencias; no se realizan inscripciones automáticamente.", color = GrisAgenda, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun DatoResumen(valor: String, descripcion: String, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp)) {
            Text(valor, color = AzulAgenda, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
            Text(descripcion, color = GrisAgenda, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TarjetaSugerida(item: ActividadSugerida) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, Color(0xFFDCE5F1))
    ) {
        Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Surface(color = Color(0xFFE6F0FF), shape = RoundedCornerShape(8.dp)) {
                Text("SUGERIDA", color = AzulAgenda, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
            }
            Text(item.nombre, color = MarinoAgenda, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Event, null, Modifier.size(17.dp), tint = AzulAgenda)
                Text(item.fecha, style = MaterialTheme.typography.bodySmall, color = GrisAgenda)
                Spacer(Modifier.width(5.dp))
                Icon(Icons.Filled.Schedule, null, Modifier.size(17.dp), tint = AzulAgenda)
                Text("${item.inicio} – ${item.fin}", style = MaterialTheme.typography.bodySmall, color = GrisAgenda)
            }
            if (item.ubicacion.isNotBlank() && item.ubicacion != "null") Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Place, null, Modifier.size(17.dp), tint = AzulAgenda)
                Text(item.ubicacion, style = MaterialTheme.typography.bodySmall, color = GrisAgenda)
            }
            if (item.motivo.isNotBlank()) Text(item.motivo, color = Color(0xFF315D9F), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
