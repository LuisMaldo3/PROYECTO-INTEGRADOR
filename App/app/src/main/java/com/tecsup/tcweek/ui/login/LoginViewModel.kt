package com.tecsup.tcweek.ui.login

import android.app.Application
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.tecsup.tcweek.R
import com.tecsup.tcweek.data.AccessException
import com.tecsup.tcweek.data.AuthRepository
import com.tecsup.tcweek.data.SesionGuardada
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val sesion = SesionGuardada(application)
    private val repositorio = AuthRepository(sesion)

    private val _estado = MutableStateFlow(LoginUiState())
    val estado = _estado.asStateFlow()

    init {
        if (sesion.leer() != null) {
            _estado.update { it.copy(momento = MomentoIngreso.Correcto, mensaje = null) }
        }
    }

    fun ingresar(context: Context) {
        val actual = _estado.value
        if (actual.momento == MomentoIngreso.Cargando || actual.momento == MomentoIngreso.Correcto) {
            return
        }

        _estado.update { it.copy(momento = MomentoIngreso.Cargando, mensaje = null) }

        viewModelScope.launch {
            try {
                val idToken = pedirCuenta(context)
                if (idToken == null) {
                    _estado.update { it.copy(momento = MomentoIngreso.Esperando, mensaje = null) }
                    return@launch
                }

                repositorio.entrar(idToken).fold(
                    onSuccess = {
                        _estado.update { it.copy(momento = MomentoIngreso.Correcto, mensaje = null) }
                    },
                    onFailure = { error ->
                        _estado.update {
                            it.copy(
                                momento = MomentoIngreso.Error,
                                mensaje = error.message ?: "No se pudo iniciar sesión."
                            )
                        }
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: AccessException) {
                _estado.update {
                    it.copy(
                        momento = MomentoIngreso.Error,
                        mensaje = e.message ?: "No se pudo iniciar sesión."
                    )
                }
            } catch (_: Exception) {
                _estado.update {
                    it.copy(
                        momento = MomentoIngreso.Error,
                        mensaje = "No se pudo iniciar sesión. Intenta nuevamente."
                    )
                }
            }
        }
    }

    private suspend fun pedirCuenta(context: Context): String? {
        return try {
            val googleOption = GetGoogleIdOption.Builder()
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()

            val response = CredentialManager.create(context).getCredential(
                context = context,
                request = request
            )
            val credential = response.credential

            if (
                credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                throw AccessException("No se recibió una credencial válida de Google.")
            }

            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (_: GetCredentialCancellationException) {
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: NoCredentialException) {
            throw AccessException(
                "No se encontró una cuenta en el celular. Agrega tu cuenta de Tecsup y vuelve a intentarlo."
            )
        } catch (e: GetCredentialException) {
            throw AccessException(
                "No se pudo abrir el acceso con la cuenta institucional. Revisa la configuración y vuelve a intentarlo."
            )
        }
    }
}