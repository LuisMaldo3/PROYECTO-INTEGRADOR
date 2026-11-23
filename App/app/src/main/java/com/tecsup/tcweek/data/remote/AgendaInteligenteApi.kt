package com.tecsup.tcweek.data.remote

import com.tecsup.tcweek.data.repository.pedirJson
import java.time.Instant
import org.json.JSONObject

/** US-77-T06: estado del equipo, igual para el capitán y los integrantes. */
data class EquipoAgenda(
    val nombre: String,
    val rol: String,
    val integrantes: Int,
    val confirmados: Int,
    val confirmado: Boolean
) {
    val pendientes get() = (integrantes - confirmados).coerceAtLeast(0)
    val esCapitan get() = rol == "capitan"
}

/** Un bloque de la agenda: lo que ya tengo ([inscrita]), lo que la IA me sugiere o una clase de mi horario. */
data class BloqueAgenda(
    val tipo: String,
    val actividadId: String,
    val nombre: String,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val ubicacion: String,
    val categoria: String = "",
    val enCurso: Boolean = false,
    val equipo: EquipoAgenda? = null,
    val motivo: String = "",
    val inscripcionDirecta: Boolean = false,
    val porEquipos: Boolean = false
) {
    val inscrita get() = tipo == "inscrita"
    val sugerida get() = tipo == "sugerida"
    /** Clase del horario confirmado: se muestra como referencia, no se abre ni se inscribe. */
    val clase get() = tipo == "clase"
}

data class DiaAgenda(val fecha: String, val etiqueta: String, val bloques: List<BloqueAgenda>)

data class AgendaInteligente(
    val generadoEn: Long,
    val origen: String,
    val aviso: String,
    val inscritas: Int,
    val sugeridas: Int,
    val enEquipo: Int,
    val equiposPorConfirmar: Int,
    val avisos: List<String>,
    val dias: List<DiaAgenda>,
    val conHorario: Boolean = false
) {
    /** Vacía si no queda ninguna actividad (las clases solas no cuentan). */
    val vacia get() = dias.none { d -> d.bloques.any { !it.clase } }
}

/** US-77 T2: cliente de GET /api/app/agenda-inteligente. */
object AgendaInteligenteApi {
    const val CLAVE = "agenda_inteligente"

    suspend fun cargar(token: String, ahora: Long = System.currentTimeMillis()): Pair<AgendaInteligente, String> {
        val cuerpo = pedirJson("/api/app/agenda-inteligente", token, "la agenda inteligente")
        return leerAgendaInteligente(JSONObject(cuerpo), ahora) to cuerpo
    }
}

private fun texto(o: JSONObject, clave: String, porDefecto: String = "") =
    o.optString(clave).takeUnless { it.isBlank() || it == "null" } ?: porDefecto

/** Lógica pura (probada). Los bloques sin hora válida se descartan para no mostrar horarios inventados. */
fun leerAgendaInteligente(json: JSONObject, ahora: Long): AgendaInteligente {
    val hora = Regex("""\d{2}:\d{2}""")
    val diasJson = json.optJSONArray("dias")
    val dias = (0 until (diasJson?.length() ?: 0)).mapNotNull { i ->
        val d = diasJson!!.optJSONObject(i) ?: return@mapNotNull null
        val bloquesJson = d.optJSONArray("bloques")
        val bloques = (0 until (bloquesJson?.length() ?: 0)).mapNotNull { j ->
            val b = bloquesJson!!.optJSONObject(j) ?: return@mapNotNull null
            val inicio = texto(b, "horaInicio")
            val fin = texto(b, "horaFin")
            if (!inicio.matches(hora) || !fin.matches(hora)) return@mapNotNull null
            val e = b.optJSONObject("equipo")
            BloqueAgenda(
                tipo = texto(b, "tipo", "inscrita"),
                actividadId = texto(b, "actividadId"),
                nombre = texto(b, "nombre", "Actividad"),
                fecha = texto(b, "fecha", texto(d, "fecha")),
                horaInicio = inicio,
                horaFin = fin,
                ubicacion = texto(b, "ubicacion", "Ubicación por confirmar"),
                categoria = texto(b, "categoria"),
                enCurso = b.optBoolean("enCurso", false),
                equipo = e?.let {
                    EquipoAgenda(texto(it, "nombre", "Tu equipo"), texto(it, "rol", "integrante"),
                        it.optInt("integrantes", 0), it.optInt("confirmados", 0), it.optBoolean("confirmado", false))
                },
                motivo = texto(b, "motivo"),
                inscripcionDirecta = b.optBoolean("inscripcionDirecta", false),
                porEquipos = texto(b, "modalidad") == "equipo"
            )
        }
        if (bloques.isEmpty()) null else DiaAgenda(texto(d, "fecha"), texto(d, "etiqueta", texto(d, "fecha")), bloques)
    }
    val resumen = json.optJSONObject("resumen") ?: JSONObject()
    val avisosJson = json.optJSONArray("avisos")
    return AgendaInteligente(
        generadoEn = runCatching { Instant.parse(json.optString("generadoEn")).toEpochMilli() }.getOrDefault(ahora),
        origen = texto(json, "origen", "reglas"),
        aviso = texto(json, "aviso"),
        inscritas = resumen.optInt("inscritas", dias.sumOf { d -> d.bloques.count { it.inscrita } }),
        sugeridas = resumen.optInt("sugeridas", dias.sumOf { d -> d.bloques.count { it.sugerida } }),
        enEquipo = resumen.optInt("enEquipo", 0),
        equiposPorConfirmar = resumen.optInt("equiposPorConfirmar", 0),
        avisos = (0 until (avisosJson?.length() ?: 0)).map { avisosJson!!.optString(it) }.filter { it.isNotBlank() },
        dias = dias,
        conHorario = json.optBoolean("conHorario", false)
    )
}

/** «Equipo Robots · confirmado (3/3)» o «Equipo Robots · falta 1 por confirmar (2/3)». */
fun textoEquipo(e: EquipoAgenda): String {
    val conteo = "(${e.confirmados}/${e.integrantes})"
    return when {
        e.confirmado -> "Equipo ${e.nombre} · confirmado $conteo"
        e.integrantes < 2 -> "Equipo ${e.nombre} · necesita otro integrante"
        e.pendientes == 1 -> "Equipo ${e.nombre} · falta 1 por confirmar $conteo"
        else -> "Equipo ${e.nombre} · faltan ${e.pendientes} por confirmar $conteo"
    }
}

/** Saca una sugerida en cuanto el estudiante se inscribe, sin esperar la recarga. */
fun quitarSugerida(agenda: AgendaInteligente, actividadId: String): AgendaInteligente {
    val dias = agenda.dias.mapNotNull { d ->
        val quedan = d.bloques.filterNot { it.sugerida && it.actividadId == actividadId }
        if (quedan.none { !it.clase }) null else d.copy(bloques = quedan)
    }
    val quitadas = agenda.dias.sumOf { d -> d.bloques.count { it.sugerida } } - dias.sumOf { d -> d.bloques.count { it.sugerida } }
    return agenda.copy(dias = dias, sugeridas = (agenda.sugeridas - quitadas).coerceAtLeast(0))
}

private val LIMA = java.time.ZoneId.of("America/Lima")

private fun instanteDe(fecha: String, hora: String): Long? = runCatching {
    java.time.LocalDateTime.parse("${fecha}T$hora").atZone(LIMA).toInstant().toEpochMilli()
}.getOrNull()

/** ¿Está pasando ahora? Se calcula con el reloj del teléfono, no con el del momento en que se cargó. */
fun enCursoAhora(b: BloqueAgenda, ahora: Long): Boolean {
    val ini = instanteDe(b.fecha, b.horaInicio) ?: return false
    val fin = instanteDe(b.fecha, b.horaFin) ?: return false
    return ahora in ini until fin
}

/**
 * Lo que sigue vigente con el reloj actual: sin las inscritas que ya terminaron ni las sugeridas que ya empezaron.
 * Así una agenda guardada (sin conexión) no muestra cosas pasadas.
 */
fun vigentes(agenda: AgendaInteligente, ahora: Long): AgendaInteligente {
    val dias = agenda.dias.mapNotNull { d ->
        val quedan = d.bloques.filter { b ->
            val fin = instanteDe(b.fecha, b.horaFin)
            val ini = instanteDe(b.fecha, b.horaInicio)
            if (b.sugerida) ini == null || ini > ahora else fin == null || fin > ahora
        }
        if (quedan.none { !it.clase }) null else d.copy(bloques = quedan)
    }
    return agenda.copy(dias = dias)
}
