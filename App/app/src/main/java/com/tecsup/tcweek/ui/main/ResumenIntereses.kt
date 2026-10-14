package com.tecsup.tcweek.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.AuthRepository
import com.tecsup.tcweek.data.InteresesApi
import kotlinx.coroutines.CancellationException

/** Perfil · «Mis intereses»: los que eligió la persona y un botón para cambiarlos. */
@Composable
fun ResumenIntereses(onElegir: () -> Unit) {
    val context = LocalContext.current
    val token = remember { AuthRepository(context).currentSession()?.llave.orEmpty() }
    var nombres by remember { mutableStateOf<List<String>?>(null) } // null = cargando
    var fallo by remember { mutableStateOf(false) }

    LaunchedEffect(token) {
        try {
            val catalogo = InteresesApi.catalogo(token)
            val mios = InteresesApi.mios(token)
            nombres = catalogo.filter { it.id in mios }.map { it.nombre }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fallo = true
            nombres = emptyList()
        }
    }

    val elegidos = nombres.orEmpty()
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("MIS INTERESES", style = MaterialTheme.typography.labelMedium, color = Color(0xFF6B7A99))
            Text(
                text = when {
                    nombres == null -> "Cargando tus intereses…"
                    fallo -> "No pudimos cargar tus intereses."
                    elegidos.isEmpty() -> "Todavía no elegiste intereses. Elígelos para recibir recomendaciones."
                    else -> elegidos.joinToString(" · ")
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (elegidos.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                color = Color(0xFF0D1B3E)
            )
            OutlinedButton(onClick = onElegir, modifier = Modifier.fillMaxWidth()) {
                Text(if (elegidos.isNotEmpty()) "Editar intereses" else "Elegir intereses")
            }
        }
    }
}
