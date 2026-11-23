package com.tecsup.tcweek.ui.agenda

import com.tecsup.tcweek.ui.catalog.BotonActualizar
import com.tecsup.tcweek.ui.catalog.DialogoInscripcionRapida
import com.tecsup.tcweek.ui.catalog.ObjetivoInscripcion
import com.tecsup.tcweek.ui.catalog.TipoMensaje
import com.tecsup.tcweek.ui.catalog.TiraMensaje
import com.tecsup.tcweek.ui.catalog.nombreDia
import com.tecsup.tcweek.ui.components.EstadoVacio
import com.tecsup.tcweek.ui.components.relojMinutos
import com.tecsup.tcweek.ui.components.rememberCargaProgramada
import com.tecsup.tcweek.ui.home.ActividadDeAviso
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.remote.AgendaInteligente
import com.tecsup.tcweek.data.remote.AgendaInteligenteApi
import com.tecsup.tcweek.data.remote.BloqueAgenda
import com.tecsup.tcweek.data.repository.CargaProgramada
import com.tecsup.tcweek.data.remote.EquipoAgenda
import com.tecsup.tcweek.data.repository.MemoriaPantallas
import com.tecsup.tcweek.data.remote.enCursoAhora
import com.tecsup.tcweek.data.repository.haceCuanto
import com.tecsup.tcweek.data.remote.leerAgendaInteligente
import com.tecsup.tcweek.data.remote.quitarSugerida
import com.tecsup.tcweek.data.remote.textoEquipo
import com.tecsup.tcweek.data.remote.vigentes
import com.tecsup.tcweek.ui.components.EsqueletoTarjetas
import com.tecsup.tcweek.ui.components.FondoVivo
import com.tecsup.tcweek.ui.components.PuntoVivo
import com.tecsup.tcweek.ui.components.aparecer
import com.tecsup.tcweek.ui.components.presionable
import com.tecsup.tcweek.ui.components.sombraSuave
import com.tecsup.tcweek.ui.theme.VynkBackground
import com.tecsup.tcweek.ui.theme.VynkBlue
import com.tecsup.tcweek.ui.theme.VynkLine
import com.tecsup.tcweek.ui.theme.VynkLineStrong as VynkLineFuerte
import com.tecsup.tcweek.ui.theme.VynkMuted
import com.tecsup.tcweek.ui.theme.VynkNavy
import com.tecsup.tcweek.ui.theme.VynkOk
import com.tecsup.tcweek.ui.theme.VynkOkSoft
import com.tecsup.tcweek.ui.theme.VynkSky
import com.tecsup.tcweek.ui.theme.VynkWarn
import com.tecsup.tcweek.ui.theme.VynkWarnSoft
import org.json.JSONObject

private enum class FiltroAgenda(val texto: String) { TODO("Todo"), MIAS("Mis actividades"), SUGERIDAS("Sugeridas por IA") }

/**
 * US-77 T4: agenda inteligente en el celular. Una línea de tiempo por día con lo que ya tengo (individual y en
 * equipo, con su estado sincronizado: T06) y lo que la IA me sugiere sin cruces, que puedo abrir o inscribirme.
 */
@Composable
fun AgendaInteligenteScreen(onVolver: () -> Unit) {
    val carga = rememberCargaProgramada(
        clave = AgendaInteligenteApi.CLAVE,
        memoria = { MemoriaPantallas.agenda },
        recordar = { datos, marca -> MemoriaPantallas.agenda = datos to marca },
        desdeJson = { json, en -> leerAgendaInteligente(JSONObject(json), en) },
        vence = { _, ahora -> ahora + CargaProgramada.VIGENCIA_AGENDA_MS },
        cargar = { token -> AgendaInteligenteApi.cargar(token) }
    )
    val ahora = relojMinutos()
    var filtro by remember { mutableStateOf(FiltroAgenda.TODO) }
    var mensaje by remember { mutableStateOf<Pair<String, TipoMensaje>?>(null) }
    var detalleId by remember { mutableStateOf<String?>(null) }
    var rapida by remember { mutableStateOf<ObjetivoInscripcion?>(null) }
    var noPuedo by remember { mutableStateOf<BloqueAgenda?>(null) }
    val agenda = carga.datos?.let { vigentes(it, ahora) }

    LazyColumn(
        Modifier.fillMaxSize().background(VynkBackground),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "cabecera") {
            CabeceraAgenda(agenda, carga.actualizadoEn, ahora, carga.cargando, onVolver, carga::actualizar)
        }
        item(key = "mensajes") {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TiraMensaje(
                    if (carga.sinConexion) "Sin conexión: esta es tu agenda de ${haceCuanto(carga.actualizadoEn, ahora)}." else null,
                    TipoMensaje.SIN_CONEXION
                )
                TiraMensaje(mensaje?.first, mensaje?.second ?: TipoMensaje.INFO, onCerrar = { mensaje = null })
                agenda?.avisos?.forEach { AvisoEquipo(it) }
            }
        }
        when {
            agenda == null && carga.error == null -> item(key = "cargando") {
                EsqueletoTarjetas(3, Modifier.padding(horizontal = 16.dp))
            }
            agenda == null -> item(key = "error") {
                EstadoVacio(Icons.Filled.EventAvailable, "No pudimos armar tu agenda", carga.error.orEmpty(), esError = true) {
                    Button(onClick = carga::actualizar, shape = RoundedCornerShape(12.dp)) { Text("Reintentar") }
                }
            }
            agenda.vacia -> item(key = "vacio") {
                EstadoVacio(Icons.Filled.EventAvailable, "Tu agenda está libre",
                    agenda.aviso.ifBlank { "Cuando te inscribas o haya actividades que encajen contigo, aparecerán aquí." })
            }
            else -> {
                item(key = "filtros") {
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FiltroAgenda.entries.forEach { f -> ChipFiltro(f.texto, filtro == f) { filtro = f } }
                    }
                }
                val dias = agenda.dias.mapNotNull { d ->
                    val bloques = d.bloques.filter {
                        when (filtro) {
                            FiltroAgenda.TODO -> true
                            FiltroAgenda.MIAS -> it.inscrita || it.clase
                            FiltroAgenda.SUGERIDAS -> it.sugerida
                        }
                    }
                    if (bloques.none { !it.clase }) null else d.copy(bloques = bloques)
                }
                if (dias.isEmpty()) item(key = "sin-filtro") {
                    Text(if (filtro == FiltroAgenda.MIAS) "Aún no tienes inscripciones próximas." else "No hay sugerencias por ahora.",
                        color = VynkMuted, modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp))
                }
                var base = 0
                dias.forEach { dia ->
                    val inicio = base
                    base += dia.bloques.size
                    item(key = "dia-${dia.fecha}") {
                        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp).animateItem()) {
                            Text(dia.etiqueta, color = VynkNavy, fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium)
                            val cuantas = dia.bloques.count { !it.clase }
                            Text(if (cuantas == 1) "1 actividad" else "$cuantas actividades",
                                color = VynkMuted, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    itemsIndexed(dia.bloques, key = { _, it -> "${it.tipo}-${it.actividadId}-${it.fecha}-${it.horaInicio}" }) { j, b ->
                        val i = minOf(inicio + j, 8)
                        FilaLineaTiempo(
                            b, enCurso = b.inscrita && enCursoAhora(b, ahora),
                            onAbrir = { if (b.actividadId.isNotBlank()) detalleId = b.actividadId },
                            onInscribir = {
                                if (b.inscripcionDirecta) rapida = ObjetivoInscripcion(b.actividadId, b.nombre,
                                    "${nombreDia(b.fecha)} · ${b.horaInicio} – ${b.horaFin}", b.ubicacion)
                                else detalleId = b.actividadId
                            },
                            onNoPuedo = { noPuedo = b },
                            modifier = Modifier.padding(horizontal = 16.dp).animateItem().aparecer(i + 2)
                        )
                    }
                }
                item(key = "nota") {
                    Text("Las sugeridas no te inscriben solas. Nunca se cruzan con tus inscripciones ni con las de tu equipo" +
                        if (agenda.conHorario) " ni con tus clases." else ". Sube tu horario para que tampoco choquen con tus clases.",
                        color = VynkMuted, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp))
                }
            }
        }
    }

    val token = carga.token
    if (token != null) {
        detalleId?.let { id -> ActividadDeAviso(id, token, alCerrar = { detalleId = null }) }
        noPuedo?.let { b ->
            HojaNoPuedoAsistir(b.actividadId, b.nombre, token, onCerrar = { noPuedo = null }, onCambiado = { texto ->
                noPuedo = null
                mensaje = texto to if (texto.startsWith("Listo")) TipoMensaje.OK else TipoMensaje.ERROR
            })
        }
        rapida?.let { objetivo ->
            DialogoInscripcionRapida(objetivo, token, onCerrar = { rapida = null }, onInscrita = { nombre ->
                rapida = null
                carga.modificar { quitarSugerida(it, objetivo.id) }
                mensaje = "¡Listo! «$nombre» ya es parte de tu agenda." to TipoMensaje.OK
            })
        }
    }
}

@Composable
private fun CabeceraAgenda(
    agenda: AgendaInteligente?, actualizadoEn: Long, ahora: Long, cargando: Boolean,
    onVolver: () -> Unit, onActualizar: () -> Unit
) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))) {
        FondoVivo(Modifier.matchParentSize())
        Column(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a mi agenda", tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text("Agenda inteligente", color = Color.White, fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.headlineSmall)
                    Text("Tu plan y lo que la IA te sugiere, sin cruces", color = Color(0xFFD5E6FF),
                        style = MaterialTheme.typography.bodySmall)
                }
                BotonActualizar(cargando, onActualizar)
            }
            Row(Modifier.padding(start = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Contador(agenda?.inscritas, "inscritas", Modifier.weight(1f))
                Contador(agenda?.sugeridas, "sugeridas", Modifier.weight(1f))
                Contador(agenda?.enEquipo, "en equipo", Modifier.weight(1f))
                Contador(agenda?.equiposPorConfirmar, "pendientes", Modifier.weight(1f), alerta = (agenda?.equiposPorConfirmar ?: 0) > 0)
            }
            Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                if (agenda != null) {
                    if (agenda.origen == "ia") PuntoVivo(VynkSky) else Box(Modifier.size(7.dp).background(Color(0xFFB8C7DD), CircleShape))
                    Text(etiquetaOrigenAgenda(agenda.origen), color = Color.White, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.weight(1f))
                Text(when {
                    cargando && agenda == null -> "Armando tu agenda…"
                    cargando -> "Actualizando…"
                    actualizadoEn > 0 -> "Actualizado ${haceCuanto(actualizadoEn, ahora)}"
                    else -> ""
                }, color = Color(0xFFD5E6FF), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun Contador(valor: Int?, texto: String, modifier: Modifier, alerta: Boolean = false) {
    val animado by animateIntAsState(valor ?: 0, tween(700), label = "contador")
    Column(
        modifier.background(Color.White.copy(alpha = if (alerta) 0.24f else 0.13f), RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(if (valor == null) "–" else "$animado", color = if (alerta) Color(0xFFFFD48A) else Color.White,
            fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
        Text(texto, color = Color(0xFFD5E6FF), style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun ChipFiltro(texto: String, activo: Boolean, onClick: () -> Unit) {
    val fondo by animateColorAsState(if (activo) VynkNavy else Color.White, label = "fondo")
    val color by animateColorAsState(if (activo) Color.White else VynkNavy, label = "texto")
    Surface(color = fondo, shape = RoundedCornerShape(50), border = if (activo) null else BorderStroke(1.dp, VynkLine),
        modifier = Modifier.presionable(onClick = onClick)) {
        Text(texto, color = color, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}

@Composable
private fun AvisoEquipo(texto: String) {
    Surface(color = VynkWarnSoft, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Groups, contentDescription = null, tint = VynkWarn, modifier = Modifier.size(20.dp))
            Text(texto, color = VynkWarn, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Hora a la izquierda, riel con punto en el medio y la tarjeta a la derecha. */
@Composable
private fun FilaLineaTiempo(
    b: BloqueAgenda, enCurso: Boolean, onAbrir: () -> Unit, onInscribir: () -> Unit, onNoPuedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(48.dp).padding(top = 14.dp), horizontalAlignment = Alignment.End) {
            Text(b.horaInicio, color = when { enCurso -> VynkOk; b.clase -> VynkMuted; else -> VynkNavy }, fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.labelLarge)
            Text(b.horaFin, color = VynkMuted, style = MaterialTheme.typography.labelSmall)
        }
        val colorPunto = when { enCurso -> VynkOk; b.sugerida -> VynkSky; b.clase -> VynkLineFuerte; else -> VynkBlue }
        Canvas(Modifier.width(26.dp).fillMaxHeight()) {
            val x = size.width / 2
            drawLine(VynkLine, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
            val y = 22.dp.toPx()
            if (b.sugerida) {
                drawCircle(Color.White, 6.dp.toPx(), Offset(x, y))
                drawCircle(colorPunto, 6.dp.toPx(), Offset(x, y), style = Stroke(2.5.dp.toPx()))
            } else {
                drawCircle(colorPunto.copy(alpha = 0.22f), 9.dp.toPx(), Offset(x, y))
                drawCircle(colorPunto, 5.dp.toPx(), Offset(x, y))
            }
        }
        Box(Modifier.weight(1f).padding(bottom = 4.dp)) {
            when {
                b.clase -> TarjetaClase(b)
                b.sugerida -> TarjetaSugerida(b, onAbrir, onInscribir)
                else -> TarjetaInscrita(b, enCurso, onAbrir, onNoPuedo)
            }
        }
    }
}

@Composable
private fun TarjetaInscrita(b: BloqueAgenda, enCurso: Boolean, onAbrir: () -> Unit, onNoPuedo: () -> Unit) {
    val forma = RoundedCornerShape(18.dp)
    val acento = if (enCurso) VynkOk else VynkBlue
    Row(
        Modifier.fillMaxWidth().sombraSuave(forma, 6.dp).clip(forma).background(Color.White)
            .border(1.dp, if (enCurso) VynkOk.copy(alpha = 0.5f) else VynkLine.copy(alpha = 0.7f), forma)
            .presionable(onClick = onAbrir)
            .semantics { contentDescription = "Inscrita: ${b.nombre}, de ${b.horaInicio} a ${b.horaFin}" }
    ) {
        Box(Modifier.width(5.dp).fillMaxHeight().background(acento))
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (enCurso) {
                    PuntoVivo(VynkOk)
                    Text("AHORA", color = VynkOk, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall)
                } else Text(if (b.equipo != null) "EN EQUIPO" else "INSCRITA", color = acento,
                    fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall)
            }
            Text(b.nombre, color = VynkNavy, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Lugar(b.ubicacion)
            b.equipo?.let { ChipEquipo(it) }
            // US-79 T2: lo que está pasando ya no tiene alternativa útil.
            if (!enCurso && b.actividadId.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clip(RoundedCornerShape(50)).presionable(onClick = onNoPuedo).padding(vertical = 4.dp, horizontal = 2.dp)) {
                Icon(Icons.Filled.EventBusy, contentDescription = null, tint = VynkWarn, modifier = Modifier.size(15.dp))
                Text("No puedo asistir", color = VynkWarn, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ChipEquipo(e: EquipoAgenda) {
    val (fondo, color) = if (e.confirmado) VynkOkSoft to VynkOk else VynkWarnSoft to VynkWarn
    Surface(color = fondo, shape = RoundedCornerShape(50)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(Icons.Filled.Groups, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Text(textoEquipo(e) + if (e.esCapitan) " · capitán" else "", color = color,
                fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TarjetaSugerida(b: BloqueAgenda, onAbrir: () -> Unit, onInscribir: () -> Unit) {
    val forma = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth().clip(forma).background(Color(0xFFF2FAFE))
            .drawBehind {
                drawRoundRect(VynkSky.copy(alpha = 0.75f), cornerRadius = CornerRadius(18.dp.toPx()),
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
            }
            .presionable(onClick = onAbrir)
            .padding(14.dp)
            .semantics { contentDescription = "Sugerida por IA: ${b.nombre}, de ${b.horaInicio} a ${b.horaFin}" },
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = VynkBlue, modifier = Modifier.size(14.dp))
            Text("SUGERIDA POR IA", color = VynkBlue, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall)
        }
        Text(b.nombre, color = VynkNavy, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        Lugar(b.ubicacion)
        if (b.motivo.isNotBlank()) Text(b.motivo, color = Color(0xFF315D9F), style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onAbrir) { Text("Ver detalle", color = VynkNavy) }
            Spacer(Modifier.width(4.dp))
            Button(onClick = onInscribir, shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VynkBlue),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                Text(if (b.porEquipos) "Con mi equipo" else "Inscribirme", fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** La clase del horario: discreta, solo para ver el día completo. */
@Composable
private fun TarjetaClase(b: BloqueAgenda) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFE9EDF3)).padding(horizontal = 14.dp, vertical = 11.dp)
            .semantics { contentDescription = "${b.nombre}, de ${b.horaInicio} a ${b.horaFin}" },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Filled.School, contentDescription = null, tint = VynkMuted, modifier = Modifier.size(17.dp))
        Text(b.nombre, color = VynkMuted, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("Tu horario", color = VynkMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun Lugar(texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(Icons.Filled.Place, contentDescription = null, tint = VynkMuted, modifier = Modifier.size(14.dp))
        Text(texto, color = VynkMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
