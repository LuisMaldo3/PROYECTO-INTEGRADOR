package com.tecsup.tcweek.data

import android.content.Context
import android.util.Base64
import android.util.Patterns
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

/**
 * [denied] = true cuando la cuenta NO tiene permiso (correo que no es @tecsup.edu.pe):
 * la UI muestra la pantalla "Acceso denegado".
 */
class AccessException(
    message: String,
    val denied: Boolean = false
) : Exception(message)

class AuthRepository(context: Context) {

    private val sesion = SesionGuardada(context.applicationContext)

    companion object {
        fun isInstitutional(email: String): Boolean {
            val mail = email.trim()

            return Patterns.EMAIL_ADDRESS.matcher(mail).matches() &&
                    mail.substringAfterLast("@")
                        .equals("tecsup.edu.pe", ignoreCase = true)
        }

        /** El token de VYNK es un JWT: se revisa su fecha de vencimiento (exp). */
        private fun vencido(token: String): Boolean = try {
            val payload = token.split(".")[1]
            val bytes = Base64.decode(
                payload,
                Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
            )
            JSONObject(String(bytes)).getLong("exp") * 1000 <= System.currentTimeMillis()
        } catch (e: Exception) {
            true
        }
    }

    private suspend fun authenticate(
        operation: suspend () -> DatosDeEntrada
    ): Result<Unit> {
        return try {
            val datos = operation()

            if (!isInstitutional(datos.correo)) {
                throw AccessException("El correo no pertenece a Tecsup.", denied = true)
            }

            sesion.guardar(datos)
            Result.success(Unit)
        } catch (e: CancellationException) {
            sesion.borrar()
            throw e
        } catch (e: Exception) {
            sesion.borrar()
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<Unit> = authenticate {
        val mail = email.trim()

        if (!isInstitutional(mail)) {
            throw AccessException("Usa tu correo @tecsup.edu.pe.")
        }

        BackendApi.loginPassword(mail, password)
    }

    suspend fun loginWithGoogle(idToken: String): Result<Unit> = authenticate {
        BackendApi.loginGoogle(idToken)
    }

    /** Hay sesión guardada, institucional y con token todavía vigente. */
    fun restoreSession(): Boolean {
        val datos = sesion.leer() ?: return false

        val valida = isInstitutional(datos.correo) && !vencido(datos.llave)
        if (!valida) sesion.borrar()
        return valida
    }

    fun currentSession(): DatosDeEntrada? =
        sesion.leer()?.takeIf { isInstitutional(it.correo) && !vencido(it.llave) }

    fun logout() = sesion.borrar()
}
