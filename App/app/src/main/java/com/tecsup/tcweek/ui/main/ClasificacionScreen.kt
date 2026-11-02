package com.tecsup.tcweek.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.AuthRepository
import com.tecsup.tcweek.data.ClasificacionApi
import com.tecsup.tcweek.data.TablaTcw

/** Sprint 4: clasificación de las cinco áreas desde puntajes oficiales publicados. */
@Composable
fun ClasificacionScreen(onVolver: () -> Unit) {
    val contexto = LocalContext.current
    val token = remember { AuthRepository(contexto).currentSession()?.llave.orEmpty() }
    var tabla by remember { mutableStateOf<TablaTcw?>(null) }
    var error by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(true) }
    var version by remember { mutableIntStateOf(0) }
    LaunchedEffect(token, version) {
        cargando = true; error = ""
        try { tabla = ClasificacionApi.consultar(token) }
        catch (e: Exception) { error = e.message ?: "No se pudieron cargar los puntos oficiales." }
        finally { cargando = false }
    }
    Column(Modifier.fillMaxSize().background(Color(0xFFF1F5FB))
        .verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton(onClick = onVolver) { Text("← Volver a Inicio") }
        Surface(color = Color(0xFF102C56), shape = RoundedCornerShape(19.dp)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("T&C WEEK · UNA SEDE", color = Color(0xFF9BDEFF), fontWeight = FontWeight.Bold)
                Text("Tabla general", color = Color.White, style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold)
                Text("Cinco áreas. Puntos registrados por organizadores y publicados por administración.",
                    color = Color(0xFFE1EDFF))
            }
        }
        if (cargando) CircularProgressIndicator(color = Color(0xFF1768B8))
        if (error.isNotBlank()) {
            Text(error, color = Color(0xFFB32938))
            Button(onClick = { version++ }) { Text("Volver a intentar") }
        }
        if (!cargando && error.isBlank()) {
            Text("${tabla?.totalCompetencias ?: 0} competencias con resultados oficiales",
                color = Color(0xFF355978))
            tabla?.areas?.forEach { area ->
                Surface(color = Color.White, shape = RoundedCornerShape(13.dp),
                    border = BorderStroke(1.dp, Color(0xFFDCE7F4))) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${area.posicion}.º · ${area.nombre}", modifier = Modifier.weight(1f),
                                color = Color(0xFF12345B), fontWeight = FontWeight.ExtraBold)
                            Text("${area.puntos} pts", color = Color(0xFF0A5D9D), fontWeight = FontWeight.Bold)
                        }
                        Text("Primeros puestos: ${area.victorias}", color = Color(0xFF657D96),
                            style = MaterialTheme.typography.bodySmall)
                        area.desglose.forEach { item ->
                            HorizontalDivider(color = Color(0xFFEDF3F9))
                            Text("${item.nombre} · ${item.puesto}.º · ${item.puntos} puntos",
                                style = MaterialTheme.typography.bodySmall, color = Color(0xFF375C7A))
                        }
                    }
                }
            }
        }
    }
}
