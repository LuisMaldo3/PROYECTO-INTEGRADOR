package com.tecsup.tcweek.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.ActividadCatalogo
import com.tecsup.tcweek.data.AuthRepository
import com.tecsup.tcweek.data.CatalogoApi
import kotlinx.coroutines.CancellationException

/** Catálogo: las actividades publicadas, agrupadas por día. */
@Composable
fun CatalogoScreen() {
    val context = LocalContext.current
    val repository = remember(context) { AuthRepository(context) }
    val token = remember { repository.currentSession()?.llave.orEmpty() }
    var actividades by remember { mutableStateOf<List<ActividadCatalogo>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }

    LaunchedEffect(token, recarga) {
        cargando = true
        error = null
        try {
            actividades = CatalogoApi.cargar(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar el catálogo."
        } finally {
            cargando = false
        }
    }

    val grupos = remember(actividades) { agruparPorDia(actividades, { it.fecha }, { it.horaInicio }) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FA))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Catálogo de actividades", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = ColorMarino)
                Text(
                    if (cargando) "Cargando actividades…" else cantidad(actividades.size, "actividad disponible", "actividades disponibles"),
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorGris
                )
            }
            TextButton(onClick = { recarga++ }, enabled = !cargando) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Actualizar")
            }
        }

        when {
            cargando -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ColorAzul)
            }

            error != null -> EstadoVacio(
                icono = Icons.Filled.ErrorOutline,
                titulo = "No pudimos cargar el catálogo",
                texto = error.orEmpty(),
                esError = true
            ) {
                OutlinedButton(onClick = { recarga++ }) { Text("Reintentar") }
            }

            actividades.isEmpty() -> EstadoVacio(
                icono = Icons.Filled.Event,
                titulo = "Pronto habrá actividades para ti",
                texto = "Cuando el administrador publique actividades, aparecerán aquí automáticamente."
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                grupos.forEach { grupo ->
                    item(key = "dia-${grupo.fecha}") {
                        EncabezadoDia(nombreDia(grupo.fecha), cantidad(grupo.items.size, "actividad", "actividades"))
                    }
                    items(grupo.items, key = { it.id }) { actividad -> TarjetaActividad(actividad) }
                }
            }
        }
    }
}

@Composable
private fun TarjetaActividad(a: ActividadCatalogo) {
    val categoria = estiloCategoria(id = a.categoriaId)
    val cupos = estadoCupos(a.cupos)
    val descripcion = a.descripcion.takeIf { it.isNotBlank() && it != "null" }
    val organizador = a.organizador.takeIf { it.isNotBlank() && it != "null" }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ColorBorde),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EtiquetaCategoria(categoria)
                Spacer(Modifier.weight(1f))
                Text(cupos.texto, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = cupos.tono.color())
            }
            Text(a.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = ColorMarino)
            Text(
                text = descripcion ?: "Sin descripción por ahora.",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = if (descripcion == null) FontStyle.Italic else FontStyle.Normal,
                color = ColorGris,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            HorizontalDivider(color = Color(0xFFEDF1F7))
            DatoConIcono(Icons.Filled.Schedule, rangoHoras(a.horaInicio, a.horaFin))
            if (organizador != null) DatoConIcono(Icons.Filled.Group, organizador)
        }
    }
}
