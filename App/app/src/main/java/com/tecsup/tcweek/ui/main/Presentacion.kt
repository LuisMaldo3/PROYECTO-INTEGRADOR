package com.tecsup.tcweek.ui.main

import java.text.Normalizer
import java.time.LocalDate

/*
 * Cómo se ven las actividades en pantalla (días, horas, cupos, categorías).
 * Lógica pura (sin pantallas) para poder probarla con una prueba unitaria.
 */

private val DIAS = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
private val MESES = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
private val FORMATO_FECHA = Regex("""\d{4}-\d{2}-\d{2}""")

/** "2026-10-19" → la fecha; null si falta, tiene otro formato o el día no existe (2026-02-30). */
fun fechaValida(iso: String?): LocalDate? =
    if (iso != null && FORMATO_FECHA.matches(iso)) runCatching { LocalDate.parse(iso) }.getOrNull() else null

/** "2026-10-19" → "Lunes 19 de octubre"; sin fecha válida, "Fecha por confirmar". */
fun nombreDia(iso: String?): String {
    val fecha = fechaValida(iso) ?: return "Fecha por confirmar"
    val dia = DIAS[fecha.dayOfWeek.value - 1].replaceFirstChar { it.uppercase() }
    return "$dia ${fecha.dayOfMonth} de ${MESES[fecha.monthValue - 1]}"
}

/** ("11:00", "12:30") → "11:00 – 12:30"; solo el inicio → "11:00"; sin horas → "Hora por confirmar". */
fun rangoHoras(inicio: String?, fin: String?): String {
    val i = inicio?.takeIf { it.isNotBlank() && it != "null" }
    val f = fin?.takeIf { it.isNotBlank() && it != "null" }
    return when {
        i != null && f != null -> "$i – $f"
        i != null -> i
        else -> "Hora por confirmar"
    }
}

/** Un día con sus actividades. [fecha] es null para las que no tienen fecha válida. */
data class GrupoDia<T>(val fecha: String?, val items: List<T>)

/**
 * Agrupa por día, de la fecha más próxima a la más lejana y, dentro de cada día, por hora de inicio.
 * Las actividades sin fecha válida van al final en un grupo con fecha null.
 */
fun <T> agruparPorDia(lista: List<T>, fecha: (T) -> String?, hora: (T) -> String?): List<GrupoDia<T>> =
    lista.groupBy { item -> fecha(item)?.takeIf { fechaValida(it) != null } }
        .map { (dia, items) -> GrupoDia(dia, items.sortedBy { hora(it).orEmpty() }) }
        .sortedWith(compareBy({ it.fecha == null }, { it.fecha.orEmpty() }))

enum class TonoCupos { OK, POCO, AGOTADO, NEUTRO }

data class EstadoCupos(val texto: String, val tono: TonoCupos)

/** Cupos → texto y tono: hay, quedan 5 o menos, agotado, o no se sabe. */
fun estadoCupos(cupos: Int?): EstadoCupos = when {
    cupos == null -> EstadoCupos("Cupos por confirmar", TonoCupos.NEUTRO)
    cupos <= 0 -> EstadoCupos("Sin cupos", TonoCupos.AGOTADO)
    cupos == 1 -> EstadoCupos("Queda 1 cupo", TonoCupos.POCO)
    cupos <= 5 -> EstadoCupos("Quedan $cupos cupos", TonoCupos.POCO)
    else -> EstadoCupos("$cupos cupos", TonoCupos.OK)
}

enum class ClaseCategoria { CULTURAL, CONCURSO, OTRA }

data class EstiloCategoria(val clase: ClaseCategoria, val nombre: String)

private fun sinTildes(texto: String): String =
    Normalizer.normalize(texto, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase().trim()

/** Categoría a partir de su id ("cultural") o de su nombre ("Actividad cultural"). Lo desconocido sale neutro y conserva su nombre. */
fun estiloCategoria(id: String? = null, nombre: String? = null): EstiloCategoria {
    when (id) {
        "cultural" -> return EstiloCategoria(ClaseCategoria.CULTURAL, "Actividad cultural")
        "concurso" -> return EstiloCategoria(ClaseCategoria.CONCURSO, "Concurso T&C Week")
    }
    val texto = sinTildes(nombre.orEmpty())
    return when {
        "concurso" in texto -> EstiloCategoria(ClaseCategoria.CONCURSO, "Concurso T&C Week")
        "cultural" in texto -> EstiloCategoria(ClaseCategoria.CULTURAL, "Actividad cultural")
        else -> EstiloCategoria(ClaseCategoria.OTRA, nombre?.takeIf { it.isNotBlank() } ?: "Actividad")
    }
}

/** "1 actividad" / "3 actividades". */
fun cantidad(n: Int, singular: String, plural: String): String = "$n ${if (n == 1) singular else plural}"
