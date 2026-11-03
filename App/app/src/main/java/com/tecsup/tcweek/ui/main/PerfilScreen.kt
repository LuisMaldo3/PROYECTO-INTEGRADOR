package com.tecsup.tcweek.ui.main

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import com.tecsup.tcweek.ui.comun.Pildora
import com.tecsup.tcweek.ui.comun.Tarjeta
import com.tecsup.tcweek.ui.theme.VynkBackground
import com.tecsup.tcweek.ui.theme.VynkBlue
import com.tecsup.tcweek.ui.theme.VynkBlueSoft
import com.tecsup.tcweek.ui.theme.VynkError
import com.tecsup.tcweek.ui.theme.VynkLineStrong
import com.tecsup.tcweek.ui.theme.VynkNavy
import com.tecsup.tcweek.ui.theme.VynkSky
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.tecsup.tcweek.data.AuthRepository
import com.tecsup.tcweek.data.DatosDeEntrada
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val TextoOscuro = Color(0xFF0B2A5B)
private val GrisTexto = Color(0xFF55657E)

/** Perfil: datos de la persona y, abajo, la opción «Cerrar sesión». */
@Composable
fun PerfilScreen(user: DatosDeEntrada, rol: String, onLogout: () -> Unit, onIntereses: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }

    val iniciales = user.nombre.ifBlank { user.correo }.trim().split(" ").filter { it.isNotBlank() }
        .take(2).joinToString("") { it.first().uppercase() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VynkBackground)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Tarjeta {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(62.dp).border(2.dp, VynkSky, CircleShape).padding(4.dp)
                        .background(VynkNavy, CircleShape), contentAlignment = Alignment.Center) {
                        Text(iniciales, color = Color.White, style = MaterialTheme.typography.titleLarge)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(user.nombre.ifBlank { "Mi cuenta" }, style = MaterialTheme.typography.titleLarge, color = TextoOscuro)
                        Text(user.correo, style = MaterialTheme.typography.bodySmall, color = GrisTexto)
                        Pildora(if (rol == ROL_USUARIO) "Estudiante" else etiquetaRol(rol), VynkBlueSoft, VynkBlue,
                            Modifier.padding(top = 4.dp))
                    }
                }
            }

            Text("Mis intereses", style = MaterialTheme.typography.titleMedium, color = TextoOscuro)
            ResumenIntereses(onElegir = onIntereses)
        }

        OutlinedButton(
            enabled = !closing,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, VynkLineStrong),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = VynkError, containerColor = Color.White),
            onClick = {
                closing = true

                scope.launch {
                    // 1) Borrar los datos de la sesión del celular: siempre, pase lo que pase después.
                    AuthRepository(context).logout()

                    // 2) Olvidar la cuenta de Google elegida. Si esto falla, la sesión de VYNK ya quedó cerrada.
                    try {
                        CredentialManager.create(context)
                            .clearCredentialState(ClearCredentialStateRequest())
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w("PerfilScreen", "No se pudo limpiar la cuenta de Google", e)
                    }

                    // 3) Llevar al inicio de sesión.
                    onLogout()
                }
            }
        ) {
            Text(if (closing) "Cerrando…" else "Cerrar sesión", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = etiqueta.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = GrisTexto
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyMedium,
            color = TextoOscuro,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        )
    }
}
