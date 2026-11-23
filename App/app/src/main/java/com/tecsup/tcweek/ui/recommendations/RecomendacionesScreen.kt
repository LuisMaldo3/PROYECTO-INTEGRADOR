package com.tecsup.tcweek.ui.recommendations

import com.tecsup.tcweek.ui.theme.ColorSeccion
import com.tecsup.tcweek.ui.components.oscurecer
import com.tecsup.tcweek.ui.catalog.BotonActualizar
import com.tecsup.tcweek.ui.catalog.DialogoInscripcionRapida
import com.tecsup.tcweek.ui.catalog.ImagenPortadaCatalogo
import com.tecsup.tcweek.ui.catalog.ObjetivoInscripcion
import com.tecsup.tcweek.ui.catalog.TipoMensaje
import com.tecsup.tcweek.ui.catalog.TiraMensaje
import com.tecsup.tcweek.ui.catalog.estiloCategoria
import com.tecsup.tcweek.ui.catalog.nombreDia
import com.tecsup.tcweek.ui.catalog.portadaTematica
import com.tecsup.tcweek.ui.components.EstadoVacio
import com.tecsup.tcweek.ui.components.relojMinutos
import com.tecsup.tcweek.ui.components.rememberCargaProgramada
import com.tecsup.tcweek.ui.home.ActividadDeAviso
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.repository.MemoriaPantallas
import com.tecsup.tcweek.data.remote.PaqueteRecomendaciones
import com.tecsup.tcweek.data.remote.RecomendacionIa
import com.tecsup.tcweek.data.remote.RecomendacionesApi
import com.tecsup.tcweek.data.remote.RecomendacionesAppApi
import com.tecsup.tcweek.data.repository.haceCuanto
import com.tecsup.tcweek.data.remote.leerRecomendacionesApp
import com.tecsup.tcweek.data.remote.origenRecomendaciones
import com.tecsup.tcweek.ui.components.EsqueletoTarjetas
import com.tecsup.tcweek.ui.components.FondoVivo
import com.tecsup.tcweek.ui.components.IconoDegradado
import com.tecsup.tcweek.ui.components.PuntoVivo
import com.tecsup.tcweek.ui.components.aparecer
import com.tecsup.tcweek.ui.components.presionable
import com.tecsup.tcweek.ui.components.sombraSuave
import com.tecsup.tcweek.ui.theme.VynkBackground
import com.tecsup.tcweek.ui.theme.VynkBlue
import com.tecsup.tcweek.ui.theme.VynkBlueSoft
import com.tecsup.tcweek.ui.theme.VynkLine
import com.tecsup.tcweek.ui.theme.VynkMuted
import com.tecsup.tcweek.ui.theme.VynkNavy
import com.tecsup.tcweek.ui.theme.VynkOk
import com.tecsup.tcweek.ui.theme.VynkOkSoft
import com.tecsup.tcweek.ui.theme.VynkSky
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * US-76 T4: recomendaciones de IA en el celular. Cada tarjeta dice por qué se recomienda, si encaja en las horas
 * libres, y permite ver el detalle, inscribirse (T06) o descartarla. Los datos se cargan de forma programada
 * (T3): se ven al instante con lo último guardado y se actualizan solos.
 */
@Composable
fun RecomendacionesScreen(onSubirHorario: () -> Unit = {}, onIntereses: () -> Unit = {}) {
    val carga = rememberCargaProgramada(
        clave = RecomendacionesAppApi.CLAVE,
        memoria = { MemoriaPantallas.recomendaciones },
        recordar = { datos, marca -> MemoriaPantallas.recomendaciones = datos to marca },
        desdeJson = { json, en -> leerRecomendacionesApp(JSONObject(json), en) },
        vence = { datos, _ -> datos.vigenteHasta },
        cargar = { token -> RecomendacionesAppApi.cargar(token) }
    )
    val ahora = relojMinutos()
    val scope = rememberCoroutineScope()
    var mensaje by remember { mutableStateOf<Pair<String, TipoMensaje>?>(null) }
    var descartando by remember { mutableStateOf<String?>(null) }
    var detalleId by remember { mutableStateOf<String?>(null) }
    var rapida by remember { mutableStateOf<ObjetivoInscripcion?>(null) }

    fun quitar(id: String) {
        carga.modificar { p -> p.copy(recomendaciones = p.recomendaciones.filterNot { it.actividad.id == id }) }
        val enMemoria = MemoriaPantallas.recomendaciones
        if (enMemoria != null) MemoriaPantallas.recomendaciones = carga.datos!! to enMemoria.second
    }

    fun descartar(r: RecomendacionIa) {
        val token = carga.token ?: return
        if (descartando != null) return
        val anterior = carga.datos ?: return
        descartando = r.actividad.id
        quitar(r.actividad.id)
        scope.launch {
            try {
                RecomendacionesApi.descartar(token, r.actividad.id)
                mensaje = "No volveremos a recomendarte «${r.actividad.nombre}»." to TipoMensaje.INFO
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                carga.datos = anterior
                mensaje = (e.message ?: "No se pudo guardar tu preferencia.") to TipoMensaje.ERROR
            } finally {
                descartando = null
            }
        }
    }

    val datos = carga.datos
    LazyColumn(
        Modifier.fillMaxSize().background(VynkBackground),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "cabecera") {
            CabeceraRecomendaciones(datos, carga.actualizadoEn, ahora, carga.cargando, onActualizar = carga::actualizar)
        }
        item(key = "mensajes") {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TiraMensaje(
                    if (carga.sinConexion) "Sin conexión: te mostramos las recomendaciones de ${haceCuanto(carga.actualizadoEn, ahora)}." else null,
                    TipoMensaje.SIN_CONEXION
                )
                TiraMensaje(mensaje?.first, mensaje?.second ?: TipoMensaje.INFO, onCerrar = { mensaje = null })
            }
        }
        if (datos != null && !datos.conHorario) item(key = "horario") {
            TarjetaSubirHorario(onSubirHorario, Modifier.padding(horizontal = 16.dp).aparecer(0))
        }
        when {
            datos == null && carga.error == null -> item(key = "cargando") {
                EsqueletoTarjetas(3, Modifier.padding(horizontal = 16.dp))
            }
            datos == null -> item(key = "error") {
                EstadoVacio(Icons.Filled.AutoAwesome, "No pudimos traer tus recomendaciones", carga.error.orEmpty(), esError = true) {
                    Button(onClick = carga::actualizar, shape = RoundedCornerShape(12.dp)) { Text("Reintentar") }
                }
            }
            datos.recomendaciones.isEmpty() -> item(key = "vacio") {
                EstadoVacio(Icons.Filled.AutoAwesome, "No hay más recomendaciones por ahora",
                    if (datos.aviso.isNotBlank()) datos.aviso
                    else "Cuando se publiquen actividades que encajen contigo aparecerán aquí.") {
                    OutlinedButton(onClick = onIntereses, shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Filled.Tune, null, Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text("Revisar mis intereses")
                    }
                }
            }
            else -> {
                item(key = "titulo") {
                    Column(Modifier.padding(horizontal = 18.dp).aparecer(1), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("${datos.recomendaciones.size} actividades para ti", color = VynkNavy,
                            fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                        if (datos.aviso.isNotBlank()) Text(datos.aviso, color = VynkMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                itemsIndexed(datos.recomendaciones, key = { _, r -> r.actividad.id }) { indice, r ->
                    TarjetaRecomendacionIa(
                        r, indice,
                        ocupada = descartando != null,
                        onDetalle = { detalleId = r.actividad.id },
                        onInscribir = {
                            if (r.inscripcionDirecta) rapida = ObjetivoInscripcion(r.actividad.id, r.actividad.nombre,
                                "${nombreDia(r.actividad.fecha)} · ${r.actividad.horaInicio} – ${r.actividad.horaFin}", r.actividad.ubicacion)
                            else detalleId = r.actividad.id // por equipos: nombre del equipo o código en el detalle
                        },
                        onDescartar = { descartar(r) },
                        modifier = Modifier.padding(horizontal = 16.dp).animateItem().aparecer(indice + 2)
                    )
                }
                item(key = "nota") {
                    Text("Las recomendaciones no te inscriben solas: tú decides. Lo que descartas no vuelve a aparecer.",
                        color = VynkMuted, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp))
                }
            }
        }
    }

    val token = carga.token
    if (token != null) {
        detalleId?.let { id -> ActividadDeAviso(id, token, alCerrar = { detalleId = null }) }
        rapida?.let { objetivo ->
            DialogoInscripcionRapida(objetivo, token, onCerrar = { rapida = null }, onInscrita = { nombre ->
                rapida = null
                quitar(objetivo.id)
                mensaje = "¡Listo! Te inscribiste en «$nombre». Ya está en tu agenda." to TipoMensaje.OK
            })
        }
    }
}

@Composable
private fun CabeceraRecomendaciones(
    datos: PaqueteRecomendaciones?, actualizadoEn: Long, ahora: Long, cargando: Boolean, onActualizar: () -> Unit
) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))) {
        Box(Modifier.matchParentSize().background(
            Brush.linearGradient(listOf(ColorSeccion.PARA_TI, oscurecer(ColorSeccion.PARA_TI, 0.55f)))))
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 22.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = VynkSky, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Para ti", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
                    Text("Elegidas con IA para ti", color = Color(0xFFD5E6FF),
                        style = MaterialTheme.typography.bodySmall)
                }
                BotonActualizar(cargando, onActualizar)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (datos != null) Surface(color = Color.White.copy(alpha = 0.16f), shape = RoundedCornerShape(50)) {
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (datos.conIa) PuntoVivo(VynkSky) else Box(Modifier.size(7.dp).background(Color(0xFFB8C7DD), CircleShape))
                        Text(origenRecomendaciones(datos.origen), color = Color.White, fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    when {
                        cargando && datos == null -> "Buscando…"
                        cargando -> "Actualizando…"
                        actualizadoEn > 0 -> "Actualizado ${haceCuanto(actualizadoEn, ahora)}"
                        else -> ""
                    },
                    color = Color(0xFFD5E6FF), style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun TarjetaSubirHorario(onSubirHorario: () -> Unit, modifier: Modifier = Modifier) {
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, VynkLine),
        modifier = modifier.fillMaxWidth().sombraSuave(RoundedCornerShape(18.dp), 6.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconoDegradado(Icons.Filled.UploadFile, VynkBlue, tamano = 40.dp)
            Column(Modifier.weight(1f)) {
                Text("Haz que encajen con tus clases", fontWeight = FontWeight.Bold, color = VynkNavy,
                    style = MaterialTheme.typography.bodyMedium)
                Text("Sube tu horario y solo te recomendaremos lo que cabe en tus horas libres.",
                    color = VynkMuted, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onSubirHorario, shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) { Text("Subir") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaRecomendacionIa(
    r: RecomendacionIa,
    indice: Int,
    ocupada: Boolean,
    onDetalle: () -> Unit,
    onInscribir: () -> Unit,
    onDescartar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val a = r.actividad
    val forma = RoundedCornerShape(22.dp)
    Column(
        modifier.fillMaxWidth().sombraSuave(forma, 10.dp).clip(forma).background(Color.White)
            .border(1.dp, VynkLine.copy(alpha = 0.7f), forma)
            .presionable(onClick = onDetalle)
            .semantics { contentDescription = "Recomendación ${indice + 1}: ${a.nombre}" }
    ) {
        Box(Modifier.fillMaxWidth()) {
            ImagenPortadaCatalogo(a.portadaUrl, a.nombre, portadaTematica(a), alto = 132.dp)
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                listOf(Color.Transparent, VynkNavy.copy(alpha = 0.78f)))))
            Surface(color = Color.White, shape = RoundedCornerShape(50), modifier = Modifier.padding(12.dp).align(Alignment.TopStart)) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = VynkBlue, modifier = Modifier.size(13.dp))
                    Text("#${indice + 1} para ti", color = VynkNavy, fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = onDescartar, enabled = !ocupada, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                Box(Modifier.size(30.dp).background(Color.Black.copy(alpha = 0.32f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Close, contentDescription = "No me interesa ${a.nombre}", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(estiloCategoria(id = a.categoriaId).nombre.uppercase(), color = VynkSky, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall)
                Text(a.nombre, color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Dato(Icons.Filled.Event, nombreDia(a.fecha))
                Dato(Icons.Filled.Schedule, "${a.horaInicio} – ${a.horaFin}")
            }
            if (a.ubicacion.isNotBlank()) Dato(Icons.Filled.Place, a.ubicacion)
            if (r.etiquetas.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                r.etiquetas.forEach { Etiqueta(it) }
            }
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(listOf(VynkBlueSoft, Color(0xFFF1F7FF)))).padding(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = VynkBlue, modifier = Modifier.size(17.dp).padding(top = 1.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Por qué te la recomendamos", color = VynkBlue, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium)
                        Text(r.motivo, color = VynkNavy, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDetalle, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, VynkLine)) {
                    Text("Ver detalle", color = VynkNavy, fontWeight = FontWeight.SemiBold)
                }
                Button(onClick = onInscribir, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(14.dp),
                    enabled = a.cupos != 0, colors = ButtonDefaults.buttonColors(containerColor = VynkBlue)) {
                    if (r.porEquipos) { Icon(Icons.Filled.Groups, null, Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)) }
                    Text(when {
                        a.cupos == 0 -> "Sin cupos"
                        r.porEquipos -> "Con mi equipo"
                        else -> "Inscribirme"
                    }, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Dato(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icono, contentDescription = null, tint = VynkBlue, modifier = Modifier.size(16.dp))
        Text(texto, color = VynkNavy, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Etiqueta(texto: String) {
    val (fondo, color) = when {
        texto.startsWith("Te interesa") -> VynkBlueSoft to VynkBlue
        texto.startsWith("Libre") -> VynkOkSoft to VynkOk
        texto.startsWith("Queda") -> Color(0xFFFFF3DC) to Color(0xFFA35F00)
        else -> Color(0xFFEFF2F7) to VynkMuted
    }
    Surface(color = fondo, shape = RoundedCornerShape(50)) {
        Text(texto, color = color, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}
