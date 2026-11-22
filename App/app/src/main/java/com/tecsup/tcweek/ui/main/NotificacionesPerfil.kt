package com.tecsup.tcweek.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.NotificacionesApi
import com.tecsup.tcweek.data.PreferenciaRecomendaciones
import com.tecsup.tcweek.ui.comun.Tarjeta
import com.tecsup.tcweek.ui.theme.VynkBlue
import com.tecsup.tcweek.ui.theme.VynkError
import com.tecsup.tcweek.ui.theme.VynkMuted
import com.tecsup.tcweek.ui.theme.VynkText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Interruptor para activar o desactivar las notificaciones de recomendaciones. Se guarda en el servidor (vale para
 * todos los celulares de la cuenta) y en el celular. Cancelaciones, cambios y recordatorios siempre llegan: son de
 * actividades en las que el estudiante ya se inscribió.
 */
@Composable
fun NotificacionesRecomendaciones(token: String) {
    val context = LocalContext.current
    val local = remember { PreferenciaRecomendaciones(context) }
    var activas by remember { mutableStateOf(local.activas()) }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // El valor del servidor manda (pudo cambiarse desde otro celular).
    LaunchedEffect(token) {
        try {
            activas = NotificacionesApi.recomendacionesActivas(token)
            local.guardar(activas)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sin conexión: se muestra el último valor conocido.
        }
    }

    Tarjeta {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Recomendaciones de actividades", style = MaterialTheme.typography.titleSmall, color = VynkText)
                    Text(
                        "Como máximo una al día, de actividades con cupos según tus intereses.",
                        style = MaterialTheme.typography.bodySmall, color = VynkMuted
                    )
                }
                Switch(
                    checked = activas,
                    enabled = !guardando,
                    colors = SwitchDefaults.colors(checkedTrackColor = VynkBlue),
                    modifier = Modifier.semantics { contentDescription = "Recibir recomendaciones de actividades" },
                    onCheckedChange = { nuevo ->
                        val anterior = activas
                        activas = nuevo // se muestra al instante y se revierte si el servidor falla
                        guardando = true
                        error = null
                        scope.launch {
                            try {
                                activas = NotificacionesApi.guardarRecomendaciones(token, nuevo)
                                local.guardar(activas)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                activas = anterior
                                error = e.message ?: "No se pudo guardar. Intenta de nuevo."
                            } finally {
                                guardando = false
                            }
                        }
                    }
                )
            }
            error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = VynkError) }
        }
    }
}
