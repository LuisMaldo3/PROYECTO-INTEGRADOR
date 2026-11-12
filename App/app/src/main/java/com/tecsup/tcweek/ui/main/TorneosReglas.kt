package com.tecsup.tcweek.ui.main

import com.tecsup.tcweek.data.EquipoTorneo
import com.tecsup.tcweek.data.PartidoTorneo
import com.tecsup.tcweek.data.Torneo
import com.tecsup.tcweek.ui.theme.Departamentos

/** T&C ARENA y Karadúo · lógica de pantalla sin Compose (se prueba con JUnit). */
data class RondaTorneo(val ronda: Int, val nombre: String, val partidos: List<PartidoTorneo>)

object TorneosReglas {
    fun agruparRondas(partidos: List<PartidoTorneo>): List<RondaTorneo> =
        partidos.groupBy { it.ronda }.toSortedMap().map { (ronda, ps) ->
            RondaTorneo(ronda, ps.first().nombreRonda, ps.sortedBy { it.posicion })
        }

    /** Número junto a cada departamento (marcador o sets); null si el torneo solo registra ganador. */
    fun puntajeDeLado(p: PartidoTorneo, lado: Char): Int? {
        val r = p.resultado ?: return null
        return when (r.modo) {
            "sets" -> if (lado == 'a') r.setsA else r.setsB
            "marcador" -> if (lado == 'a') r.a else r.b
            else -> null
        }
    }

    /** «Ganó TD», «Ganó TD en el desempate»; vacío si todavía no hay ganador. */
    fun fraseGanador(p: PartidoTorneo, nombreDe: (String) -> String = { Departamentos.nombre(it) }): String {
        if (p.descanso || p.ganador == null) return ""
        val nombre = nombreDe(p.ganador)
        return if (p.resultado?.ganadorPenales != null) "Ganó $nombre en el desempate" else "Ganó $nombre"
    }

    /** Enfrentamientos con rivales y horario, por fecha y hora. */
    fun calendario(t: Torneo): List<PartidoTorneo> = t.partidos
        .filter { !it.descanso && it.a != null && it.b != null && it.fecha.isNotBlank() && it.hora.isNotBlank() && it.estado != "cancelado" }
        .sortedWith(compareBy({ it.fecha }, { it.hora }))

    fun porDia(partidos: List<PartidoTorneo>): List<Pair<String, List<PartidoTorneo>>> =
        partidos.groupBy { it.fecha }.toSortedMap().map { (fecha, ps) -> fecha to ps }

    /** Jugados sobre los que realmente se juegan (los descansos no cuentan). */
    fun avance(t: Torneo): Pair<Int, Int> {
        val jugables = t.partidos.filter { !it.descanso }
        return jugables.count { it.estado == "finalizado" } to jugables.size
    }

    fun deTipo(torneos: List<Torneo>, tipo: String): List<Torneo> =
        torneos.filter { it.tipo == tipo }.sortedBy { it.deporte.lowercase() }

    /** Nombre, área y departamento de un participante. Si no está entre los equipos, se trata como un departamento. */
    fun equipo(t: Torneo, id: String?): EquipoTorneo? {
        if (id == null) return null
        t.equipos[id]?.let { return it }
        val d = Departamentos.porId(id) ?: return EquipoTorneo(id, id, "", null)
        return EquipoTorneo(id, d.nombre, d.area, id)
    }
}
