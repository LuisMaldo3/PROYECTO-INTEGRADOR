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

    @Test fun laActividadPendienteSoloAceptaIdentificadoresValidosYSeConsumeUnaVez() {
        AvisoPendiente.consumir()
        AvisoPendiente.abrir("../malo")
        assertNull(AvisoPendiente.actividadId.value)
        AvisoPendiente.abrir(null)
        assertNull(AvisoPendiente.actividadId.value)

        AvisoPendiente.abrir("act1")
        assertEquals("act1", AvisoPendiente.actividadId.value)
        AvisoPendiente.consumir()
        assertNull(AvisoPendiente.actividadId.value)
    }

    @Test fun unToqueNuevoReemplazaAlPendiente() {
        AvisoPendiente.abrir("act1")
        AvisoPendiente.abrir("act2")
        assertEquals("act2", AvisoPendiente.actividadId.value)
        AvisoPendiente.consumir()
        assertTrue(AvisoPendiente.actividadId.value == null)
    }
}
