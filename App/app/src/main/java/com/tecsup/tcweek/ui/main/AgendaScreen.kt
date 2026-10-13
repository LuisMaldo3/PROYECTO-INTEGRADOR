package com.tecsup.tcweek.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.tecsup.tcweek.data.AgendaApi
import com.tecsup.tcweek.data.ActividadAgenda
import com.tecsup.tcweek.data.SesionGuardada
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Navy = Color(0xFF142C5B)
private val Blue = Color(0xFF1464D8)

@Composable
fun AgendaScreen() {
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<ActividadAgenda>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var upcoming by remember { mutableStateOf(false) }
    LaunchedEffect(reload) {
        loading = true; error = null
        try {
            val token = SesionGuardada(context).leer()?.llave ?: throw IllegalStateException("Inicia sesión para consultar tu agenda.")
            items = AgendaApi.cargar(token)
        } catch (e: Exception) { error = e.message ?: "No se pudo cargar la agenda." }
        finally { loading = false }
    }
    val filtered = if (upcoming) items.filter { it.fecha >= LocalDate.now().toString() } else items
    LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F8FD)), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Navy, Blue)), RoundedCornerShape(24.dp)).padding(24.dp)) {
                Text("TU ESPACIO · VYNK", color = Color(0xFFB9D9FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("Mi agenda.", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("Organiza tu semana y disfruta cada actividad.", color = Color(0xFFE0ECFF), fontSize = 13.sp)
                Spacer(Modifier.height(18.dp))
                Text("${items.size} actividades inscritas", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Mis actividades", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Navy, modifier = Modifier.weight(1f))
                FilterChip(selected = upcoming, onClick = { upcoming = !upcoming }, label = { Text(if (upcoming) "Próximas" else "Todas") })
            }
        }
        if (loading) item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        else if (error != null) item { Column { Text(error.orEmpty(), color = Color(0xFFBD3344)); TextButton(onClick = { reload++ }) { Text("Reintentar") } } }
        else if (filtered.isEmpty()) item { Text("Todavía no tienes actividades para mostrar. Inscríbete y aparecerán aquí.", color = Color.Gray) }
        else {
            filtered.groupBy { it.fecha }.forEach { (date, dayItems) ->
                item(key = "day-$date") {
                    val label = runCatching { LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "PE"))) }.getOrDefault(date)
                    Text(label.replaceFirstChar { it.uppercase() }, fontSize = 15.sp, color = Blue, fontWeight = FontWeight.Bold)
                }
                items(dayItems, key = { it.id.ifBlank { it.nombre + it.fecha } }) { activity -> AgendaCard(activity) }
            }
        }
    }
}

@Composable
private fun AgendaCard(a: ActividadAgenda) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(a.categoria.uppercase(), fontSize = 11.sp, color = Blue, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(color = if (a.estado == "cancelada") Color(0xFFFFECEF) else Color(0xFFE6F8EE), shape = RoundedCornerShape(50)) {
                    Text(if (a.estado == "cancelada") "Cancelada" else "Inscrito", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 11.sp, color = if (a.estado == "cancelada") Color(0xFFB63449) else Color(0xFF15875E))
                }
            }
            Text(a.nombre, color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("◷  ${a.inicio.ifBlank { "Por confirmar" }}${if (a.fin.isNotBlank()) " – ${a.fin}" else ""}", color = Color(0xFF526785), fontSize = 13.sp)
            Text("⌖  ${a.ubicacion}", color = Color(0xFF526785), fontSize = 13.sp)
        }
    }
}
