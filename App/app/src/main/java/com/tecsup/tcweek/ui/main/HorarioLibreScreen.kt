package com.tecsup.tcweek.ui.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.BloqueHorario
import com.tecsup.tcweek.data.DisponibilidadApi
import com.tecsup.tcweek.data.SesionGuardada
import com.tecsup.tcweek.data.SugerenciasLibres
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val azulHueco = Color(0xFF165BBB)
private val marinoHueco = Color(0xFF102A56)
private val diasHueco = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

@Composable
fun HorarioLibreScreen(onVolver: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val token = remember { SesionGuardada(context).leer()?.llave.orEmpty() }
    var bloques by remember { mutableStateOf<List<BloqueHorario>>(emptyList()) }
    var confirmado by remember { mutableStateOf(false) }
    var pendiente by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(true) }
    var operando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var avisos by remember { mutableStateOf<List<String>>(emptyList()) }
    var resultado by remember { mutableStateOf<SugerenciasLibres?>(null) }
    var recarga by remember { mutableIntStateOf(0) }

    LaunchedEffect(token) {
        try {
            if (token.isBlank()) throw IllegalStateException("Inicia sesión para usar tu horario.")
            val r = DisponibilidadApi.obtener(token)
            bloques = r.bloques
            confirmado = r.confirmado
        } catch (e: CancellationException) { throw e }
          catch (e: Exception) { error = e.message.orEmpty() }
        finally { cargando = false }
    }
    LaunchedEffect(token, confirmado, pendiente, recarga) {
        if (confirmado && !pendiente && token.isNotBlank()) {
            try { resultado = DisponibilidadApi.sugerencias(token) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message.orEmpty() }
        }
    }
    val selector = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            operando = true; error = ""; avisos = emptyList()
            try {
                val r = DisponibilidadApi.interpretar(context, token, uri)
                bloques = r.bloques; avisos = r.avisos; pendiente = true; resultado = null
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message.orEmpty() }
            finally { operando = false }
        }
    }
    fun cambiar(index: Int, bloque: BloqueHorario) {
        bloques = bloques.toMutableList().also { it[index] = bloque }
        pendiente = true
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF4F7FD))) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(12.dp)) {
            IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
            Column(Modifier.weight(1f)) {
                Text("Mis horas libres", color = marinoHueco, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                Text("Horario leído con Gemini · revisa y confirma", style = MaterialTheme.typography.bodySmall)
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = marinoHueco), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = Color(0xFFB4D6FF))
                        Text("Dale espacio a tus intereses", fontWeight = FontWeight.ExtraBold, color = Color.White, style = MaterialTheme.typography.titleLarge)
                        Text("Sube una foto de tus clases, corrige los datos y descubre actividades que caben entre tus ocupaciones.", color = Color(0xFFDCEBFF))
                    }
                }
            }
            item {
                Button(onClick = { selector.launch("image/*") }, enabled = !operando && !cargando, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = azulHueco)) {
                    Icon(Icons.Filled.CloudUpload, null)
                    Text(if (operando) "  Leyendo imagen…" else "  Subir foto del horario")
                }
                Text("JPG, PNG o WEBP · Máximo 5 MB · La foto no se almacena.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                if (cargando || operando) CircularProgressIndicator(color = azulHueco)
                if (error.isNotBlank()) Text(error, color = Color(0xFFAF233D))
                avisos.forEach { Text("⚠ $it", color = Color(0xFF986313)) }
            }
            item {
                Text("Horario detectado", color = marinoHueco, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                Text(if (pendiente) "Corrige los bloques y confirma los cambios." else if (confirmado) "Horario confirmado" else "Todavía no has confirmado tu horario.", color = Color(0xFF637692))
            }
            items(bloques.size) { i ->
                val b = bloques[i]
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFD9E5F6))) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        var expanded by remember { mutableStateOf(false) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                OutlinedButton(onClick = { expanded = true }) { Text(diasHueco.getOrElse(b.dia - 1) { "Día" }) }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    diasHueco.forEachIndexed { idx, day -> DropdownMenuItem(text = { Text(day) }, onClick = {
                                        cambiar(i, b.copy(dia = idx + 1)); expanded = false
                                    }) }
                                }
                            }
                            OutlinedButton(onClick = { bloques = bloques.filterIndexed { index, _ -> index != i }; pendiente = true }) { Text("Eliminar") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(b.inicio, { cambiar(i, b.copy(inicio = it)) }, label = { Text("Inicio HH:mm") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(b.fin, { cambiar(i, b.copy(fin = it)) }, label = { Text("Fin HH:mm") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        OutlinedTextField(b.nombre, { cambiar(i, b.copy(nombre = it.take(80))) }, label = { Text("Curso / ocupación") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                }
            }
            item {
                OutlinedButton(onClick = { bloques = bloques + BloqueHorario(1, "08:00", "09:00", "Clase"); pendiente = true }, modifier = Modifier.fillMaxWidth()) { Text("+ Añadir bloque que falta") }
                Button(onClick = {
                    scope.launch {
                        operando = true; error = ""
                        try {
                            val guardado = DisponibilidadApi.guardar(token, bloques)
                            bloques = guardado.bloques; confirmado = guardado.confirmado; pendiente = false; recarga++
                        } catch (e: CancellationException) { throw e }
                          catch (e: Exception) { error = e.message.orEmpty() }
                        finally { operando = false }
                    }
                }, enabled = !operando && !cargando, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = azulHueco)) { Text("Confirmar y guardar horario") }
                Text("Verifica los días y las horas. La IA puede interpretar mal la foto.", color = Color(0xFF667793), style = MaterialTheme.typography.bodySmall)
            }
            item {
                Text("✦ Sugerencias en tus huecos", color = marinoHueco, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                if (!confirmado || pendiente) Text("Confirma tu horario para recibir recomendaciones.")
                else {
                    OutlinedButton(onClick = { recarga++ }) { Text("Actualizar recomendaciones") }
                    Text(resultado?.aviso ?: "Buscando actividades compatibles…", color = Color(0xFF65718E))
                    Text(if (resultado?.origen == "ia") "Priorizadas con Gemini" else "Sugerencias por reglas, sin IA", color = azulHueco)
                }
            }
            if (confirmado && !pendiente && resultado != null) {
                if (resultado!!.actividades.isEmpty()) item { Text("Por ahora no hay actividades compatibles con tus huecos.") }
                items(resultado!!.actividades, key = { it.id }) { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFDBE7F7))) {
                        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(item.nombre, fontWeight = FontWeight.ExtraBold, color = marinoHueco)
                            Text("${item.fecha} · ${item.inicio} – ${item.fin}", color = azulHueco)
                            Text("Hueco libre: ${item.huecoInicio} – ${item.huecoFin}", color = Color(0xFF1E6D51))
                            if (item.motivo.isNotBlank()) Text(item.motivo, color = Color(0xFF506784))
                        }
                    }
                }
            }
            item { Text("Las sugerencias no realizan inscripciones automáticamente.", style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
        }
    }
}
