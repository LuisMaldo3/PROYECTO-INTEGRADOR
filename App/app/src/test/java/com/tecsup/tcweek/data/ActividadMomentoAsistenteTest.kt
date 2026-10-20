package com.tecsup.tcweek.data

import org.junit.Assert.*
import org.junit.Test

class ActividadMomentoAsistenteTest {
    @Test fun muestraActividadConHorarioYCupos() {
        val actividad = ActividadMomentoAsistente("a1", "Robótica", "2026-10-26", "14:00", "16:00", "Tecsup", 3)
        assertTrue(actividad.esMostrable())
        assertEquals("3 cupos disponibles", actividad.etiquetaCupos())
        assertTrue(actividad.horario().contains("14:00"))
    }
    @Test fun rechazaActividadIncompletaSinInventarCupos() {
        val actividad = ActividadMomentoAsistente("a2", "Taller", "", "14:00", "", "", null)
        assertFalse(actividad.esMostrable())
        assertEquals("Cupos por confirmar", actividad.etiquetaCupos())
    }
}
