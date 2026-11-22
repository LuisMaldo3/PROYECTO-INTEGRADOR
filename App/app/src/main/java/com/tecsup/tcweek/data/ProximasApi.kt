package com.tecsup.tcweek.data

import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Una actividad que todavía no empieza. [empiezaEn] en milisegundos: la app cuenta los minutos con su reloj. */
data class ActividadProxima(val actividad: ActividadCatalogo, val empiezaEn: Long, val inscrito: Boolean)

/**
 * US-72: cliente del servicio de próximas actividades.
 *
 *  GET /api/proximas?limite=3  ->  { ahora, total, actividades: [ {...catálogo, empiezaEn, minutosParaEmpezar, inscrito} ] }
 */
object ProximasApi {
    suspend fun cargar(token: String, limite: Int = 3): List<ActividadProxima> = withContext(Dispatchers.IO) {
        val conn = URL(Servidor.DIRECCION.trimEnd('/') + "/api/proximas?limite=$limite").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 12000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $token")
            val codigo = conn.responseCode
            if (codigo !in 200..299) throw IllegalStateException(
                if (codigo == 401) "Tu sesión expiró. Inicia sesión nuevamente."
                else "No se pudieron cargar las próximas actividades (HTTP $codigo)."
            )
            leerProximas(JSONObject(conn.inputStream.bufferedReader().use { it.readText() }))
        } finally {
            conn.disconnect()
        }
    }
}

/** Convierte la respuesta; descarta las que no traen una hora de inicio válida. Lógica pura para probarla. */
fun leerProximas(json: JSONObject): List<ActividadProxima> {
    val lista = json.optJSONArray("actividades") ?: return emptyList()
    return (0 until lista.length()).mapNotNull { i ->
        val a = lista.optJSONObject(i) ?: return@mapNotNull null
        val inicio = runCatching { Instant.parse(a.optString("empiezaEn")).toEpochMilli() }.getOrNull() ?: return@mapNotNull null
        ActividadProxima(CatalogoApi.parsear(a), inicio, a.optBoolean("inscrito", false))
    }.sortedBy { it.empiezaEn }
}

/* ---------- US-72 T02: carga programada (lógica pura) ---------- */

/** Cada cuánto se vuelve a pedir la lista al servidor aunque nada haya cambiado. */
const val RECARGA_PROXIMAS_MS = 5 * 60_000L

/** Cada cuánto se actualiza el «Empieza en N min» en pantalla (sin pedir nada al servidor). */
const val TICK_PROXIMAS_MS = 30_000L

/** Hay que volver a pedir la lista: pasó el tiempo de recarga o la primera ya empezó (dejó de ser «próxima»). */
fun debeRecargarProximas(lista: List<ActividadProxima>, ultimaCarga: Long, ahora: Long): Boolean =
    ahora - ultimaCarga >= RECARGA_PROXIMAS_MS || lista.any { it.empiezaEn <= ahora }

/** Solo las que todavía no empiezan, según el reloj del celular (entre recargas). */
fun vigentes(lista: List<ActividadProxima>, ahora: Long): List<ActividadProxima> = lista.filter { it.empiezaEn > ahora }

/** «Empieza en 25 min», «Empieza en 2 h 15 min», «Mañana a las 10:00» o «lun 23/11 a las 10:00». */
fun textoFalta(empiezaEn: Long, ahora: Long, zona: ZoneId = ZoneId.of("America/Lima")): String {
    val minutos = (empiezaEn - ahora + 59_999) / 60_000
    if (minutos <= 0) return "Empieza ahora"
    if (minutos < 60) return "Empieza en $minutos min"
    val inicio = Instant.ofEpochMilli(empiezaEn).atZone(zona)
    val hoy = Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate()
    val hora = "%02d:%02d".format(inicio.hour, inicio.minute)
    return when (inicio.toLocalDate()) {
        hoy -> if (minutos % 60 == 0L) "Empieza en ${minutos / 60} h" else "Empieza en ${minutos / 60} h ${minutos % 60} min"
        hoy.plusDays(1) -> "Mañana a las $hora"
        else -> {
            val dias = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")
            "${dias[inicio.dayOfWeek.value - 1]} %02d/%02d a las $hora".format(inicio.dayOfMonth, inicio.monthValue)
        }
    }
}

/** Falta poco (15 min o menos): la tarjeta lo resalta. */
fun empiezaPronto(empiezaEn: Long, ahora: Long): Boolean = empiezaEn - ahora in 1..15 * 60_000L
