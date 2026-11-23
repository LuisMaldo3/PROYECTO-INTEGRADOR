package com.tecsup.tcweek.data.remote

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Preferencia de notificaciones de recomendaciones de la cuenta (vale para todos sus celulares).
 *
 *  GET /api/notificaciones/preferencias                        -> { recomendaciones }
 *  PUT /api/notificaciones/preferencias  { recomendaciones }   -> { recomendaciones }
 */
object NotificacionesApi {
    suspend fun recomendacionesActivas(token: String): Boolean =
        pedir("GET", token, null).optBoolean("recomendaciones", true)

    suspend fun guardarRecomendaciones(token: String, activas: Boolean): Boolean =
        pedir("PUT", token, JSONObject().put("recomendaciones", activas)).optBoolean("recomendaciones", activas)

    private suspend fun pedir(metodo: String, token: String, cuerpo: JSONObject?): JSONObject = withContext(Dispatchers.IO) {
        val conn = URL(Servidor.DIRECCION.trimEnd('/') + "/api/notificaciones/preferencias").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = metodo
            conn.connectTimeout = 8000
            conn.readTimeout = 12000
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/json")
            if (cuerpo != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(cuerpo.toString().toByteArray(Charsets.UTF_8)) }
            }
            val codigo = conn.responseCode
            if (codigo !in 200..299) throw IllegalStateException(
                if (codigo == 401) "Tu sesión expiró. Vuelve a ingresar."
                else "No se pudo guardar tu preferencia de notificaciones (HTTP $codigo)."
            )
            JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }
}

/**
 * Copia local de la preferencia: si el estudiante apagó las recomendaciones, una que ya venía en camino
 * tampoco se muestra.
 */
class PreferenciaRecomendaciones(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("vynk_push", Context.MODE_PRIVATE)

    fun activas(): Boolean = prefs.getBoolean(CLAVE, true)

    fun guardar(activas: Boolean) {
        prefs.edit().putBoolean(CLAVE, activas).apply()
    }

    private companion object {
        const val CLAVE = "recomendaciones_activas"
    }
}
