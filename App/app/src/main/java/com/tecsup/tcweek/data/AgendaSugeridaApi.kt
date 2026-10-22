package com.tecsup.tcweek.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ActividadSugerida(
    val id: String, val nombre: String, val fecha: String, val inicio: String,
    val fin: String, val ubicacion: String, val motivo: String
)
data class RespuestaAgendaSugerida(
    val origen: String, val aviso: String, val inscripciones: Int, val actividades: List<ActividadSugerida>
)

/** GET autenticado, sin pasar IDs de otros estudiantes y con tratamiento de errores HTTP. */
object AgendaSugeridaApi {
    suspend fun cargar(token: String): RespuestaAgendaSugerida = withContext(Dispatchers.IO) {
        val conn = URL(Servidor.DIRECCION.trimEnd('/') + "/api/agenda/sugerida")
            .openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 28000
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/json")
            val status = conn.responseCode
            if (status !in 200..299) throw IllegalStateException(when (status) {
                401 -> "Tu sesión expiró. Vuelve a iniciar sesión."
                404 -> "Actualiza Spring Boot para usar la agenda sugerida."
                503 -> "No se pudo conectar con MongoDB."
                else -> "No se pudo generar la agenda sugerida (HTTP $status)."
            })
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val arr = json.optJSONArray("actividades")
            val lista = (0 until (arr?.length() ?: 0)).map { index ->
                val a = arr!!.getJSONObject(index)
                ActividadSugerida(
                    id = a.optString("id"), nombre = a.optString("nombre", "Actividad"),
                    fecha = a.optString("fecha"), inicio = a.optString("horaInicio"),
                    fin = a.optString("horaFin"), ubicacion = a.optString("ubicacionId", ""),
                    motivo = a.optString("motivo", "Actividad compatible con tu horario.")
                )
            }
            RespuestaAgendaSugerida(
                origen = json.optString("origen", "reglas"), aviso = json.optString("aviso"),
                inscripciones = json.optJSONArray("inscripciones")?.length() ?: 0,
                actividades = lista
            )
        } catch (e: java.net.ConnectException) {
            throw IllegalStateException("No se pudo conectar con el backend ${Servidor.DIRECCION}.", e)
        } catch (e: java.net.SocketTimeoutException) {
            throw IllegalStateException("El servidor tardó demasiado. Inténtalo de nuevo.", e)
        } finally { conn.disconnect() }
    }
}
