package com.tecsup.tcweek.data

/** US-78: texto que envía el botón «¿Qué hay ahora?» del chat (el servidor lo reconoce y usa la hora real). */
const val PREGUNTA_AHORA = "¿Qué hay ahora?"

/** Acepta consultas por momento, las que combinan tema y hora, y «¿Qué hay ahora?». */
fun esConsultaDeActividades(tipo: String): Boolean = tipo == "momento" || tipo == "tema_horario" || tipo == "ahora"

/**
 * Datos estructurados del catálogo; nunca se crean actividades en el móvil.
 * Los campos de [estadoAhora] en adelante solo llegan en respuestas a «¿Qué hay ahora?» (US-78).
 */
data class ActividadMomentoAsistente(
    val id: String, val nombre: String, val fecha: String, val horaInicio: String,
    val horaFin: String, val organizador: String, val cuposDisponibles: Int?,
    val ubicacion: String = "",
    /** «en_curso» o «pronto». */
    val estadoAhora: String = "",
    val minutos: Int = 0,
    val detalleAhora: String = "",
    val inscrito: Boolean = false,
    /** Nombre de la inscripción con la que se cruza, si hay. */
    val cruzaCon: String = "",
    val inscripcionDirecta: Boolean = false
) {
    val enCurso get() = estadoAhora == "en_curso"
    fun horario(): String = "$fecha · $horaInicio – $horaFin"
    fun etiquetaCupos(): String = cuposDisponibles?.let { "$it cupos disponibles" } ?: "Cupos por confirmar"
    fun esMostrable(): Boolean = nombre.isNotBlank() && fecha.isNotBlank() && horaInicio.isNotBlank() && horaFin.isNotBlank()

    /** Píldora de la tarjeta: «En curso · quedan 35 min» o «Empieza en 20 min». */
    fun etiquetaAhora(): String = when {
        enCurso -> "En curso · quedan ${textoMinutos(minutos)}"
        estadoAhora == "pronto" -> "Empieza en ${textoMinutos(minutos)}"
        else -> ""
    }
}

/** 35 → «35 min», 90 → «1 h 30 min», 120 → «2 h». */
fun textoMinutos(minutos: Int): String = when {
    minutos < 60 -> "$minutos min"
    minutos % 60 == 0 -> "${minutos / 60} h"
    else -> "${minutos / 60} h ${minutos % 60} min"
}
