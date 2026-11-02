package com.tecsup.tcweek.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AvisoCampus(val id: String, val titulo: String, val mensaje: String,
    val publicadoEn: String, val portadaUrl: String, val creditoUrl: String, val autor: String)

object AvisosApi {
    suspend fun recientes(token: String): List<AvisoCampus> = withContext(Dispatchers.IO) {
        val conn = URL(Servidor.DIRECCION.trimEnd('/') + "/api/avisos/publicados").openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 7000; conn.readTimeout = 14000
            conn.setRequestProperty("Accept", "application/json")
            if (token.isNotBlank()) conn.setRequestProperty("Authorization", "Bearer $token")
            if (conn.responseCode !in 200..299) return@withContext emptyList()
            val datos = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val a = datos.optJSONArray("avisos") ?: return@withContext emptyList()
            (0 until minOf(a.length(), 5)).map { i ->
                val d = a.getJSONObject(i)
                val portada = d.optJSONObject("portada")
                val url = portada?.optString("url").orEmpty()
                AvisoCampus(d.optString("id"), d.optString("titulo"), d.optString("mensaje"),
                    d.optString("publicadoEn"), if (url.startsWith("/api/portadas/archivo/"))
                        Servidor.DIRECCION.trimEnd('/') + url else if (url.startsWith("https://")) url else "",
                    portada?.optString("creditoUrl").orEmpty(), portada?.optString("autor").orEmpty())
            }
        } finally { conn.disconnect() }
    }
}
