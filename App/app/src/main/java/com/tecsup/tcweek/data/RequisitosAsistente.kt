package com.tecsup.tcweek.data

/** Requisitos oficiales de una actividad; ausentes significa por confirmar, no "sin requisitos". */
data class RequisitosAsistente(
    val actividadId: String,
    val nombre: String,
    val items: List<String> = emptyList(),
    val confirmados: Boolean = false
) {
    fun tieneInformacion(): Boolean = confirmados && items.isNotEmpty()
    fun estado(): String = if (tieneInformacion())
        "Requisitos publicados por administración" else "Requisitos por confirmar con el organizador"
}
