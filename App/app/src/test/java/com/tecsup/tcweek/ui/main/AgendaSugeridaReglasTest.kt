package com.tecsup.tcweek.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

class AgendaSugeridaReglasTest {
    @Test fun identificaAgendaDeGemini() = assertEquals("✦ Priorizada con Gemini", etiquetaOrigenAgenda("ia"))
    @Test fun noConfundeRespaldoConIa() = assertEquals("Propuesta sin IA, por reglas", etiquetaOrigenAgenda("reglas"))
    @Test fun sinActividadesNoAfirmaQueUsoIa() = assertEquals("No hay sugerencias disponibles", etiquetaOrigenAgenda("sin_actividades"))
}
