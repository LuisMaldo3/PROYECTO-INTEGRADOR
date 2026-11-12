package com.tecsup.tcweek.ui.theme

import androidx.annotation.DrawableRes
import com.tecsup.tcweek.R

/**
 * Los cinco departamentos que compiten en T&C Week (no son las carreras). Mismos identificadores,
 * nombres y colores que la web y el panel. Cada uno tiene un único escudo, igual para todos los deportes.
 * Los colores son la identidad definida para el proyecto: validar con TECSUP. La denominación del
 * departamento morado (DPI o DPG) está por confirmar.
 */
data class Departamento(
    val id: String,
    val nombre: String,
    val corto: String,
    val area: String,
    val color: ColorArea,
    @DrawableRes val escudo: Int,
    val porConfirmar: Boolean = false
)

object Departamentos {
    val todos = listOf(
        Departamento("quimica", "Química", "QUÍM", "Minería y Procesos Químicos", colorArea("Química"), R.drawable.escudo_quimica),
        Departamento("mecanica", "Mecánica", "MEC", "Mecánica", colorArea("Mecánica"), R.drawable.escudo_mecanica),
        Departamento("td", "TD", "TD", "Tecnología Digital", colorArea("Digital"), R.drawable.escudo_td),
        Departamento("electricidad", "Electricidad", "ELEC", "Electricidad y Electrónica", colorArea("Electricidad"), R.drawable.escudo_electricidad),
        Departamento("dpg", "DPG", "DPG", "Gestión y Producción", colorArea("Gestión"), R.drawable.escudo_dpg, porConfirmar = true)
    )

    fun porId(id: String?): Departamento? = todos.firstOrNull { it.id == id }

    /** Departamento al que pertenece un área de las que ya guardan los estudiantes. */
    fun deArea(area: String?): Departamento? = todos.firstOrNull { it.area == area }

    fun nombre(id: String?): String = porId(id)?.nombre ?: "Por definir"
}
