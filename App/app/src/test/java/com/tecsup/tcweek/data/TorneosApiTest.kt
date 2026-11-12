package com.tecsup.tcweek.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TorneosApiTest {
    private val json = """
      {"torneos":[{"id":"t1","tipo":"arena","deporte":"Fútbol","modoResultado":"marcador","estado":"en_juego",
        "participantes":["td","quimica","mecanica"],"campeon":null,"subcampeon":null,"campeonValidado":false,
        "partidos":[
          {"id":"R1-P1","ronda":1,"posicion":1,"nombreRonda":"Semifinales","a":"td","b":null,"estado":"finalizado","descanso":true,
           "fecha":"","hora":"","lugar":"","ganador":"td","provisional":false,"resultado":null},
          {"id":"R1-P2","ronda":1,"posicion":2,"nombreRonda":"Semifinales","a":"quimica","b":"mecanica","estado":"finalizado","descanso":false,
           "fecha":"2026-11-12","hora":"16:30","lugar":"Cancha 1","ganador":"mecanica","provisional":true,
           "resultado":{"modo":"marcador","a":2,"b":2,"ganadorPenales":"b","observaciones":""}}]}]}
    """.trimIndent()

    @Test fun leeTorneosPartidosYResultados() {
        val t = TorneosApi.leer(json).single()
        assertEquals("Fútbol", t.deporte)
        assertEquals(listOf("td", "quimica", "mecanica"), t.participantes)
        assertNull(t.campeon)
        assertEquals(2, t.partidos.size)
    }

    @Test fun losDescansosYLosRivalesVaciosSeLeenComoNulos() {
        val descanso = TorneosApi.leer(json).single().partidos[0]
        assertTrue(descanso.descanso)
        assertNull(descanso.b)
        assertNull(descanso.resultado)
    }

    @Test fun elResultadoConservaMarcadorDesempateYEstadoProvisional() {
        val p = TorneosApi.leer(json).single().partidos[1]
        assertEquals(2, p.resultado?.a)
        assertEquals(2, p.resultado?.b)
        assertEquals("b", p.resultado?.ganadorPenales)
        assertNull(p.resultado?.setsA)
        assertTrue(p.provisional)
        assertFalse(TorneosApi.leer(json).single().partidos[0].provisional)
    }

    @Test fun unaRespuestaSinTorneosNoRompe() {
        assertTrue(TorneosApi.leer("""{"torneos":[]}""").isEmpty())
        assertTrue(TorneosApi.leer("""{}""").isEmpty())
    }
}
