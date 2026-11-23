package com.tecsup.tcweek.ui.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.domain.CambiosAgenda
import com.tecsup.tcweek.data.remote.InscripcionApi
import com.tecsup.tcweek.ui.theme.VynkBlue
import com.tecsup.tcweek.ui.theme.VynkError
import com.tecsup.tcweek.ui.theme.VynkErrorSoft
import com.tecsup.tcweek.ui.theme.VynkMuted
import com.tecsup.tcweek.ui.theme.VynkNavy
import com.tecsup.tcweek.ui.theme.VynkOk
import com.tecsup.tcweek.ui.theme.VynkOkSoft
import com.tecsup.tcweek.ui.theme.VynkWarn
import com.tecsup.tcweek.ui.theme.VynkWarnSoft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Lo necesario para confirmar una inscripción rápida desde una recomendación o una sugerida (US-76-T06). */
data class ObjetivoInscripcion(val id: String, val nombre: String, val cuando: String, val lugar: String)

/**
 * Confirmación antes de inscribirse en un toque. Solo para actividades individuales con cupos; las de equipo se
 * inscriben desde el detalle (piden nombre de equipo o código).
 */
@Composable
fun DialogoInscripcionRapida(objetivo: ObjetivoInscripcion, token: String, onCerrar: () -> Unit, onInscrita: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var enviando by remember(objetivo.id) { mutableStateOf(false) }
    var error by remember(objetivo.id) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!enviando) onCerrar() },
        icon = { Icon(Icons.Filled.Event, contentDescription = null, tint = VynkBlue) },
        title = { Text("¿Te inscribes?", fontWeight = FontWeight.ExtraBold, color = VynkNavy) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(objetivo.nombre, fontWeight = FontWeight.Bold, color = VynkNavy, style = MaterialTheme.typography.titleMedium)
                FilaIcono(Icons.Filled.Event, objetivo.cuando)
                if (objetivo.lugar.isNotBlank()) FilaIcono(Icons.Filled.Place, objetivo.lugar)
                Text("Quedará en tu agenda y te recordaremos antes de que empiece.", color = VynkMuted,
                    style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = VynkError, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                enabled = !enviando,
                onClick = {
                    enviando = true
                    error = null
                    scope.launch {
                        try {
                            InscripcionApi.inscribir(token, objetivo.id)
                            CambiosAgenda.avisar() // agenda, programación y recomendaciones se actualizan solas
                            onInscrita(objetivo.nombre)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            error = e.message ?: "No se pudo completar la inscripción."
                        } finally {
                            enviando = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VynkBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (enviando) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Inscribirme", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onCerrar, enabled = !enviando) { Text("Ahora no") } }
    )
}

@Composable
private fun FilaIcono(icono: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(icono, contentDescription = null, tint = VynkBlue, modifier = Modifier.size(16.dp))
        Text(texto, color = VynkNavy, style = MaterialTheme.typography.bodyMedium)
    }
}

enum class TipoMensaje { OK, ERROR, SIN_CONEXION, INFO }

/** Tira de mensaje que aparece y desaparece con animación. */
@Composable
fun TiraMensaje(texto: String?, tipo: TipoMensaje, modifier: Modifier = Modifier, onCerrar: (() -> Unit)? = null) {
    AnimatedVisibility(texto != null, modifier = modifier, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        val (fondo, color, icono) = when (tipo) {
            TipoMensaje.OK -> Triple(VynkOkSoft, VynkOk, Icons.Filled.CheckCircle)
            TipoMensaje.ERROR -> Triple(VynkErrorSoft, VynkError, Icons.Filled.Info)
            TipoMensaje.SIN_CONEXION -> Triple(VynkWarnSoft, VynkWarn, Icons.Filled.CloudOff)
            TipoMensaje.INFO -> Triple(Color(0xFFE8F0FD), VynkBlue, Icons.Filled.Info)
        }
        Surface(color = fondo, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Text(texto.orEmpty(), color = color, style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
                if (onCerrar != null) IconButton(onClick = onCerrar) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar mensaje", tint = color, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/** Botón de actualizar que gira mientras carga. */
@Composable
fun BotonActualizar(cargando: Boolean, onClick: () -> Unit, tinte: Color = Color.White) {
    val giro = rememberInfiniteTransition(label = "giro")
    val angulo by giro.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "angulo")
    Box(
        Modifier.size(40.dp).background(tinte.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, enabled = !cargando) {
            Icon(Icons.Filled.Refresh, contentDescription = "Actualizar", tint = tinte,
                modifier = Modifier.size(20.dp).rotate(if (cargando) angulo else 0f))
        }
    }
}
