package com.tecsup.tcweek.data.remote

import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** La que viene después, en resumen: para planificar el traslado. */
data class SiguienteActividad(val id: String, val nombre: String, val horaInicio: String, val ubicacion: String, val empiezaEn: Long)

/**
 * US-74: mi próxima actividad (o la que está en curso) con su ubicación completa.
 * [empiezaEn] y [terminaEn] en milisegundos: la app decide con su reloj si ya empezó o terminó.
 */
data class MiProximaActividad(
    val actividad: ActividadCatalogo,
    val empiezaEn: Long,
    val terminaEn: Long,
    val despues: SiguienteActividad? = null
) {
    fun enCurso(ahora: Long) = empiezaEn <= ahora && ahora < terminaEn
    fun terminada(ahora: Long) = ahora >= terminaEn
}

/**
 * Cliente de GET /api/mi-proxima.
 *  -> { tiene, actividad: {...detalle, ubicacionDetalle, empiezaEn, terminaEn, enCurso}, despues: {...} | null }
 */
object MiProximaApi {
    suspend fun cargar(token: String): MiProximaActividad? = withContext(Dispatchers.IO) {
        val conn = URL(Servidor.DIRECCION.trimEnd('/') + "/api/mi-proxima").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 12000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $token")
            val codigo = conn.responseCode
            if (codigo !in 200..299) throw IllegalStateException(
                if (codigo == 401) "Tu sesión expiró. Inicia sesión nuevamente." else "No se pudo consultar tu próxima actividad (HTTP $codigo)."
            )
            leerMiProxima(JSONObject(conn.inputStream.bufferedReader().use { it.readText() }))
        } finally {
            conn.disconnect()
        }
    }
}

private fun instante(texto: String?): Long? = runCatching { Instant.parse(texto).toEpochMilli() }.getOrNull()

/** Lógica pura para probarla: null si no tiene ninguna próxima actividad o la respuesta no sirve. */
fun leerMiProxima(json: JSONObject): MiProximaActividad? {
    if (!json.optBoolean("tiene", false)) return null
    val a = json.optJSONObject("actividad") ?: return null
    val inicio = instante(a.optString("empiezaEn")) ?: return null
    val fin = instante(a.optString("terminaEn"))?.takeIf { it > inicio } ?: (inicio + 60 * 60_000L)
    val d = json.optJSONObject("despues")
    val despues = d?.let {
        val empieza = instante(it.optString("empiezaEn")) ?: return@let null
        SiguienteActividad(it.optString("id"), it.optString("nombre"), it.optString("horaInicio"),
            it.optString("ubicacion").takeUnless { u -> u == "null" }.orEmpty(), empieza)
    }
    return MiProximaActividad(CatalogoApi.parsear(a), inicio, fin, despues)
}

/* ---------- US-74 T03: carga programada (lógica pura) ---------- */

/** Volver a pedirla: cada 5 minutos, o antes si la actividad ya terminó (pasa a la siguiente). */
fun debeRecargarMiProxima(actual: MiProximaActividad?, ultimaCarga: Long, ahora: Long): Boolean =
    ahora - ultimaCarga >= RECARGA_PROXIMAS_MS || (actual != null && actual.terminada(ahora))

/** «Ahora mismo · termina 11:00», «Empieza en 25 min», «Mañana a las 10:00»… */
fun estadoMiProxima(p: MiProximaActividad, ahora: Long): String = when {
    p.enCurso(ahora) -> {
        val fin = java.time.Instant.ofEpochMilli(p.terminaEn).atZone(java.time.ZoneId.of("America/Lima"))
        "Ahora mismo · termina %02d:%02d".format(fin.hour, fin.minute)
    }
    else -> textoFalta(p.empiezaEn, ahora)
}
