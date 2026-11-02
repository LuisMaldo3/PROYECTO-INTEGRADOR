package com.tecsup.tcweek.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Modelo compartido por catálogo, detalle, similitud y resultados. */
data class ActividadCatalogo(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val cupos: Int?,
    val maximo: Int?,
    val organizador: String,
    val categoriaId: String = "",
    val portadaUrl: String = "",
    val portadaAutor: String = "",
    val portadaLicencia: String = "",
    val portadaCreditoUrl: String = "",
    val tipoActividad: String = "actividad",
    val modalidad: String = "individual",
    val reglas: String = "",
    val areaTecsup: String = "",
    val ubicacion: String = "",
    val estado: String = "programada"
)

object CatalogoApi {
    suspend fun cargar(token: String): List<ActividadCatalogo> {
        val json = obtener(token, "/api/catalogo")
        val lista = json.optJSONArray("actividades") ?: return emptyList()
        return (0 until lista.length()).map { parsear(lista.getJSONObject(it)) }
    }

    /** US-22: abre incluso un evento finalizado que ya salió del catálogo de inscripciones. */
    suspend fun detalle(token: String, actividadId: String): ActividadCatalogo =
        parsear(obtener(token, "/api/catalogo/${codificar(actividadId)}"))

    /** US-22 T01..T06: exclusivamente actividades programadas relacionadas. */
    suspend fun similares(token: String, actividadId: String): List<ActividadCatalogo> {
        val json = obtener(token, "/api/catalogo/${codificar(actividadId)}/similares")
        val lista = json.optJSONArray("actividades") ?: return emptyList()
        return (0 until lista.length()).map { parsear(lista.getJSONObject(it)) }
    }

    private fun codificar(id: String) = java.net.URLEncoder.encode(id, "UTF-8")

    private suspend fun obtener(token: String, ruta: String): JSONObject = withContext(Dispatchers.IO) {
        val conn = URL(Servidor.DIRECCION.trimEnd('/') + ruta).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 15000
            conn.setRequestProperty("Accept", "application/json")
            if (token.isNotBlank()) conn.setRequestProperty("Authorization", "Bearer $token")
            val status = conn.responseCode
            if (status !in 200..299) {
                throw IllegalStateException(if (status == 401 || status == 403) "Tu sesión no tiene acceso a las actividades. Inicia sesión nuevamente." else "No se pudo consultar la actividad (HTTP $status).")
            }
            JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally { conn.disconnect() }
    }

    internal fun parsear(a: JSONObject): ActividadCatalogo {
        val portada = a.optJSONObject("portada")
        val url = a.optString("portadaUrl").takeUnless { it == "null" }.orEmpty()
        val imagenValida = when {
            url.startsWith("/api/portadas/archivo/") -> Servidor.DIRECCION.trimEnd('/') + url
            url.startsWith("https://upload.wikimedia.org/") -> url
            portada?.optString("tipo") == "url" && url.startsWith("https://") -> url
            else -> ""
        }
        return ActividadCatalogo(
            id = a.optString("id"),
            nombre = a.optString("nombre", "Actividad"),
            descripcion = a.optString("descripcion"),
            fecha = a.optString("fecha"),
            horaInicio = a.optString("horaInicio"),
            horaFin = a.optString("horaFin"),
            cupos = if (a.isNull("cuposDisponibles")) null else a.optInt("cuposDisponibles"),
            maximo = if (a.isNull("maximoParticipantes")) null else a.optInt("maximoParticipantes"),
            organizador = a.optString("organizador"),
            categoriaId = a.optString("categoriaId").takeUnless { it == "null" }.orEmpty(),
            portadaUrl = imagenValida,
            portadaAutor = portada?.optString("autor").orEmpty(),
            portadaLicencia = portada?.optString("licencia").orEmpty(),
            portadaCreditoUrl = portada?.optString("creditoUrl").orEmpty(),
            tipoActividad = a.optString("tipoActividad", "actividad"),
            modalidad = a.optString("modalidad", "individual"),
            reglas = a.optString("reglasCompetencia").takeUnless { it == "null" }.orEmpty(),
            areaTecsup = a.optString("areaTecsup").takeUnless { it == "null" }.orEmpty(),
            ubicacion = a.optString("ubicacion").takeUnless { it == "null" }.orEmpty(),
            estado = a.optString("estado", "programada")
        )
    }
}
