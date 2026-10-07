package com.tecsup.tcweek.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Cliente del backend Spring Boot (Usuario-back).
 * La app NUNCA habla directo con MongoDB: usa estos endpoints.
 *
 *  POST /auth/login-password  { correo, password }  -> { token, usuario }
 *  POST /auth/login           { idToken }           -> { token, usuario }   (Google)
 */
object BackendApi {

    suspend fun loginPassword(correo: String, password: String): DatosDeEntrada =
        post(
            "/auth/login-password",
            JSONObject().put("correo", correo).put("password", password)
        )

    suspend fun loginGoogle(idToken: String): DatosDeEntrada =
        post("/auth/login", JSONObject().put("idToken", idToken))

    private suspend fun post(path: String, body: JSONObject): DatosDeEntrada =
        withContext(Dispatchers.IO) {
            val conn = URL(Servidor.DIRECCION + path).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.connectTimeout = 8000
                conn.readTimeout = 15000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(text) }.getOrNull()

                if (code in 200..299 && json != null) {
                    val usuario = json.getJSONObject("usuario")
                    return@withContext DatosDeEntrada(
                        llave = json.getString("token"),
                        // optString devolveria el texto "null" si el backend manda null
                        nombre = if (usuario.isNull("nombre")) "" else usuario.optString("nombre"),
                        correo = usuario.optString("correo"),
                        rol = usuario.optString("rol", "usuario")
                    )
                }

                val mensaje = json?.optString("mensaje").orEmpty()
                when (json?.optString("codigo")) {
                    // Correo que no es @tecsup.edu.pe -> pantalla "Acceso denegado"
                    "DOMINIO_NO_PERMITIDO" -> throw AccessException(
                        mensaje.ifBlank { "Usa tu correo @tecsup.edu.pe" },
                        denied = true
                    )

                    else -> throw AccessException(
                        mensaje.ifBlank { "El servidor respondió con un error ($code)." }
                    )
                }
            } catch (e: java.net.SocketTimeoutException) {
                android.util.Log.e("BackendApi", "Tiempo agotado en ${Servidor.DIRECCION}$path", e)
                throw AccessException(
                    "El servidor tardó demasiado en responder (${e.message}). " +
                            "Revisa la consola del backend."
                )
            } catch (e: IOException) {
                android.util.Log.e("BackendApi", "Fallo al llamar a ${Servidor.DIRECCION}$path", e)
                throw AccessException(
                    "No se pudo conectar con el servidor (${Servidor.DIRECCION}). " +
                            "Detalle: ${e.javaClass.simpleName}: ${e.message}"
                )
            } finally {
                conn.disconnect()
            }
        }
}
