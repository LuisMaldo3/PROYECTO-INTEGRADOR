package com.tecsup.tcweek.data

/** Certificación enviada por Spring Boot tras cruzar clases e inscripciones. */
data class CompatibilidadHorario(
    val cabeEnHorario: Boolean = false,
    val huecoInicio: String = "",
    val huecoFin: String = "",
    val motivo: String = ""
) {
    fun mostrarEtiqueta(): Boolean = cabeEnHorario && huecoInicio.matches(Regex("""\d{2}:\d{2}""")) &&
        huecoFin.matches(Regex("""\d{2}:\d{2}"""))
}
