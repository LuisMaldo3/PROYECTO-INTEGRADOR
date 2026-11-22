package com.tecsup.tcweek.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramacionDiaTest {
    /** Respuesta con la forma exacta que entrega GET /api/programacion. */
    private val respuesta = """
        {"fecha":"2026-11-22","hoy":"2026-11-21",
         "dias":[{"fecha":"2026-11-23","total":1},{"fecha":"2026-11-22","total":3},{"fecha":"mal","total":9}],
         "total":3,
         "actividades":[
          {"id":"a2","nombre":"Concierto","fecha":"2026-11-22","horaInicio":"16:00","horaFin":"17:00","estado":"programada",
           "cuposDisponibles":10,"maximoParticipantes":30,"ubicacion":"Patio central","ubicacionReferencia":"",
           "ubicacionDetalle":{"lugar":"Patio central","confirmada":true},"inscrito":true,"estadoDia":"proxima"},
          {"id":"a1","nombre":"Charla","fecha":"2026-11-22","horaInicio":"10:00","horaFin":"11:00","estado":"programada",
           "cuposDisponibles":null,"maximoParticipantes":null,"ubicacion":"Auditorio","ubicacionReferencia":"Frente a la biblioteca",
           "ubicacionDetalle":{"lugar":"Auditorio","pabellon":"Pabellón A","referencia":"Frente a la biblioteca",
           "confirmada":true,"latitud":-12.0445,"longitud":-76.9529},
           "portadaUrl":"https://upload.wikimedia.org/foto.jpg","inscrito":false,"estadoDia":"en_curso"},
          {"id":"a3","nombre":"Feria","fecha":"2026-11-22","horaInicio":"12:00","horaFin":"13:00","estado":"cancelada",
           "ubicacion":"Patio central","inscrito":false,"estadoDia":"cancelada"}]}
    """.trimIndent()

    @Test fun leeLaProgramacionOrdenadaPorHora() {
        val p = leerProgramacion(JSONObject(respuesta))
        assertEquals("2026-11-22", p.fecha)
        assertEquals("2026-11-21", p.hoy)
        assertEquals(listOf("a1", "a3", "a2"), p.actividades.map { it.actividad.id })
        assertEquals(listOf(EstadoDia.EN_CURSO, EstadoDia.CANCELADA, EstadoDia.PROXIMA), p.actividades.map { it.estadoDia })
        assertEquals(listOf(false, false, true), p.actividades.map { it.inscrito })
    }

    @Test fun losDiasSeOrdenanYSeIgnoranLosMalEscritos() {
        val p = leerProgramacion(JSONObject(respuesta))
        assertEquals(listOf(DiaProgramacion("2026-11-22", 3), DiaProgramacion("2026-11-23", 1)), p.dias)
    }

    @Test fun cadaActividadTraeSuLugarReferenciaYPortada() {
        val charla = leerProgramacion(JSONObject(respuesta)).actividades.first().actividad
        assertEquals("Auditorio", charla.ubicacion)
        assertEquals("Frente a la biblioteca", charla.ubicacionReferencia)
        assertEquals("Pabellón A", charla.ubicacionDetalle.pabellon)
        assertTrue(charla.ubicacionDetalle.tieneMapa)
        assertEquals("https://upload.wikimedia.org/foto.jpg", charla.portadaUrl)
        assertNull(charla.cupos)
    }

    @Test fun unaActividadCanceladaNoSePuedeAbrir() {
        val p = leerProgramacion(JSONObject(respuesta))
        assertFalse(p.actividades.first { it.actividad.id == "a3" }.sePuedeAbrir)
        assertTrue(p.actividades.first { it.actividad.id == "a1" }.sePuedeAbrir)
    }

    @Test fun unaRespuestaVaciaNoRompeLaPantalla() {
        val p = leerProgramacion(JSONObject("""{"fecha":"2026-12-31","hoy":"2026-11-21"}"""))
        assertTrue(p.actividades.isEmpty())
        assertTrue(p.dias.isEmpty())
    }

    // ---------- Cambio de día (T03) ----------

    private val dias = listOf(DiaProgramacion("2026-11-20", 2), DiaProgramacion("2026-11-22", 3), DiaProgramacion("2026-11-25", 1))

    @Test fun pasaAlDiaAnteriorYAlSiguienteConActividades() {
        assertEquals("2026-11-20", diaAnterior(dias, "2026-11-22"))
        assertEquals("2026-11-25", diaSiguiente(dias, "2026-11-22"))
        // Desde un día sin actividades también salta al más cercano de cada lado.
        assertEquals("2026-11-22", diaAnterior(dias, "2026-11-23"))
        assertEquals("2026-11-25", diaSiguiente(dias, "2026-11-23"))
    }

    @Test fun enLosExtremosNoHayMasDias() {
        assertNull(diaAnterior(dias, "2026-11-20"))
        assertNull(diaSiguiente(dias, "2026-11-25"))
        assertNull(diaSiguiente(emptyList(), "2026-11-25"))
    }

    @Test fun nombraLosDiasCercanosYElRestoConFecha() {
        assertEquals("Hoy", etiquetaDia("2026-11-21", "2026-11-21"))
        assertEquals("Mañana", etiquetaDia("2026-11-22", "2026-11-21"))
        assertEquals("Ayer", etiquetaDia("2026-11-20", "2026-11-21"))
        assertEquals("mié 25/11", etiquetaDia("2026-11-25", "2026-11-21"))
        assertEquals("mal", etiquetaDia("mal", "2026-11-21"))
    }
}
