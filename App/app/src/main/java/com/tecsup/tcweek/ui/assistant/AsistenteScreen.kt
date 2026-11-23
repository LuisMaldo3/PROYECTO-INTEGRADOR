package com.tecsup.tcweek.ui.assistant

import com.tecsup.tcweek.ui.theme.ColorSeccion
import com.tecsup.tcweek.ui.components.oscurecer
import com.tecsup.tcweek.ui.home.ActividadDeAviso
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.domain.ActividadMomentoAsistente
import com.tecsup.tcweek.data.remote.AsistenteApi
import com.tecsup.tcweek.domain.ConversacionChat
import com.tecsup.tcweek.domain.EjemplosConsultasNaturales
import com.tecsup.tcweek.data.repository.HistorialAsistente
import com.tecsup.tcweek.domain.LARGO_MAXIMO_PREGUNTA
import com.tecsup.tcweek.data.remote.MensajeAsistente
import com.tecsup.tcweek.domain.RequisitosAsistente
import com.tecsup.tcweek.domain.ResultadoEnvio
import com.tecsup.tcweek.data.repository.SesionGuardada
import com.tecsup.tcweek.domain.UbicacionAsistente
import com.tecsup.tcweek.domain.esConsultaDeActividades
import com.tecsup.tcweek.domain.mensajesTrasRespuesta
import com.tecsup.tcweek.domain.puedeIniciarNuevaConversacion
import com.tecsup.tcweek.domain.validarPregunta
import com.tecsup.tcweek.domain.textoVisible
import com.tecsup.tcweek.ui.components.DegradadoVynk
import com.tecsup.tcweek.ui.components.Esqueleto
import com.tecsup.tcweek.ui.components.FondoVivo
import com.tecsup.tcweek.ui.components.PuntoVivo
import com.tecsup.tcweek.ui.components.PuntosEscribiendo
import com.tecsup.tcweek.ui.components.aparecer
import com.tecsup.tcweek.ui.components.presionable
import com.tecsup.tcweek.ui.components.sombraSuave
import com.tecsup.tcweek.ui.theme.VynkBackground
import com.tecsup.tcweek.ui.theme.VynkBlue
import com.tecsup.tcweek.ui.theme.VynkBlueSoft
import com.tecsup.tcweek.ui.theme.VynkError
import com.tecsup.tcweek.ui.theme.VynkErrorSoft
import com.tecsup.tcweek.ui.theme.VynkLine
import com.tecsup.tcweek.ui.theme.VynkLineStrong
import com.tecsup.tcweek.ui.theme.VynkMuted
import com.tecsup.tcweek.ui.theme.VynkNavy
import com.tecsup.tcweek.ui.theme.VynkOk
import com.tecsup.tcweek.ui.theme.VynkSky
import com.tecsup.tcweek.ui.theme.VynkSubtle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import com.tecsup.tcweek.domain.PREGUNTA_AHORA
import com.tecsup.tcweek.ui.theme.VynkWarn
import com.tecsup.tcweek.ui.theme.VynkWarnSoft
import com.tecsup.tcweek.ui.theme.VynkOkSoft

private val AzulChat = VynkBlue
private val MarinoChat = VynkNavy

/**
 * US-75: el asistente virtual en el celular. Historial sincronizado con la web, preguntas sugeridas según el
 * estudiante, indicador de «escribiendo», reintento de un mensaje que no salió y burbujas que entran con movimiento.
 */
@Composable
fun AsistenteScreen() {
    val context = LocalContext.current
    val sesion = remember { SesionGuardada(context).leer() }
    val correo = sesion?.correo.orEmpty()
    val token = sesion?.llave.orEmpty()
    val historial = remember(correo) { HistorialAsistente(context, correo) }
    val conversacion = remember { ConversacionChat() }
    val mensajes = remember(correo) { mutableStateListOf<MensajeAsistente>().apply { addAll(historial.cargar()) } }
    var sugerencias by remember { mutableStateOf(EjemplosConsultasNaturales.preguntas) }
    var borrador by rememberSaveable { mutableStateOf("") }
    var escribiendo by remember { mutableStateOf(false) }
    var sincronizando by remember { mutableStateOf(true) }
    var reiniciando by remember { mutableStateOf(false) }
    var enLinea by remember { mutableStateOf<Boolean?>(null) }
    // Pregunta que no se pudo enviar: se muestra con «Reintentar».
    var fallida by remember { mutableStateOf<Pair<String, String>?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var detalleId by remember { mutableStateOf<String?>(null) }
    val lista = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val ocupado = escribiendo || sincronizando || reiniciando

    fun enviar(texto: String) {
        if (ocupado) return
        validarPregunta(texto)?.let { aviso = it; return }
        if (token.isBlank()) { aviso = "Inicia sesión para consultar el asistente."; return }
        val pregunta = texto.trim()
        fallida = null
        aviso = null
        borrador = ""
        mensajes.add(MensajeAsistente("usuario", pregunta))
        escribiendo = true
        scope.launch {
            when (val r = conversacion.enviar(token, pregunta)) {
                is ResultadoEnvio.Respondida -> {
                    val sinPregunta = mensajes.dropLast(1)
                    val nuevos = mensajesTrasRespuesta(sinPregunta, pregunta, r)
                    mensajes.clear(); mensajes.addAll(nuevos)
                    historial.guardar(mensajes)
                    enLinea = true
                }
                is ResultadoEnvio.Fallida -> {
                    mensajes.removeAt(mensajes.lastIndex) // la pregunta vuelve como «no enviada»
                    if (r.reintentable) fallida = pregunta to r.mensaje else { aviso = r.mensaje; borrador = pregunta }
                }
            }
            escribiendo = false
        }
    }

    LaunchedEffect(correo) {
        sincronizando = true
        if (token.isNotBlank()) {
            try {
                val remotos = AsistenteApi.consultarConversacion(token)
                mensajes.clear(); mensajes.addAll(remotos)
                historial.guardar(remotos)
                enLinea = true
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { enLinea = false }
            sugerencias = AsistenteApi.sugerencias(token)
        } else enLinea = false
        sincronizando = false
    }

    fun nuevaConversacion() {
        if (token.isBlank() || ocupado) return
        reiniciando = true
        scope.launch {
            try {
                AsistenteApi.nuevaConversacion(token)
                mensajes.clear(); historial.borrar()
                borrador = ""; fallida = null; aviso = null
                sugerencias = AsistenteApi.sugerencias(token)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { aviso = e.message ?: "No fue posible iniciar una nueva conversación." }
            finally { reiniciando = false }
        }
    }

    // Al llegar una respuesta se ve la pregunta arriba y el comienzo de la respuesta debajo (se lee desde el
    // principio); mientras escribe o si un mensaje falló, se baja hasta el final.
    LaunchedEffect(mensajes.size, escribiendo, fallida) {
        withFrameNanos { } // esperar a que la lista tenga los elementos nuevos
        val total = lista.layoutInfo.totalItemsCount
        if (total == 0) return@LaunchedEffect
        val ultimaPregunta = mensajes.indexOfLast { it.rol == "usuario" }
        if (!escribiendo && fallida == null && ultimaPregunta >= 0 && mensajes.last().rol == "asistente")
            lista.animateScrollToItem(ultimaPregunta)
        else lista.animateScrollToItem(total - 1, scrollOffset = 100_000)
    }

    Column(Modifier.fillMaxSize().background(VynkBackground).imePadding()) {
        CabeceraChat(
            estado = when {
                sincronizando -> "Sincronizando…"
                reiniciando -> "Nueva conversación…"
                escribiendo -> "Escribiendo…"
                enLinea == false -> "Sin conexión con el servidor"
                else -> "En línea"
            },
            enLinea = enLinea != false,
            puedeReiniciar = puedeIniciarNuevaConversacion(mensajes.size, escribiendo, sincronizando, reiniciando),
            onNueva = { nuevaConversacion() }
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = lista,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (mensajes.isEmpty() && !sincronizando) item(key = "bienvenida") { Bienvenida(sugerencias, !ocupado) { enviar(it) } }
            if (mensajes.isEmpty() && sincronizando) item(key = "cargando") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Esqueleto(Modifier.fillMaxWidth(0.7f).height(56.dp), RoundedCornerShape(18.dp))
                    Esqueleto(Modifier.fillMaxWidth(0.5f).height(40.dp).align(Alignment.End), RoundedCornerShape(18.dp))
                }
            }
            itemsIndexed(mensajes, key = { i, m -> "$i-${m.rol}-${m.contenido.hashCode()}" }) { _, mensaje ->
                Box(Modifier.aparecer(desplazamiento = 14.dp)) { BurbujaAsistente(mensaje, onAbrir = { detalleId = it }) }
            }
            if (escribiendo) item(key = "escribiendo") { Box(Modifier.aparecer()) { BurbujaEscribiendo() } }
            fallida?.let { (pregunta, motivo) ->
                item(key = "fallida") {
                    Box(Modifier.aparecer()) {
                        BurbujaFallida(pregunta, motivo, onReintentar = { fallida = null; enviar(pregunta) },
                            onDescartar = { fallida = null; borrador = pregunta })
                    }
                }
            }
            if (mensajes.isNotEmpty() && !ocupado && fallida == null) item(key = "sugerencias") {
                // No se vuelve a sugerir lo que ya preguntó.
                val hechas = mensajes.filter { it.rol == "usuario" }.map { it.contenido.trim() }.toSet()
                val pendientes = sugerencias.filter { it.trim() !in hechas }.take(3)
                if (pendientes.isNotEmpty()) FilaSugerencias(pendientes) { enviar(it) }
            }
        }
        aviso?.let {
            Text(it, color = VynkError, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().background(VynkErrorSoft).padding(horizontal = 16.dp, vertical = 8.dp))
        }
        // US-78 T2: siempre a mano, sin escribir nada.
        BotonQueHayAhora(habilitado = !ocupado, onClick = { enviar(PREGUNTA_AHORA) })
        BarraEscribir(
            texto = borrador,
            onCambiar = { if (it.length <= LARGO_MAXIMO_PREGUNTA) { borrador = it; aviso = null } },
            habilitado = !sincronizando && !reiniciando,
            puedeEnviar = !ocupado && borrador.isNotBlank(),
            onEnviar = { enviar(borrador) }
        )
    }
    detalleId?.let { id -> if (token.isNotBlank()) ActividadDeAviso(id, token, alCerrar = { detalleId = null }) }
}

/** US-78 T2: botón «¿Qué hay ahora?» encima de la barra para escribir. */
@Composable
private fun BotonQueHayAhora(habilitado: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(VynkBackground).padding(start = 12.dp, end = 12.dp, top = 6.dp)) {
        Row(
            Modifier.sombraSuave(RoundedCornerShape(50), 5.dp).clip(RoundedCornerShape(50))
                .background(if (habilitado) DegradadoVynk else SolidColor(VynkLine))
                .presionable(enabled = habilitado, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .semantics { contentDescription = "Qué hay ahora: buscar actividades de este momento" },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (habilitado) PuntoVivo(VynkSky) else Box(Modifier.size(7.dp).background(VynkSubtle, CircleShape))
            Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
            Text(PREGUNTA_AHORA, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun CabeceraChat(estado: String, enLinea: Boolean, puedeReiniciar: Boolean, onNueva: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.matchParentSize().background(
            Brush.linearGradient(listOf(ColorSeccion.ASISTENTE, oscurecer(ColorSeccion.ASISTENTE, 0.55f)))))
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Box(Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = VynkSky, modifier = Modifier.size(24.dp))
                }
                if (enLinea) PuntoVivo(modifier = Modifier.align(Alignment.BottomEnd))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Asistente IA", color = Color.White, style = MaterialTheme.typography.titleMedium)
                AnimatedContent(estado, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "estado") { e ->
                    Text(e, color = Color(0xFFCFE0F7), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
            IconButton(onClick = onNueva, enabled = puedeReiniciar) {
                Icon(Icons.Filled.AddComment, contentDescription = "Nueva conversación",
                    tint = if (puedeReiniciar) Color.White else Color.White.copy(alpha = 0.35f))
            }
        }
    }
}

/** Primera vez: saludo y preguntas sugeridas para empezar sin escribir. */
@Composable
private fun Bienvenida(sugerencias: List<String>, habilitado: Boolean, onElegir: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.aparecer(0)) {
            BurbujaAsistente(MensajeAsistente("asistente",
                "¡Hola! Soy el asistente de VYNK. Pregúntame dónde es una actividad, a qué hora empieza, si quedan cupos o qué requisitos tiene."))
        }
        Box(Modifier.padding(start = 46.dp).aparecer(1)) { TarjetaQueHayAhora(habilitado) { onElegir(PREGUNTA_AHORA) } }
        Text("Prueba con una de estas", color = VynkMuted, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 46.dp).aparecer(1))
        sugerencias.forEachIndexed { i, s ->
            Row(
                Modifier.padding(start = 46.dp).fillMaxWidth().aparecer(i + 2)
                    .sombraSuave(RoundedCornerShape(16.dp), 4.dp).clip(RoundedCornerShape(16.dp)).background(Color.White)
                    .presionable(enabled = habilitado) { onElegir(s) }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = VynkSky, modifier = Modifier.size(16.dp))
                Text(s, color = MarinoChat, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = VynkLineStrong, modifier = Modifier.size(15.dp))
            }
        }
    }
}

/** Invitación grande en la bienvenida: lo más útil cuando uno está en el campus. */
@Composable
private fun TarjetaQueHayAhora(habilitado: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().sombraSuave(RoundedCornerShape(20.dp), 8.dp).clip(RoundedCornerShape(20.dp))
            .presionable(enabled = habilitado, onClick = onClick)
    ) {
        FondoVivo(Modifier.matchParentSize())
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = VynkSky, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(PREGUNTA_AHORA, color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                Text("Lo que está pasando y lo que empieza en la próxima hora", color = Color(0xFFD5E6FF),
                    style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

/** Después de cada respuesta: sugerencias en chips para seguir la conversación con un toque. */
@Composable
private fun FilaSugerencias(sugerencias: List<String>, onElegir: (String) -> Unit) {
    LazyRow(contentPadding = PaddingValues(start = 46.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(sugerencias) { s ->
            Text(s, color = AzulChat, style = MaterialTheme.typography.labelLarge, maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 260.dp).clip(RoundedCornerShape(50)).background(VynkBlueSoft)
                    .presionable { onElegir(s) }.padding(horizontal = 14.dp, vertical = 9.dp))
        }
    }
}

@Composable
private fun Avatar() {
    Box(Modifier.size(36.dp).clip(CircleShape).background(DegradadoVynk), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = VynkSky, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun BurbujaEscribiendo() {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Avatar()
        Box(Modifier.sombraSuave(RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp), 4.dp)
            .clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)).background(Color.White).padding(horizontal = 18.dp, vertical = 16.dp)) {
            PuntosEscribiendo()
        }
    }
}

@Composable
private fun BurbujaFallida(pregunta: String, motivo: String, onReintentar: () -> Unit, onDescartar: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(pregunta, color = Color.White, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(0.8f).wrapContentWidth(Alignment.End)
                .clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)).background(AzulChat.copy(alpha = 0.45f))
                .padding(horizontal = 16.dp, vertical = 12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = VynkError, modifier = Modifier.size(15.dp))
            Text(" No se envió", color = VynkError, style = MaterialTheme.typography.labelMedium)
            TextButton(onClick = onReintentar) { Text("Reintentar") }
            TextButton(onClick = onDescartar) { Text("Editar") }
        }
        Text(motivo, color = VynkMuted, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.fillMaxWidth(0.8f).wrapContentWidth(Alignment.End))
    }
}

/** Barra para escribir: píldora blanca y botón de enviar que se enciende cuando hay texto. */
@Composable
private fun BarraEscribir(texto: String, onCambiar: (String) -> Unit, habilitado: Boolean, puedeEnviar: Boolean, onEnviar: () -> Unit) {
    val colorBoton by animateColorAsState(if (puedeEnviar) VynkNavy else VynkLine, tween(220), label = "boton")
    val escala by animateFloatAsState(if (puedeEnviar) 1f else 0.9f, spring(Spring.DampingRatioMediumBouncy), label = "escala")
    Row(
        Modifier.fillMaxWidth().background(VynkBackground).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.weight(1f).sombraSuave(RoundedCornerShape(24.dp), 6.dp).clip(RoundedCornerShape(24.dp))
                .background(Color.White).padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            if (texto.isEmpty()) Text("Pregunta por horarios, lugares o requisitos…", color = VynkSubtle, style = MaterialTheme.typography.bodyMedium)
            BasicTextField(
                value = texto, onValueChange = onCambiar, enabled = habilitado,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MarinoChat), maxLines = 4,
                cursorBrush = SolidColor(AzulChat),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (puedeEnviar) onEnviar() }),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Escribe tu pregunta" }
            )
        }
        Box(
            Modifier.size(52.dp).graphicsLayer { scaleX = escala; scaleY = escala }.clip(CircleShape).background(colorBoton)
                .presionable(enabled = puedeEnviar, onClick = onEnviar),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar pregunta", tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun BurbujaAsistente(mensaje: MensajeAsistente, onAbrir: (String) -> Unit = {}) {
    val esUsuario = mensaje.rol == "usuario"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (esUsuario) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom) {
        if (!esUsuario) { Avatar(); Spacer(Modifier.width(10.dp)) }
        Column(Modifier.fillMaxWidth(if (esUsuario) 0.82f else 1f), horizontalAlignment = if (esUsuario) Alignment.End else Alignment.Start) {
            val forma = if (esUsuario) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
            Box(
                Modifier.then(if (esUsuario) Modifier else Modifier.sombraSuave(forma, 4.dp)).clip(forma)
                    .background(if (esUsuario) DegradadoUsuario else SolidColor(Color.White))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(if (esUsuario) mensaje.contenido else textoVisible(mensaje),
                    color = if (esUsuario) Color.White else MarinoChat, style = MaterialTheme.typography.bodyMedium)
            }
            if (!esUsuario && mensaje.ubicaciones.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().padding(top = 9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    mensaje.ubicaciones.take(7).forEach { lugar -> TarjetaUbicacionAsistente(lugar) }
                }
            }
            if (!esUsuario && esConsultaDeActividades(mensaje.tipoConsulta) && mensaje.actividades.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().padding(top = 9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    mensaje.actividades.take(7).forEachIndexed { i, actividad ->
                        if (mensaje.tipoConsulta == "ahora") TarjetaAhora(actividad, Modifier.aparecer(i), onAbrir)
                        else TarjetaActividadMomento(actividad)
                    }
                }
            }
            if (!esUsuario && mensaje.tipoConsulta == "requisitos" && mensaje.requisitos.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().padding(top = 9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    mensaje.requisitos.take(7).forEach { requisito -> TarjetaRequisitosAsistente(requisito) }
                }
            }
            if (!esUsuario && mensaje.origen.isNotBlank()) {
                Row(Modifier.padding(top = 6.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = VynkOk, modifier = Modifier.size(13.dp))
                    Text(" Según el cronograma oficial", color = VynkMuted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private val DegradadoUsuario = Brush.linearGradient(listOf(Color(0xFF1557D0), Color(0xFF0B2A5B)))

/** La tarjeta muestra únicamente datos procedentes de la actividad publicada. */
@Composable
private fun TarjetaUbicacionAsistente(lugar: UbicacionAsistente) {
    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
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
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
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


/**
 * US-78 T3: actividad de este momento. Verde si está en curso, azul si empieza pronto; dice si ya estás inscrito o
 * si se cruza con algo tuyo. Al tocarla se abre el detalle (con inscripción).
 */
@Composable
private fun TarjetaAhora(a: ActividadMomentoAsistente, modifier: Modifier = Modifier, onAbrir: (String) -> Unit) {
    val color = if (a.enCurso) VynkOk else AzulChat
    val forma = RoundedCornerShape(18.dp)
    Row(
        modifier.fillMaxWidth().height(IntrinsicSize.Min).sombraSuave(forma, 5.dp).clip(forma).background(Color.White)
            .presionable(enabled = a.id.isNotBlank()) { onAbrir(a.id) }
            .semantics { contentDescription = "${a.nombre}. ${a.etiquetaAhora()}" }
    ) {
        Box(Modifier.width(5.dp).fillMaxHeight().background(color))
        Column(Modifier.weight(1f).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (a.enCurso) PuntoVivo(VynkOk) else Icon(Icons.Filled.Schedule, null, tint = color, modifier = Modifier.size(14.dp))
                Text(a.etiquetaAhora(), color = color, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelMedium)
            }
            Text(a.nombre, color = MarinoChat, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(Icons.Filled.Schedule, null, tint = VynkMuted, modifier = Modifier.size(14.dp))
                Text("${a.horaInicio} – ${a.horaFin}", color = VynkMuted, style = MaterialTheme.typography.bodySmall)
                if (a.ubicacion.isNotBlank()) {
                    Icon(Icons.Filled.Place, null, tint = VynkMuted, modifier = Modifier.size(14.dp).padding(start = 2.dp))
                    Text(a.ubicacion, color = VynkMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
            }
            when {
                a.inscrito -> Etiqueta("Ya estás inscrito", VynkOk, VynkOkSoft)
                a.cruzaCon.isNotBlank() -> Etiqueta("Se cruza con «${a.cruzaCon}»", VynkWarn, VynkWarnSoft)
                a.cuposDisponibles != null && a.cuposDisponibles <= 5 -> Etiqueta(
                    if (a.cuposDisponibles == 1) "Queda 1 cupo" else "Quedan ${a.cuposDisponibles} cupos", VynkWarn, VynkWarnSoft)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = VynkLineStrong,
            modifier = Modifier.align(Alignment.CenterVertically).padding(end = 8.dp))
    }
}

@Composable
private fun Etiqueta(texto: String, color: Color, fondo: Color) {
    Text(texto, color = color, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(fondo).padding(horizontal = 10.dp, vertical = 4.dp))
}

/** Tarjeta de requisitos sin suponer condiciones no registradas por administración. */
@Composable
private fun TarjetaRequisitosAsistente(datos: RequisitosAsistente) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
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
