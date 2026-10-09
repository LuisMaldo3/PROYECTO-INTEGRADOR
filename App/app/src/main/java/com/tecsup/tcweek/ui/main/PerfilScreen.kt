package com.tecsup.tcweek.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialException
import com.tecsup.tcweek.data.AuthRepository
import com.tecsup.tcweek.data.DatosDeEntrada
import kotlinx.coroutines.launch

private val TextoOscuro = Color(0xFF0D1B3E)
private val GrisTexto = Color(0xFF6B7A99)

/** Perfil: datos de la persona y cierre de sesión. */
@Composable
fun PerfilScreen(user: DatosDeEntrada, rol: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "Mi perfil",
            style = MaterialTheme.typography.headlineMedium,
            color = TextoOscuro
        )

        if (user.nombre.isNotBlank()) {
            Text(text = user.nombre, style = MaterialTheme.typography.titleMedium, color = TextoOscuro)
        }
        Text(text = user.correo, color = GrisTexto)
        Text(text = "Rol: ${etiquetaRol(rol)}", color = GrisTexto)

        Button(
            enabled = !closing,
            onClick = {
                closing = true

                scope.launch {
                    AuthRepository(context).logout()

                    try {
                        CredentialManager.create(context)
                            .clearCredentialState(ClearCredentialStateRequest())
                    } catch (_: ClearCredentialException) {
                        // La sesión de VYNK ya quedó cerrada.
                    }

                    onLogout()
                }
            }
        ) {
            Text(if (closing) "Cerrando..." else "Cerrar sesión")
        }
    }
}
