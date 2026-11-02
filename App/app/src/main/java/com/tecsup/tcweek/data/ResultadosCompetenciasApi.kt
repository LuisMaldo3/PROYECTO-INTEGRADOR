package com.tecsup.tcweek.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** US-22 T07/T08: los resultados proceden únicamente de la publicación administrativa. */
data class PosicionOficial(val puesto: Int, val nombre: String, val area: String, val puntos: Int)
data class ResultadoOficial(
    val actividadId: String,
    val nombre: String,
    val fecha: String,
    val portadaUrl: String,
    val posiciones: List<PosicionOficial>
)

object ResultadosCompetenciasApi {
    suspend fun listar(token: String): List<ResultadoOficial> = withContext(Dispatchers.IO) {
        val conexion = URL(Servidor.DIRECCION.trimEnd('/') + "/api/competencias/resultados")
            .openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = "GET"
            conexion.connectTimeout = 8000
            conexion.readTimeout = 15000
            conexion.setRequestProperty("Accept", "application/json")
            if (token.isNotBlank()) conexion.setRequestProperty("Authorization", "Bearer $token")
            val status = conexion.responseCode
            if (status !in 200..299) throw IllegalStateException("No se pudieron cargar resultados oficiales (HTTP $status).")
            val json = JSONObject(conexion.inputStream.bufferedReader().use { it.readText() })
            val lista = json.optJSONArray("resultados") ?: return@withContext emptyList()
            (0 until lista.length()).map { indice -> parsear(lista.getJSONObject(indice)) }
        } finally { conexion.disconnect() }
    }

    internal fun parsear(json: JSONObject): ResultadoOficial {
        val posicionesJson = json.optJSONArray("posiciones")
        val posiciones = (0 until (posicionesJson?.length() ?: 0)).map { idx ->
            val p = posicionesJson!!.getJSONObject(idx)
            PosicionOficial(p.optInt("puesto"), p.optString("nombre"),
                p.optString("areaTecsup").takeUnless { it == "null" }.orEmpty(),
                p.optInt("puntos"))
        }.sortedBy { it.puesto }
        val url = json.optString("portadaUrl").takeUnless { it == "null" }.orEmpty()
        val tipo = json.optJSONObject("portada")?.optString("tipo")
        val imagen = when {
            url.startsWith("/api/portadas/archivo/") -> Servidor.DIRECCION.trimEnd('/') + url
            url.startsWith("https://upload.wikimedia.org/") -> url
            tipo == "url" && url.startsWith("https://") -> url
            else -> ""
        }
        return ResultadoOficial(json.optString("actividadId"),json.optString("nombre"),
            json.optString("fecha"), imagen, posiciones)
    }
}
