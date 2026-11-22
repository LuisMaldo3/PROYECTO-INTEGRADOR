package com.tecsup.tcweek.data

import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MiProximaYChatTest {
    private fun ms(iso: String) = Instant.parse(iso).toEpochMilli()
    private val ahora = ms("2026-11-23T15:30:00Z") // lunes 23/11, 10:30 en Lima

    // ---------- US-74: mi próxima actividad ----------

    private val respuesta = """{"tiene":true,
        "actividad":{"id":"a1","nombre":"Charla de robótica","horaInicio":"11:00","horaFin":"12:00","fecha":"2026-11-23",
          "ubicacion":"Auditorio","ubicacionReferencia":"Frente a la biblioteca",
          "ubicacionDetalle":{"lugar":"Auditorio","pabellon":"Pabellón A","referencia":"Frente a la biblioteca","confirmada":true},
          "empiezaEn":"2026-11-23T16:00:00Z","terminaEn":"2026-11-23T17:00:00Z","enCurso":false},
        "despues":{"id":"a2","nombre":"Torneo de fútbol","horaInicio":"16:00","ubicacion":"Polideportivo","empiezaEn":"2026-11-23T21:00:00Z"}}"""

    @Test fun leeMiProximaActividadConSuLugarYLaQueSigue() {
        val p = leerMiProxima(JSONObject(respuesta))!!
        assertEquals("Charla de robótica", p.actividad.nombre)
        assertEquals("Auditorio", p.actividad.ubicacionDetalle.lugar)
        assertEquals("Pabellón A", p.actividad.ubicacionDetalle.pabellon)
        assertEquals("Frente a la biblioteca", p.actividad.ubicacionReferencia)
        assertEquals(ms("2026-11-23T16:00:00Z"), p.empiezaEn)
        assertEquals("Torneo de fútbol", p.despues!!.nombre)
        assertEquals("Polideportivo", p.despues!!.ubicacion)
    }

    @Test fun sinProximaOConDatosInvalidosNoHayTarjeta() {
        assertNull(leerMiProxima(JSONObject("""{"tiene":false,"actividad":null,"despues":null}""")))
        assertNull(leerMiProxima(JSONObject("""{"tiene":true,"actividad":{"id":"a","empiezaEn":"mal"}}""")))
        val sinFin = leerMiProxima(JSONObject("""{"tiene":true,"actividad":{"id":"a","empiezaEn":"2026-11-23T16:00:00Z"}}"""))!!
        assertEquals(ms("2026-11-23T17:00:00Z"), sinFin.terminaEn)
        assertNull(sinFin.despues)
    }

    @Test fun diceSiEstaEnCursoOCuantoFalta() {
        val p = leerMiProxima(JSONObject(respuesta))!!
        assertEquals("Empieza en 30 min", estadoMiProxima(p, ahora))
        assertTrue(p.enCurso(ms("2026-11-23T16:10:00Z")))
        assertEquals("Ahora mismo · termina 12:00", estadoMiProxima(p, ms("2026-11-23T16:10:00Z")))
        assertTrue(p.terminada(ms("2026-11-23T17:00:00Z")))
    }

    @Test fun recargaCadaCincoMinutosOAlTerminarLaActividad() {
        val p = leerMiProxima(JSONObject(respuesta))!!
        assertTrue(debeRecargarMiProxima(null, 0, ahora))
        assertFalse(debeRecargarMiProxima(p, ahora - 60_000, ahora))
        assertTrue(debeRecargarMiProxima(p, ahora - RECARGA_PROXIMAS_MS, ahora))
        assertTrue("terminó: pasa a la siguiente", debeRecargarMiProxima(p, ms("2026-11-23T16:59:00Z"), ms("2026-11-23T17:00:00Z")))
    }

    // ---------- US-75: conversación ----------

    private class Falso(var respuestas: MutableList<Any>) : ServicioChat {
        val preguntas = mutableListOf<String>()
        var historiales = 0
        override suspend fun preguntar(token: String, texto: String): RespuestaAsistente {
            preguntas.add(texto)
            val siguiente = respuestas.removeAt(0)
            if (siguiente is Exception) throw siguiente
            return siguiente as RespuestaAsistente
        }
        override suspend fun historial(token: String): List<MensajeAsistente> {
            historiales++
            return listOf(MensajeAsistente("usuario", "¿Hola?"), MensajeAsistente("asistente", "Hola desde la web"))
        }
    }

    private fun respuesta(texto: String) = RespuestaAsistente(texto, "catalogo", "")

    @Test fun enviaLaPreguntaLimpiaYDevuelveLaRespuesta() = runBlocking {
        val falso = Falso(mutableListOf(respuesta("Es en el Auditorio")))
        val r = ConversacionChat(falso).enviar("t", "  ¿Dónde es la charla?  ")
        assertEquals(listOf("¿Dónde es la charla?"), falso.preguntas)
        assertEquals("Es en el Auditorio", (r as ResultadoEnvio.Respondida).respuesta.respuesta)
        assertNull(r.historial)
    }

    @Test fun siOtraSesionCambioLaConversacionSincronizaYReintentaUnaVez() = runBlocking {
        val falso = Falso(mutableListOf(ErrorAsistente(409, "cambió"), respuesta("Respuesta tras sincronizar")))
        val r = ConversacionChat(falso).enviar("t", "¿Qué hay hoy?") as ResultadoEnvio.Respondida
        assertEquals(1, falso.historiales)
        assertEquals(2, falso.preguntas.size)
        assertEquals("Hola desde la web", r.historial!!.last().contenido)
        val mensajes = mensajesTrasRespuesta(listOf(MensajeAsistente("usuario", "viejo")), "¿Qué hay hoy?", r)
        assertEquals(listOf("¿Hola?", "Hola desde la web", "¿Qué hay hoy?", "Respuesta tras sincronizar"), mensajes.map { it.contenido })
    }

    @Test fun losErroresDeRedSePuedenReintentarYLosDeSesionNo() = runBlocking {
        val red = ConversacionChat(Falso(mutableListOf(IllegalStateException("Sin conexión")))).enviar("t", "hola")
        assertTrue((red as ResultadoEnvio.Fallida).reintentable)
        assertEquals("Sin conexión", red.mensaje)
        val caido = ConversacionChat(Falso(mutableListOf(ErrorAsistente(503, "Mongo caído")))).enviar("t", "hola")
        assertTrue((caido as ResultadoEnvio.Fallida).reintentable)
        val sesion = ConversacionChat(Falso(mutableListOf(ErrorAsistente(401, "Tu sesión expiró")))).enviar("t", "hola")
        assertFalse((sesion as ResultadoEnvio.Fallida).reintentable)
    }

    @Test fun unaPreguntaVaciaOLargaNoSeEnvia() = runBlocking {
        val falso = Falso(mutableListOf())
        val vacia = ConversacionChat(falso).enviar("t", "   ")
        assertFalse((vacia as ResultadoEnvio.Fallida).reintentable)
        assertTrue(falso.preguntas.isEmpty())
        assertEquals("Escribe una pregunta.", validarPregunta(" "))
        assertEquals(null, validarPregunta("x".repeat(600)))
        assertEquals("La pregunta puede tener hasta 600 caracteres.", validarPregunta("x".repeat(601)))
    }

    @Test fun mensajesTrasRespuestaAgregaPreguntaYRespuestaConSusDatos() {
        val r = ResultadoEnvio.Respondida(RespuestaAsistente("En el Auditorio", "catalogo", "", tipoConsulta = "ubicacion"))
        val m = mensajesTrasRespuesta(emptyList(), " ¿Dónde? ", r)
        assertEquals(listOf("usuario", "asistente"), m.map { it.rol })
        assertEquals("¿Dónde?", m[0].contenido)
        assertEquals("ubicacion", m[1].tipoConsulta)
    }

    @Test fun lasSugerenciasDelServidorSeLimpianYHayRespaldo() {
        val s = leerSugerencias(JSONObject("""{"sugerencias":["¿Dónde se realiza Charla IA?","","¿Dónde se realiza Charla IA?","a","b","c","d"]}"""))
        assertEquals(listOf("¿Dónde se realiza Charla IA?", "a", "b", "c", "d"), s)
        assertEquals(EjemplosConsultasNaturales.preguntas, leerSugerencias(JSONObject("{}")))
        assertEquals(EjemplosConsultasNaturales.preguntas, leerSugerencias(JSONObject("""{"sugerencias":[]}""")))
    }

    @Test fun conTarjetasLaBurbujaMuestraSoloLaIntroduccion() {
        val texto = "Estas son las ubicaciones registradas para las actividades:\n\n• Charla\n  Lugar: Auditorio\n\n• Taller\n  Lugar: Lab"
        val lugar = UbicacionAsistente("a1", "Charla", "2026-11-23", "11:00", "Auditorio", "", "", "", "", "", true)
        assertEquals("Estas son las ubicaciones registradas para las actividades:",
            textoVisible(MensajeAsistente("asistente", texto, ubicaciones = listOf(lugar))))
        assertEquals("sin tarjetas se muestra completo", texto, textoVisible(MensajeAsistente("asistente", texto)))
        assertEquals("Hola", textoVisible(MensajeAsistente("asistente", "Hola", ubicaciones = listOf(lugar))))
    }
}
