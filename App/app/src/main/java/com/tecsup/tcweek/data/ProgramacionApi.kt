package com.tecsup.tcweek.data

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Estado de una actividad en el momento de consultar la programación. */
enum class EstadoDia { PROXIMA, EN_CURSO, FINALIZADA, CANCELADA }

/** Una actividad de la programación del día: la del catálogo más si el estudiante está inscrito. */
data class ActividadProgramada(val actividad: ActividadCatalogo, val inscrito: Boolean, val estadoDia: EstadoDia) {
    /** Las canceladas no tienen detalle ni inscripción. */
    val sePuedeAbrir: Boolean get() = estadoDia != EstadoDia.CANCELADA
}

/** Un día con actividades, para cambiar de día. */
data class DiaProgramacion(val fecha: String, val total: Int)

data class ProgramacionDia(
    val fecha: String,
    val hoy: String,
    val dias: List<DiaProgramacion>,
    val actividades: List<ActividadProgramada>
)

/**
 * US-70: cliente del servicio de programación del día.
 *
 *  GET /api/programacion?fecha=AAAA-MM-DD  (sin fecha: hoy o el próximo día con actividades)
 */
object ProgramacionApi {
    suspend fun cargar(token: String, fecha: String? = null): ProgramacionDia = withContext(Dispatchers.IO) {
        val consulta = if (fecha.isNullOrBlank()) "" else "?fecha=" + URLEncoder.encode(fecha, "UTF-8")
        val conn = URL(Servidor.DIRECCION.trimEnd('/') + "/api/programacion" + consulta).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 15000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $token")
            val codigo = conn.responseCode
            if (codigo !in 200..299) throw IllegalStateException(
                when (codigo) {
                    401 -> "Tu sesión expiró. Inicia sesión nuevamente."
                    400 -> "La fecha elegida no es válida."
                    else -> "No se pudo cargar la programación (HTTP $codigo)."
                }
            )
            leerProgramacion(JSONObject(conn.inputStream.bufferedReader().use { it.readText() }))
        } finally {
            conn.disconnect()
        }
    }
}

/** Convierte la respuesta del backend. Lógica pura para poder probarla sin red. */
fun leerProgramacion(json: JSONObject): ProgramacionDia {
    val dias = json.optJSONArray("dias")
    val lista = json.optJSONArray("actividades")
    return ProgramacionDia(
        fecha = json.optString("fecha"),
        hoy = json.optString("hoy"),
        dias = (0 until (dias?.length() ?: 0)).mapNotNull { i ->
            val d = dias!!.optJSONObject(i) ?: return@mapNotNull null
            val fecha = d.optString("fecha")
            if (fechaIso(fecha)) DiaProgramacion(fecha, d.optInt("total", 0)) else null
        }.sortedBy { it.fecha },
        actividades = (0 until (lista?.length() ?: 0)).mapNotNull { i ->
            val a = lista!!.optJSONObject(i) ?: return@mapNotNull null
            ActividadProgramada(
                actividad = CatalogoApi.parsear(a),
                inscrito = a.optBoolean("inscrito", false),
                estadoDia = when (a.optString("estadoDia")) {
                    "en_curso" -> EstadoDia.EN_CURSO
                    "finalizada" -> EstadoDia.FINALIZADA
                    "cancelada" -> EstadoDia.CANCELADA
                    else -> EstadoDia.PROXIMA
                }
            )
        }.sortedBy { it.actividad.horaInicio }
    )
}

private val FECHA_ISO = Regex("\\d{4}-\\d{2}-\\d{2}")
internal fun fechaIso(texto: String?) = texto != null && FECHA_ISO.matches(texto)

/* ---------- US-70 T03: carga y cambio de día (lógica pura) ---------- */

/** Día anterior con actividades, o null si es el primero. */
fun diaAnterior(dias: List<DiaProgramacion>, actual: String): String? =
    dias.map { it.fecha }.filter { it < actual }.maxOrNull()

/** Día siguiente con actividades, o null si es el último. */
fun diaSiguiente(dias: List<DiaProgramacion>, actual: String): String? =
    dias.map { it.fecha }.filter { it > actual }.minOrNull()

/** «Hoy», «Mañana», «Ayer» o «sáb 21/11». */
fun etiquetaDia(fecha: String, hoy: String): String {
    val f = runCatching { java.time.LocalDate.parse(fecha) }.getOrNull() ?: return fecha
    val h = runCatching { java.time.LocalDate.parse(hoy) }.getOrNull()
    if (h != null) when (f) {
        h -> return "Hoy"
        h.plusDays(1) -> return "Mañana"
        h.minusDays(1) -> return "Ayer"
        else -> {}
    }
    val dias = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")
    return "${dias[f.dayOfWeek.value - 1]} %02d/%02d".format(f.dayOfMonth, f.monthValue)
}
