package com.tecsup.tcweek.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ActividadCatalogo(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val cupos: Int?,
    val maximo: Int?,
    val organizador: String
)

object CatalogoApi {
    suspend fun cargar(token: String): List<ActividadCatalogo> = withContext(Dispatchers.IO) {
        val conn = URL(Servidor.DIRECCION.trimEnd('/') + "/api/catalogo").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 15000
            conn.setRequestProperty("Accept", "application/json")
            if (token.isNotBlank()) conn.setRequestProperty("Authorization", "Bearer $token")
            val status = conn.responseCode
            if (status !in 200..299) {
                throw IllegalStateException(if (status == 401 || status == 403) "Tu sesión no tiene acceso al catálogo. Inicia sesión nuevamente." else "No se pudo cargar el catálogo (HTTP $status).")
            }
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val lista = json.optJSONArray("actividades") ?: return@withContext emptyList()
            (0 until lista.length()).map { i ->
                val a = lista.getJSONObject(i)
                ActividadCatalogo(
                    id = a.optString("id"),
                    nombre = a.optString("nombre", "Actividad"),
                    descripcion = a.optString("descripcion"),
                    fecha = a.optString("fecha"),
                    horaInicio = a.optString("horaInicio"),
                    horaFin = a.optString("horaFin"),
                    cupos = if (a.isNull("cuposDisponibles")) null else a.optInt("cuposDisponibles"),
                    maximo = if (a.isNull("maximoParticipantes")) null else a.optInt("maximoParticipantes"),
                    organizador = a.optString("organizador")
                )
            }
        } finally {
            conn.disconnect()
        }
    }
}
