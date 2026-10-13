package com.tecsup.tcweek.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

data class ActividadAgenda(val id: String, val nombre: String, val fecha: String, val inicio: String, val fin: String, val ubicacion: String, val categoria: String, val estado: String)

object AgendaApi {
    suspend fun cargar(token: String): List<ActividadAgenda> = withContext(Dispatchers.IO) {
        val conn = URL(Servidor.DIRECCION + "/api/agenda").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 12000
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException(if (code == 401) "Tu sesión expiró. Inicia sesión nuevamente." else "No se pudo cargar la agenda ($code).")
            val json = JSONArray(conn.inputStream.bufferedReader().use { it.readText() })
            (0 until json.length()).map { index ->
                val item = json.getJSONObject(index)
                ActividadAgenda(item.optString("id"), item.optString("nombreActividad", "Actividad"), item.optString("fecha"), item.optString("horaInicio"), item.optString("horaFin"), item.optString("ubicacion", "Por confirmar"), item.optString("categoria", "Actividad"), item.optString("estado", "inscrita"))
            }.sortedWith(compareBy({ it.fecha }, { it.inicio }))
        } finally { conn.disconnect() }
    }
}
