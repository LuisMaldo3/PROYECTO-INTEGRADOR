package com.tecsup.tcweek.data

import org.junit.Assert.*
import org.junit.Test

class ConsultasNaturalesTest {
    @Test fun reconoceRespuestasTematicasConTarjetas() {
        assertTrue(esConsultaDeActividades("tema_horario"))
        assertTrue(esConsultaDeActividades("momento"))
        assertFalse(esConsultaDeActividades("ubicacion"))
    }

    @Test fun muestraEjemplosDeTemaYHorario() {
        assertTrue(EjemplosConsultasNaturales.preguntas.any { it.contains("deportes") && it.contains("3") })
        assertTrue(EjemplosConsultasNaturales.preguntas.all { it.length <= 600 })
    }
}
