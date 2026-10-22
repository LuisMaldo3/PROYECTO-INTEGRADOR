package com.tecsup.tcweek.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ReglasHorarioTest {
    @Test fun aceptaBloquesCorrectos() {
        assertNull(ReglasHorario.validar(listOf(BloqueHorario(1, "08:00", "09:30", "Clases"))))
    }
    @Test fun rechazaDiaInvalido() {
        assertNotNull(ReglasHorario.validar(listOf(BloqueHorario(8, "08:00", "09:30", "Error"))))
    }
    @Test fun rechazaHorasInvertidasYFormatoIncorrecto() {
        assertNotNull(ReglasHorario.validar(listOf(BloqueHorario(1, "12:00", "11:00", "Error"))))
        assertNotNull(ReglasHorario.validar(listOf(BloqueHorario(1, "25:00", "26:00", "Error"))))
    }
}
