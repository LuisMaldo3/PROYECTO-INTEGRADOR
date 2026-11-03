package com.tecsup.tcweek.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import com.tecsup.tcweek.ui.comun.Pildora
import com.tecsup.tcweek.ui.comun.Tarjeta
import com.tecsup.tcweek.ui.comun.TextoSuave
import com.tecsup.tcweek.ui.theme.VynkBlueSoft
import com.tecsup.tcweek.ui.theme.VynkNavy
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
@OptIn(ExperimentalLayoutApi::class)
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
    Tarjeta {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                nombres == null -> TextoSuave("Cargando tus intereses…")
                fallo -> TextoSuave("No pudimos cargar tus intereses.")
                elegidos.isEmpty() -> TextoSuave("Todavía no elegiste intereses. Elígelos para recibir recomendaciones.")
                else -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    elegidos.forEach { Pildora(it, VynkBlueSoft, VynkNavy) }
                }
            }
            OutlinedButton(onClick = onElegir, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                Text(if (elegidos.isNotEmpty()) "Editar intereses" else "Elegir intereses", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
