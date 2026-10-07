package com.tecsup.tcweek.ui.login

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.tecsup.tcweek.R
import com.tecsup.tcweek.data.AccessException
import com.tecsup.tcweek.data.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = AuthRepository(app)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val restored = repository.restoreSession()

                _uiState.update {
                    it.copy(
                        isSuccess = restored,
                        errorMessage = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSuccess = false,
                        errorMessage = errorText(e)
                    )
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onEmailChange(value: String) {
        _uiState.update {
            it.copy(
                email = value,
                emailError = null,
                errorMessage = null
            )
        }
    }

    fun onPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                passwordError = null,
                errorMessage = null
            )
        }
    }

    fun togglePasswordVisibility() {
        _uiState.update {
            it.copy(passwordVisible = !it.passwordVisible)
        }
    }

    fun onLoginClick() {
        val state = _uiState.value

        if (state.isLoading || state.isSuccess) return

        val email = state.email.trim()

        val emailError = when {
            email.isBlank() ->
                "Ingresa tu correo institucional"

            !AuthRepository.isInstitutional(email) ->
                "Usa un correo válido @tecsup.edu.pe"

            else -> null
        }

        val passwordError = if (state.password.isBlank()) {
            "Ingresa tu contraseña"
        } else {
            null
        }

        _uiState.update {
            it.copy(
                emailError = emailError,
                passwordError = passwordError,
                errorMessage = null
            )
        }

        if (emailError != null || passwordError != null) return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                finish(
                    repository.login(
                        email,
                        state.password
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                finish(Result.failure(e))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onGoogleClick(context: Context) {
        val state = _uiState.value

        if (state.isLoading || state.isSuccess) return

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                emailError = null,
                passwordError = null
            )
        }

        viewModelScope.launch {
            try {
                val manager = CredentialManager.create(context)
                val clientId = context.getString(R.string.google_server_client_id)

                val response = try {
                    manager.getCredential(
                        context = context,
                        request = GetCredentialRequest.Builder()
                            .addCredentialOption(
                                GetGoogleIdOption.Builder()
                                    .setServerClientId(clientId)
                                    .setFilterByAuthorizedAccounts(false)
                                    .setAutoSelectEnabled(false)
                                    .build()
                            )
                            .build()
                    )
                } catch (e: NoCredentialException) {
                    // El dispositivo/emulador no tiene cuenta de Google agregada:
                    // abre el inicio de sesión de Google (permite agregarla ahí),
                    // mostrando solo cuentas @tecsup.edu.pe.
                    manager.getCredential(
                        context = context,
                        request = GetCredentialRequest.Builder()
                            .addCredentialOption(
                                GetSignInWithGoogleOption.Builder(clientId)
                                    .setHostedDomainFilter("tecsup.edu.pe")
                                    .build()
                            )
                            .build()
                    )
                }

                val credential = response.credential

                if (
                    credential !is CustomCredential ||
                    credential.type !=
                    GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    throw AccessException(
                        "No se recibió una credencial válida de Google."
                    )
                }

                val googleCredential =
                    GoogleIdTokenCredential.createFrom(credential.data)

                finish(
                    repository.loginWithGoogle(
                        googleCredential.idToken
                    )
                )
            } catch (e: GetCredentialCancellationException) {
                _uiState.update {
                    it.copy(
                        isSuccess = false,
                        errorMessage = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Detalle real (por ejemplo SHA-1 no registrado) visible en Logcat.
                Log.e("LoginViewModel", "Fallo el acceso con Google", e)
                finish(Result.failure(e))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onDeniedHandled() {
        _uiState.update { it.copy(deniedReason = null) }
    }

    private fun finish(result: Result<Unit>) {
        val error = result.exceptionOrNull()

        if (error is CancellationException) {
            throw error
        }

        if (result.isSuccess) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSuccess = true,
                    password = "",
                    emailError = null,
                    passwordError = null,
                    errorMessage = null
                )
            }
        } else if (error is AccessException && error.denied) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSuccess = false,
                    password = "",
                    errorMessage = null,
                    deniedReason = error.message ?: "Acceso no permitido."
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSuccess = false,
                    errorMessage = error?.let(::errorText)
                        ?: "No se pudo iniciar sesión."
                )
            }
        }
    }

    private fun errorText(error: Throwable): String {
        return when (error) {
            is AccessException ->
                error.message ?: "Acceso no permitido."

            is NoCredentialException ->
                "No hay una cuenta de Google en este dispositivo. " +
                        "Ve a Ajustes > Contraseñas y cuentas > Agregar cuenta > Google, " +
                        "inicia con tu correo @tecsup.edu.pe y vuelve a intentarlo."

            is GetCredentialException ->
                "No se pudo abrir el acceso con Google. " +
                        "Revisa la configuración y vuelve a intentarlo."

            else ->
                "No se pudo iniciar sesión. Intenta nuevamente."
        }
    }
}
