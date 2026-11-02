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
import com.tecsup.tcweek.data.ResultadosCompetenciasApi
import com.tecsup.tcweek.data.ResultadoOficial
import com.tecsup.tcweek.data.ActividadCatalogo
import com.tecsup.tcweek.data.CatalogoApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.tecsup.tcweek.R

/** Sprint 4: clasificación de las cinco áreas desde puntajes oficiales publicados. */
@Composable
fun ClasificacionScreen(onVolver: () -> Unit) {
    val contexto = LocalContext.current
    val token = remember { AuthRepository(contexto).currentSession()?.llave.orEmpty() }
    var tabla by remember { mutableStateOf<TablaTcw?>(null) }
    var error by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(true) }
    var version by remember { mutableIntStateOf(0) }
    var resultados by remember { mutableStateOf<List<ResultadoOficial>>(emptyList()) }
    var errorResultados by remember { mutableStateOf("") }
    var seleccionado by remember { mutableStateOf<ResultadoOficial?>(null) }
    var otraActividad by remember { mutableStateOf<ActividadCatalogo?>(null) }
    var similares by remember { mutableStateOf<List<ActividadCatalogo>>(emptyList()) }
    var cargandoRelacionadas by remember { mutableStateOf(false) }
    LaunchedEffect(token, version) {
        errorResultados = ""
        try { resultados = ResultadosCompetenciasApi.listar(token) }
        catch (e: Exception) { errorResultados = e.message ?: "No se encontraron resultados oficiales." }
    }
    LaunchedEffect(seleccionado?.actividadId, token) {
        similares = emptyList()
        if (seleccionado != null) {
            cargandoRelacionadas = true
            try { similares = CatalogoApi.similares(token, seleccionado!!.actividadId) }
            catch (_: Exception) { /* Un error en recomendaciones no oculta resultados oficiales. */ }
            finally { cargandoRelacionadas = false }
        }
    }
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
            HorizontalDivider(color = Color(0xFFDCE6F1))
            Text("Resultados por competencia", color = Color(0xFF11385C),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            if (errorResultados.isNotBlank()) Text(errorResultados, color = Color(0xFF905261))
            if (resultados.isEmpty() && errorResultados.isBlank())
                Text("Aún no se han publicado ganadores oficiales.", color = Color(0xFF60788F))
            resultados.forEach { resultado ->
                Surface(onClick = { seleccionado = resultado }, color = Color.White,
                    shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color(0xFFD9E7F3))) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Color(0xFF1E74A8))
                            Text(resultado.nombre, color = Color(0xFF133A62), fontWeight = FontWeight.Bold)
                        }
                        resultado.posiciones.firstOrNull { it.puesto == 1 }?.let { ganador ->
                            Text("🏆 Ganador: ${ganador.nombre}", color = Color(0xFF19618B))
                        }
                        Text("Ver detalle, posiciones e imagen →", color = Color(0xFF1673B7),
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            HorizontalDivider(color = Color(0xFFDCE6F1))
            Text("Clasificación por áreas", color = Color(0xFF11385C),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
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
    seleccionado?.let { resultado ->
        AlertDialog(
            onDismissRequest = { seleccionado = null },
            title = { Text(resultado.nombre, color = Color(0xFF0F355C), fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    if (resultado.portadaUrl.isNotBlank())
                        ImagenPortadaCatalogo(resultado.portadaUrl, resultado.nombre)
                    else
                        Image(painter = painterResource(R.drawable.vynk_actividades),
                            contentDescription = "Ilustración de la competencia ${resultado.nombre}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(160.dp))
                    Text("✓ Resultado oficial validado por administración", color = Color(0xFF176B55),
                        fontWeight = FontWeight.Bold)
                    resultado.posiciones.forEach { posicion ->
                        Surface(color = if (posicion.puesto == 1) Color(0xFFE7F3FC) else Color(0xFFF6F9FC),
                            shape = RoundedCornerShape(10.dp)) {
                            Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("${posicion.puesto}.º · ${posicion.nombre}", fontWeight = FontWeight.Bold,
                                    color = Color(0xFF163B64))
                                Text("${posicion.area.ifBlank { "Área por confirmar" }} · ${posicion.puntos} puntos",
                                    color = Color(0xFF52748C))
                            }
                        }
                    }
                    Text("Portada de la competencia; no es una fotografía personal del ganador.",
                        color = Color(0xFF627D96), style = MaterialTheme.typography.labelSmall)
                    HorizontalDivider(color = Color(0xFFDCE8F3))
                    Text("Actividades similares", color = Color(0xFF173F68), fontWeight = FontWeight.Bold)
                    if (cargandoRelacionadas) CircularProgressIndicator(modifier = Modifier.size(21.dp))
                    if (!cargandoRelacionadas && similares.isEmpty())
                        Text("No hay actividades similares abiertas en este momento.", color = Color(0xFF687E93))
                    similares.forEach { actividad ->
                        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                            otraActividad = actividad
                            seleccionado = null
                        }) { Text("${actividad.nombre} →", maxLines = 2) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { seleccionado = null }) { Text("Cerrar") } }
        )
    }
    otraActividad?.let { actividad ->
        DialogoActividad(actividad, token, alCerrar = { otraActividad = null },
            onVerActividad = { otraActividad = it })
    }
}
