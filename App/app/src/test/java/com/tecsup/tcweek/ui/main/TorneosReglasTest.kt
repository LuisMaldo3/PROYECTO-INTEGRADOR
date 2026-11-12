package com.tecsup.tcweek.ui.main

import com.tecsup.tcweek.data.PartidoTorneo
import com.tecsup.tcweek.data.ResultadoTorneo
import com.tecsup.tcweek.data.Torneo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TorneosReglasTest {
    private fun partido(
        id: String = "R1-P1", ronda: Int = 1, posicion: Int = 1, nombreRonda: String = "Semifinales",
        a: String? = "td", b: String? = "mecanica", estado: String = "pendiente", descanso: Boolean = false,
        fecha: String = "", hora: String = "", ganador: String? = null, resultado: ResultadoTorneo? = null
    ) = PartidoTorneo(id, ronda, posicion, nombreRonda, a, b, estado, descanso, fecha, hora, "", ganador, false, resultado)

    private fun torneo(vararg ps: PartidoTorneo, tipo: String = "arena", deporte: String = "Fútbol") =
        Torneo("t1", tipo, deporte, "marcador", "en_juego", listOf("td", "mecanica"), null, null, ps.toList())

    @Test fun lasRondasSalenEnOrdenYLosEnfrentamientosPorPosicion() {
        val rondas = TorneosReglas.agruparRondas(listOf(
            partido(id = "F", ronda = 2, nombreRonda = "Final"), partido(id = "B", posicion = 2), partido()))
        assertEquals(listOf("Semifinales", "Final"), rondas.map { it.nombre })
        assertEquals(listOf("R1-P1", "B"), rondas[0].partidos.map { it.id })
    }

    @Test fun elPuntajeDependeDeComoSeMideElTorneo() {
        val marcador = partido(resultado = ResultadoTorneo("marcador", 3, 1, null, null, null))
        val sets = partido(resultado = ResultadoTorneo("sets", null, null, 2, 0, null))
        val ganador = partido(resultado = ResultadoTorneo("ganador", null, null, null, null, null))
        assertEquals(3, TorneosReglas.puntajeDeLado(marcador, 'a'))
        assertEquals(0, TorneosReglas.puntajeDeLado(sets, 'b'))
        assertNull(TorneosReglas.puntajeDeLado(ganador, 'a'))
        assertNull(TorneosReglas.puntajeDeLado(partido(), 'a'))
    }

    @Test fun laFraseDelGanadorUsaElNombreYAvisaDelDesempate() {
        assertEquals("Ganó Química", TorneosReglas.fraseGanador(partido(ganador = "quimica")))
        assertEquals("Ganó TD en el desempate", TorneosReglas.fraseGanador(partido(ganador = "td", resultado = ResultadoTorneo("marcador", 1, 1, null, null, "a"))))
        assertEquals("", TorneosReglas.fraseGanador(partido()))
        assertEquals("", TorneosReglas.fraseGanador(partido(descanso = true, ganador = "td")))
    }

    @Test fun elCalendarioSoloTraeEnfrentamientosConRivalesYHorarioOrdenados() {
        val t = torneo(
            partido(id = "c", fecha = "2026-11-18", hora = "10:00", estado = "programado"),
            partido(id = "a", fecha = "2026-11-17", hora = "16:00", estado = "programado"),
            partido(id = "sin-horario"), partido(id = "descanso", descanso = true, fecha = "2026-11-17", hora = "09:00"),
            partido(id = "sin-rival", b = null, fecha = "2026-11-17", hora = "09:00"),
            partido(id = "cancelado", fecha = "2026-11-17", hora = "12:00", estado = "cancelado")
        )
        assertEquals(listOf("a", "c"), TorneosReglas.calendario(t).map { it.id })
        assertEquals(listOf("2026-11-17" to 1, "2026-11-18" to 1), TorneosReglas.porDia(TorneosReglas.calendario(t)).map { it.first to it.second.size })
    }

    @Test fun elAvanceNoCuentaLosDescansosYLosTorneosSeFiltranPorTipo() {
        assertEquals(1 to 2, TorneosReglas.avance(torneo(partido(descanso = true, estado = "finalizado"), partido(estado = "finalizado"), partido())))
        val lista = listOf(torneo(tipo = "arena", deporte = "Vóley"), torneo(tipo = "karaduo", deporte = "Karadúo"), torneo(tipo = "arena", deporte = "Fútbol"))
        assertEquals(listOf("Fútbol", "Vóley"), TorneosReglas.deTipo(lista, "arena").map { it.deporte })
    }
}
