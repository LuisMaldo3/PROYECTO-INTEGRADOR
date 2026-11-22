package com.tecsup.tcweek.data

import java.time.Instant
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** US-76 (recomendaciones de IA) y US-77 (agenda inteligente) en la app. */
class RecomendacionesYAgendaTest {
    private fun ms(iso: String) = Instant.parse(iso).toEpochMilli()
    private val ahora = ms("2026-11-23T15:30:00Z") // lunes 23/11, 10:30 en Lima

    // ---------- US-76 T2: leer las recomendaciones ----------

    private val recomendaciones = """{"generadoEn":"2026-11-23T15:30:00Z","vigenteHasta":"2026-11-23T15:45:00Z",
        "origen":"ia","conHorario":true,"aviso":"Disponibilidad confirmada","total":3,
        "recomendaciones":[
          {"id":"a1","nombre":"Torneo de fútbol 7","fecha":"2026-11-24","horaInicio":"16:00","horaFin":"17:00",
           "categoriaId":"deportes","ubicacion":"Cancha 2","cuposDisponibles":3,"maximoParticipantes":30,
           "motivo":"Te gustan los deportes","etiquetas":["Te interesa: deportes","Mañana","Quedan 3 cupos"],
           "cabeEnHorario":true,"huecoInicio":"13:00","huecoFin":"21:00","inscripcionDirecta":true},
          {"id":"a2","nombre":"Hackathon","tipoActividad":"competencia","modalidad":"equipos","fecha":"2026-11-25",
           "horaInicio":"09:00","horaFin":"13:00","motivo":"null","etiquetas":["Por equipos"],"inscripcionDirecta":false},
          {"id":"a1","nombre":"Repetida","fecha":"2026-11-24","horaInicio":"16:00","horaFin":"17:00"},
          {"nombre":"Sin id"}]}"""

    @Test fun leeLasRecomendacionesConTodoParaMostrarlasEInscribirse() {
        val p = leerRecomendacionesApp(JSONObject(recomendaciones), ahora)
        assertEquals(ahora, p.generadoEn)
        assertEquals(ms("2026-11-23T15:45:00Z"), p.vigenteHasta)
        assertTrue(p.conIa)
        assertTrue(p.conHorario)
        assertEquals("sin repetidas ni sin id", listOf("a1", "a2"), p.recomendaciones.map { it.actividad.id })
        val futbol = p.recomendaciones[0]
        assertEquals("Cancha 2", futbol.actividad.ubicacion)
        assertEquals(listOf("Te interesa: deportes", "Mañana", "Quedan 3 cupos"), futbol.etiquetas)
        assertTrue(futbol.horario.mostrarEtiqueta())
        assertTrue(futbol.inscripcionDirecta)
        assertFalse(futbol.porEquipos)
        val hackathon = p.recomendaciones[1]
        assertTrue(hackathon.porEquipos)
        assertFalse("por equipos se inscribe desde el detalle", hackathon.inscripcionDirecta)
        assertEquals("Actividad disponible que encaja con tu agenda.", hackathon.motivo)
    }

    @Test fun sinFechasUsaElMomentoDeLecturaYLaVigenciaPorDefecto() {
        val p = leerRecomendacionesApp(JSONObject("""{"origen":"reglas","recomendaciones":[]}"""), ahora)
        assertEquals(ahora, p.generadoEn)
        assertEquals(ahora + CargaProgramada.VIGENCIA_RECOMENDACIONES_MS, p.vigenteHasta)
        assertFalse(p.conIa)
        assertEquals("Por tus intereses · IA no disponible", origenRecomendaciones(p.origen))
        assertEquals("Elegidas con IA (Gemini)", origenRecomendaciones("ia"))
    }

    // ---------- T3: carga programada ----------

    @Test fun recargaSiNuncaCargoSiVencioOSiCambioLaAgenda() {
        val marca = Marca(cargadoEn = ahora, vigenteHasta = ahora + 60_000, versionAgenda = 2)
        assertTrue(CargaProgramada.debeRecargar(null, ahora, 2))
        assertFalse(CargaProgramada.debeRecargar(marca, ahora + 59_000, 2))
        assertTrue("venció", CargaProgramada.debeRecargar(marca, ahora + 60_000, 2))
        assertTrue("se inscribió o canceló", CargaProgramada.debeRecargar(marca, ahora + 1_000, 3))
        assertEquals(59_000L, CargaProgramada.esperaHastaVencer(marca, ahora + 1_000))
        assertEquals("nunca menos de 30 s", 30_000L, CargaProgramada.esperaHastaVencer(marca, ahora + 59_000))
    }

    @Test fun diceQueTanRecienteEsLoQueSeVe() {
        assertEquals("hace un momento", haceCuanto(ahora, ahora + 20_000))
        assertEquals("hace 5 min", haceCuanto(ahora, ahora + 5 * 60_000))
        assertEquals("hace 2 h", haceCuanto(ahora, ahora + 125 * 60_000))
        assertEquals("hace 1 d", haceCuanto(ahora, ahora + 25 * 3_600_000L))
    }

    // ---------- US-77: agenda inteligente ----------

    private val agenda = """{"generadoEn":"2026-11-23T15:30:00Z","origen":"reglas","aviso":"Te sugerimos actividades",
        "resumen":{"inscritas":2,"sugeridas":2,"enEquipo":1,"equiposPorConfirmar":1},
        "avisos":["Tu equipo «Robots» en «Competencia» tiene 2 de 3 integrantes confirmados; falta 1."],
        "dias":[
          {"fecha":"2026-11-23","etiqueta":"Hoy · lunes 23 de noviembre","bloques":[
            {"tipo":"inscrita","actividadId":"t1","nombre":"Taller de IA","fecha":"2026-11-23","horaInicio":"10:00","horaFin":"11:00",
             "ubicacion":"Laboratorio 4","modalidad":"individual","equipo":null},
            {"tipo":"sugerida","actividadId":"s0","nombre":"Ya empezó","fecha":"2026-11-23","horaInicio":"10:15","horaFin":"12:00"},
            {"tipo":"sugerida","actividadId":"s1","nombre":"Charla de drones","fecha":"2026-11-23","horaInicio":"15:00","horaFin":"16:00",
             "ubicacion":"Auditorio","motivo":"Te gustan los drones","inscripcionDirecta":true,"modalidad":"individual"},
            {"tipo":"inscrita","actividadId":"x","nombre":"Sin hora","horaInicio":"","horaFin":""}]},
          {"fecha":"2026-11-24","etiqueta":"Mañana · martes 24 de noviembre","bloques":[
            {"tipo":"inscrita","actividadId":"c1","nombre":"Competencia de robots","fecha":"2026-11-24","horaInicio":"10:00","horaFin":"12:00",
             "ubicacion":"Cancha 2","modalidad":"equipo",
             "equipo":{"nombre":"Robots","rol":"capitan","integrantes":3,"confirmados":2,"confirmado":false}}]}]}"""

    @Test fun leeLaAgendaPorDiaConElEstadoDelEquipo() {
        val a = leerAgendaInteligente(JSONObject(agenda), ahora)
        assertEquals(2, a.dias.size)
        assertEquals("Hoy · lunes 23 de noviembre", a.dias[0].etiqueta)
        assertEquals("el bloque sin hora se descarta", listOf("t1", "s0", "s1"), a.dias[0].bloques.map { it.actividadId })
        assertEquals(2, a.inscritas)
        assertEquals(1, a.equiposPorConfirmar)
        assertEquals(1, a.avisos.size)
        val robots = a.dias[1].bloques[0]
        assertTrue(robots.inscrita)
        assertTrue(robots.porEquipos)
        val equipo = robots.equipo!!
        assertTrue(equipo.esCapitan)
        assertEquals(1, equipo.pendientes)
        assertEquals("Equipo Robots · falta 1 por confirmar (2/3)", textoEquipo(equipo))
        assertNull(a.dias[0].bloques[0].equipo)
        val drones = a.dias[0].bloques[2]
        assertTrue(drones.sugerida)
        assertTrue(drones.inscripcionDirecta)
        assertEquals("Te gustan los drones", drones.motivo)
    }

    @Test fun conElRelojDelCelularMarcaLoQueEstaEnCursoYQuitaLoQueYaPaso() {
        val a = leerAgendaInteligente(JSONObject(agenda), ahora)
        val taller = a.dias[0].bloques[0]
        assertTrue("10:30 está entre 10:00 y 11:00", enCursoAhora(taller, ahora))
        assertFalse(enCursoAhora(taller, ms("2026-11-23T16:00:00Z")))
        val alas10y30 = vigentes(a, ahora)
        assertEquals("la sugerida que ya empezó no se ofrece", listOf("t1", "s1"), alas10y30.dias[0].bloques.map { it.actividadId })
        val alas11y30 = vigentes(a, ms("2026-11-23T16:30:00Z"))
        assertEquals("el taller ya terminó", listOf("s1"), alas11y30.dias[0].bloques.map { it.actividadId })
        val pasadoManana = vigentes(a, ms("2026-11-25T15:00:00Z"))
        assertTrue(pasadoManana.vacia)
    }

    @Test fun alInscribirseLaSugeridaSaleDeInmediato() {
        val a = leerAgendaInteligente(JSONObject(agenda), ahora)
        val sin = quitarSugerida(a, "s1")
        assertEquals(listOf("t1", "s0"), sin.dias[0].bloques.map { it.actividadId })
        assertEquals(1, sin.sugeridas)
        val inscritaNoSeQuita = quitarSugerida(a, "t1")
        assertEquals(a.dias, inscritaNoSeQuita.dias)
    }

    @Test fun textoDelEquipoSegunSuEstado() {
        assertEquals("Equipo A · confirmado (3/3)", textoEquipo(EquipoAgenda("A", "integrante", 3, 3, true)))
        assertEquals("Equipo A · necesita otro integrante", textoEquipo(EquipoAgenda("A", "capitan", 1, 1, false)))
        assertEquals("Equipo A · faltan 2 por confirmar (2/4)", textoEquipo(EquipoAgenda("A", "capitan", 4, 2, false)))
    }

    @Test fun lasClasesSeMuestranPeroNoCuentanComoActividades() {
        val json = """{"conHorario":true,"dias":[{"fecha":"2026-11-24","etiqueta":"Mañana","bloques":[
            {"tipo":"clase","actividadId":"","nombre":"Clases","fecha":"2026-11-24","horaInicio":"08:00","horaFin":"13:00"},
            {"tipo":"sugerida","actividadId":"s1","nombre":"Feria","fecha":"2026-11-24","horaInicio":"13:30","horaFin":"14:30"}]}]}"""
        val a = leerAgendaInteligente(JSONObject(json), ahora)
        assertTrue(a.conHorario)
        assertTrue(a.dias[0].bloques[0].clase)
        assertFalse(a.vacia)
        val sinLaFeria = quitarSugerida(a, "s1")
        assertTrue("un día con solo clases ya no se muestra", sinLaFeria.vacia)
        assertTrue(sinLaFeria.dias.isEmpty())
    }
}
