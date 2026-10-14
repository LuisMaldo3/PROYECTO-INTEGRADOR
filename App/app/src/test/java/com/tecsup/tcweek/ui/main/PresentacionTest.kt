package com.tecsup.tcweek.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PresentacionTest {

    private data class Act(val id: String, val fecha: String?, val hora: String?)

    @Test
    fun nombreDia_conDiaDeLaSemana() {
        assertEquals("Lunes 19 de octubre", nombreDia("2026-10-19"))
        assertEquals("Viernes 23 de octubre", nombreDia("2026-10-23"))
        assertEquals("Viernes 1 de enero", nombreDia("2027-01-01"))
    }

    @Test
    fun nombreDia_sinFechaValida() {
        for (mala in listOf(null, "", "hoy", "2026-02-30", "19/10/2026", "2026-1-9")) {
            assertEquals("Fecha por confirmar", nombreDia(mala))
        }
        assertNull(fechaValida("2026-13-01"))
    }

    @Test
    fun rangoHoras_rangoSoloInicioYSinHoras() {
        assertEquals("11:00 – 12:30", rangoHoras("11:00", "12:30"))
        assertEquals("11:00", rangoHoras("11:00", null))
        assertEquals("11:00", rangoHoras("11:00", ""))
        assertEquals("Hora por confirmar", rangoHoras(null, null))
        assertEquals("Hora por confirmar", rangoHoras("", "null"))
    }

    @Test
    fun agruparPorDia_enOrdenYConLoSinFechaAlFinal() {
        val grupos = agruparPorDia(
            listOf(
                Act("a", "2026-10-20", "15:00"),
                Act("b", "2026-10-19", "15:30"),
                Act("c", null, "09:00"),
                Act("d", "2026-10-19", "09:00"),
                Act("e", "rara", null)
            ),
            fecha = { it.fecha },
            hora = { it.hora }
        )
        assertEquals(listOf<String?>("2026-10-19", "2026-10-20", null), grupos.map { it.fecha })
        assertEquals(listOf("d", "b"), grupos[0].items.map { it.id })
        assertEquals(listOf("c", "e"), grupos[2].items.map { it.id }.sorted())
        assertEquals(emptyList<GrupoDia<Act>>(), agruparPorDia(emptyList<Act>(), { it.fecha }, { it.hora }))
    }

    @Test
    fun estadoCupos_todosLosCasos() {
        assertEquals(EstadoCupos("50 cupos", TonoCupos.OK), estadoCupos(50))
        assertEquals(EstadoCupos("6 cupos", TonoCupos.OK), estadoCupos(6))
        assertEquals(EstadoCupos("Quedan 5 cupos", TonoCupos.POCO), estadoCupos(5))
        assertEquals(EstadoCupos("Queda 1 cupo", TonoCupos.POCO), estadoCupos(1))
        assertEquals(EstadoCupos("Sin cupos", TonoCupos.AGOTADO), estadoCupos(0))
        assertEquals(EstadoCupos("Cupos por confirmar", TonoCupos.NEUTRO), estadoCupos(null))
    }

    @Test
    fun estiloCategoria_porIdOPorNombre() {
        assertEquals(ClaseCategoria.CULTURAL, estiloCategoria(id = "cultural").clase)
        assertEquals(ClaseCategoria.CONCURSO, estiloCategoria(id = "concurso").clase)
        assertEquals(ClaseCategoria.CULTURAL, estiloCategoria(nombre = "Actividad cultural").clase)
        assertEquals(ClaseCategoria.CONCURSO, estiloCategoria(nombre = "Concurso T&C Week").clase)
        assertEquals(ClaseCategoria.CONCURSO, estiloCategoria(nombre = "CONCURSOS").clase)
    }

    @Test
    fun estiloCategoria_loDesconocidoSaleNeutro() {
        assertEquals(EstiloCategoria(ClaseCategoria.OTRA, "Taller"), estiloCategoria(id = "otra", nombre = "Taller"))
        assertEquals("Actividad", estiloCategoria().nombre)
        assertEquals(ClaseCategoria.OTRA, estiloCategoria(id = "constructor").clase)
    }

    @Test
    fun cantidad_singularYPlural() {
        assertEquals("1 actividad", cantidad(1, "actividad", "actividades"))
        assertEquals("0 actividades", cantidad(0, "actividad", "actividades"))
    }
}
