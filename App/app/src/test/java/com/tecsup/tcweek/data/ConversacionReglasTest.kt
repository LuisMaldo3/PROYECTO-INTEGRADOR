package com.tecsup.tcweek.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversacionReglasTest {
    @Test fun permiteNuevaConversacionSiHayHistorialYNoHayTrabajoPendiente() {
        assertTrue(puedeIniciarNuevaConversacion(2, false, false, false))
    }

    @Test fun evitaReiniciarMientrasSeConsultaOSincroniza() {
        assertFalse(puedeIniciarNuevaConversacion(2, true, false, false))
        assertFalse(puedeIniciarNuevaConversacion(2, false, true, false))
        assertFalse(puedeIniciarNuevaConversacion(2, false, false, true))
        assertFalse(puedeIniciarNuevaConversacion(0, false, false, false))
    }
}
