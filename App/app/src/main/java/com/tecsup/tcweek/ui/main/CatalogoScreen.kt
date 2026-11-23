package com.tecsup.tcweek.ui.main

import android.content.Intent
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.tecsup.tcweek.R
import com.tecsup.tcweek.data.InscripcionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.ActividadCatalogo
import com.tecsup.tcweek.data.AuthRepository
import com.tecsup.tcweek.data.CatalogoApi
import com.tecsup.tcweek.ui.comun.EsqueletoTarjetas
import com.tecsup.tcweek.ui.comun.aparecer
import com.tecsup.tcweek.ui.comun.presionable
import com.tecsup.tcweek.ui.comun.sombraSuave
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.Brush
import com.tecsup.tcweek.data.FiltrosCatalogo
import com.tecsup.tcweek.data.Franja
import com.tecsup.tcweek.data.OpcionesFiltro
import androidx.compose.runtime.saveable.rememberSaveable
import com.tecsup.tcweek.data.CambiosAgenda
import com.tecsup.tcweek.data.RespaldoPortada
import com.tecsup.tcweek.data.portadaRemotaSegura
import com.tecsup.tcweek.data.respaldoPortada
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CancellationException
import androidx.compose.material.icons.filled.Place
import com.tecsup.tcweek.ui.comun.EtiquetaArea
import com.tecsup.tcweek.ui.comun.Pildora
import com.tecsup.tcweek.ui.theme.VynkLine
import com.tecsup.tcweek.ui.theme.VynkMuted
import com.tecsup.tcweek.ui.theme.VynkNavy
import com.tecsup.tcweek.ui.theme.VynkSubtle
import com.tecsup.tcweek.ui.theme.colorArea
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.AutoAwesome
import com.tecsup.tcweek.ui.theme.VynkSky
import com.tecsup.tcweek.ui.theme.VynkBlue
import com.tecsup.tcweek.data.PreferenciaOrdenCatalogo
import com.tecsup.tcweek.data.ResultadoCatalogo
import com.tecsup.tcweek.data.seccionesCatalogo

/** Catálogo: las actividades publicadas, agrupadas por día. */
@Composable
fun CatalogoScreen(onProgramacion: () -> Unit = {}, onVerUbicacion: (ActividadCatalogo) -> Unit = {}) {
    val context = LocalContext.current
    val repository = remember(context) { AuthRepository(context) }
    val token = remember { repository.currentSession()?.llave.orEmpty() }
    var actividades by remember { mutableStateOf<List<ActividadCatalogo>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }
    var seleccionada by remember { mutableStateOf<ActividadCatalogo?>(null) }
    // US-73: filtros elegidos (se conservan al girar la pantalla) y opciones que ofrece el servidor.
    var franjaId by rememberSaveable { mutableStateOf("") }
    var categoriaId by rememberSaveable { mutableStateOf("") }
    var areaId by rememberSaveable { mutableStateOf("") }
    // US-80 T2: «Para ti primero» o «Por fecha», recordado entre aperturas.
    val preferenciaOrden = remember(context) { PreferenciaOrdenCatalogo(context) }
    var porFecha by rememberSaveable { mutableStateOf(preferenciaOrden.porFecha) }
    val filtros = FiltrosCatalogo(Franja.de(franjaId), categoriaId, areaId, porFecha)
    var opciones by remember { mutableStateOf(OpcionesFiltro()) }
    var resultado by remember { mutableStateOf(ResultadoCatalogo(emptyList(), OpcionesFiltro())) }

    // US-73 T03: cada cambio de filtro pide al servidor la lista filtrada; el pedido anterior se cancela solo.
    LaunchedEffect(token, recarga, filtros) {
        cargando = true
        error = null
        try {
            val nuevo = CatalogoApi.buscar(token, filtros)
            resultado = nuevo
            actividades = nuevo.actividades
            opciones = nuevo.opciones
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar el catálogo."
        } finally {
            cargando = false
        }
    }

    val secciones = remember(resultado) { seccionesCatalogo(resultado) }
    val grupos = remember(secciones) { agruparPorDia(secciones.resto, { it.fecha }, { it.horaInicio }) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F5F9))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Actividades", style = MaterialTheme.typography.headlineSmall, color = VynkNavy)
                Text(
                    when {
                        cargando -> "Cargando actividades…"
                        filtros.activos > 0 -> cantidad(actividades.size, "actividad", "actividades") + " con estos filtros"
                        else -> cantidad(actividades.size, "actividad disponible", "actividades disponibles")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorGris
                )
            }
            TextButton(onClick = onProgramacion) {
                Icon(Icons.Filled.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Por día", style = MaterialTheme.typography.labelLarge)
            }
            TextButton(onClick = { recarga++ }, enabled = !cargando) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Actualizar", style = MaterialTheme.typography.labelLarge)
            }
        }

        BarraFiltros(filtros, opciones) { nuevo ->
            franjaId = nuevo.franja?.id.orEmpty()
            categoriaId = nuevo.categoria
            areaId = nuevo.area
        }
        // Solo tiene sentido elegir el orden si hay intereses guardados.
        if (resultado.intereses.isNotEmpty() || porFecha) SelectorOrden(porFecha) { valor ->
            porFecha = valor
            preferenciaOrden.porFecha = valor
        }

        when {
            cargando -> EsqueletoTarjetas(3)

            error != null -> EstadoVacio(
                icono = Icons.Filled.ErrorOutline,
                titulo = "No pudimos cargar las actividades",
                texto = error.orEmpty(),
                esError = true
            ) {
                OutlinedButton(onClick = { recarga++ }) { Text("Reintentar") }
            }

            actividades.isEmpty() && filtros.activos > 0 -> EstadoVacio(
                icono = Icons.Filled.Event,
                titulo = "Ninguna actividad coincide",
                texto = "Prueba con otro horario, categoría o área."
            ) {
                OutlinedButton(onClick = { franjaId = ""; categoriaId = ""; areaId = "" }) { Text("Quitar filtros") }
            }

            actividades.isEmpty() -> EstadoVacio(
                icono = Icons.Filled.Event,
                titulo = "Todavía no hay actividades",
                texto = "Las actividades publicadas aparecerán aquí."
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // US-80 T3: primero lo que te interesa.
                if (secciones.paraTi.isNotEmpty()) {
                    item(key = "para-ti") { EncabezadoParaTi(secciones.paraTi.size, resultado.intereses) }
                    itemsIndexed(secciones.paraTi, key = { _, a -> "pt-" + a.id }) { i, actividad ->
                        Box(Modifier.aparecer(i)) { TarjetaActividad(actividad) { seleccionada = actividad } }
                    }
                    if (secciones.resto.isNotEmpty()) item(key = "mas") {
                        Text("Más actividades", style = MaterialTheme.typography.titleMedium, color = VynkNavy,
                            modifier = Modifier.padding(top = 10.dp))
                    }
                }
                grupos.forEach { grupo ->
                    item(key = "dia-${grupo.fecha}") {
                        EncabezadoDia(nombreDia(grupo.fecha), cantidad(grupo.items.size, "actividad", "actividades"))
                    }
                    itemsIndexed(grupo.items, key = { _, a -> a.id }) { i, actividad ->
                        Box(Modifier.aparecer(i)) { TarjetaActividad(actividad) { seleccionada = actividad } }
                    }
                }
            }
        }
        if (seleccionada != null) {
            DialogoActividad(seleccionada!!, token, alCerrar = { seleccionada = null; recarga++ }, onVerActividad = { seleccionada = it },
                onVerUbicacion = { seleccionada = null; onVerUbicacion(it) })
        }
    }
}

/** US-80 T3: marca «Para ti» sobre la portada. */
@Composable
fun InsigniaParaTi(modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(Brush.horizontalGradient(listOf(VynkSky, VynkBlue)))
            .padding(horizontal = 10.dp, vertical = 5.dp).semantics { contentDescription = "Para ti" },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
        Text("Para ti", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EncabezadoParaTi(total: Int, intereses: List<String>) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(Brush.linearGradient(listOf(VynkSky, VynkBlue))),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("Para ti · " + cantidad(total, "actividad", "actividades"), style = MaterialTheme.typography.titleMedium, color = VynkNavy)
            if (intereses.isNotEmpty()) Text("Según tus intereses: " + intereses.take(4).joinToString(", "),
                style = MaterialTheme.typography.bodySmall, color = VynkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** US-80 T2: elegir el orden del catálogo. */
@Composable
private fun SelectorOrden(porFecha: Boolean, onCambiar: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Orden", style = MaterialTheme.typography.labelMedium, color = VynkMuted)
        listOf(false to "Para ti primero", true to "Por fecha").forEach { (valor, texto) ->
            val activo = porFecha == valor
            Text(texto, style = MaterialTheme.typography.labelLarge,
                color = if (activo) Color.White else VynkNavy,
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(if (activo) VynkNavy else Color.White)
                    .border(1.dp, if (activo) VynkNavy else VynkLine, RoundedCornerShape(50))
                    .presionable { onCambiar(valor) }
                    .padding(horizontal = 12.dp, vertical = 7.dp))
        }
    }
}

@Composable
private fun TarjetaActividad(a: ActividadCatalogo, onClick: () -> Unit) {
    val categoria = estiloCategoria(id = a.categoriaId)
    val cupos = estadoCupos(a.cupos)
    val descripcion = a.descripcion.takeIf { it.isNotBlank() && it != "null" }
    val area = colorArea(a.areaTecsup)
    val contexto = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().sombraSuave(RoundedCornerShape(22.dp), 8.dp).presionable(onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth()) {
            if (a.portadaUrl.isNotBlank()) ImagenPortadaCatalogo(a.portadaUrl, a.nombre, portadaTematica(a))
            else Image(painter = painterResource(id = portadaTematica(a)), contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(158.dp))
            // Velo inferior para que la hora se lea sobre cualquier portada.
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color(0xCC0B2A5B))))
            if (a.tipoActividad == "competencia") Pildora("Competencia", Color.White, VynkNavy, Modifier.padding(10.dp))
            if (a.paraTi) InsigniaParaTi(Modifier.align(Alignment.TopEnd).padding(10.dp))
            Pildora(rangoHoras(a.horaInicio, a.horaFin), Color.White.copy(alpha = 0.92f), VynkNavy,
                Modifier.align(Alignment.BottomStart).padding(10.dp))
        }
        Box(Modifier.fillMaxWidth().height(4.dp).background(area.fuerte))
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (a.portadaCreditoUrl.startsWith("https://commons.wikimedia.org/")) {
                Text("Foto: ${a.portadaAutor} · ${a.portadaLicencia}", style = MaterialTheme.typography.labelSmall, color = VynkSubtle,
                    modifier = Modifier.clickable { contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.portadaCreditoUrl))) })
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (a.areaTecsup.isNotBlank()) EtiquetaArea(a.areaTecsup) else EtiquetaCategoria(categoria)
                Spacer(Modifier.weight(1f))
                Text(cupos.texto, style = MaterialTheme.typography.labelMedium, color = cupos.tono.color())
            }
            Text(a.nombre, style = MaterialTheme.typography.titleMedium, color = VynkNavy)
            if (a.paraTi && a.motivoParaTi.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = VynkSky, modifier = Modifier.size(14.dp))
                Text(a.motivoParaTi, style = MaterialTheme.typography.labelMedium, color = VynkBlue)
            }
            if (descripcion != null) Text(descripcion, style = MaterialTheme.typography.bodySmall, color = VynkMuted,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            HorizontalDivider(color = VynkLine, modifier = Modifier.padding(vertical = 2.dp))
            DatoConIcono(Icons.Filled.Schedule, rangoHoras(a.horaInicio, a.horaFin))
            // US-71 T02: el lugar en cada tarjeta, con su referencia si existe.
            if (a.ubicacion.isNotBlank()) DatoConIcono(Icons.Filled.Place,
                a.ubicacion + if (a.ubicacionReferencia.isNotBlank()) " · ${a.ubicacionReferencia}" else "")
        }
    }
}


/** Muestra portadas publicadas. No se envía la clave de Gemini a Android. */
@Composable
fun ImagenPortadaCatalogo(url: String, nombre: String, respaldo: Int = R.drawable.vynk_actividades, alto: Dp = 158.dp) {
    var foto by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url) {
        if (!portadaRemotaSegura(url)) return@LaunchedEffect // solo https o el propio backend
        foto = withContext(Dispatchers.IO) {
            try {
                val conexion = URL(url).openConnection() as HttpURLConnection
                conexion.connectTimeout = 6500
                conexion.readTimeout = 10000
                try {
                    if (conexion.responseCode !in 200..299) return@withContext null
                    val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                    conexion.inputStream.use { BitmapFactory.decodeStream(it, null, options)?.asImageBitmap() }
                } finally { conexion.disconnect() }
            } catch (_: Exception) { null }
        }
    }
    // US-71 T06: mientras carga, o si no se pudo descargar (sin red, enlace caído), se ve la ilustración local.
    if (foto == null) {
        Image(painter = painterResource(respaldo), contentDescription = "Ilustración de $nombre",
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(alto))
    } else {
        Image(bitmap = foto!!, contentDescription = "Portada de $nombre",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(alto)
                .background(Color(0xFF123B5F)))
    }
}


/** US-71 T05: ilustración local por área (o por tema) si la actividad no tiene portada o no se puede descargar. */
internal fun portadaTematica(a: ActividadCatalogo): Int = when (respaldoPortada(a.nombre, a.areaTecsup, a.categoriaId)) {
    RespaldoPortada.DEPORTES -> R.drawable.vynk_futbol
    RespaldoPortada.QUIMICA -> R.drawable.vynk_quimica
    RespaldoPortada.ELECTRONICA -> R.drawable.vynk_electronica
    RespaldoPortada.MECANICA -> R.drawable.vynk_mecanica
    RespaldoPortada.DIGITAL -> R.drawable.vynk_digital
    RespaldoPortada.GESTION -> R.drawable.vynk_gestion
    RespaldoPortada.GENERAL -> R.drawable.vynk_actividades
}

@Composable
internal fun DialogoActividad(a: ActividadCatalogo, token: String, alCerrar: () -> Unit, onVerActividad: (ActividadCatalogo) -> Unit = {}, nota: String = "",
                               onVerUbicacion: ((ActividadCatalogo) -> Unit)? = null) {
    var consultando by remember(a.id) { mutableStateOf(true) }
    var similares by remember(a.id) { mutableStateOf<List<ActividadCatalogo>>(emptyList()) }
    var similaresError by remember(a.id) { mutableStateOf("") }
    var cargandoSimilares by remember(a.id) { mutableStateOf(true) }
    LaunchedEffect(a.id, token) {
        cargandoSimilares = true
        similaresError = ""
        try { similares = CatalogoApi.similares(token, a.id) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { similaresError = e.message ?: "No se pudieron consultar actividades similares." }
        finally { cargandoSimilares = false }
    }
    var inscrito by remember(a.id) { mutableStateOf(false) }
    var capitan by remember(a.id) { mutableStateOf(false) }
    var procesando by remember(a.id) { mutableStateOf(false) }
    var error by remember(a.id) { mutableStateOf("") }
    var codigoPrivado by remember(a.id) { mutableStateOf("") }
    // US-79: «No puedo asistir» abre las alternativas; tras cambiarse se muestra el resultado aquí.
    var noPuedo by remember(a.id) { mutableStateOf(false) }
    var resultadoCambio by remember(a.id) { mutableStateOf("") }
    var nombreEquipo by remember(a.id) { mutableStateOf("") }
    var modoEquipo by remember(a.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val esEquipos = a.tipoActividad == "competencia" && a.modalidad == "equipos"
    val finalizada = a.estado == "finalizada"
    LaunchedEffect(a.id, token) {
        consultando = true
        try {
            val datos = InscripcionApi.estado(token, a.id)
            inscrito = datos.optBoolean("inscrito", false)
            capitan = datos.optBoolean("capitan", false)
        } catch (e: Exception) { error = e.message.orEmpty() }
        finally { consultando = false }
    }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(a.nombre, color = Color(0xFF0B2A5B), fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (a.portadaUrl.isBlank()) Image(painterResource(portadaTematica(a)),
                    contentDescription = "Ilustración de ${a.nombre}", contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(155.dp))
                else ImagenPortadaCatalogo(a.portadaUrl, a.nombre, portadaTematica(a))
                // Abierta desde una alerta de cambio (horario o lugar): el valor anterior y el nuevo, resaltados.
                if (nota.isNotBlank()) Text(nota, color = Color(0xFF0B2A5B), fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF4D6), RoundedCornerShape(10.dp)).padding(12.dp))
                Text(a.descripcion, color = Color(0xFF49627D))
                Text("Fecha: ${a.fecha} · ${a.horaInicio}", fontWeight = FontWeight.SemiBold)
                Text("Lugar: ${a.ubicacion.ifBlank { "Por confirmar" }}")
                if (a.ubicacionReferencia.isNotBlank()) Text("Cómo llegar: ${a.ubicacionReferencia}", color = Color(0xFF49627D))
                if (onVerUbicacion != null) TextButton(onClick = { onVerUbicacion(a) }) {
                    Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("  Ver ubicación")
                }
                Text("Encargado: ${a.organizador.ifBlank { "Tecsup" }}")
                if (a.areaTecsup.isNotBlank()) Text("Área: ${a.areaTecsup}")
                Text("Disponibles: ${a.cupos?.toString() ?: "Por confirmar"} ${if (esEquipos) "equipos" else "cupos"}")
                if (a.reglas.isNotBlank()) Text("Reglas: ${a.reglas}")
                HorizontalDivider(color = Color(0xFFDFEAF4))
                Text("Actividades similares", color = Color(0xFF123C6A), fontWeight = FontWeight.Bold)
                if (cargandoSimilares) Text("Buscando actividades relacionadas…", color = ColorGris)
                if (similaresError.isNotBlank()) Text(similaresError, color = ColorGris)
                if (!cargandoSimilares && similares.isEmpty() && similaresError.isBlank())
                    Text("Por ahora no hay otras actividades similares disponibles.", color = ColorGris)
                similares.forEach { otra ->
                    OutlinedButton(onClick = { onVerActividad(otra) }, modifier = Modifier.fillMaxWidth()) {
                        Text("${otra.nombre}  →", maxLines = 2)
                    }
                }
                if (finalizada) Text("Esta competencia ya finalizó. No admite inscripciones nuevas.",
                    color = Color(0xFF255A87), fontWeight = FontWeight.Bold)
                if (esEquipos && !inscrito && !finalizada) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(onClick = { modoEquipo = false }) { Text(if (!modoEquipo) "✓ Crear equipo" else "Crear equipo") }
                        TextButton(onClick = { modoEquipo = true }) { Text(if (modoEquipo) "✓ Unirme" else "Unirme") }
                    }
                    OutlinedTextField(value = nombreEquipo, onValueChange = { nombreEquipo = it },
                        label = { Text(if (modoEquipo) "Código privado del equipo" else "Nombre del equipo") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                if (inscrito && capitan && esEquipos)
                    Text("Atención: cancelar como capitán retirará a tu equipo completo de esta competencia.",
                        color = Color(0xFF954B1B), fontWeight = FontWeight.Bold)
                if (inscrito && !finalizada && !consultando) BotonNoPuedoAsistir(onClick = { noPuedo = true })
                if (resultadoCambio.isNotBlank()) Text(resultadoCambio, color = Color(0xFF12805C), fontWeight = FontWeight.SemiBold)
                if (codigoPrivado.isNotBlank()) Text("Código del equipo: $codigoPrivado · Compártelo con tus integrantes.",
                    color = Color(0xFF0F5D9B), fontWeight = FontWeight.Bold)
                if (error.isNotBlank()) Text(error, color = Color(0xFFB92F39))
            }
        },
        confirmButton = {
            Button(enabled = !finalizada && !consultando && !procesando && (inscrito || !esEquipos || nombreEquipo.length >= 3),
                onClick = {
                    procesando = true; error = ""
                    scope.launch {
                        try {
                            val r = if (inscrito) InscripcionApi.cancelar(token, a.id)
                                else if (esEquipos && modoEquipo) InscripcionApi.unirse(token, a.id, nombreEquipo)
                                else InscripcionApi.inscribir(token, a.id, if (esEquipos) nombreEquipo else "")
                            codigoPrivado = r.optString("codigoEquipo")
                            inscrito = !inscrito
                            CambiosAgenda.avisar() // la agenda y la programación se actualizan solas
                        } catch (e: Exception) { error = e.message.orEmpty() }
                        finally { procesando = false }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF164F94))) {
                Text(if (procesando) "Procesando…" else if (inscrito) "Cancelar inscripción" else "Inscribirme")
            }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cerrar") } },
        containerColor = Color.White, shape = RoundedCornerShape(19.dp)
    )
    if (noPuedo) HojaNoPuedoAsistir(a.id, a.nombre, token, onCerrar = { noPuedo = false }, onCambiado = { mensaje ->
        noPuedo = false
        resultadoCambio = mensaje
        // Si la cancelación falló, el estado real se vuelve a consultar al abrir de nuevo el detalle.
        if (mensaje.startsWith("Listo")) inscrito = false
    })
}
