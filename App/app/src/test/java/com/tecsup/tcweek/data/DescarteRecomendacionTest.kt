package com.tecsup.tcweek.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DescarteRecomendacionTest {
    private fun item(id: String) = Recomendacion(
        ActividadCatalogo(id = id, nombre = "Actividad $id", descripcion = "", fecha = "2026-11-10",
            horaInicio = "10:00", horaFin = "11:00", cupos = 10, maximo = 20,
            organizador = "Tecsup", categoriaId = "tecnologia"), motivo = "Afinidad")

    @Test fun eliminaSoloLaActividadSeleccionada() {
        val resultado = quitarRecomendacion(listOf(item("a"), item("b"), item("c")), "b")
        assertEquals(listOf("a", "c"), resultado.map { it.actividad.id })
    }

    @Test fun descartarDosVecesEsIdempotente() {
        val lista = listOf(item("a"), item("b"))
        assertEquals(quitarRecomendacion(lista, "a"), quitarRecomendacion(quitarRecomendacion(lista, "a"), "a"))
    }

    @Test fun noSeDescartanOtrasActividades() {
        assertEquals(2, quitarRecomendacion(listOf(item("a"), item("b")), "otro").size)
        assertTrue(quitarRecomendacion(listOf(item("a")), "a").isEmpty())
    }
}
