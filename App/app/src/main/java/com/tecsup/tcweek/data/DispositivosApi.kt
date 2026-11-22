package com.tecsup.tcweek.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Le dice al backend a qué celular enviar los recordatorios de actividades.
 *
 *  PUT  /api/dispositivos       { token }   registra o actualiza el celular de la sesión
 *  POST /api/dispositivos/baja  { token }   lo olvida (al cerrar sesión)
 *
 * «token» es el identificador de Firebase Cloud Messaging de este celular, no la llave de la sesión.
 */
object DispositivosApi {
    suspend fun registrar(llaveSesion: String, tokenCelular: String) =
        enviar("PUT", "/api/dispositivos", llaveSesion, tokenCelular)

    suspend fun quitar(llaveSesion: String, tokenCelular: String) =
        enviar("POST", "/api/dispositivos/baja", llaveSesion, tokenCelular)

    private suspend fun enviar(metodo: String, ruta: String, llaveSesion: String, tokenCelular: String) =
        withContext(Dispatchers.IO) {
            val conn = URL(Servidor.DIRECCION.trimEnd('/') + ruta).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = metodo
                conn.connectTimeout = 8000
                conn.readTimeout = 15000
                conn.doOutput = true
                conn.setRequestProperty("Authorization", "Bearer $llaveSesion")
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use {
                    it.write(JSONObject().put("token", tokenCelular).toString().toByteArray(Charsets.UTF_8))
                }
                val codigo = conn.responseCode
                if (codigo !in 200..299) {
                    throw IllegalStateException(
                        if (codigo == 401) "Tu sesión expiró. Vuelve a ingresar."
                        else "No se pudo registrar el celular para los recordatorios (HTTP $codigo)."
                    )
                }
            } finally {
                conn.disconnect()
            }
        }
}
