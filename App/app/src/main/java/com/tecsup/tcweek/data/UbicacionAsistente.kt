package com.tecsup.tcweek.data

/** Ubicación publicada por Tecsup. Nunca se presupone el campus, aula o dirección. */
data class UbicacionAsistente(
    val actividadId: String,
    val nombre: String,
    val fecha: String,
    val horaInicio: String,
    val lugar: String,
    val sede: String,
    val pabellon: String,
    val aula: String,
    val direccion: String,
    val referencia: String,
    val confirmada: Boolean,
) {
    fun detalles(): List<String> = listOf(sede, pabellon, aula, direccion, referencia)
        .filter { it.isNotBlank() }
}
