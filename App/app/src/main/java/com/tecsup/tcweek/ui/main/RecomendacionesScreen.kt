package com.tecsup.tcweek.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.*
import kotlinx.coroutines.CancellationException

private val MarinoRec = Color(0xFF0C2855)
private val AzulRec = Color(0xFF145FC4)
@Composable
fun RecomendacionesScreen() {
    val context = LocalContext.current
    var datos by remember { mutableStateOf<RespuestaRecomendaciones?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var version by remember { mutableIntStateOf(0) }
    LaunchedEffect(version) {
        cargando = true; error = null
        try { val token = SesionGuardada(context).leer()?.llave ?: throw IllegalStateException("Inicia sesión para ver recomendaciones.")
            datos = RecomendacionesApi.cargar(token)
        } catch(e: CancellationException) { throw e }
        catch(e: Exception) { error = e.message ?: "No fue posible cargar las recomendaciones." }
        finally { cargando = false }
    }
    LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFFF3F6FC)), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MarinoRec), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("VYNK  ·  PARA TI", color = Color(0xFF9FCCFF), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("Descubre actividades para ti", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text("Actividades reales seleccionadas según tus intereses. Gemini se usa cuando está disponible.", color = Color(0xFFD7E6FF))
                    OutlinedButton(onClick = { version++ }, enabled = !cargando) { Text("Actualizar recomendaciones", color = Color.White) }
                }
            }
        }
        if(cargando) item { Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AzulRec) } }
        else if(error != null) item { Text(error.orEmpty(), color = Color(0xFFB42318)); Button(onClick = { version++ }) { Text("Reintentar") } }
        else {
            item { Text(if(datos?.origen == "ia") "✦ Recomendaciones generadas con Gemini" else "Sugerencias automáticas · SIN IA", color = AzulRec, fontWeight = FontWeight.Bold)
                Text(datos?.aviso.orEmpty(), color = Color(0xFF586A83)) }
            if(datos?.actividades.isNullOrEmpty()) item { Card { Text("No hay recomendaciones disponibles. Selecciona tus intereses o espera nuevas actividades.", modifier = Modifier.padding(22.dp)) } }
            itemsIndexed(datos?.actividades.orEmpty()) { index, recomendacion ->
                val a = recomendacion.actividad
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("#${index+1}  ·  ${a.categoriaId.ifBlank { "Actividad VYNK" }}", color = AzulRec, fontWeight = FontWeight.Bold)
                        Text(a.nombre, style = MaterialTheme.typography.titleLarge, color = MarinoRec, fontWeight = FontWeight.Bold)
                        if(a.descripcion.isNotBlank()) Text(a.descripcion, color = Color(0xFF586A83))
                        Text("📅 ${a.fecha}    🕒 ${a.horaInicio} – ${a.horaFin}", color = Color(0xFF475C79))
                        Surface(color = Color(0xFFEAF3FF), shape = RoundedCornerShape(12.dp)) {
                            Column(Modifier.padding(14.dp)) { Text("✦ ¿Por qué para ti?", color = AzulRec, fontWeight = FontWeight.Bold)
                                Text(recomendacion.motivo, color = MarinoRec) }
                        }
                    }
                }
            }
        }
    }
}
