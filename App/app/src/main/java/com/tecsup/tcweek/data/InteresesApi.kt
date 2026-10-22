package com.tecsup.tcweek.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Un interés disponible (id fijo y legible, como "tecnologia"). */
data class Interes(val id: String, val nombre: String, val descripcion: String)

/** Estado compartido con la web: omitir no equivale a configurar. */
data class EstadoIntereses(val configurados: Boolean, val omitido: Boolean)

/** Error de la API de intereses. [estado] es el código HTTP (0 si no hubo conexión). */
class ErrorIntereses(val estado: Int, mensaje: String) : Exception(mensaje)

/**
 * Servicios de intereses de Usuario-back (todos exigen el token de la sesión):
 *  GET /api/intereses        -> los disponibles
 *  GET /api/intereses/mios   -> lo que eligió la persona
 *  PUT /api/intereses/mios   -> guarda lo que eligió ({ "intereses": [...] })
 */
object InteresesApi {

    suspend fun catalogo(token: String): List<Interes> {
        val json = pedir("GET", "/api/intereses", token)
        val lista = json.optJSONArray("intereses") ?: JSONArray()
        return (0 until lista.length()).map { i ->
            val item = lista.getJSONObject(i)
            Interes(
                id = item.optString("id"),
                nombre = item.optString("nombre"),
                descripcion = item.optString("descripcion").takeUnless { it == "null" }.orEmpty()
            )
        }
    }

    suspend fun mios(token: String): List<String> = ids(pedir("GET", "/api/intereses/mios", token))

    suspend fun estado(token: String): EstadoIntereses {
        val json = pedir("GET", "/api/intereses/mios", token)
        return EstadoIntereses(
            configurados = json.optBoolean("configurados", false),
            omitido = json.optBoolean("omitido", false)
        )
    }

    /** Devuelve lo que quedó guardado. Si el servidor rechaza algo, lanza [ErrorIntereses] con su mensaje. */
    suspend fun guardar(token: String, ids: List<String>): List<String> {
        val cuerpo = JSONObject().put("intereses", JSONArray(ids)).toString()
        return ids(pedir("PUT", "/api/intereses/mios", token, cuerpo))
    }

    /** Registra que desea elegir más tarde. La omisión queda asociada a su cuenta. */
    suspend fun omitir(token: String): EstadoIntereses {
        val json = pedir("POST", "/api/intereses/omitir", token)
        return EstadoIntereses(
            configurados = json.optBoolean("configurados", false),
            omitido = json.optBoolean("omitido", false)
        )
    }

    private fun ids(json: JSONObject): List<String> {
        val lista = json.optJSONArray("intereses") ?: return emptyList()
        return (0 until lista.length()).map { lista.getString(it) }
    }

    private suspend fun pedir(metodo: String, ruta: String, token: String, cuerpo: String? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val conn = try {
                URL(Servidor.DIRECCION.trimEnd('/') + ruta).openConnection() as HttpURLConnection
            } catch (e: Exception) {
                throw ErrorIntereses(0, "No se pudo conectar con el servidor.")
            }
            try {
                conn.requestMethod = metodo
                conn.connectTimeout = 8000
                conn.readTimeout = 15000
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $token")
                if (cuerpo != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }
                }
                val estado = try {
                    conn.responseCode
                } catch (e: java.io.IOException) {
                    throw ErrorIntereses(0, "No se pudo conectar con el servidor. Revisa tu conexión.")
                }
                val texto = (if (estado in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(texto) }.getOrNull()
                if (estado !in 200..299) {
                    val mensaje = when {
                        estado == 401 -> "Tu sesión expiró. Inicia sesión nuevamente."
                        !json?.optString("mensaje").isNullOrBlank() -> json!!.optString("mensaje")
                        else -> "No se pudo completar la operación. Inténtalo nuevamente."
                    }
                    throw ErrorIntereses(estado, mensaje)
                }
                json ?: JSONObject()
            } finally {
                conn.disconnect()
            }
        }
}
