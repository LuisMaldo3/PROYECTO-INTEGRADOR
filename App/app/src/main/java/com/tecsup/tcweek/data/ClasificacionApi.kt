package com.tecsup.tcweek.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AreaPuntaje(val nombre: String, val puntos: Long, val posicion: Int,
    val victorias: Int, val desglose: List<CompetenciaPuntaje>)
data class CompetenciaPuntaje(val nombre: String, val puesto: Int, val puntos: Int)
data class TablaTcw(val totalCompetencias: Int, val areas: List<AreaPuntaje>)

/** Consulta de solo lectura; nunca acepta puntajes desde el teléfono. */
object ClasificacionApi {
    suspend fun consultar(token: String): TablaTcw = withContext(Dispatchers.IO) {
        val conexion = URL(Servidor.DIRECCION.trimEnd('/') + "/api/competencias/clasificacion")
            .openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = "GET"
            conexion.connectTimeout = 8000
            conexion.readTimeout = 12000
            conexion.setRequestProperty("Authorization", "Bearer $token")
            conexion.setRequestProperty("Accept", "application/json")
            if (conexion.responseCode !in 200..299) throw IllegalStateException(
                "No se pudo consultar la clasificación (HTTP ${conexion.responseCode}).")
            val json = JSONObject(conexion.inputStream.bufferedReader().use { it.readText() })
            val lista = json.optJSONArray("areas")
            val resultado = (0 until (lista?.length() ?: 0)).map { i ->
                val a = lista!!.getJSONObject(i)
                val registros = a.optJSONArray("desglose")
                AreaPuntaje(a.optString("area"), a.optLong("puntos"), a.optInt("posicion", i + 1),
                    a.optInt("primerosPuestos"),
                    (0 until (registros?.length() ?: 0)).map { j ->
                        val p = registros!!.getJSONObject(j)
                        CompetenciaPuntaje(p.optString("competencia"), p.optInt("puesto"), p.optInt("puntos"))
                    })
            }
            TablaTcw(json.optInt("totalCompetencias"), resultado)
        } finally { conexion.disconnect() }
    }
}
