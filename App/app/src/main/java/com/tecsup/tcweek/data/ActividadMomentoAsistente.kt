package com.tecsup.tcweek.data

/** Acepta tanto consultas por momento como las que combinan tema y hora. */
fun esConsultaDeActividades(tipo: String): Boolean = tipo == "momento" || tipo == "tema_horario"

/** Datos estructurados del catálogo; nunca se crean actividades en el móvil. */
data class ActividadMomentoAsistente(
    val id: String, val nombre: String, val fecha: String, val horaInicio: String,
    val horaFin: String, val organizador: String, val cuposDisponibles: Int?
) {
    fun horario(): String = "$fecha · $horaInicio – $horaFin"
    fun etiquetaCupos(): String = cuposDisponibles?.let { "$it cupos disponibles" } ?: "Cupos por confirmar"
    fun esMostrable(): Boolean = nombre.isNotBlank() && fecha.isNotBlank() && horaInicio.isNotBlank() && horaFin.isNotBlank()
}
