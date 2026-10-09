package com.tecsup.tcweek.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DestinosTest {

    @Test
    fun elMenuTieneLasCincoPantallasDelEstudianteEnOrden() {
        assertEquals(
            listOf("Inicio", "Catálogo", "Agenda", "Asistente", "Perfil"),
            Destino.entries.map { it.titulo }
        )
    }

    @Test
    fun lasRutasNoSeRepiten() {
        val rutas = Destino.entries.map { it.ruta }
        assertEquals(rutas.size, rutas.toSet().size)
    }

    @Test
    fun perfilEstaEnElMenuParaPoderCerrarSesion() {
        assertTrue(Destino.PERFIL in Destino.entries)
    }

    @Test
    fun elRolSeNormaliza_mayusculasYEspacios() {
        assertEquals("organizador", normalizarRol("  Organizador "))
        assertEquals("administrador", normalizarRol("ADMINISTRADOR"))
        assertEquals("usuario", normalizarRol("otro"))
        assertEquals("usuario", normalizarRol(null))
        assertEquals("usuario", normalizarRol(""))
    }

    @Test
    fun etiquetasDeRol() {
        assertEquals("Usuario", etiquetaRol("usuario"))
        assertEquals("Organizador", etiquetaRol("ORGANIZADOR"))
        assertEquals("Administrador", etiquetaRol("administrador"))
        assertEquals("Usuario", etiquetaRol(null))
    }
}
