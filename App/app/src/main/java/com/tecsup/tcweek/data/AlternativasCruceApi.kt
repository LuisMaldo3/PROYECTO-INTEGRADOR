package com.tecsup.tcweek.data

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class CruceDetectado(
    val id: String, val actividadA: String, val actividadB: String,
    val fecha: String, val desde: String, val hasta: String, val minutos: Int
)
data class AlternativaCruce(
    val id: String, val nombre: String, val fecha: String,
    val inicio: String, val fin: String, val ubicacion: String, val motivo: String
)
data class AvisoAlternativasCruce(
    val cruce: CruceDetectado, val alternativas: List<AlternativaCruce> = emptyList(),
    val error: String? = null
)

/** Consultas autenticadas; el backend limita los cruces a la cuenta del JWT. */
object AlternativasCruceApi {
    private suspend fun consultar(ruta: String, token: String): String = withContext(Dispatchers.IO) {
        require(token.isNotBlank()) { "Inicia sesión para consultar alternativas." }
        val conn = URL(Servidor.DIRECCION.trimEnd('/') + ruta).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 15000
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/json")
            val estado = conn.responseCode
            if (estado !in 200..299) throw IllegalStateException(when (estado) {
                401 -> "Tu sesión expiró. Inicia sesión nuevamente."
                404 -> "Este cruce ya no está disponible. Actualiza tu agenda."
                else -> "No se pudieron obtener las alternativas (HTTP $estado)."
            })
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally { conn.disconnect() }
    }

    suspend fun cruces(token: String): List<CruceDetectado> {
        val json = JSONArray(consultar("/api/agenda/cruces", token))
        return (0 until json.length()).map { index ->
            val item = json.getJSONObject(index)
            CruceDetectado(item.optString("id"), item.optString("actividadA"),
                item.optString("actividadB"), item.optString("fecha"),
                item.optString("desde"), item.optString("hasta"), item.optInt("minutos"))
        }
    }

    suspend fun alternativas(token: String, cruceId: String): List<AlternativaCruce> {
        val id = URLEncoder.encode(cruceId, "UTF-8")
        val json = JSONObject(consultar("/api/agenda/cruces/$id/alternativas", token))
        val opciones = json.optJSONArray("alternativas") ?: JSONArray()
        return (0 until opciones.length()).map { index ->
            val a = opciones.getJSONObject(index)
            AlternativaCruce(a.optString("id"), a.optString("nombre", "Actividad"),
                a.optString("fecha"), a.optString("horaInicio"), a.optString("horaFin"),
                a.optString("ubicacionId", ""),
                a.optString("motivo", "Horario compatible con tus inscripciones actuales."))
        }
    }
}
