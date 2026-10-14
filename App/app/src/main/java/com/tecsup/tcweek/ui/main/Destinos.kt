package com.tecsup.tcweek.ui.main

/*
 * Pantallas del menú inferior.
 *
 * La app móvil es solo para el estudiante (usuario): el organizador y el administrador
 * usan la web. Por eso el menú es el mismo para todos y no depende del rol.
 * Es lógica pura (sin pantallas), así se puede probar con una prueba unitaria.
 */

const val ROL_USUARIO = "usuario"
const val ROL_ORGANIZADOR = "organizador"
const val ROL_ADMINISTRADOR = "administrador"

private val ROLES_VALIDOS = setOf(ROL_USUARIO, ROL_ORGANIZADOR, ROL_ADMINISTRADOR)

/** Devuelve un rol válido: lo que no se reconoce (o falta) cuenta como "usuario". */
fun normalizarRol(rol: String?): String {
    val limpio = rol?.trim()?.lowercase().orEmpty()
    return if (limpio in ROLES_VALIDOS) limpio else ROL_USUARIO
}

/** Texto del rol para mostrar en el perfil. */
fun etiquetaRol(rol: String?): String = when (normalizarRol(rol)) {
    ROL_ORGANIZADOR -> "Organizador"
    ROL_ADMINISTRADOR -> "Administrador"
    else -> "Usuario"
}

/** Pantalla que se abre desde Perfil: no tiene botón propio en el menú inferior (mientras se está en ella se marca «Perfil»). */
const val RUTA_INTERESES = "intereses"

/** Botones del menú inferior, en el orden en que se muestran. */
enum class Destino(val ruta: String, val titulo: String) {
    INICIO("inicio", "Inicio"),
    CATALOGO("catalogo", "Catálogo"),
    AGENDA("agenda", "Agenda"),
    ASISTENTE("asistente", "Asistente"),
    PERFIL("perfil", "Perfil")
}
