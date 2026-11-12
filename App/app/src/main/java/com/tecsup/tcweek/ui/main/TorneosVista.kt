package com.tecsup.tcweek.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.AuthRepository
import com.tecsup.tcweek.data.EquipoTorneo
import com.tecsup.tcweek.data.PartidoTorneo
import com.tecsup.tcweek.data.Torneo
import com.tecsup.tcweek.data.TorneosApi
import com.tecsup.tcweek.ui.comun.Pildora
import com.tecsup.tcweek.ui.comun.Tarjeta
import com.tecsup.tcweek.ui.comun.TextoSuave
import com.tecsup.tcweek.ui.theme.Departamentos
import com.tecsup.tcweek.ui.theme.VynkBlue
import com.tecsup.tcweek.ui.theme.VynkBlueSoft
import com.tecsup.tcweek.ui.theme.VynkError
import com.tecsup.tcweek.ui.theme.VynkErrorSoft
import com.tecsup.tcweek.ui.theme.VynkMuted
import com.tecsup.tcweek.ui.theme.VynkNavy
import com.tecsup.tcweek.ui.theme.VynkOk
import com.tecsup.tcweek.ui.theme.VynkOkSoft
import com.tecsup.tcweek.ui.theme.VynkSurface2
import com.tecsup.tcweek.ui.theme.VynkWarn
import com.tecsup.tcweek.ui.theme.colorArea
import com.tecsup.tcweek.ui.theme.VynkWarnSoft

@Composable
private fun Escudo(id: String?, alto: Int = 28) {
    val d = Departamentos.porId(id)
    if (d == null) {
        Box(Modifier.size(width = (alto * 96 / 112).dp, height = alto.dp).border(2.dp, Color(0xFFC6D1DF), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 14.dp, bottomEnd = 14.dp)))
    } else {
        Image(painterResource(d.escudo), contentDescription = "Escudo de ${d.nombre}", contentScale = ContentScale.Fit, modifier = Modifier.height(alto.dp).width((alto * 96 / 112).dp))
    }
}

@Composable
private fun LadoFila(e: EquipoTorneo?, puntaje: Int?, gana: Boolean, descansa: Boolean = false) {
    val color = when {
        e == null -> Color.Transparent
        e.area.isNotBlank() -> colorArea(e.area).fuerte
        else -> Departamentos.porId(e.departamento)?.color?.fuerte ?: Color.Transparent
    }
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(if (gana) VynkSurface2 else Color.Transparent), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(5.dp).fillMaxHeight().background(color))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.padding(vertical = 8.dp)) { Escudo(e?.departamento, 26) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(e?.nombre ?: if (descansa) "—" else "Por definir", style = MaterialTheme.typography.titleSmall,
                fontWeight = if (gana) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (e != null) VynkNavy else VynkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (e != null && e.area.isNotBlank()) Text(e.area, style = MaterialTheme.typography.labelSmall, color = VynkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (gana) Box(Modifier.padding(end = 8.dp).size(18.dp).background(VynkNavy, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, contentDescription = "Ganó", tint = Color.White, modifier = Modifier.size(12.dp))
        }
        if (puntaje != null) Text("$puntaje", Modifier.padding(end = 14.dp), style = MaterialTheme.typography.titleLarge, color = VynkNavy)
    }
}

private fun colorEstado(estado: String): Pair<Color, Color> = when (estado) {
    "finalizado" -> VynkNavy to Color.White
    "en_curso" -> VynkOkSoft to VynkOk
    "suspendido" -> VynkWarnSoft to VynkWarn
    "cancelado" -> VynkErrorSoft to VynkError
    "programado" -> VynkBlueSoft to VynkBlue
    else -> VynkSurface2 to VynkMuted
}

private val NOMBRES_ESTADO = mapOf("pendiente" to "Pendiente", "programado" to "Programado", "en_curso" to "En curso", "finalizado" to "Finalizado", "suspendido" to "Suspendido", "cancelado" to "Cancelado")

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PartidoCard(p: PartidoTorneo, t: Torneo) {
    Tarjeta {
        Column {
            if (p.nombreRonda == "Tercer puesto") Text("Tercer puesto", Modifier.padding(start = 14.dp, top = 8.dp), style = MaterialTheme.typography.labelMedium, color = VynkBlue)
            if (p.descanso) {
                LadoFila(TorneosReglas.equipo(t, p.a ?: p.b), null, gana = true, descansa = true)
            } else {
                LadoFila(TorneosReglas.equipo(t, p.a), TorneosReglas.puntajeDeLado(p, 'a'), gana = p.ganador != null && p.a == p.ganador)
                HorizontalDivider(color = Color(0xFFDDE4EE))
                LadoFila(TorneosReglas.equipo(t, p.b), TorneosReglas.puntajeDeLado(p, 'b'), gana = p.ganador != null && p.b == p.ganador)
            }
            HorizontalDivider(color = Color(0xFFDDE4EE))
            FlowRow(Modifier.fillMaxWidth().background(VynkSurface2).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (p.descanso) {
                    Pildora("Descansa", Color.White, VynkMuted)
                    Text("Avanza sin jugar", style = MaterialTheme.typography.labelSmall, color = VynkMuted)
                } else {
                    val (fondo, texto) = colorEstado(p.estado)
                    Pildora(NOMBRES_ESTADO[p.estado] ?: p.estado, fondo, texto)
                    if (p.resultado != null) Text(TorneosReglas.fraseGanador(p) { TorneosReglas.equipo(t, it)?.nombre ?: it }, style = MaterialTheme.typography.labelSmall, color = VynkMuted)
                    if (p.fecha.isNotBlank() && p.hora.isNotBlank())
                        Text("${nombreDia(p.fecha)} · ${p.hora}" + if (p.lugar.isNotBlank()) " · ${p.lugar}" else "", style = MaterialTheme.typography.labelSmall, color = VynkMuted)
                    else if (p.resultado == null)
                        Text(if (p.a != null && p.b != null) "Horario por confirmar" else "Esperando rivales", style = MaterialTheme.typography.labelSmall, color = VynkMuted)
                    if (p.provisional) Pildora("Provisional", VynkWarnSoft, VynkWarn)
                }
            }
        }
    }
}

@Composable
private fun CampeonCard(t: Torneo) {
    val e = TorneosReglas.equipo(t, t.campeon) ?: return
    val c = if (e.area.isNotBlank()) colorArea(e.area) else colorArea(Departamentos.porId(e.departamento)?.area)
    Row(Modifier.fillMaxWidth().background(c.suave, RoundedCornerShape(18.dp)).border(2.dp, c.fuerte, RoundedCornerShape(18.dp)).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Escudo(e.departamento, 64)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Campeón", style = MaterialTheme.typography.labelMedium, color = VynkMuted)
            Text(e.nombre, style = MaterialTheme.typography.headlineSmall, color = VynkNavy)
            if (e.area.isNotBlank()) Text(e.area, style = MaterialTheme.typography.bodySmall, color = VynkMuted)
            TorneosReglas.equipo(t, t.subcampeon)?.let { Text("Subcampeón: ${it.nombre}", style = MaterialTheme.typography.bodySmall, color = VynkMuted) }
        }
    }
}

/** T&C ARENA (tipo «arena») o Karadúo (tipo «karaduo») para estudiantes: llaves, calendario y campeón. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorneosVista(tipo: String) {
    val contexto = LocalContext.current
    val token = remember { AuthRepository(contexto).currentSession()?.llave.orEmpty() }
    var torneos by remember { mutableStateOf<List<Torneo>?>(null) }
    var error by remember { mutableStateOf("") }
    var version by remember { mutableIntStateOf(0) }
    var elegido by remember { mutableStateOf<String?>(null) }
    var calendario by remember { mutableStateOf(false) }

    LaunchedEffect(token, version) {
        error = ""
        try { torneos = TorneosApi.consultar(token) }
        catch (e: Exception) { error = e.message ?: "No se pudieron cargar los torneos."; if (torneos == null) torneos = emptyList() }
    }

    val lista = TorneosReglas.deTipo(torneos.orEmpty(), tipo)
    val torneo = lista.firstOrNull { it.id == elegido } ?: lista.firstOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        when {
            torneos == null -> CircularProgressIndicator(color = VynkBlue)
            error.isNotBlank() -> { Text(error, color = VynkError, style = MaterialTheme.typography.bodyMedium); Button(onClick = { torneos = null; version++ }) { Text("Reintentar") } }
            torneo == null -> Tarjeta { TextoSuave(if (tipo == "karaduo") "Todavía no se publicó el torneo de Karadúo." else "Todavía no hay torneos deportivos publicados.", Modifier.padding(16.dp)) }
            else -> {
                TextoSuave(if (tipo == "karaduo") "Enfrentamientos entre los participantes inscritos hasta la final." else "Eliminación directa entre los equipos inscritos: los ganadores avanzan hasta la final.")
                if (lista.size > 1) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    lista.forEach { t ->
                        FilterChip(selected = t.id == torneo.id, onClick = { elegido = t.id },
                            label = { Text(t.deporte, style = MaterialTheme.typography.labelLarge) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VynkNavy, selectedLabelColor = Color.White))
                    }
                }
                val (jugados, total) = TorneosReglas.avance(torneo)
                Tarjeta {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(torneo.deporte, style = MaterialTheme.typography.titleLarge, color = VynkNavy)
                        Text("${torneo.participantes.size} participantes · $jugados de $total enfrentamientos jugados", style = MaterialTheme.typography.bodySmall, color = VynkMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { torneo.participantes.forEach { Escudo(TorneosReglas.equipo(torneo, it)?.departamento, 36) } }
                    }
                }
                CampeonCard(torneo)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("Llaves", "Calendario").forEachIndexed { i, titulo ->
                        SegmentedButton(selected = (i == 1) == calendario, onClick = { calendario = i == 1 }, shape = SegmentedButtonDefaults.itemShape(i, 2),
                            colors = SegmentedButtonDefaults.colors(activeContainerColor = VynkBlueSoft, activeContentColor = VynkNavy), icon = {}) { Text(titulo, style = MaterialTheme.typography.labelLarge) }
                    }
                }
                if (!calendario) {
                    TorneosReglas.agruparRondas(torneo.partidos).forEach { ronda ->
                        Text(ronda.nombre, style = MaterialTheme.typography.titleMedium, color = VynkNavy)
                        ronda.partidos.forEach { PartidoCard(it, torneo) }
                    }
                } else {
                    val dias = TorneosReglas.porDia(TorneosReglas.calendario(torneo))
                    if (dias.isEmpty()) Tarjeta { TextoSuave("Todavía no hay horarios publicados. Cuando la organización programe los enfrentamientos, aparecerán aquí.", Modifier.padding(16.dp)) }
                    dias.forEach { (fecha, ps) ->
                        Text(nombreDia(fecha), style = MaterialTheme.typography.titleMedium, color = VynkNavy)
                        ps.forEach { PartidoCard(it, torneo) }
                    }
                }
            }
        }
    }
}
