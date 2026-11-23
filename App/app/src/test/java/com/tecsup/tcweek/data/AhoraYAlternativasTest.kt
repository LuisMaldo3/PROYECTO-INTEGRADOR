package com.tecsup.tcweek.data

import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** US-78 («¿Qué hay ahora?» en el chat) y US-79 («No puedo asistir»). */
class AhoraYAlternativasTest {

    // ---------- US-78 ----------

    private val respuestaAhora = """[
        {"id":"a1","nombre":"Taller de IA","fecha":"2026-11-23","horaInicio":"10:00","horaFin":"12:00","ubicacion":"Auditorio",
         "cuposDisponibles":8,"estadoAhora":"en_curso","minutos":90,"detalleAhora":"En curso · termina en 1 h 30 min (12:00)",
         "inscrito":false,"cruzaCon":"Feria","inscripcionDirecta":true},
        {"id":"a4","nombre":"Torneo de fútbol","fecha":"2026-11-23","horaInicio":"11:00","horaFin":"12:00","ubicacion":"Cancha 2",
         "estadoAhora":"pronto","minutos":30,"inscrito":true,"cruzaCon":null,"inscripcionDirecta":false}]"""

    @Test fun leeLasActividadesDeAhoraConSuEstado() {
        val lista = leerActividadesMomento(JSONArray(respuestaAhora))
        assertEquals(2, lista.size)
        val taller = lista[0]
        assertTrue(taller.enCurso)
        assertEquals("En curso · quedan 1 h 30 min", taller.etiquetaAhora())
        assertEquals("Auditorio", taller.ubicacion)
        assertEquals("Feria", taller.cruzaCon)
        assertTrue(taller.inscripcionDirecta)
        val futbol = lista[1]
        assertFalse(futbol.enCurso)
        assertEquals("Empieza en 30 min", futbol.etiquetaAhora())
        assertTrue(futbol.inscrito)
        assertEquals("cruzaCon null no se muestra como «null»", "", futbol.cruzaCon)
    }

    @Test fun lasRespuestasAntiguasSiguenFuncionando() {
        val vieja = leerActividadesMomento(JSONArray("""[{"id":"x","nombre":"Charla","fecha":"2026-11-23","horaInicio":"09:00","horaFin":"10:00"}]"""))
        assertEquals("", vieja[0].etiquetaAhora())
        assertEquals("", vieja[0].ubicacion)
    }

    @Test fun elBotonEnviaLaPreguntaYSusTarjetasReemplazanLasVinetas() {
        assertEquals("¿Qué hay ahora?", PREGUNTA_AHORA)
        assertTrue(esConsultaDeActividades("ahora"))
        val actividades = leerActividadesMomento(JSONArray(respuestaAhora))
        val m = MensajeAsistente("asistente",
            "Ahora mismo (10:30) hay 1 actividad en curso y 1 empieza dentro de la próxima hora:\n\n• Taller de IA — En curso\n• Torneo",
            tipoConsulta = "ahora", actividades = actividades)
        assertEquals("Ahora mismo (10:30) hay 1 actividad en curso y 1 empieza dentro de la próxima hora:", textoVisible(m))
    }

    @Test fun minutosLegibles() {
        assertEquals("35 min", textoMinutos(35))
        assertEquals("1 h 30 min", textoMinutos(90))
        assertEquals("2 h", textoMinutos(120))
    }

    // ---------- US-79 ----------

    private val alternativas = """{"actividad":{"id":"o1","nombre":"Taller de robótica","fecha":"2026-11-24","horaInicio":"10:00","horaFin":"12:00"},
        "motivo":"horario","origen":"reglas","puedeCambiar":true,"aviso":"Ninguna se cruza con tus otras inscripciones.",
        "alternativas":[
          {"id":"c2","nombre":"Robótica móvil","fecha":"2026-11-25","horaInicio":"10:00","horaFin":"12:00","categoriaId":"charlas-talleres",
           "ubicacion":"Laboratorio TD-204","motivo":"Se parece a la que dejas: es de la misma categoría.",
           "etiquetas":["Misma categoría","El miércoles 25 a las 10:00"],"inscripcionDirecta":true},
          {"id":"h1","nombre":"Hackathon","tipoActividad":"competencia","modalidad":"equipos","fecha":"2026-11-25",
           "horaInicio":"16:00","horaFin":"19:00","motivo":"","inscripcionDirecta":false},
          {"id":"c2","nombre":"Repetida"}]}"""

    @Test fun leeLasAlternativasYLaActividadQueDejo() {
        val r = leerAlternativas(JSONObject(alternativas))
        assertEquals("o1", r.actividadId)
        assertEquals("Taller de robótica", r.nombre)
        assertTrue(r.puedeCambiar)
        assertFalse(r.conIa)
        assertEquals(listOf("c2", "h1"), r.alternativas.map { it.actividad.id })
        assertEquals("Laboratorio TD-204", r.alternativas[0].actividad.ubicacion)
        assertEquals(listOf("Misma categoría", "El miércoles 25 a las 10:00"), r.alternativas[0].etiquetas)
        assertTrue(r.alternativas[1].porEquipos)
        assertFalse(r.alternativas[1].inscripcionDirecta)
        assertEquals("Encaja en tu agenda.", r.alternativas[1].motivo)
        assertEquals("Por parecido · IA no disponible", origenAlternativas(r.origen))
        assertEquals("Propuestas por la IA (Gemini)", origenAlternativas("ia"))
    }

    private class Falso(val fallaInscribir: Boolean = false, val fallaCancelar: Boolean = false) : ServicioInscripcion {
        val llamadas = mutableListOf<String>()
        override suspend fun inscribir(token: String, id: String) {
            llamadas += "inscribir $id"
            if (fallaInscribir) throw IllegalStateException("Sin cupos.")
        }
        override suspend fun cancelar(token: String, id: String) {
            llamadas += "cancelar $id"
            if (fallaCancelar) throw IllegalStateException("HTTP 409.")
        }
    }

    @Test fun alCambiarsePrimeroSeInscribeYSoloDespuesCancela() = runBlocking {
        val bien = Falso()
        assertEquals(ResultadoCambio.Cambiado, AlternativasAsistenciaApi.cambiar("t", "nueva", "vieja", bien))
        assertEquals(listOf("inscribir nueva", "cancelar vieja"), bien.llamadas)

        val sinCupos = Falso(fallaInscribir = true)
        val r = AlternativasAsistenciaApi.cambiar("t", "nueva", "vieja", sinCupos)
        assertEquals(ResultadoCambio.NoInscrito("Sin cupos."), r)
        assertEquals("si no se inscribe, no cancela la anterior", listOf("inscribir nueva"), sinCupos.llamadas)

        val noCancela = Falso(fallaCancelar = true)
        assertEquals(ResultadoCambio.SinCancelar("HTTP 409."), AlternativasAsistenciaApi.cambiar("t", "nueva", "vieja", noCancela))
    }

    @Test fun losMotivosQueSeEnvianAlServidor() {
        assertEquals(listOf("horario", "otro"), MotivoInasistencia.entries.map { it.clave })
        assertEquals("No puedo a esa hora", MotivoInasistencia.HORARIO.texto)
    }
}
