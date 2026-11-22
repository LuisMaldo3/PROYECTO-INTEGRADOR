package com.tecsup.tcweek.data

import java.time.Instant
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProximasYFiltrosTest {
    private fun ms(iso: String) = Instant.parse(iso).toEpochMilli()
    // Lunes 23/11/2026, 09:30 en Lima.
    private val ahora = ms("2026-11-23T14:30:00Z")

    // ---------- US-72: próximas actividades ----------

    @Test fun leeLasProximasOrdenadasYDescartaLasSinHora() {
        val json = JSONObject("""{"ahora":"2026-11-23T14:30:00Z","total":3,"actividades":[
            {"id":"b","nombre":"Concierto","empiezaEn":"2026-11-23T21:00:00Z","inscrito":false,"horaInicio":"16:00","ubicacion":"Patio central"},
            {"id":"a","nombre":"Charla IA","empiezaEn":"2026-11-23T15:00:00Z","inscrito":true,"horaInicio":"10:00"},
            {"id":"c","nombre":"Sin hora","empiezaEn":"mal"}]}""")
        val lista = leerProximas(json)
        assertEquals(listOf("a", "b"), lista.map { it.actividad.id })
        assertTrue(lista[0].inscrito)
        assertEquals(ms("2026-11-23T15:00:00Z"), lista[0].empiezaEn)
        assertEquals("Patio central", lista[1].actividad.ubicacion)
        assertTrue(leerProximas(JSONObject("{}")).isEmpty())
    }

    @Test fun diceCuantoFaltaDeFormaNatural() {
        assertEquals("Empieza en 30 min", textoFalta(ms("2026-11-23T15:00:00Z"), ahora))
        assertEquals("Empieza en 1 min", textoFalta(ahora + 10_000, ahora))
        assertEquals("Empieza ahora", textoFalta(ahora, ahora))
        assertEquals("Empieza en 2 h", textoFalta(ms("2026-11-23T16:30:00Z"), ahora))
        assertEquals("Empieza en 6 h 30 min", textoFalta(ms("2026-11-23T21:00:00Z"), ahora))
        assertEquals("Mañana a las 10:00", textoFalta(ms("2026-11-24T15:00:00Z"), ahora))
        assertEquals("jue 26/11 a las 19:00", textoFalta(ms("2026-11-27T00:00:00Z"), ahora))
    }

    @Test fun usaElDiaDePeruYNoElDiaUtc() {
        // 23:00 en Lima del lunes = 04:00 UTC del martes: sigue siendo «hoy» para el estudiante.
        val tarde = ms("2026-11-24T02:00:00Z") // 21:00 Lima
        assertEquals("Empieza en 2 h", textoFalta(ms("2026-11-24T04:00:00Z"), tarde))
    }

    @Test fun resaltaLasQueEmpiezanEnQuinceMinutosOMenos() {
        assertTrue(empiezaPronto(ahora + 15 * 60_000L, ahora))
        assertFalse(empiezaPronto(ahora + 16 * 60_000L, ahora))
        assertFalse(empiezaPronto(ahora - 1, ahora))
    }

    @Test fun recargaCadaCincoMinutosOCuandoLaPrimeraYaEmpezo() {
        val lista = listOf(ActividadProxima(actividad("a"), ahora + 3_600_000, false))
        assertTrue("primera carga", debeRecargarProximas(emptyList(), 0, ahora))
        assertFalse(debeRecargarProximas(lista, ahora - 60_000, ahora))
        assertTrue(debeRecargarProximas(lista, ahora - RECARGA_PROXIMAS_MS, ahora))
        assertTrue("la primera ya empezó", debeRecargarProximas(lista, ahora - 60_000, ahora + 3_600_000))
    }

    @Test fun entreRecargasOcultaLasQueYaEmpezaron() {
        val lista = listOf(ActividadProxima(actividad("a"), ahora + 1000, false), ActividadProxima(actividad("b"), ahora + 99_000, false))
        assertEquals(listOf("b"), vigentes(lista, ahora + 1000).map { it.actividad.id })
    }

    private fun actividad(id: String) = ActividadCatalogo(id, "Actividad $id", "", "2026-11-23", "10:00", "11:00", null, null, "")

    // ---------- US-73: filtros ----------

    @Test fun sinFiltrosNoAgregaParametros() {
        assertEquals("", FiltrosCatalogo().consulta())
        assertEquals(0, FiltrosCatalogo().activos)
    }

    @Test fun armaLaConsultaConHorarioCategoriaYArea() {
        val f = FiltrosCatalogo(Franja.TARDE, "charlas-talleres", "Tecnología Digital")
        assertEquals("?franja=tarde&categoria=charlas-talleres&area=Tecnolog%C3%ADa+Digital", f.consulta())
        assertEquals(3, f.activos)
        assertEquals("?area=Mec%C3%A1nica", FiltrosCatalogo(area = "Mecánica").consulta())
        assertEquals(1, FiltrosCatalogo(franja = Franja.NOCHE).activos)
    }

    @Test fun reconoceLasFranjasPorSuId() {
        assertEquals(Franja.MANANA, Franja.de("manana"))
        assertEquals(null, Franja.de(""))
        assertEquals(null, Franja.de("madrugada"))
    }

    @Test fun leeLasOpcionesDelServidorConLasCincoAreasSiempre() {
        val o = leerOpcionesFiltro(JSONObject("""{
            "categorias":[{"id":"deportes","nombre":"Deportes","total":3},{"id":"","nombre":"x","total":1}],
            "areas":[{"nombre":"Tecnología Digital","total":2}],
            "franjas":[{"id":"manana","total":1},{"id":"tarde","total":2},{"id":"otra","total":9}]}"""))
        assertEquals(listOf(OpcionFiltro("deportes", "Deportes", 3)), o.categorias)
        assertEquals(AREAS_TECSUP, o.areas.map { it.nombre })
        assertEquals(2, o.areas.first { it.nombre == "Tecnología Digital" }.total)
        assertEquals(0, o.areas.first { it.nombre == "Mecánica" }.total)
        assertEquals(mapOf(Franja.MANANA to 1, Franja.TARDE to 2), o.franjas)
    }

    @Test fun sinOpcionesUsaLasCincoAreasPorDefecto() {
        assertEquals(5, leerOpcionesFiltro(null).areas.size)
        assertTrue(leerOpcionesFiltro(null).categorias.isEmpty())
    }
}
