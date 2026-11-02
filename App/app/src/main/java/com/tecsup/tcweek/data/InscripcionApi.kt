package com.tecsup.tcweek.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object InscripcionApi {
    private suspend fun pedir(token: String, id: String, sufijo: String,
        metodo: String = "GET", contenido: JSONObject? = null): JSONObject = withContext(Dispatchers.IO) {
        require(id.matches(Regex("[a-zA-Z0-9_-]{1,100}"))) { "Identificador de actividad inválido" }
        val url = URL(Servidor.DIRECCION.trimEnd('/') + "/api/actividades/" + id + sufijo)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = metodo; conn.connectTimeout = 8000; conn.readTimeout = 14000
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/json")
            if (contenido != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(contenido.toString().toByteArray(Charsets.UTF_8)) }
            }
            val codigo = conn.responseCode
            val flujo = if (codigo in 200..299) conn.inputStream else conn.errorStream
            val respuesta = flujo?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (codigo !in 200..299) throw IllegalStateException(
                if (codigo == 401) "Tu sesión expiró. Vuelve a ingresar."
                else "No se pudo completar la inscripción (HTTP $codigo): " +
                    (runCatching { JSONObject(respuesta).optString("mensaje") }.getOrDefault("")))
            JSONObject(respuesta)
        } finally { conn.disconnect() }
    }
    suspend fun estado(token: String, id: String) = pedir(token,id,"/mi-inscripcion")
    suspend fun inscribir(token: String, id: String, equipo: String = "") =
        pedir(token,id,"/inscripcion","POST",JSONObject().put("nombreEquipo",equipo))
    suspend fun unirse(token: String, id: String, codigo: String) =
        pedir(token,id,"/unirse-equipo","POST",JSONObject().put("codigoEquipo",codigo))
    suspend fun cancelar(token: String, id: String) = pedir(token,id,"/inscripcion","DELETE")
}
