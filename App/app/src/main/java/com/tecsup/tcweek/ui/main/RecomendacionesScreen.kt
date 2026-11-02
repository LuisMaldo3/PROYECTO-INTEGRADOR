package com.tecsup.tcweek.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val MarinoRec = Color(0xFF0C2142)
private val AzulRec = Color(0xFF1769DB)
private val CelesteRec = Color(0xFF55B9F7)
private val FondoRec = Color(0xFFF1F5FB)
private val VerdeRec = Color(0xFF08796A)

@Composable
fun RecomendacionesScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var datos by remember { mutableStateOf<RespuestaRecomendaciones?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var version by remember { mutableIntStateOf(0) }
    var descartandoId by remember { mutableStateOf<String?>(null) }
    var avisoDescarte by remember { mutableStateOf<String?>(null) }
    var errorDescarte by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(version) {
        cargando = true
        error = null
        try {
            val token = SesionGuardada(context).leer()?.llave
                ?: throw IllegalStateException("Inicia sesión para ver recomendaciones.")
            datos = RecomendacionesApi.cargar(token)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "No fue posible cargar las recomendaciones." }
        finally { cargando = false }
    }

    fun descartar(recomendacion: Recomendacion) {
        if (descartandoId != null) return
        val anterior = datos ?: return
        val actividad = recomendacion.actividad
        descartandoId = actividad.id
        avisoDescarte = null
        errorDescarte = null
        datos = anterior.copy(actividades = quitarRecomendacion(anterior.actividades, actividad.id))
        scope.launch {
            try {
                val token = SesionGuardada(context).leer()?.llave
                    ?: throw IllegalStateException("Inicia sesión para guardar tus preferencias.")
                RecomendacionesApi.descartar(token, actividad.id)
                avisoDescarte = "No volveremos a recomendarte «${actividad.nombre}»."
            } catch (e: CancellationException) {
                datos = anterior
                throw e
            } catch (e: Exception) {
                datos = anterior
                errorDescarte = e.message ?: "No se pudo registrar tu preferencia."
            } finally { descartandoId = null }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(FondoRec),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item {
            Surface(shape = RoundedCornerShape(22.dp), shadowElevation = 5.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(Brush.linearGradient(listOf(MarinoRec, Color(0xFF1451A3), AzulRec)))
                        .padding(23.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("DESCUBRE · VYNK", style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF9EDCFF), fontWeight = FontWeight.ExtraBold)
                    Text("Experiencias que van contigo", style = MaterialTheme.typography.headlineSmall,
                        color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text("Actividades seleccionadas según tus intereses y tu disponibilidad real.",
                        style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE0EDFF))
                    OutlinedButton(
                        onClick = { version++ }, enabled = !cargando && descartandoId == null,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF8DC5F6)),
                        shape = RoundedCornerShape(11.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Actualizar sugerencias")
                    }
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(14.dp), color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFDDE7F3))) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(color = Color(0xFFE7F3FF), shape = RoundedCornerShape(9.dp)) {
                        Text("PARA TI", modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelSmall, color = AzulRec, fontWeight = FontWeight.Bold)
                    }
                    Text("Tú decides qué quieres descubrir.", color = MarinoRec,
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
        }
        avisoDescarte?.let { mensaje -> item {
            Surface(color = Color(0xFFE4F7F0), shape = RoundedCornerShape(12.dp)) {
                Text(mensaje, modifier = Modifier.padding(14.dp), color = VerdeRec)
            }
        } }
        errorDescarte?.let { mensaje -> item {
            Surface(color = Color(0xFFFFEFEF), shape = RoundedCornerShape(12.dp)) {
                Text(mensaje, modifier = Modifier.padding(14.dp), color = Color(0xFFAD2838))
            }
        } }

        if (cargando) item {
            Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AzulRec)
            }
        } else if (error != null) item {
            Surface(color = Color.White, shape = RoundedCornerShape(15.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(error.orEmpty(), color = Color(0xFFAE2B3C))
                    Button(onClick = { version++ }) { Text("Reintentar") }
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(if (datos?.origen == "ia") "Sugerencias personalizadas" else "Actividades compatibles",
                        color = MarinoRec, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                    Text(datos?.aviso.orEmpty(), color = Color(0xFF677A91), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (datos?.actividades.isNullOrEmpty()) item {
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFDCE7F4))) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No hay más actividades por ahora", color = MarinoRec,
                            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(if (datos?.origen == "sin_horario")
                            "Primero confirma tu horario desde Mi agenda → Mis horas libres."
                            else "Las actividades que descartas se excluyen también en tus próximas recomendaciones.",
                            color = Color(0xFF5D718A))
                    }
                }
            }
            itemsIndexed(datos?.actividades.orEmpty(), key = { _, r -> r.actividad.id }) { index, recomendacion ->
                val a = recomendacion.actividad
                Surface(
                    shape = RoundedCornerShape(19.dp), color = Color.White,
                    shadowElevation = 2.dp, border = BorderStroke(1.dp, Color(0xFFDEE8F5))
                ) {
                    Column(Modifier.fillMaxWidth().padding(19.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(color = Color(0xFFE8F2FF), shape = RoundedCornerShape(9.dp)) {
                                Text("PARA TI  ·  ${index + 1}", Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                    style = MaterialTheme.typography.labelSmall, color = AzulRec, fontWeight = FontWeight.ExtraBold)
                            }
                            Spacer(Modifier.weight(1f))
                            Text(a.categoriaId.ifBlank { "VYNK" }, color = Color(0xFF63768D),
                                style = MaterialTheme.typography.labelSmall)
                        }
                        Text(a.nombre, style = MaterialTheme.typography.titleLarge,
                            color = MarinoRec, fontWeight = FontWeight.ExtraBold)
                        if (a.descripcion.isNotBlank()) Text(a.descripcion,
                            style = MaterialTheme.typography.bodyMedium, color = Color(0xFF526781))
                        Surface(color = Color(0xFFF3F7FD), shape = RoundedCornerShape(11.dp)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("Fecha  ${a.fecha}", color = MarinoRec, style = MaterialTheme.typography.bodySmall)
                                Text("Horario  ${a.horaInicio} – ${a.horaFin}", color = MarinoRec,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (recomendacion.horario.mostrarEtiqueta()) {
                            Surface(color = Color(0xFFE7F7F0), shape = RoundedCornerShape(11.dp)) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text("Compatible con tu horario", color = VerdeRec, fontWeight = FontWeight.Bold)
                                    Text("Libre: ${recomendacion.horario.huecoInicio} – ${recomendacion.horario.huecoFin}",
                                        color = Color(0xFF246F5E), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        Surface(color = Color(0xFFEAF3FF), shape = RoundedCornerShape(11.dp)) {
                            Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("Por qué te la recomendamos", color = AzulRec, fontWeight = FontWeight.Bold)
                                Text(recomendacion.motivo, color = MarinoRec, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        HorizontalDivider(color = Color(0xFFE6EDF5))
                        OutlinedButton(
                            onClick = { descartar(recomendacion) }, enabled = descartandoId == null,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(11.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF39526F)),
                            border = BorderStroke(1.dp, Color(0xFFD0DDEC))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("No me interesa", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
