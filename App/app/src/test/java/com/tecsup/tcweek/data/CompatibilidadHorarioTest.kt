package com.tecsup.tcweek.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompatibilidadHorarioTest {
    @Test fun soloMuestraEtiquetaConCertificacionYHorasValidas() {
        assertFalse(CompatibilidadHorario().mostrarEtiqueta())
        assertFalse(CompatibilidadHorario(true, "", "").mostrarEtiqueta())
        assertTrue(CompatibilidadHorario(true, "10:00", "12:00").mostrarEtiqueta())
        assertFalse(CompatibilidadHorario(false, "10:00", "12:00").mostrarEtiqueta())
    }
}
