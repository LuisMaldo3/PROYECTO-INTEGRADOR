package com.tecsup.tcweek.ui.main

import androidx.compose.ui.graphics.Color
import com.tecsup.tcweek.ui.theme.Departamentos
import com.tecsup.tcweek.ui.theme.colorArea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DepartamentosTest {
    @Test fun losCincoDepartamentosTienenElColorDefinido() {
        assertEquals(Color(0xFF12A150), Departamentos.porId("quimica")?.color?.fuerte)   // verde
        assertEquals(Color(0xFFF5B700), Departamentos.porId("mecanica")?.color?.fuerte)  // amarillo
        assertEquals(Color(0xFFDC2626), Departamentos.porId("td")?.color?.fuerte)        // rojo
        assertEquals(Color(0xFF1D6FE0), Departamentos.porId("electricidad")?.color?.fuerte) // azul
        assertEquals(Color(0xFF7C3AED), Departamentos.porId("dpg")?.color?.fuerte)       // morado
        assertEquals(5, Departamentos.todos.size)
    }

    @Test fun cadaAreaDeLosEstudiantesPerteneceAUnDepartamentoSinRepetirse() {
        val areas = listOf("Gestión y Producción", "Tecnología Digital", "Minería y Procesos Químicos", "Electricidad y Electrónica", "Mecánica")
        val ids = areas.map { Departamentos.deArea(it)?.id }
        assertEquals(5, ids.toSet().size)
        assertEquals("td", Departamentos.deArea("Tecnología Digital")?.id)
        assertNull(Departamentos.deArea("Carrera inventada"))
    }

    @Test fun elColorPorAreaCoincideConElDelDepartamentoYElAmarilloLlevaTextoOscuro() {
        Departamentos.todos.forEach { assertEquals(it.id, it.color, colorArea(it.area)) }
        assertNotEquals(Color.White, colorArea("Mecánica").texto)
        assertEquals(Color.White, colorArea("Tecnología Digital").texto)
    }

    @Test fun cadaDepartamentoTieneSuPropioEscudo() {
        assertEquals(5, Departamentos.todos.map { it.escudo }.toSet().size)
        assertEquals("Por definir", Departamentos.nombre(null))
        assertEquals("Mecánica", Departamentos.nombre("mecanica"))
    }
}
