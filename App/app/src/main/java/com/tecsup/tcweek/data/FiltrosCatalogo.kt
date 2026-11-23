package com.tecsup.tcweek.data

import java.net.URLEncoder
import org.json.JSONObject

/** US-73: franjas de horario por hora de inicio (mañana antes de las 12:00, tarde 12:00–17:59, noche desde las 18:00). */
enum class Franja(val id: String, val titulo: String) {
    MANANA("manana", "Mañana"), TARDE("tarde", "Tarde"), NOCHE("noche", "Noche");

    companion object {
        fun de(id: String?): Franja? = entries.firstOrNull { it.id == id }
    }
}

/** Las cinco áreas de Tecsup (US-73 T05). */
val AREAS_TECSUP = listOf(
    "Gestión y Producción", "Tecnología Digital", "Minería y Procesos Químicos", "Electricidad y Electrónica", "Mecánica"
)

/** Filtros elegidos en el catálogo. Vacío = todas las actividades. */
data class FiltrosCatalogo(
    val franja: Franja? = null, val categoria: String = "", val area: String = "",
    /** US-80: por defecto el servidor pone primero lo que te interesa; true = solo por fecha y hora. */
    val porFecha: Boolean = false
) {
    /** Cuántos filtros hay activos (para el botón «Limpiar»). */
    val activos: Int get() = listOf(franja != null, categoria.isNotBlank(), area.isNotBlank()).count { it }

    /** Parámetros para GET /api/catalogo; vacío si no hay filtros. */
    fun consulta(): String {
        val partes = buildList {
            franja?.let { add("franja=${it.id}") }
            if (categoria.isNotBlank()) add("categoria=" + URLEncoder.encode(categoria, "UTF-8"))
            if (area.isNotBlank()) add("area=" + URLEncoder.encode(area, "UTF-8"))
            if (porFecha) add("orden=fecha")
        }
        return if (partes.isEmpty()) "" else "?" + partes.joinToString("&")
    }
}

data class OpcionFiltro(val id: String, val nombre: String, val total: Int)

/** Opciones que ofrece el servidor, contadas sobre todas las actividades. */
data class OpcionesFiltro(
    val categorias: List<OpcionFiltro> = emptyList(),
    val areas: List<OpcionFiltro> = AREAS_TECSUP.map { OpcionFiltro(it, it, 0) },
    val franjas: Map<Franja, Int> = emptyMap()
)

data class ResultadoCatalogo(
    val actividades: List<ActividadCatalogo>,
    val opciones: OpcionesFiltro,
    /** US-80: «intereses» si el servidor puso primero las «Para ti», «fecha» si no. */
    val orden: String = "fecha",
    val paraTi: Int = 0,
    val intereses: List<String> = emptyList()
)

/**
 * US-80 T2: cómo se muestra el catálogo. Con orden por intereses, arriba la sección «Para ti» (en el orden del
 * servidor: más afinidad primero) y debajo el resto agrupado por día. Por fecha, todo junto por día.
 */
data class SeccionesCatalogo(val paraTi: List<ActividadCatalogo>, val resto: List<ActividadCatalogo>)

fun seccionesCatalogo(resultado: ResultadoCatalogo): SeccionesCatalogo =
    if (resultado.orden == "intereses") {
        val (mias, otras) = resultado.actividades.partition { it.paraTi }
        SeccionesCatalogo(mias, otras)
    } else SeccionesCatalogo(emptyList(), resultado.actividades)

/** US-80 T2: el orden elegido se recuerda entre aperturas de la app. */
class PreferenciaOrdenCatalogo(context: android.content.Context) {
    private val prefs = context.applicationContext.getSharedPreferences("vynk_preferencias", android.content.Context.MODE_PRIVATE)
    var porFecha: Boolean
        get() = prefs.getBoolean("catalogo_por_fecha", false)
        set(valor) { prefs.edit().putBoolean("catalogo_por_fecha", valor).apply() }
}

/** Lee las opciones de filtro de la respuesta del catálogo. Lógica pura para probarla. */
fun leerOpcionesFiltro(json: JSONObject?): OpcionesFiltro {
    if (json == null) return OpcionesFiltro()
    fun lista(clave: String, id: String, nombre: String): List<OpcionFiltro> {
        val arreglo = json.optJSONArray(clave) ?: return emptyList()
        return (0 until arreglo.length()).mapNotNull { i ->
            val o = arreglo.optJSONObject(i) ?: return@mapNotNull null
            val valor = o.optString(id)
            if (valor.isBlank()) null else OpcionFiltro(valor, o.optString(nombre, valor), o.optInt("total", 0))
        }
    }
    val areas = lista("areas", "nombre", "nombre").associateBy { it.id }
    return OpcionesFiltro(
        categorias = lista("categorias", "id", "nombre"),
        // Siempre las cinco áreas, en su orden, aunque alguna no tenga actividades.
        areas = AREAS_TECSUP.map { areas[it] ?: OpcionFiltro(it, it, 0) },
        franjas = lista("franjas", "id", "id").mapNotNull { o -> Franja.de(o.id)?.let { it to o.total } }.toMap()
    )
}
