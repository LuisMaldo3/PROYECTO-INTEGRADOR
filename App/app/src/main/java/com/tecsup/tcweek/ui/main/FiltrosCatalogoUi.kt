package com.tecsup.tcweek.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.FiltrosCatalogo
import com.tecsup.tcweek.data.Franja
import com.tecsup.tcweek.data.OpcionesFiltro
import com.tecsup.tcweek.ui.theme.VynkMuted
import com.tecsup.tcweek.ui.theme.VynkNavy
import com.tecsup.tcweek.ui.theme.colorArea

/**
 * US-73 T02 y T05: filtros del catálogo por horario (mañana, tarde, noche), categoría y las cinco áreas de Tecsup.
 * Tocar un filtro ya elegido lo quita. Cada opción muestra cuántas actividades tiene en total.
 */
@Composable
fun BarraFiltros(filtros: FiltrosCatalogo, opciones: OpcionesFiltro, onCambiar: (FiltrosCatalogo) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        FilaFiltro("Horario") {
            items(Franja.entries, key = { it.id }) { franja ->
                Chip(
                    texto = conTotal(franja.titulo, opciones.franjas[franja]),
                    elegido = filtros.franja == franja,
                    onClick = { onCambiar(filtros.copy(franja = if (filtros.franja == franja) null else franja)) }
                )
            }
        }
        if (opciones.categorias.isNotEmpty()) FilaFiltro("Categoría") {
            items(opciones.categorias, key = { it.id }) { c ->
                Chip(
                    texto = conTotal(c.nombre, c.total),
                    elegido = filtros.categoria == c.id,
                    onClick = { onCambiar(filtros.copy(categoria = if (filtros.categoria == c.id) "" else c.id)) }
                )
            }
        }
        FilaFiltro("Área") {
            items(opciones.areas, key = { it.id }) { area ->
                val color = colorArea(area.nombre)
                Chip(
                    texto = conTotal(area.nombre, area.total),
                    elegido = filtros.area == area.id,
                    fondoElegido = color.fuerte,
                    textoElegido = color.texto,
                    onClick = { onCambiar(filtros.copy(area = if (filtros.area == area.id) "" else area.id)) }
                )
            }
        }
        if (filtros.activos > 0) Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (filtros.activos == 1) "1 filtro activo" else "${filtros.activos} filtros activos",
                style = MaterialTheme.typography.labelMedium, color = VynkMuted
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onCambiar(FiltrosCatalogo()) }) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(" Quitar filtros")
            }
        }
    }
}

@Composable
private fun FilaFiltro(titulo: String, contenido: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, style = MaterialTheme.typography.labelMedium, color = VynkMuted,
            modifier = Modifier.padding(start = 16.dp, end = 6.dp))
        LazyRow(contentPadding = PaddingValues(end = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), content = contenido)
    }
}

@Composable
private fun Chip(texto: String, elegido: Boolean, onClick: () -> Unit,
                 fondoElegido: Color = VynkNavy, textoElegido: Color = Color.White) {
    FilterChip(
        selected = elegido,
        onClick = onClick,
        label = { Text(texto, style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = fondoElegido, selectedLabelColor = textoElegido
        )
    )
}

private fun conTotal(nombre: String, total: Int?) = if (total == null) nombre else "$nombre · $total"
