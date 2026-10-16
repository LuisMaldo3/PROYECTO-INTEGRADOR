package com.tecsup.tcweek.ui.main

/** Texto estable al recibir una respuesta sin opciones y sin errores de conexión. */
fun resumenAlternativas(cantidad: Int, error: String? = null): String = when {
    error != null -> "No se pudieron obtener alternativas."
    cantidad == 0 -> "Por ahora no hay opciones compatibles con cupos."
    cantidad == 1 -> "1 alternativa disponible"
    else -> "$cantidad alternativas disponibles"
}
