package com.tecsup.tcweek.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.ActividadCatalogo
import com.tecsup.tcweek.data.CatalogoApi
import com.tecsup.tcweek.ui.theme.VynkBlue
import kotlinx.coroutines.CancellationException

/**
 * Abre el detalle de la actividad de un aviso. Usa el mismo diálogo del catálogo, así el estudiante ve fecha,
 * lugar y su estado de inscripción igual que si la hubiera buscado él mismo. Si el aviso fue un cambio de
 * horario, [nota] muestra arriba el horario anterior y el nuevo.
 */
@Composable
internal fun ActividadDeAviso(actividadId: String, token: String, nota: String = "", alCerrar: () -> Unit) {
    var actividad by remember(actividadId) { mutableStateOf<ActividadCatalogo?>(null) }
    var error by remember(actividadId) { mutableStateOf<String?>(null) }

    LaunchedEffect(actividadId, token) {
        actividad = null
        error = null
        try {
            actividad = CatalogoApi.detalle(token, actividadId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "No se pudo abrir la actividad."
        }
    }

    val cargada = actividad
    val fallo = error
    when {
        cargada != null -> DialogoActividad(
            cargada, token, alCerrar = alCerrar, onVerActividad = { actividad = it },
            // La nota es de esta actividad: no se arrastra a una «similar» que se abra desde el diálogo.
            nota = if (cargada.id == actividadId) nota else ""
        )

        fallo != null -> AlertDialog(
            onDismissRequest = alCerrar,
            title = { Text("No pudimos abrir la actividad") },
            text = { Text(fallo) },
            confirmButton = { TextButton(onClick = alCerrar) { Text("Cerrar") } }
        )

        else -> AlertDialog(
            onDismissRequest = alCerrar,
            confirmButton = {},
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(Modifier.size(28.dp), color = VynkBlue)
                    Text("Abriendo tu actividad…")
                }
            }
        )
    }
}
