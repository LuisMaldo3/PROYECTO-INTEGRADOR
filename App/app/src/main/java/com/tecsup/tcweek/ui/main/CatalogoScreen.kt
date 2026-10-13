package com.tecsup.tcweek.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.ActividadCatalogo
import com.tecsup.tcweek.data.AuthRepository
import com.tecsup.tcweek.data.CatalogoApi
import androidx.compose.ui.platform.LocalContext

private val Azul = Color(0xFF0B63FF)
private val AzulOscuro = Color(0xFF0A2049)

@Composable
fun CatalogoScreen() {
    val context = LocalContext.current
    val repository = remember(context) { AuthRepository(context) }
    val token = remember { repository.currentSession()?.llave.orEmpty() }
    var actividades by remember { mutableStateOf<List<ActividadCatalogo>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }

    LaunchedEffect(token, recarga) {
        cargando = true
        error = null
        try { actividades = CatalogoApi.cargar(token) }
        catch (e: Exception) { error = e.message ?: "No se pudo cargar el catálogo." }
        finally { cargando = false }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF5F7FC))) {
        Column(Modifier.fillMaxWidth().background(AzulOscuro).padding(20.dp)) {
            Text("VYNK", color = Color(0xFF8CB6FF), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Catálogo de actividades", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Actividades publicadas para T&C Week", color = Color(0xFFD5E2FA), style = MaterialTheme.typography.bodyMedium)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (cargando) "Cargando actividades" else "${actividades.size} actividades disponibles", color = AzulOscuro, style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = { recarga++ }, enabled = !cargando) { Text("Actualizar", color = Azul) }
        }
        when {
            cargando -> Box(Modifier.fillMaxWidth().padding(24.dp)) { CircularProgressIndicator(color = Azul) }
            error != null -> Column(Modifier.padding(20.dp)) {
                Text("No se pudo consultar el catálogo", fontWeight = FontWeight.Bold, color = AzulOscuro)
                Spacer(Modifier.height(6.dp))
                Text(error.orEmpty(), color = Color(0xFF596579))
                TextButton(onClick = { recarga++ }) { Text("Reintentar") }
            }
            actividades.isEmpty() -> Column(Modifier.padding(24.dp)) {
                Text("Todavía no hay actividades publicadas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AzulOscuro)
                Spacer(Modifier.height(8.dp))
                Text("Cuando el administrador publique actividades, aparecerán aquí automáticamente.", color = Color(0xFF596579))
            }
            else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(actividades, key = { it.id }) { actividad -> TarjetaActividad(actividad) }
            }
        }
    }
}

@Composable
private fun TarjetaActividad(a: ActividadCatalogo) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(a.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AzulOscuro)
            if (a.descripcion.isNotBlank() && a.descripcion != "null") Text(a.descripcion, color = Color(0xFF56647A), style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider(color = Color(0xFFE8ECF4))
            Text("Fecha: ${a.fecha.ifBlank { "Por confirmar" }}", color = AzulOscuro)
            Text("Horario: ${a.horaInicio.ifBlank { "--:--" }} – ${a.horaFin.ifBlank { "--:--" }}", color = AzulOscuro)
            if (a.cupos != null) Text(if (a.cupos > 0) "${a.cupos} cupos disponibles" else "Sin cupos disponibles", color = if (a.cupos > 0) Color(0xFF137B58) else Color(0xFFB45309), fontWeight = FontWeight.SemiBold)
            if (a.organizador.isNotBlank() && a.organizador != "null") Text("Organiza: ${a.organizador}", color = Color(0xFF68768B), style = MaterialTheme.typography.bodySmall)
        }
    }
}
