package com.tecsup.tcweek.data

import java.time.Instant
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** US-80 (catálogo por intereses) y US-81 (pantalla de inicio). */
class InteresesEInicioTest {
    private fun ms(iso: String) = Instant.parse(iso).toEpochMilli()
    private val ahora = ms("2026-11-23T15:30:00Z") // lunes 23/11, 10:30 en Lima

    // ---------- US-80 ----------

    @Test fun leeLaMarcaParaTiYSuMotivo() {
        val a = CatalogoApi.parsear(JSONObject("""{"id":"a1","nombre":"Fútbol","paraTi":true,"motivoParaTi":"Te interesa: deportes"}"""))
        assertTrue(a.paraTi)
        assertEquals("Te interesa: deportes", a.motivoParaTi)
        val b = CatalogoApi.parsear(JSONObject("""{"id":"a2","nombre":"Charla","paraTi":false,"motivoParaTi":null}"""))
        assertFalse(b.paraTi)
        assertEquals("motivo null no se muestra como «null»", "", b.motivoParaTi)
    }

    @Test fun elOrdenElegidoViajaAlServidorSoloSiEsPorFecha() {
        assertEquals("", FiltrosCatalogo().consulta())
        assertEquals("?orden=fecha", FiltrosCatalogo(porFecha = true).consulta())
        assertEquals("?franja=tarde&orden=fecha", FiltrosCatalogo(Franja.TARDE, porFecha = true).consulta())
        assertEquals("el orden no cuenta como filtro activo", 0, FiltrosCatalogo(porFecha = true).activos)
    }

    @Test fun conOrdenPorInteresesLasParaTiVanEnSuPropiaSeccion() {
        val lista = listOf(
            ActividadCatalogo("a2", "Fútbol", "", "2026-11-24", "16:00", "17:00", 10, 30, "", paraTi = true),
            ActividadCatalogo("a1", "Cocina", "", "2026-11-23", "09:00", "10:00", 10, 30, ""),
            ActividadCatalogo("a3", "Concierto", "", "2026-11-24", "11:00", "12:00", 10, 30, ""))
        val porIntereses = seccionesCatalogo(ResultadoCatalogo(lista, OpcionesFiltro(), orden = "intereses"))
        assertEquals(listOf("a2"), porIntereses.paraTi.map { it.id })
        assertEquals(listOf("a1", "a3"), porIntereses.resto.map { it.id })
        val porFecha = seccionesCatalogo(ResultadoCatalogo(lista, OpcionesFiltro(), orden = "fecha"))
        assertTrue(porFecha.paraTi.isEmpty())
        assertEquals(3, porFecha.resto.size)
    }

    // ---------- US-81 ----------

    private val resumen = """{"generadoEn":"2026-11-23T15:30:00Z","vigenteHasta":"2026-11-23T15:32:00Z",
        "saludo":{"nombre":"Ana","fecha":"2026-11-23","mensaje":"Hoy tienes 2 actividades."},
        "resumen":{"inscritas":3,"hoy":2,"semana":3,"equiposPorConfirmar":1},
        "proximas":[
          {"id":"p3","nombre":"Torneo de robots","fecha":"2026-11-24","horaInicio":"10:00","horaFin":"12:00",
           "empiezaEn":"2026-11-24T15:00:00Z","terminaEn":"2026-11-24T17:00:00Z","cuando":"mañana · 10:00",
           "equipo":{"nombre":"Robots","confirmado":false}},
          {"id":"p1","nombre":"Taller de IA","fecha":"2026-11-23","horaInicio":"10:00","horaFin":"11:00","ubicacion":"Auditorio",
           "empiezaEn":"2026-11-23T15:00:00Z","terminaEn":"2026-11-23T16:00:00Z","cuando":"hoy · 10:00","equipo":null},
          {"id":"sin-hora","nombre":"Mal"}],
        "destacadas":[
          {"id":"d1","nombre":"Hackathon","tipoActividad":"competencia","portadaUrl":null,"tienePortada":false,
           "destacadaPor":"Competencia","cuando":"el miércoles 25 · 09:00"},
          {"id":"d2","nombre":"Feria","portadaUrl":"/api/portadas/archivo/abc","portada":{"tipo":"subida"},"tienePortada":true,
           "paraTi":true,"destacadaPor":"Para ti","cuando":"mañana · 12:00"}],
        "avisos":[
          {"id":"v1","titulo":"Feria de proyectos","mensaje":"El viernes","publicadoEn":"2026-11-22T10:00:00Z",
           "imagenUrl":"/api/portadas/archivo/bbbbbbbbbbbbbbbbbbbbbbbb","nuevo":true},
          {"id":"v2","titulo":"Antiguo","mensaje":"","publicadoEn":"2026-11-01T10:00:00Z","imagenUrl":"http://inseguro/x.jpg","nuevo":false},
          {"id":"v3","titulo":"","mensaje":"Sin título","imagenUrl":null}]}"""

    @Test fun leeElResumenDeInicio() {
        val r = leerResumenInicio(JSONObject(resumen), ahora)
        assertEquals("Ana", r.nombre)
        assertEquals("Hoy tienes 2 actividades.", r.mensaje)
        assertEquals(ms("2026-11-23T15:32:00Z"), r.vigenteHasta)
        assertEquals(1, r.equiposPorConfirmar)
        assertEquals("ordenadas por inicio, sin las que no tienen hora", listOf("p1", "p3"), r.proximas.map { it.actividad.id })
        assertTrue(r.proximas[0].enCurso(ahora))
        assertEquals("Robots", r.proximas[1].equipo)
        assertFalse(r.proximas[1].equipoConfirmado)
        assertEquals("Auditorio", r.proximas[0].actividad.ubicacion)
        assertFalse("sin portada usa la ilustración", r.destacadas[0].tienePortada)
        assertTrue(r.destacadas[1].tienePortada)
        assertTrue(r.destacadas[1].paraTi)
        assertTrue(r.avisos[0].imagenUrl.endsWith("/api/portadas/archivo/bbbbbbbbbbbbbbbbbbbbbbbb"))
        assertTrue(r.avisos[0].imagenUrl.startsWith("http"))
        assertTrue(r.avisos[0].nuevo)
        assertEquals("solo https o el propio servidor", "", r.avisos[1].imagenUrl)
        assertEquals("Comunicado", r.avisos[2].titulo)
    }

    @Test fun lasProximasSeFiltranConElRelojYSinRepetirMiProxima() {
        val r = leerResumenInicio(JSONObject(resumen), ahora)
        assertEquals(listOf("p3"), proximasVigentes(r, ahora, omitir = "p1").map { it.actividad.id })
        assertEquals("el taller ya terminó", listOf("p3"), proximasVigentes(r, ms("2026-11-23T16:30:00Z")).map { it.actividad.id })
    }

    @Test fun sinFechasUsaLaVigenciaPorDefecto() {
        val r = leerResumenInicio(JSONObject("{}"), ahora)
        assertEquals(ahora + InicioApi.VIGENCIA_MS, r.vigenteHasta)
        assertTrue(r.proximas.isEmpty() && r.destacadas.isEmpty() && r.avisos.isEmpty())
    }

    @Test fun imagenesSeguras() {
        assertTrue(imagenSegura("/api/portadas/archivo/x").endsWith("/api/portadas/archivo/x"))
        assertEquals("https://upload.wikimedia.org/a.jpg", imagenSegura("https://upload.wikimedia.org/a.jpg"))
        assertEquals("", imagenSegura("javascript:alert(1)"))
    }
}
