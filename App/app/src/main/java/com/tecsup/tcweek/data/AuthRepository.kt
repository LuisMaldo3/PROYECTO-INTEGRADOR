package com.tecsup.tcweek.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class AccessException(message: String) : Exception(message)

class AuthRepository(
    private val sesion: SesionGuardada
) {

    suspend fun entrar(idToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        val conexion = (URL("${Servidor.DIRECCION}/auth/login").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            doOutput = true
            connectTimeout = 15000
            readTimeout = 15000
        }

        try {
            val cuerpo = JSONObject().put("idToken", idToken).toString()
            conexion.outputStream.use { salida ->
                salida.write(cuerpo.toByteArray(Charsets.UTF_8))
            }

            val codigo = conexion.responseCode
            val texto = leer(conexion, codigo)

            if (codigo !in 200..299) {
                return@withContext Result.failure(AccessException(mensajeDe(codigo, texto)))
            }

            val json = JSONObject(texto)
            val llave = json.optString("token")
            if (llave.isBlank()) {
                return@withContext Result.failure(
                    AccessException("No se pudo iniciar sesión. Intenta nuevamente.")
                )
            }

            val usuario = json.optJSONObject("usuario")
            sesion.guardar(
                DatosDeEntrada(
                    llave = llave,
                    nombre = usuario?.optString("nombre").orEmpty(),
                    correo = usuario?.optString("correo").orEmpty(),
                    rol = usuario?.optString("rol").orEmpty().ifBlank { "usuario" }
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            Result.failure(
                AccessException("No se pudo conectar. Revisa tu conexión a Internet.")
            )
        } catch (_: Exception) {
            Result.failure(
                AccessException("No se pudo iniciar sesión. Intenta nuevamente.")
            )
        } finally {
            conexion.disconnect()
        }
    }

    private fun leer(conexion: HttpURLConnection, codigo: Int): String {
        val entrada = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
        return entrada?.bufferedReader()?.use { it.readText() }.orEmpty()
    }

    private fun mensajeDe(codigo: Int, texto: String): String {
        val delServidor = runCatching { JSONObject(texto).optString("mensaje") }.getOrNull()
        if (!delServidor.isNullOrBlank()) return delServidor
        return when (codigo) {
            401 -> "No se pudo verificar tu cuenta"
            403 -> "Usa tu correo @tecsup.edu.pe"
            else -> "No se pudo iniciar sesión. Intenta nuevamente."
        }
    }
}