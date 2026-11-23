package com.tecsup.tcweek.ui.home

import com.tecsup.tcweek.ui.catalog.TipoMensaje
import com.tecsup.tcweek.ui.catalog.TiraMensaje
import com.tecsup.tcweek.ui.catalog.nombreDia
import com.tecsup.tcweek.ui.components.relojMinutos
import com.tecsup.tcweek.ui.components.rememberCargaProgramada
import com.tecsup.tcweek.ui.theme.VynkBackground
import com.tecsup.tcweek.ui.theme.VynkBlue
import com.tecsup.tcweek.ui.theme.VynkNavy
import com.tecsup.tcweek.ui.theme.VynkSubtle
import com.tecsup.tcweek.ui.theme.colorArea
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
import com.tecsup.tcweek.domain.AvisoPendiente
import com.tecsup.tcweek.ui.components.Esqueleto
import com.tecsup.tcweek.ui.components.FondoVivo
import com.tecsup.tcweek.ui.components.IconoDegradado
import com.tecsup.tcweek.ui.components.aparecer
import com.tecsup.tcweek.ui.components.presionable
import com.tecsup.tcweek.ui.components.sombraSuave
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
import com.tecsup.tcweek.data.repository.AuthRepository
import com.tecsup.tcweek.data.remote.InicioApi
import com.tecsup.tcweek.data.repository.MemoriaPantallas
import com.tecsup.tcweek.data.remote.ResumenInicio
import com.tecsup.tcweek.data.remote.leerResumenInicio
import com.tecsup.tcweek.data.remote.proximasVigentes
import androidx.compose.ui.text.font.FontWeight
import com.tecsup.tcweek.ui.components.AccesoRapido
import com.tecsup.tcweek.ui.components.IndicadorAcceso
import com.tecsup.tcweek.ui.components.Pildora
import com.tecsup.tcweek.ui.components.Tarjeta
import com.tecsup.tcweek.ui.components.TextoSuave
import com.tecsup.tcweek.ui.components.TituloSeccion
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
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Peru).format(Instant.parse(iso).atZone(ZoneId.of("America/Lima")))
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
    // US-81 T2/T3: todo el inicio en una llamada, con carga programada (se ve al instante con lo guardado, se
    // actualiza cada 2 minutos, al inscribirse o cancelar, y los avisos nuevos llegan solos).
    val carga = rememberCargaProgramada(
        clave = InicioApi.CLAVE,
        memoria = { MemoriaPantallas.inicio },
        recordar = { datos, marca -> MemoriaPantallas.inicio = datos to marca },
        desdeJson = { json, en -> leerResumenInicio(org.json.JSONObject(json), en) },
        vence = { datos, _ -> datos.vigenteHasta },
        cargar = { t -> InicioApi.cargar(t) }
    )
    val resumen = carga.datos
    val ahora = relojMinutos()

    val hoy = remember { LocalDate.now(java.time.ZoneId.of("America/Lima")) }
    Column(Modifier.fillMaxSize().background(VynkBackground).verticalScroll(rememberScrollState())) {
        Portada(resumen?.nombre?.ifBlank { null } ?: primerNombre, resumen, hoy, onCatalogo, onProgramacion)
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
            if (carga.sinConexion) TiraMensaje("Sin conexión: te mostramos tu inicio de ${com.tecsup.tcweek.data.repository.haceCuanto(carga.actualizadoEn, ahora)}.",
                TipoMensaje.SIN_CONEXION)

            // US-81 T4: mis próximas actividades (la primera ya está en «Mi próxima actividad»).
            Box(Modifier.aparecer(2)) {
                SeccionMisProximas(resumen?.let { proximasVigentes(it, ahora, omitir = idMiProxima) }, ahora,
                    onAbrir = { AvisoPendiente.abrir(it) }, onAgenda = onAgenda, onExplorar = onCatalogo)
            }

            Column(Modifier.aparecer(3), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TituloSeccion("Accesos rápidos")
                // US-82: cada acceso lleva su indicador (lo de hoy, lo que falta confirmar, lo sugerido).
                val paquete = MemoriaPantallas.recomendaciones?.first
                val accesos = listOf(
                    AccesoDef("Mi agenda", "Tus inscripciones", Icons.Default.Event, ColorSeccion.AGENDA,
                        indicadorAgenda(resumen), onAgenda),
                    AccesoDef("Para ti", "Recomendadas con IA", Icons.Default.AutoAwesome, ColorSeccion.PARA_TI,
                        indicadorRecomendaciones(paquete?.recomendaciones?.size ?: 0), onRecomendaciones),
                    AccesoDef("Asistente IA", "Pregunta lo que necesites", Icons.Default.QuestionAnswer,
                        ColorSeccion.ASISTENTE, IndicadorAcceso("IA"), onAsistente),
                    AccesoDef("Actividades", "Catálogo del evento", Icons.Default.Search, ColorSeccion.ACTIVIDADES, null, onCatalogo),
                    AccesoDef("Clasificación", "Resultados y posiciones", Icons.Default.EmojiEvents,
                        ColorSeccion.CLASIFICACION, null, onResultados),
                    AccesoDef("Por día", "Programación completa", Icons.Default.CalendarMonth,
                        ColorSeccion.PROGRAMACION, null, onProgramacion)
                )
                accesos.chunked(2).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        fila.forEach { a ->
                            AccesoRapido(a.titulo, a.descripcion, a.icono, a.color, Modifier.weight(1f), a.indicador, a.accion)
                        }
                    }
                }
            }

            // US-81-T07: actividades y competencias destacadas, siempre con portada.
            Box(Modifier.aparecer(4)) { SeccionDestacadas(resumen?.destacadas, onAbrir = { AvisoPendiente.abrir(it) }, onVerTodo = onCatalogo) }

            // US-81-T06: avisos recientes con imagen.
            Box(Modifier.aparecer(5)) { SeccionAvisos(resumen?.avisos) }

            // US-72: lo que empieza pronto en el campus.
            Box(Modifier.aparecer(6)) { TarjetaProximamente(token, omitir = idMiProxima) }
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
private fun Portada(nombre: String, resumen: ResumenInicio?, hoy: LocalDate, onCatalogo: () -> Unit, onProgramacion: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        FondoVivo(Modifier.matchParentSize())
        Image(painterResource(R.drawable.bg_campus), contentDescription = null, contentScale = ContentScale.Crop,
            alpha = 0.14f, modifier = Modifier.matchParentSize())
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 66.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(nombreDia(hoy.toString()).uppercase(Peru), color = VynkSky, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.aparecer(0))
            Text(if (nombre.isBlank()) "Bienvenido a VYNK" else "Hola, $nombre", color = Color.White,
                style = MaterialTheme.typography.headlineMedium, modifier = Modifier.aparecer(0))
            Text(resumen?.mensaje?.ifBlank { null } ?: "Descubre lo que pasa hoy en el campus.",
                color = Color(0xFFD6E4F7), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.aparecer(1))
            // US-81: resumen en cifras.
            if (resumen != null && resumen.inscritas > 0) Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp).aparecer(1)) {
                Cifra(resumen.hoy, "hoy")
                Cifra(resumen.semana, "esta semana")
                Cifra(resumen.inscritas, if (resumen.inscritas == 1) "inscrita" else "inscritas")
                if (resumen.equiposPorConfirmar > 0) Cifra(resumen.equiposPorConfirmar,
                    if (resumen.equiposPorConfirmar == 1) "pendiente" else "pendientes", alerta = true)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.aparecer(1)) {
                BotonVidrio("Explorar", Icons.Default.Search, relleno = true, onClick = onCatalogo)
                BotonVidrio("Por día", Icons.Default.CalendarMonth, relleno = false, onClick = onProgramacion)
            }
        }
    }
}

@Composable
private fun Cifra(valor: Int, texto: String, alerta: Boolean = false) {
    Row(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = if (alerta) 0.22f else 0.12f))
        .padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$valor", color = if (alerta) Color(0xFFFFD48A) else Color.White, style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold)
        Text(" $texto", color = Color(0xFFD6E4F7), style = MaterialTheme.typography.labelMedium, maxLines = 1)
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
internal fun MetaAviso(iso: String, claro: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (esReciente(iso)) Pildora("Nuevo", VynkSky, Color.White)
        Text(fechaAviso(iso), style = MaterialTheme.typography.labelSmall, color = if (claro) Color(0xFFD6E4F7) else VynkSubtle)
    }
}

/** Foto del aviso: si no carga, no deja un hueco vacío. */
@Composable
internal fun FotoAviso(url: String, alto: androidx.compose.ui.unit.Dp = 150.dp, respaldo: @Composable () -> Unit = {}) {
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
    val f = foto
    if (f != null) Image(f, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(alto))
    else respaldo()
}

/** Un acceso rápido del Inicio: a dónde lleva, cómo se ve y qué indicador muestra. */
private class AccesoDef(
    val titulo: String,
    val descripcion: String,
    val icono: ImageVector,
    val color: Color,
    val indicador: IndicadorAcceso?,
    val accion: () -> Unit
)

/** Indicador de «Mi agenda»: lo que falta confirmar manda; si no, lo de hoy; si no, el total de inscripciones. */
private fun indicadorAgenda(resumen: ResumenInicio?): IndicadorAcceso? = when {
    resumen == null -> null
    resumen.equiposPorConfirmar > 0 -> IndicadorAcceso("${resumen.equiposPorConfirmar} por confirmar", alerta = true)
    resumen.hoy > 0 -> IndicadorAcceso("${resumen.hoy} hoy")
    resumen.inscritas > 0 -> IndicadorAcceso(if (resumen.inscritas == 1) "1 inscrita" else "${resumen.inscritas} inscritas")
    else -> null
}

/** Indicador de «Para ti»: cuántas recomendaciones hay ya cargadas (nada si todavía no se pidieron). */
private fun indicadorRecomendaciones(cantidad: Int): IndicadorAcceso? =
    if (cantidad <= 0) null else IndicadorAcceso(if (cantidad == 1) "1 sugerida" else "$cantidad sugeridas")
