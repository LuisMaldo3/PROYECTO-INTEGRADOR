package com.tecsup.tcweek.data

/** Comprobación local previa al PUT; el backend vuelve a validar. */
object ReglasHorario {
    fun validar(items: List<BloqueHorario>): String? {
        if (items.size > 100) return "Máximo 100 bloques por horario."
        for (b in items) {
            if (b.dia !in 1..7) return "Revisa el día del bloque."
            if (!Regex("([01]\\d|2[0-3]):[0-5]\\d").matches(b.inicio) || !Regex("([01]\\d|2[0-3]):[0-5]\\d").matches(b.fin)) return "Las horas deben tener formato HH:mm."
            if (b.inicio >= b.fin) return "La hora de fin debe ser posterior al inicio."
        }
        return null
    }
}
