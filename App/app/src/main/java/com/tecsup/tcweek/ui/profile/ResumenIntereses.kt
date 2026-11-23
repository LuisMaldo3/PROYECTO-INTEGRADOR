package com.tecsup.tcweek.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import com.tecsup.tcweek.ui.components.Pildora
import com.tecsup.tcweek.ui.components.Tarjeta
import com.tecsup.tcweek.ui.components.TextoSuave
import com.tecsup.tcweek.ui.theme.VynkBlueSoft
import com.tecsup.tcweek.ui.theme.VynkNavy
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.tecsup.tcweek.data.repository.AuthRepository
import com.tecsup.tcweek.data.remote.InteresesApi
import kotlinx.coroutines.CancellationException

private val ColoresChips = listOf(
    Color(0xFF7C3AED), Color(0xFF1557D0), Color(0xFF0891B2), Color(0xFF0F9D58),
    Color(0xFFE07B00), Color(0xFFDB2777), Color(0xFFDC2626), Color(0xFF4F46E5)
)

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
                    elegidos.forEachIndexed { i, nombre ->
                        val c = ColoresChips[i % ColoresChips.size]
                        Pildora(nombre, c.copy(alpha = 0.14f), c)
                    }
                }
            }
            Button(
                onClick = onElegir,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VynkNavy, contentColor = Color.White)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (elegidos.isNotEmpty()) "Editar intereses" else "Elegir intereses", style = MaterialTheme.typography.labelLarge)
            }
            if (elegidos.isNotEmpty()) TextoSuave("Al guardar, tus recomendaciones se actualizan solas.")
        }
    }
}
