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
    onResultados: () -> Unit = {}
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

    Column(
        Modifier.fillMaxSize().background(VynkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Bienvenida(primerNombre, agenda.size, onCatalogo, onRecomendaciones)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TituloSeccion("Avisos de Tecsup")
            val lista = avisos
            when {
                lista == null -> TextoSuave("Cargando avisos…")
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

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TituloSeccion("Accesos rápidos")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Acceso("Actividades", Icons.Default.Search, VynkBlue, Modifier.weight(1f), onCatalogo)
                Acceso("Resultados", Icons.Default.EmojiEvents, colorArea("Gestión").fuerte, Modifier.weight(1f), onResultados)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Acceso("Mi agenda", Icons.Default.Event, colorArea("Química").fuerte, Modifier.weight(1f), onAgenda)
                Acceso("Asistente", Icons.Default.QuestionAnswer, colorArea("Digital").fuerte, Modifier.weight(1f), onAsistente)
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun Bienvenida(nombre: String, inscripciones: Int, onCatalogo: () -> Unit, onRecomendaciones: () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(VynkNavy)) {
        Image(painterResource(R.drawable.bg_campus), contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            0f to Color(0xF7092048), 0.6f to Color(0xE0092048), 1f to Color(0x99092048))))
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (nombre.isBlank()) "Bienvenido a VYNK" else "Hola, $nombre", color = Color.White,
                style = MaterialTheme.typography.headlineSmall)
            Text(if (inscripciones == 1) "Tienes 1 actividad próxima." else if (inscripciones > 1) "Tienes $inscripciones actividades próximas." else "Encuentra actividades del campus e inscríbete.",
                color = Color(0xFFE2ECFA), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCatalogo, shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = VynkNavy),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Explorar", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(onClick = onRecomendaciones, shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Para ti", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
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
    Tarjeta(modifier.clickable(onClick = onClick)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(36.dp).background(color.copy(alpha = .12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Text(titulo, style = MaterialTheme.typography.titleSmall, color = VynkNavy)
        }
    }
}
