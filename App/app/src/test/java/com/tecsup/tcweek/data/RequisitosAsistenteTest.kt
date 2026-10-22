package com.tecsup.tcweek.data

import org.junit.Assert.*
import org.junit.Test

class RequisitosAsistenteTest {
    @Test fun muestraSoloRequisitosPublicados() {
        val r = RequisitosAsistente("a1", "Taller de Robótica", listOf("Traer laptop"), true)
        assertTrue(r.tieneInformacion())
        assertEquals("Requisitos publicados por administración", r.estado())
        assertEquals("Traer laptop", r.items.first())
    }

    @Test fun requisitoFaltanteNoSignificaQueNoHayaCondiciones() {
        val r = RequisitosAsistente("a2", "Charla", emptyList(), false)
        assertFalse(r.tieneInformacion())
        assertTrue(r.estado().contains("por confirmar"))
        assertFalse(RequisitosAsistente("a3", "Feria", emptyList(), true).tieneInformacion())
    }
}
