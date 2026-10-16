package com.tecsup.tcweek.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

class AlternativasCruceReglasTest {
    @Test fun informaCuandoNoHayOpciones() = assertEquals(
        "Por ahora no hay opciones compatibles con cupos.", resumenAlternativas(0))
    @Test fun cuentaOpcionesReales() = assertEquals("2 alternativas disponibles", resumenAlternativas(2))
    @Test fun noConfundeFallaDeRedConAusenciaDeOpciones() = assertEquals(
        "No se pudieron obtener alternativas.", resumenAlternativas(0, "Sin conexión"))
}
