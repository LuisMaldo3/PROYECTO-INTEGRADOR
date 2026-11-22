package com.tecsup.tcweek.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CancelacionRecomendacionAvisoTest {
    private fun cancelacion(vararg pares: Pair<String, String>) = mapOf(
        "tipo" to TIPO_AVISO_CANCELACION,
        "actividadId" to "act1",
        "titulo" to "Se canceló Torneo de fútbol",
        "cuerpo" to "La actividad del lun 23/11 · 10:00–11:00 · Cancha 2 fue cancelada. Ya no está en tu agenda."
    ) + pares

    private fun recomendacion(vararg pares: Pair<String, String>) = mapOf(
        "tipo" to TIPO_AVISO_RECOMENDACION,
        "actividadId" to "act2",
        "titulo" to "Te puede interesar: Taller de IA",
        "cuerpo" to "mar 24/11 · 15:00–16:00 · Laboratorio 3\nPorque te interesa Inteligencia artificial\nHay cupos disponibles."
    ) + pares

    @Test fun leeLaNotificacionDeCancelacion() {
        val aviso = leerAvisoActividad(cancelacion())!!
        assertTrue(aviso.esCancelacion)
        assertEquals("Se canceló Torneo de fútbol", aviso.titulo)
        assertTrue(aviso.cuerpo.endsWith("Ya no está en tu agenda."))
    }

    @Test fun alTocarUnaCancelacionSeAbreLaAgendaYNoLaActividad() {
        val aviso = leerAvisoActividad(cancelacion())!!
        assertTrue(aviso.abreAgenda)
        assertTrue(aviso.cambiaAgenda)
        assertEquals("", aviso.nota)
    }

    @Test fun laCancelacionUsaTextosPorDefectoYSinActividadValidaSeIgnora() {
        val aviso = leerAvisoActividad(cancelacion("titulo" to "", "cuerpo" to " "))!!
        assertEquals("Se canceló una de tus actividades", aviso.titulo)
        assertEquals("La actividad fue cancelada. Ya no está en tu agenda.", aviso.cuerpo)
        assertNull(leerAvisoActividad(cancelacion("actividadId" to "../x")))
    }

    @Test fun leeLaRecomendacionYAlTocarlaAbreLaActividad() {
        val aviso = leerAvisoActividad(recomendacion())!!
        assertTrue(aviso.esRecomendacion)
        assertFalse(aviso.abreAgenda)
        assertFalse(aviso.cambiaAgenda)
        assertEquals("act2", aviso.actividadId)
        assertEquals("Te puede interesar: Taller de IA", aviso.titulo)
        assertTrue(aviso.cuerpo.contains("Porque te interesa Inteligencia artificial"))
    }

    @Test fun laRecomendacionUsaTextosPorDefectoYRecortaLoLargo() {
        val aviso = leerAvisoActividad(recomendacion("titulo" to "", "cuerpo" to "x".repeat(600)))!!
        assertEquals("Te puede interesar una actividad", aviso.titulo)
        assertEquals(250, aviso.cuerpo.length)
    }

    @Test fun losCambiosTambienRecarganLaAgendaPeroLosRecordatoriosNo() {
        val horario = leerAvisoActividad(mapOf("tipo" to TIPO_AVISO_CAMBIO_HORARIO, "actividadId" to "a",
            "horarioNuevo" to "lun 23/11 · 10:00"))!!
        val recordatorio = leerAvisoActividad(mapOf("tipo" to TIPO_AVISO_RECORDATORIO, "actividadId" to "a"))!!
        assertTrue(horario.cambiaAgenda)
        assertFalse(recordatorio.cambiaAgenda)
    }

    @Test fun cadaTipoTieneSuPropiaNotificacion() {
        val ids = listOf(TIPO_AVISO_RECORDATORIO, TIPO_AVISO_CAMBIO_HORARIO, TIPO_AVISO_CAMBIO_UBICACION,
            TIPO_AVISO_CANCELACION, TIPO_AVISO_RECOMENDACION).map { idNotificacionAviso("act1", it) }.toSet()
        assertEquals(5, ids.size)
    }

    @Test fun alTocarUnaCancelacionLaAperturaPideLaAgenda() {
        AvisoPendiente.abrir("act1", agenda = true)
        assertEquals(AperturaAviso("act1", "", agenda = true), AvisoPendiente.apertura.value)
        AvisoPendiente.consumir()
        assertNull(AvisoPendiente.apertura.value)
    }

    @Test fun recibirUnaCancelacionAvisaALaAgendaQueSeRecargue() {
        val antes = CambiosAgenda.version.value
        CambiosAgenda.avisar()
        assertEquals(antes + 1, CambiosAgenda.version.value)
    }

    @Test fun laAgendaSoloMuestraActividadesVigentes() {
        fun item(id: String, estado: String) = ActividadAgenda(id, "Actividad", "2026-11-23", "10:00", "11:00", "Cancha 2", "Deportes", estado)
        val vigentes = agendaVigente(listOf(item("a", "inscrita"), item("b", "cancelada"), item("c", "INSCRITA")))
        assertEquals(listOf("a", "c"), vigentes.map { it.id })
    }
}
