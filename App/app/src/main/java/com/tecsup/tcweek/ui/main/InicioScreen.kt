package com.tecsup.tcweek.ui.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.tecsup.tcweek.data.AuthRepository
import com.tecsup.tcweek.data.AvisosApi
import com.tecsup.tcweek.data.AvisoCampus
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val InicioNavy = Color(0xFF0C2142)
private val InicioBlue = Color(0xFF1769DB)

@Composable
fun InicioScreen(
    onCatalogo: () -> Unit = {},
    onRecomendaciones: () -> Unit = {},
    onAgenda: () -> Unit = {},
    onAsistente: () -> Unit = {},
    onResultados: () -> Unit = {}
) {
    val context = LocalContext.current
    val token = remember(context) { AuthRepository(context).currentSession()?.llave.orEmpty() }
    var avisos by remember { mutableStateOf<List<AvisoCampus>>(emptyList()) }
    LaunchedEffect(token) {
        avisos = runCatching { AvisosApi.recientes(token) }.getOrDefault(emptyList())
    }
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF1F5FB))
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Surface(shape = RoundedCornerShape(22.dp), shadowElevation = 5.dp) {
            Column(Modifier.fillMaxWidth()
                .background(InicioNavy)
                .padding(24.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text("VYNK · COMUNIDAD TECSUP", color = Color(0xFFB1E6FF),
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Conecta con lo que te inspira", color = Color.White,
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("Encuentra actividades, organiza tu agenda y descubre nuevas oportunidades.",
                    color = Color(0xFFE2EEFF), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onRecomendaciones, colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White, contentColor = InicioNavy),
                    shape = RoundedCornerShape(10.dp)) {
                    Text("Ver recomendaciones", fontWeight = FontWeight.Bold)
                }
            }
        }
        if (avisos.isNotEmpty()) {
            Text("Avisos recientes", color = InicioNavy, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold)
            avisos.forEach { aviso ->
                Surface(shape = RoundedCornerShape(15.dp), color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFDCE7F4))) {
                    Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        if (aviso.portadaUrl.isNotBlank()) ImagenPortadaCatalogo(aviso.portadaUrl, aviso.titulo)
                        Text(aviso.publicadoEn.take(10), style = MaterialTheme.typography.labelSmall, color = Color(0xFF2477B8))
                        Text(aviso.titulo, color = InicioNavy, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium)
                        Text(aviso.mensaje, color = Color(0xFF556981), style = MaterialTheme.typography.bodySmall)
                        if (aviso.creditoUrl.startsWith("https://commons.wikimedia.org/"))
                            Text("Foto: ${aviso.autor.ifBlank { "Wikimedia Commons" }}", color = InicioBlue,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.clickable { context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(aviso.creditoUrl))) })
                    }
                }
            }
        }
        Text("Tu espacio", color = InicioNavy, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 5.dp))
        HomeAcceso("Explorar actividades", "Consulta eventos y talleres publicados.",
            Icons.Default.Search, Color(0xFF1769DB), Color(0xFFE7F1FF), onCatalogo)
        HomeAcceso("Resultados y clasificación", "Sigue los ganadores y los puntos oficiales de las cinco áreas.",
            Icons.Default.EmojiEvents, InicioBlue, Color(0xFFE7F1FF), onResultados)
        HomeAcceso("Mi agenda", "Revisa tus actividades y horarios disponibles.",
            Icons.Default.Event, InicioBlue, Color(0xFFE7F1FF), onAgenda)
        HomeAcceso("Asistente VYNK", "Pregunta por actividades, requisitos o ubicaciones.",
            Icons.Default.QuestionAnswer, InicioBlue, Color(0xFFE7F1FF), onAsistente)
        Surface(color = Color(0xFFEAF2FC), shape = RoundedCornerShape(13.dp)) {
            Text("Tu experiencia mejora con tus intereses y tus preferencias. Puedes descartar cualquier recomendación que no sea para ti.",
                modifier = Modifier.fillMaxWidth().padding(16.dp), color = Color(0xFF345A87),
                style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HomeAcceso(titulo: String, descripcion: String, icono: ImageVector,
    color: Color, fondoIcono: Color, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFDCE7F4)), shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            Surface(color = fondoIcono, shape = RoundedCornerShape(13.dp)) {
                Icon(icono, contentDescription = null, tint = color,
                    modifier = Modifier.padding(12.dp).size(25.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(titulo, color = InicioNavy, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium)
                Text(descripcion, color = Color(0xFF63768D), style = MaterialTheme.typography.bodySmall)
            }
            Text("›", color = color, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
