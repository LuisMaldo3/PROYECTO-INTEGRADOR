package com.tecsup.tcweek.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InteresesTest {

    @Test
    fun alternar_marcaYDesmarcaSinModificarLaOriginal() {
        val inicial = listOf("teatro")
        val marcado = alternar(inicial, "musica")
        assertEquals(listOf("teatro", "musica"), marcado)
        assertEquals(listOf("teatro"), inicial)
        assertEquals(listOf("musica"), alternar(marcado, "teatro"))
        assertEquals(listOf("deportes"), alternar(emptyList(), "deportes"))
    }

    @Test
    fun alternar_dosVecesDejaTodoComoEstaba() {
        assertEquals(listOf("teatro"), alternar(alternar(listOf("teatro"), "musica"), "musica"))
    }

    @Test
    fun mismosIntereses_noImportaElOrden() {
        assertTrue(mismosIntereses(listOf("a", "b"), listOf("b", "a")))
        assertFalse(mismosIntereses(listOf("a"), listOf("a", "b")))
        assertFalse(mismosIntereses(listOf("a", "b"), listOf("a", "c")))
        assertTrue(mismosIntereses(emptyList(), emptyList()))
    }

    @Test
    fun puedeGuardar_soloConAlMenosUnoCambiosYSinGuardarYa() {
        assertTrue(puedeGuardar(listOf("teatro"), emptyList()))
        assertFalse(puedeGuardar(emptyList(), listOf("teatro")))
        assertFalse(puedeGuardar(listOf("teatro"), listOf("teatro")))
        assertFalse(puedeGuardar(listOf("musica", "teatro"), listOf("teatro", "musica")))
        assertFalse(puedeGuardar(listOf("teatro"), emptyList(), guardando = true))
    }

    @Test
    fun ordenarComoCatalogo_ordenaYDescartaLosDesconocidos() {
        val catalogo = listOf("teatro", "musica", "tecnologia", "deportes")
        assertEquals(listOf("teatro", "deportes"), ordenarComoCatalogo(listOf("deportes", "teatro", "astrologia"), catalogo))
        assertEquals(emptyList<String>(), ordenarComoCatalogo(emptyList(), catalogo))
    }

    @Test
    fun textos_contadorYAyuda() {
        assertEquals("Elige al menos uno", textoContador(0))
        assertEquals("1 interés elegido", textoContador(1))
        assertEquals("4 intereses elegidos", textoContador(4))
        assertTrue(textoAyuda(0).startsWith("Marca los temas"))
        assertTrue(textoAyuda(MINIMO_SUGERIDO - 1).contains("salen mejor"))
        assertTrue(textoAyuda(MINIMO_SUGERIDO).startsWith("Listo"))
    }

    @Test
    fun iconos_cadaInteresDelBackendTieneElSuyo() {
        val ids = listOf("teatro", "musica", "danza", "libros", "tecnologia", "ia", "emprendimiento", "videojuegos", "deportes", "gastronomia", "concursos")
        val iconos = ids.map { iconoDeInteres(it) }
        assertEquals(ids.size, iconos.toSet().size)
        assertFalse(iconos.contains(IconoInteres.ESTRELLA))
        assertEquals(IconoInteres.ESTRELLA, iconoDeInteres("nuevo"))
        assertNotEquals(IconoInteres.ESTRELLA, iconoDeInteres("ia"))
    }
    @Test
    fun onboarding_abreSoloSiNoEligioNiOmitio() {
        assertTrue(debeSolicitarIntereses(configurados = false, omitido = false))
        assertFalse(debeSolicitarIntereses(configurados = true, omitido = false))
        assertFalse(debeSolicitarIntereses(configurados = false, omitido = true))
        assertFalse(debeSolicitarIntereses(configurados = true, omitido = true))
    }

    @Test
    fun onboarding_luegoDeElegirPuedeGuardarPeroSinCambiosNo() {
        val seleccion = alternar(emptyList(), "tecnologia")
        assertTrue(puedeGuardar(seleccion, emptyList()))
        assertFalse(puedeGuardar(seleccion, seleccion))
        assertFalse(puedeGuardar(emptyList(), emptyList()))
        assertFalse(puedeGuardar(seleccion, emptyList(), guardando = true))
    }

    @Test
    fun onboarding_omitirNoCambiaUnaSeleccionExistente() {
        val misIntereses = listOf("teatro", "deportes")
        assertFalse(debeSolicitarIntereses(configurados = true, omitido = true))
        assertEquals(misIntereses, ordenarComoCatalogo(misIntereses, misIntereses))
    }
}
