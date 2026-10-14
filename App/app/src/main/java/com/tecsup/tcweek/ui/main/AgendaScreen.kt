package com.tecsup.tcweek.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.ActividadAgenda
import com.tecsup.tcweek.data.AgendaApi
import com.tecsup.tcweek.data.SesionGuardada
import kotlinx.coroutines.CancellationException
import java.time.LocalDate

/** Agenda: las actividades en las que está inscrita la persona, agrupadas por día. */
@Composable
fun AgendaScreen() {
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<ActividadAgenda>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var upcoming by remember { mutableStateOf(false) }

    LaunchedEffect(reload) {
        loading = true
        error = null
        try {
            val token = SesionGuardada(context).leer()?.llave ?: throw IllegalStateException("Inicia sesión para consultar tu agenda.")
            items = AgendaApi.cargar(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar la agenda."
        } finally {
            loading = false
        }
    }

    val hoy = LocalDate.now().toString()
    val visibles = items.filter { fechaValida(it.fecha) != null && (!upcoming || it.fecha >= hoy) }
    val grupos = agruparPorDia(visibles, { it.fecha }, { it.inicio })

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FA))) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
            Text("Mi agenda", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = ColorMarino)
            Text(
                if (loading) "Cargando tu agenda…" else cantidad(items.size, "actividad inscrita", "actividades inscritas"),
                style = MaterialTheme.typography.bodySmall,
                color = ColorGris
            )
            Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !upcoming, onClick = { upcoming = false }, label = { Text("Todas") })
                FilterChip(selected = upcoming, onClick = { upcoming = true }, label = { Text("Próximas") })
            }
        }

        when {
            loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ColorAzul)
            }

            error != null -> EstadoVacio(
                icono = Icons.Filled.ErrorOutline,
                titulo = "No pudimos cargar tu agenda",
                texto = error.orEmpty(),
                esError = true
            ) {
                OutlinedButton(onClick = { reload++ }) { Text("Reintentar") }
            }

            visibles.isEmpty() -> EstadoVacio(
                icono = Icons.Filled.Event,
                titulo = "Tu agenda está lista para empezar",
                texto = if (upcoming) "No tienes actividades próximas." else "Cuando te inscribas en una actividad, aparecerá aquí."
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                grupos.forEach { grupo ->
                    item(key = "dia-${grupo.fecha}") {
                        EncabezadoDia(nombreDia(grupo.fecha), cantidad(grupo.items.size, "actividad", "actividades"))
                    }
                    items(grupo.items, key = { it.id.ifBlank { it.nombre + it.fecha + it.inicio } }) { actividad -> TarjetaAgenda(actividad) }
                }
            }
        }
    }
}

@Composable
private fun TarjetaAgenda(a: ActividadAgenda) {
    val cancelada = a.estado == "cancelada"
    val categoria = estiloCategoria(nombre = a.categoria)
    val ubicacion = a.ubicacion.takeIf { it.isNotBlank() && it != "null" && it != "Por confirmar" } ?: "Ubicación por confirmar"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ColorBorde),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().alpha(if (cancelada) 0.65f else 1f)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(modifier = Modifier.width(64.dp)) {
                Text(a.inicio.ifBlank { "--:--" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = ColorMarino)
                if (a.fin.isNotBlank()) Text("hasta ${a.fin}", style = MaterialTheme.typography.labelSmall, color = ColorGris)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    EtiquetaCategoria(categoria)
                    Surface(color = if (cancelada) Color(0xFFEEF2F7) else Color(0xFFE2F6EA), shape = RoundedCornerShape(50)) {
                        Text(
                            if (cancelada) "Cancelada" else "Inscrito",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (cancelada) Color(0xFF5A6A82) else Color(0xFF14673B)
                        )
                    }
                }
                Text(
                    a.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorMarino,
                    textDecoration = if (cancelada) TextDecoration.LineThrough else TextDecoration.None
                )
                DatoConIcono(Icons.Filled.Place, ubicacion)
            }
        }
    }
}
