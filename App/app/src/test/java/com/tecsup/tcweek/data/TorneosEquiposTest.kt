package com.tecsup.tcweek.data

import com.tecsup.tcweek.ui.main.TorneosReglas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Las llaves muestran equipos (nombre y área), no solo departamentos. */
class TorneosEquiposTest {
    private val publico = """
        {"torneos":[{"id":"t1","tipo":"arena","deporte":"Fútbol","modoResultado":"marcador","estado":"en_juego",
          "participantes":["td","td","mecanica"],
          "equipos":[{"id":"e1","nombre":"Los Halcones","area":"Tecnología Digital","departamento":"td"},
                     {"id":"e2","nombre":"Bits FC","area":"Tecnología Digital","departamento":"td"},
                     {"id":"e3","nombre":"Mecatrónicos","area":"Mecánica","departamento":"mecanica"}],
          "campeon":"td","campeonId":"e2","subcampeon":"td","subcampeonId":"e1",
          "partidos":[{"id":"R1-P1","ronda":1,"posicion":1,"nombreRonda":"Final","a":"td","b":"td","aId":"e1","bId":"e2",
                       "ganador":"td","ganadorId":"e2","estado":"finalizado","descanso":false,"fecha":"","hora":"","lugar":"",
                       "resultado":{"modo":"marcador","a":1,"b":2}}]}]}
    """.trimIndent()

    @Test fun dosEquiposDelMismoDepartamentoNoSeConfunden() {
        val t = TorneosApi.leer(publico).single()
        assertEquals(listOf("e1", "e2", "e3"), t.participantes)
        val p = t.partidos.single()
        assertEquals(Triple("e1", "e2", "e2"), Triple(p.a, p.b, p.ganador))
        assertEquals("e2", t.campeon)
        assertEquals("Bits FC", TorneosReglas.equipo(t, "e2")?.nombre)
        assertEquals("td", TorneosReglas.equipo(t, "e2")?.departamento)
        assertEquals("Ganó Bits FC", TorneosReglas.fraseGanador(p) { TorneosReglas.equipo(t, it)?.nombre ?: it })
    }

    @Test fun unTorneoAntiguoSoloConDepartamentosSeSigueViendoIgual() {
        val viejo = TorneosApi.leer("""{"torneos":[{"id":"x","tipo":"arena","deporte":"Vóley","participantes":["td","dpg"],"campeon":"td",
            "partidos":[{"id":"R1-P1","ronda":1,"nombreRonda":"Final","a":"td","b":"dpg","ganador":"td","estado":"finalizado"}]}]}""").single()
        assertEquals(listOf("td", "dpg"), viejo.participantes)
        assertEquals("td", viejo.partidos.single().a)
        assertEquals("TD", TorneosReglas.equipo(viejo, "td")?.nombre)
        assertNull(TorneosReglas.equipo(viejo, null))
    }
}
