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
data class FiltrosCatalogo(val franja: Franja? = null, val categoria: String = "", val area: String = "") {
    /** Cuántos filtros hay activos (para el botón «Limpiar»). */
    val activos: Int get() = listOf(franja != null, categoria.isNotBlank(), area.isNotBlank()).count { it }

    /** Parámetros para GET /api/catalogo; vacío si no hay filtros. */
    fun consulta(): String {
        val partes = buildList {
            franja?.let { add("franja=${it.id}") }
            if (categoria.isNotBlank()) add("categoria=" + URLEncoder.encode(categoria, "UTF-8"))
            if (area.isNotBlank()) add("area=" + URLEncoder.encode(area, "UTF-8"))
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

data class ResultadoCatalogo(val actividades: List<ActividadCatalogo>, val opciones: OpcionesFiltro)

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
