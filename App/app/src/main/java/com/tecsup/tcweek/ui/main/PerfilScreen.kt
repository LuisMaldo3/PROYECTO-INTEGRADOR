package com.tecsup.tcweek.ui.main

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.tecsup.tcweek.data.AuthRepository
import com.tecsup.tcweek.data.DatosDeEntrada
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val TextoOscuro = Color(0xFF0D1B3E)
private val GrisTexto = Color(0xFF6B7A99)

/** Perfil: datos de la persona y, abajo, la opción «Cerrar sesión». */
@Composable
fun PerfilScreen(user: DatosDeEntrada, rol: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Mi perfil",
                style = MaterialTheme.typography.headlineMedium,
                color = TextoOscuro
            )

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    FilaDato("Nombre", user.nombre.ifBlank { "—" })
                    HorizontalDivider()
                    FilaDato("Correo", user.correo)
                    HorizontalDivider()
                    FilaDato("Rol", etiquetaRol(rol))
                }
            }
        }

        OutlinedButton(
            enabled = !closing,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextoOscuro),
            onClick = {
                closing = true

                scope.launch {
                    // 1) Borrar los datos de la sesión del celular: siempre, pase lo que pase después.
                    AuthRepository(context).logout()

                    // 2) Olvidar la cuenta de Google elegida. Si esto falla no importa: la sesión
                    //    de VYNK ya quedó cerrada, y no debe impedir llegar al inicio de sesión.
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
            Text(if (closing) "Cerrando..." else "Cerrar sesión")
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
