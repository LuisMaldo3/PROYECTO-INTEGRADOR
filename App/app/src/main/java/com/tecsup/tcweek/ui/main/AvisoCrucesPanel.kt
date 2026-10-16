package com.tecsup.tcweek.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.AvisoAlternativasCruce

private val marino = Color(0xFF152B52)
private val gris = Color(0xFF5C6C83)

/** Los conflictos se mantienen visibles. Las alternativas no generan inscripciones. */
@Composable
fun AvisoCrucesPanel(avisos: List<AvisoAlternativasCruce>, error: String? = null) {
    if (avisos.isEmpty() && error == null) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (error != null) {
            Surface(color = Color(0xFFFFF3D9), shape = RoundedCornerShape(12.dp)) {
                Text(error, color = Color(0xFF8D5D16), modifier = Modifier.padding(14.dp))
            }
        }
        avisos.forEach { aviso ->
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFAEC)),
                border = BorderStroke(1.dp, Color(0xFFF2D79A)),
                shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = Color(0xFFB27612))
                        Text("Cruce de horario", fontWeight = FontWeight.ExtraBold, color = Color(0xFF82520B))
                    }
                    Text("${aviso.cruce.actividadA} y ${aviso.cruce.actividadB}",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = marino)
                    Text("${aviso.cruce.fecha} · ${aviso.cruce.desde}–${aviso.cruce.hasta} · ${aviso.cruce.minutos} min de cruce",
                        color = gris, style = MaterialTheme.typography.bodySmall)
                    Surface(color = Color(0xFFEAF1FD), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Icon(Icons.Filled.EventAvailable, null, tint = Color(0xFF1764B5))
                                Text(resumenAlternativas(aviso.alternativas.size, aviso.error), color = marino, fontWeight = FontWeight.ExtraBold)
                            }
                            when {
                                aviso.error != null -> Text(aviso.error, color = Color(0xFF9D3344), style = MaterialTheme.typography.bodySmall)
                                aviso.alternativas.isEmpty() -> Text("Por ahora no hay opciones compatibles con cupos.", color = gris, style = MaterialTheme.typography.bodySmall)
                                else -> aviso.alternativas.forEach { alternativa ->
                                    Surface(color = Color.White, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Color(0xFFDDE7F4))) {
                                        Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(alternativa.nombre, fontWeight = FontWeight.Bold, color = marino)
                                            Text("${alternativa.fecha} · ${alternativa.inicio}–${alternativa.fin}", color = Color(0xFF1764B5), style = MaterialTheme.typography.bodySmall)
                                            if (alternativa.ubicacion.isNotBlank() && alternativa.ubicacion != "null") {
                                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Filled.Place, null, tint = gris)
                                                    Text(alternativa.ubicacion, color = gris, style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Text("Son sugerencias; inscríbete desde el catálogo si deseas participar.", color = gris, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
