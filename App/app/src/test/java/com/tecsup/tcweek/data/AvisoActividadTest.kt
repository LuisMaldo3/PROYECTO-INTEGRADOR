package com.tecsup.tcweek.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AvisoActividadTest {
    private fun datos(vararg pares: Pair<String, String>) = mapOf(
        "tipo" to TIPO_AVISO_RECORDATORIO,
        "actividadId" to "6730f1c2a1b2c3d4e5f60718",
        "titulo" to "Tu actividad empieza pronto",
        "cuerpo" to "Torneo de fútbol empieza en 25 min (10:00) · Cancha 2"
    ) + pares

    @Test fun leeUnRecordatorioCompleto() {
        val aviso = leerAvisoActividad(datos())
        assertEquals(
            AvisoActividad(
                "6730f1c2a1b2c3d4e5f60718", "Tu actividad empieza pronto",
                "Torneo de fútbol empieza en 25 min (10:00) · Cancha 2"
            ), aviso
        )
    }

    @Test fun ignoraMensajesQueNoSonRecordatorios() {
        assertNull(leerAvisoActividad(datos("tipo" to "promocion")))
        assertNull(leerAvisoActividad(mapOf("actividadId" to "abc123")))
        assertNull(leerAvisoActividad(emptyMap()))
    }

    @Test fun ignoraRecordatoriosSinActividadValida() {
        assertNull(leerAvisoActividad(datos("actividadId" to "")))
        assertNull(leerAvisoActividad(datos("actividadId" to "   ")))
        assertNull(leerAvisoActividad(datos("actividadId" to "../admin")))
        assertNull(leerAvisoActividad(datos("actividadId" to "a b")))
        assertNull(leerAvisoActividad(datos("actividadId" to "x".repeat(101))))
        assertNull(leerAvisoActividad(mapOf("tipo" to TIPO_AVISO_RECORDATORIO)))
    }

    @Test fun usaTextosPorDefectoSiFaltanOEstanVacios() {
        val aviso = leerAvisoActividad(datos("titulo" to "  ", "cuerpo" to ""))!!
        assertEquals("Tu actividad empieza pronto", aviso.titulo)
        assertEquals("Toca para ver los detalles.", aviso.cuerpo)
    }

    @Test fun recortaTextosDemasiadoLargos() {
        val aviso = leerAvisoActividad(datos("titulo" to "T".repeat(500), "cuerpo" to "C".repeat(500)))!!
        assertEquals(80, aviso.titulo.length)
        assertEquals(200, aviso.cuerpo.length)
    }

    @Test fun limpiaEspaciosDelIdentificador() {
        assertEquals("abc-123_X", leerAvisoActividad(datos("actividadId" to "  abc-123_X "))!!.actividadId)
    }

    @Test fun unSegundoAvisoDeLaMismaActividadReemplazaAlAnterior() {
        assertEquals(idNotificacionAviso("act1"), idNotificacionAviso("act1"))
        assertNotEquals(idNotificacionAviso("act1"), idNotificacionAviso("act2"))
    }

    @Test fun unCambioDeHorarioNoBorraElRecordatorioDeLaMismaActividad() {
        assertNotEquals(idNotificacionAviso("act1"), idNotificacionAviso("act1", TIPO_AVISO_CAMBIO_HORARIO))
        assertEquals(idNotificacionAviso("act1", TIPO_AVISO_CAMBIO_HORARIO), idNotificacionAviso("act1", TIPO_AVISO_CAMBIO_HORARIO))
    }

    // ---------- Cambio de horario ----------

    private fun cambio(vararg pares: Pair<String, String>) = mapOf(
        "tipo" to TIPO_AVISO_CAMBIO_HORARIO,
        "actividadId" to "act1",
        "titulo" to "Cambió el horario de Torneo de fútbol",
        "horarioAnterior" to "dom 22/11 · 10:00–11:00",
        "horarioNuevo" to "lun 23/11 · 15:00–16:30"
    ) + pares

    @Test fun leeLaAlertaDeCambioDeHorarioConElHorarioAnteriorYElNuevo() {
        val aviso = leerAvisoActividad(cambio())!!
        assertTrue(aviso.esCambioHorario)
        assertEquals("act1", aviso.actividadId)
        assertEquals("Cambió el horario de Torneo de fútbol", aviso.titulo)
        assertEquals("dom 22/11 · 10:00–11:00", aviso.horarioAnterior)
        assertEquals("lun 23/11 · 15:00–16:30", aviso.horarioNuevo)
        assertEquals("Antes: dom 22/11 · 10:00–11:00\nAhora: lun 23/11 · 15:00–16:30", aviso.cuerpo)
        assertEquals("El horario cambió.\nAntes: dom 22/11 · 10:00–11:00\nAhora: lun 23/11 · 15:00–16:30", aviso.nota)
    }

    @Test fun unRecordatorioNoTraeNotaDeCambioDeHorario() {
        val aviso = leerAvisoActividad(datos())!!
        assertTrue(!aviso.esCambioHorario)
        assertEquals("", aviso.nota)
    }

    @Test fun sinHorarioNuevoLaAlertaSeIgnora() {
        assertNull(leerAvisoActividad(cambio("horarioNuevo" to "  ")))
        assertNull(leerAvisoActividad(cambio("actividadId" to "../x")))
    }

    @Test fun laAlertaUsaTextosPorDefectoYRecortaLoLargo() {
        val aviso = leerAvisoActividad(cambio("titulo" to "", "horarioAnterior" to "", "horarioNuevo" to "N".repeat(200)))!!
        assertEquals("Cambió el horario de tu actividad", aviso.titulo)
        assertEquals("sin horario", aviso.horarioAnterior)
        assertEquals(60, aviso.horarioNuevo.length)
    }

    // ---------- Apertura al tocar ----------

    @Test fun laActividadPendienteSoloAceptaIdentificadoresValidosYSeConsumeUnaVez() {
        AvisoPendiente.consumir()
        AvisoPendiente.abrir("../malo")
        assertNull(AvisoPendiente.apertura.value)
        AvisoPendiente.abrir(null)
        assertNull(AvisoPendiente.apertura.value)

        AvisoPendiente.abrir("act1")
        assertEquals(AperturaAviso("act1", ""), AvisoPendiente.apertura.value)
        AvisoPendiente.consumir()
        assertNull(AvisoPendiente.apertura.value)
    }

    @Test fun alTocarUnaAlertaDeHorarioSeAbreLaActividadConLaNota() {
        AvisoPendiente.abrir("act1", "El horario cambió.\nAntes: A\nAhora: B")
        assertEquals(AperturaAviso("act1", "El horario cambió.\nAntes: A\nAhora: B"), AvisoPendiente.apertura.value)
        AvisoPendiente.abrir("act2", "x".repeat(1000))
        assertEquals(300, AvisoPendiente.apertura.value!!.nota.length)
        AvisoPendiente.consumir()
    }

    @Test fun unToqueNuevoReemplazaAlPendiente() {
        AvisoPendiente.abrir("act1")
        AvisoPendiente.abrir("act2")
        assertEquals("act2", AvisoPendiente.apertura.value!!.actividadId)
        AvisoPendiente.consumir()
        assertNull(AvisoPendiente.apertura.value)
    }
}
