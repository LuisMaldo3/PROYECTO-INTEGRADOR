package com.tecsup.tcweek.ui.main

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.ui.layout.layout
import com.tecsup.tcweek.data.AvisoPendiente
import com.tecsup.tcweek.ui.comun.Esqueleto
import com.tecsup.tcweek.ui.comun.FondoVivo
import com.tecsup.tcweek.ui.comun.IconoDegradado
import com.tecsup.tcweek.ui.comun.aparecer
import com.tecsup.tcweek.ui.comun.presionable
import com.tecsup.tcweek.ui.comun.sombraSuave
import com.tecsup.tcweek.ui.theme.VynkSky
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.R
import com.tecsup.tcweek.data.ActividadAgenda
import com.tecsup.tcweek.data.AgendaApi
import com.tecsup.tcweek.data.AuthRepository
import com.tecsup.tcweek.data.AvisoCampus
import com.tecsup.tcweek.data.AvisosApi
import com.tecsup.tcweek.ui.comun.Pildora
import com.tecsup.tcweek.ui.comun.Tarjeta
import com.tecsup.tcweek.ui.comun.TextoSuave
import com.tecsup.tcweek.ui.comun.TituloSeccion
import com.tecsup.tcweek.ui.theme.*
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val Peru = Locale.forLanguageTag("es-PE")

private fun fechaAviso(iso: String): String = runCatching {
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Peru).format(Instant.parse(iso).atZone(ZoneId.systemDefault()))
}.getOrElse { iso.take(10) }

private fun esReciente(iso: String): Boolean = runCatching {
    Instant.parse(iso).isAfter(Instant.now().minusSeconds(3 * 24 * 3600))
}.getOrDefault(false)

@Composable
fun InicioScreen(
    onCatalogo: () -> Unit = {},
    onRecomendaciones: () -> Unit = {},
    onAgenda: () -> Unit = {},
    onAsistente: () -> Unit = {},
    onResultados: () -> Unit = {},
    onProgramacion: () -> Unit = {},
    onVerUbicacion: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val sesion = remember(context) { AuthRepository(context).currentSession() }
    val token = sesion?.llave.orEmpty()
    val primerNombre = sesion?.nombre.orEmpty().trim().split(" ").firstOrNull().orEmpty()
    var avisos by remember { mutableStateOf<List<AvisoCampus>?>(null) }
    var agenda by remember { mutableStateOf<List<ActividadAgenda>>(emptyList()) }

    // Los avisos se vuelven a consultar cada minuto: lo que publica administración llega solo.
    LaunchedEffect(token) {
        while (true) {
            avisos = runCatching { AvisosApi.recientes(token) }.getOrDefault(avisos ?: emptyList())
            delay(60_000)
        }
    }
    LaunchedEffect(token) {
        val hoy = LocalDate.now().toString()
        agenda = runCatching { AgendaApi.cargar(token) }.getOrDefault(emptyList())
            .filter { it.estado != "cancelada" && it.fecha >= hoy }
    }

    val hoy = remember { LocalDate.now(java.time.ZoneId.of("America/Lima")) }
    Column(Modifier.fillMaxSize().background(VynkBackground).verticalScroll(rememberScrollState())) {
        Portada(primerNombre, agenda.size, hoy, onCatalogo, onProgramacion)
        var idMiProxima by remember { mutableStateOf<String?>(null) }

        // US-74: dónde tengo que estar. Monta sobre la portada para que sea lo primero que se ve.
        TarjetaMiProxima(
            token = token,
            onVerUbicacion = onVerUbicacion,
            onAbrir = { AvisoPendiente.abrir(it) },
            onExplorar = onCatalogo,
            modifier = Modifier.padding(horizontal = 16.dp).subir(46.dp).aparecer(1),
            onCargada = { idMiProxima = it }
        )

        Column(
            Modifier.padding(horizontal = 16.dp).padding(top = 22.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            // US-72: lo que empieza pronto en el campus.
            Box(Modifier.aparecer(2)) { TarjetaProximamente(token, omitir = idMiProxima) }

            Column(Modifier.aparecer(3), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TituloSeccion("Accesos rápidos")
                val accesos = listOf(
                    Triple("Actividades", Icons.Default.Search, VynkBlue) to onCatalogo,
                    Triple("Por día", Icons.Default.CalendarMonth, colorArea("Electricidad").fuerte) to onProgramacion,
                    Triple("Mi agenda", Icons.Default.Event, colorArea("Química").fuerte) to onAgenda,
                    Triple("Asistente", Icons.Default.QuestionAnswer, colorArea("Digital").fuerte) to onAsistente,
                    Triple("Resultados", Icons.Default.EmojiEvents, colorArea("Gestión").fuerte) to onResultados,
                    Triple("Para ti", Icons.Default.AutoAwesome, VynkSky) to onRecomendaciones
                )
                accesos.chunked(3).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        fila.forEach { (datos, accion) ->
                            Acceso(datos.first, datos.second, datos.third, Modifier.weight(1f), accion)
                        }
                    }
                }
            }

            Column(Modifier.aparecer(4), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TituloSeccion("Avisos de Tecsup")
                val lista = avisos
                when {
                    lista == null -> Esqueleto(Modifier.fillMaxWidth().height(120.dp), RoundedCornerShape(18.dp))
                    lista.isEmpty() -> Tarjeta { TextoSuave("No hay avisos por ahora. Cuando administración publique uno, aparecerá aquí.", Modifier.padding(16.dp)) }
                    else -> {
                        AvisoDestacado(lista.first())
                        if (lista.size > 1) Tarjeta {
                            Column {
                                lista.drop(1).forEachIndexed { i, aviso ->
                                    if (i > 0) HorizontalDivider(color = VynkLine)
                                    AvisoPlegable(aviso)
                                }
                            }
                        }
                    }
                }
            }

            Column(Modifier.aparecer(5), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TituloSeccion("Próximo en tu agenda", "Ver agenda", onAgenda)
                if (agenda.isEmpty()) Tarjeta {
                    TextoSuave("No tienes actividades próximas. Explora las actividades y elige una.", Modifier.padding(16.dp))
                } else Tarjeta {
                    Column {
                        agenda.take(3).forEachIndexed { i, a ->
                            if (i > 0) HorizontalDivider(color = VynkLine)
                            FilaAgenda(a)
                        }
                    }
                }
            }
        }
    }
}

/** Sube el elemento [cuanto] sin dejar hueco debajo: para montar una tarjeta sobre la portada. */
private fun Modifier.subir(cuanto: androidx.compose.ui.unit.Dp) = this.then(
    Modifier.layout { medible, restricciones ->
        val p = medible.measure(restricciones)
        val px = cuanto.roundToPx()
        layout(p.width, (p.height - px).coerceAtLeast(0)) { p.place(0, -px) }
    }
)

/**
 * Portada viva del Inicio: degradado de marca con destellos que flotan, la foto del campus como textura,
 * el saludo con la fecha y dos accesos.
 */
@Composable
private fun Portada(nombre: String, inscripciones: Int, hoy: LocalDate, onCatalogo: () -> Unit, onProgramacion: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        FondoVivo(Modifier.matchParentSize())
        Image(painterResource(R.drawable.bg_campus), contentDescription = null, contentScale = ContentScale.Crop,
            alpha = 0.14f, modifier = Modifier.matchParentSize())
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 66.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(nombreDia(hoy.toString()).uppercase(Peru), color = VynkSky, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.aparecer(0))
            Text(if (nombre.isBlank()) "Bienvenido a VYNK" else "Hola, $nombre", color = Color.White,
                style = MaterialTheme.typography.headlineMedium, modifier = Modifier.aparecer(0))
            Text(
                when {
                    inscripciones == 1 -> "Tienes 1 actividad en tu agenda."
                    inscripciones > 1 -> "Tienes $inscripciones actividades en tu agenda."
                    else -> "Descubre lo que pasa hoy en el campus."
                },
                color = Color(0xFFD6E4F7), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.aparecer(1)
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.aparecer(1)) {
                BotonVidrio("Explorar", Icons.Default.Search, relleno = true, onClick = onCatalogo)
                BotonVidrio("Por día", Icons.Default.CalendarMonth, relleno = false, onClick = onProgramacion)
            }
        }
    }
}

/** Botón sobre la portada: blanco sólido o «vidrio» translúcido. */
@Composable
private fun BotonVidrio(texto: String, icono: ImageVector, relleno: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(14.dp))
            .background(if (relleno) Color.White else Color.White.copy(alpha = 0.14f))
            .presionable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icono, contentDescription = null, tint = if (relleno) VynkNavy else Color.White, modifier = Modifier.size(17.dp))
        Text(texto, style = MaterialTheme.typography.labelLarge, color = if (relleno) VynkNavy else Color.White)
    }
}

@Composable
private fun AvisoDestacado(aviso: AvisoCampus) {
    val context = LocalContext.current
    Tarjeta {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(VynkSky))
            Column(Modifier.weight(1f)) {
                if (aviso.portadaUrl.isNotBlank()) FotoAviso(aviso.portadaUrl)
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetaAviso(aviso.publicadoEn)
                    Text(aviso.titulo, style = MaterialTheme.typography.titleMedium, color = VynkNavy)
                    Text(aviso.mensaje, style = MaterialTheme.typography.bodyMedium, color = VynkMuted)
                    if (aviso.creditoUrl.startsWith("https://commons.wikimedia.org/"))
                        Text("Foto: ${aviso.autor.ifBlank { "Wikimedia Commons" }}", color = VynkSubtle,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(aviso.creditoUrl))) })
                }
            }
        }
    }
}

@Composable
private fun AvisoPlegable(aviso: AvisoCampus) {
    var abierto by remember(aviso.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clickable { abierto = !abierto }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                MetaAviso(aviso.publicadoEn)
                Text(aviso.titulo, style = MaterialTheme.typography.titleSmall, color = VynkNavy,
                    maxLines = if (abierto) 3 else 1, overflow = TextOverflow.Ellipsis)
            }
            Text(if (abierto) "−" else "+", style = MaterialTheme.typography.titleLarge, color = VynkSubtle)
        }
        if (abierto) Text(aviso.mensaje, style = MaterialTheme.typography.bodySmall, color = VynkMuted)
    }
}

@Composable
private fun MetaAviso(iso: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (esReciente(iso)) Pildora("Nuevo", VynkSky, Color.White)
        Text(fechaAviso(iso), style = MaterialTheme.typography.labelSmall, color = VynkSubtle)
    }
}

/** Foto del aviso: si no carga, no deja un hueco vacío. */
@Composable
private fun FotoAviso(url: String) {
    var foto by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url) {
        foto = withContext(Dispatchers.IO) {
            runCatching {
                val c = URL(url).openConnection() as HttpURLConnection
                c.connectTimeout = 6500; c.readTimeout = 10000
                try {
                    if (c.responseCode !in 200..299) null
                    else c.inputStream.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 2 })?.asImageBitmap() }
                } finally { c.disconnect() }
            }.getOrNull()
        }
    }
    foto?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(150.dp)) }
}

@Composable
private fun FilaAgenda(a: ActividadAgenda) {
    val fecha = runCatching { LocalDate.parse(a.fecha) }.getOrNull()
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.width(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(fecha?.dayOfMonth?.toString() ?: "—", style = MaterialTheme.typography.titleLarge, color = VynkNavy)
            Text(fecha?.format(DateTimeFormatter.ofPattern("MMM", Peru))?.trimEnd('.') ?: "", style = MaterialTheme.typography.labelSmall, color = VynkMuted)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(a.nombre, style = MaterialTheme.typography.titleSmall, color = VynkNavy, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${a.inicio}${if (a.fin.isNotBlank()) " – ${a.fin}" else ""} · ${a.ubicacion}", style = MaterialTheme.typography.bodySmall,
                color = VynkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Acceso(titulo: String, icono: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.sombraSuave(RoundedCornerShape(18.dp), 5.dp).clip(RoundedCornerShape(18.dp)).background(Color.White)
            .presionable(onClick = onClick).padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconoDegradado(icono, color)
        Text(titulo, style = MaterialTheme.typography.labelLarge, color = VynkNavy, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
