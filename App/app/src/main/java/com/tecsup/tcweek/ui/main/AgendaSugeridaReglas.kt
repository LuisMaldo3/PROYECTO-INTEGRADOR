package com.tecsup.tcweek.ui.main

/** Etiqueta honesta: nunca se anuncia una respuesta de respaldo como si fuera Gemini. */
fun etiquetaOrigenAgenda(origen: String): String = when (origen) {
    "ia" -> "✦ Priorizada con Gemini"
    "reglas" -> "Propuesta sin IA, por reglas"
    else -> "No hay sugerencias disponibles"
}
