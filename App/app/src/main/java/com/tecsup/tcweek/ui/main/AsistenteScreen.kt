package com.tecsup.tcweek.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.ActividadMomentoAsistente
import com.tecsup.tcweek.data.EjemplosConsultasNaturales
import com.tecsup.tcweek.data.esConsultaDeActividades
import com.tecsup.tcweek.data.AsistenteApi
import com.tecsup.tcweek.data.HistorialAsistente
import com.tecsup.tcweek.data.MensajeAsistente
import com.tecsup.tcweek.data.SesionGuardada
import com.tecsup.tcweek.data.UbicacionAsistente
import com.tecsup.tcweek.data.RequisitosAsistente
import com.tecsup.tcweek.data.puedeIniciarNuevaConversacion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val AzulChat = Color(0xFF1765D8)
private val MarinoChat = Color(0xFF0B2857)
private val FondoChat = Color(0xFFF2F5F9)
private val Sugerencias = EjemplosConsultasNaturales.preguntas

/** Jordi: chat conectado al servicio, historial por usuario y estados reales de carga/error. */
@Composable
fun AsistenteScreen() {
    val context = LocalContext.current
    val sesion = remember { SesionGuardada(context).leer() }
    val correo = sesion?.correo.orEmpty()
    val historial = remember(correo) { HistorialAsistente(context, correo) }
    val mensajes = remember(correo) { mutableStateListOf<MensajeAsistente>().apply { addAll(historial.cargar()) } }
    var borrador by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var sincronizando by remember { mutableStateOf(true) }
    var reiniciando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var estadoConexion by remember { mutableStateOf("Verificando conexión…") }
    val lista = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun enviar(texto: String) {
        val consulta = texto.trim()
        if (consulta.isBlank() || cargando || sincronizando || reiniciando || consulta.length > 600) return
        val token = sesion?.llave
        if (token.isNullOrBlank()) {
            error = "Inicia sesión para consultar el asistente."
            return
        }
        mensajes.add(MensajeAsistente("usuario", consulta))
        historial.guardar(mensajes)
        borrador = ""
        cargando = true
        error = null
        scope.launch {
            try {
                val respuesta = AsistenteApi.preguntar(token, consulta)
                mensajes.add(MensajeAsistente("asistente", respuesta.respuesta, respuesta.origen, respuesta.aviso, respuesta.ubicaciones, respuesta.tipoConsulta, respuesta.actividades, respuesta.requisitos))
                historial.guardar(mensajes)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "No se pudo obtener la respuesta." }
            finally { cargando = false }
        }
    }

    LaunchedEffect(correo) {
        val token = sesion?.llave
        sincronizando = true
        if (token.isNullOrBlank()) {
            estadoConexion = "Inicia sesión para consultar"
        } else {
            try {
                val remotos = AsistenteApi.consultarConversacion(token)
                mensajes.clear()
                mensajes.addAll(remotos)
                historial.guardar(remotos)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = "Sin sincronización: ${e.message}" }
            estadoConexion = if (AsistenteApi.verificar(token).contains("sin conexión", ignoreCase = true)) "Sin conexión con el servidor" else "En línea"
        }
        sincronizando = false
    }

    fun nuevaConversacion() {
        val token = sesion?.llave
        if (token.isNullOrBlank() || cargando || sincronizando || reiniciando) return
        reiniciando = true
        scope.launch {
            try {
                AsistenteApi.nuevaConversacion(token)
                mensajes.clear()
                historial.borrar()
                borrador = ""
                error = null
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "No fue posible borrar la conversación." }
            finally { reiniciando = false }
        }
    }

    LaunchedEffect(mensajes.size, cargando) {
        if (mensajes.isNotEmpty()) lista.animateScrollToItem(mensajes.size - 1 + if (cargando) 1 else 0)
    }

    Column(Modifier.fillMaxSize().background(FondoChat)) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color(0xFFE8F0FD), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = AzulChat, modifier = Modifier.padding(10.dp).size(21.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("Asistente de VYNK", color = MarinoChat, style = MaterialTheme.typography.titleSmall)
                Text(if (sincronizando) "Sincronizando conversación…" else if (reiniciando) "Iniciando conversación…" else if (cargando) "Consultando el cronograma…" else estadoConexion, color = Color(0xFF68809D), style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = { nuevaConversacion() }, enabled = puedeIniciarNuevaConversacion(mensajes.size, cargando, sincronizando, reiniciando)) {
                Icon(Icons.Filled.AddComment, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text("Nueva conversación", style = MaterialTheme.typography.labelSmall)
            }
        }
        HorizontalDivider(color = Color(0xFFE4EBF6))
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = lista,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (mensajes.isEmpty()) {
                item {
                    BurbujaAsistente(MensajeAsistente("asistente", "Hola, soy el asistente de VYNK. Pregúntame por fechas, horarios, lugares, cupos o requisitos de las actividades."))
                    Spacer(Modifier.height(20.dp))
                    Text("Preguntas frecuentes", color = MarinoChat, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(9.dp))
                    Sugerencias.forEach { sugerencia ->
                        OutlinedCard(
                            onClick = { enviar(sugerencia) },
                            enabled = !cargando && !sincronizando && !reiniciando,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            shape = RoundedCornerShape(13.dp),
                            colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
                        ) {
                            Text("↗  $sugerencia", modifier = Modifier.padding(15.dp), color = MarinoChat, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            items(mensajes) { mensaje -> BurbujaAsistente(mensaje) }
            if (cargando) item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp, color = AzulChat)
                    Text("Consultando el catálogo de Tecsup…", color = Color(0xFF5E7696), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (error != null) item {
                Surface(color = Color(0xFFFFECEC), shape = RoundedCornerShape(14.dp)) {
                    Text(error.orEmpty(), modifier = Modifier.fillMaxWidth().padding(16.dp), color = Color(0xFFA42938), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Surface(shadowElevation = 8.dp, color = Color.White) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(
                    value = borrador,
                    onValueChange = { if (it.length <= 600) borrador = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Pregunta por horarios, ubicaciones o requisitos…", style = MaterialTheme.typography.bodyMedium) },
                    maxLines = 4,
                    shape = RoundedCornerShape(17.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { enviar(borrador) }),
                    enabled = !cargando && !sincronizando && !reiniciando,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AzulChat)
                )
                FilledIconButton(
                    onClick = { enviar(borrador) },
                    enabled = !cargando && !sincronizando && !reiniciando && borrador.isNotBlank(),
                    modifier = Modifier.size(53.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = AzulChat)
                ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar pregunta") }
            }
        }
    }
}

@Composable
private fun BurbujaAsistente(mensaje: MensajeAsistente) {
    val esUsuario = mensaje.rol == "usuario"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (esUsuario) Arrangement.End else Arrangement.Start) {
        if (!esUsuario) {
            Surface(color = Color(0xFFE5F0FF), shape = CircleShape) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = AzulChat, modifier = Modifier.padding(8.dp).size(17.dp))
            }
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.fillMaxWidth(0.87f), horizontalAlignment = if (esUsuario) Alignment.End else Alignment.Start) {
            Text(if (esUsuario) "Tú" else "VYNK", color = Color(0xFF6C829D), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 5.dp))
            Surface(color = if (esUsuario) AzulChat else Color.White, shape = RoundedCornerShape(18.dp), shadowElevation = if (esUsuario) 0.dp else 1.dp) {
                Text(mensaje.contenido, color = if (esUsuario) Color.White else MarinoChat, modifier = Modifier.padding(15.dp), style = MaterialTheme.typography.bodyMedium)
            }
            if (!esUsuario && mensaje.ubicaciones.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().padding(top = 9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    mensaje.ubicaciones.take(7).forEach { lugar -> TarjetaUbicacionAsistente(lugar) }
                }
            }
            if (!esUsuario && esConsultaDeActividades(mensaje.tipoConsulta) && mensaje.actividades.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().padding(top = 9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    mensaje.actividades.take(7).forEach { actividad -> TarjetaActividadMomento(actividad) }
                }
            }
            if (!esUsuario && mensaje.tipoConsulta == "requisitos" && mensaje.requisitos.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().padding(top = 9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    mensaje.requisitos.take(7).forEach { requisito -> TarjetaRequisitosAsistente(requisito) }
                }
            }
            if (!esUsuario && mensaje.origen.isNotBlank()) {
                Text("Según el cronograma oficial", color = Color(0xFF567291), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 7.dp))
            }
        }
    }
}

/** La tarjeta muestra únicamente datos procedentes de la actividad publicada. */
@Composable
private fun TarjetaUbicacionAsistente(lugar: UbicacionAsistente) {
    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFFE8F0FD)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(13.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = Color(0xFFE8F0FD), shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Filled.Place, contentDescription = null, tint = AzulChat,
                    modifier = Modifier.padding(8.dp).size(21.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(lugar.nombre.ifBlank { "Actividad publicada" }, color = MarinoChat,
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(if (lugar.confirmada) "Ubicación registrada" else "Ubicación por confirmar",
                    color = if (lugar.confirmada) Color(0xFF12805C) else Color(0xFF92592A),
                    style = MaterialTheme.typography.labelSmall)
                Text(lugar.lugar, color = AzulChat, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium)
                lugar.detalles().forEach { detalle ->
                    Text(detalle, color = Color(0xFF55657E), style = MaterialTheme.typography.bodySmall)
                }
                if (lugar.fecha.isNotBlank()) Text(
                    lugar.fecha + if (lugar.horaInicio.isNotBlank()) " · ${lugar.horaInicio}" else "",
                    color = Color(0xFF55657E), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}


/** Actividad verificada para el momento consultado; no realiza inscripciones. */
@Composable
private fun TarjetaActividadMomento(actividad: ActividadMomentoAsistente) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFFE8F0FD))
    ) {
        Row(Modifier.padding(13.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = Color(0xFFE8F0FD), shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = AzulChat,
                    modifier = Modifier.padding(8.dp).size(21.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(actividad.nombre, color = MarinoChat, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium)
                Text(actividad.horario(), color = Color(0xFF415D85), style = MaterialTheme.typography.bodySmall)
                if (actividad.organizador.isNotBlank()) Text(
                    "Organiza: ${actividad.organizador}", color = Color(0xFF55657E),
                    style = MaterialTheme.typography.bodySmall)
                Text(actividad.etiquetaCupos(), color = Color(0xFF12805C),
                    fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}


/** Tarjeta de requisitos sin suponer condiciones no registradas por administración. */
@Composable
private fun TarjetaRequisitosAsistente(datos: RequisitosAsistente) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFFE8F0FD))
    ) {
        Row(Modifier.padding(13.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = Color(0xFFE8F0FD), shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = AzulChat,
                    modifier = Modifier.padding(8.dp).size(21.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(datos.nombre, color = MarinoChat, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium)
                Text(datos.estado(), color = if (datos.tieneInformacion()) Color(0xFF12805C) else Color(0xFF92592A),
                    style = MaterialTheme.typography.labelSmall)
                if (datos.tieneInformacion()) datos.items.take(12).forEach { item ->
                    Text("• $item", color = Color(0xFF405C7B), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
