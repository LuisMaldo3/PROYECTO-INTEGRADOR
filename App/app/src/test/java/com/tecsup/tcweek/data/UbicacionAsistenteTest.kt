package com.tecsup.tcweek.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UbicacionAsistenteTest {
    @Test fun lugarConfirmadoMuestraDetallesPublicados() {
        val ubicacion = UbicacionAsistente("1", "Taller", "2026-11-10", "09:00", "Auditorio", "", "B", "201", "", "", true)
        assertEquals("Auditorio", ubicacion.lugar)
        assertEquals(listOf("B", "201"), ubicacion.detalles())
        assertTrue(ubicacion.confirmada)
    }
    @Test fun lugarPendienteNoInventaAula() {
        val ubicacion = UbicacionAsistente("2", "Charla", "", "", "Ubicación por confirmar", "", "", "", "", "", false)
        assertTrue(ubicacion.detalles().isEmpty())
    }
}
