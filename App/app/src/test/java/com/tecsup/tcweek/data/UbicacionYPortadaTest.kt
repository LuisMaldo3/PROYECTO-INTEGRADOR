package com.tecsup.tcweek.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UbicacionYPortadaTest {
    // ---------- Ubicación (US-71) ----------

    @Test fun leeLaFichaCompletaDelLugar() {
        val u = UbicacionActividad.leer(JSONObject("""{"lugar":"Auditorio","sede":"Lima","pabellon":"Pabellón A","aula":"",
            "direccion":"","referencia":"Frente a la biblioteca","confirmada":true,"latitud":-12.0445,"longitud":-76.9529}"""))
        assertEquals("Auditorio", u.lugar)
        assertEquals(listOf("Sede" to "Lima", "Pabellón" to "Pabellón A", "Cómo llegar" to "Frente a la biblioteca"), u.filas())
        assertTrue(u.tieneMapa)
        assertTrue(u.enlaceMapa()!!.startsWith("geo:-12.0445,-76.9529?q=-12.0445,-76.9529(Auditorio)"))
    }

    @Test fun sinCoordenadasNoOfreceMapaNiLasInventa() {
        val u = UbicacionActividad.leer(JSONObject("""{"lugar":"Patio central","confirmada":true}"""))
        assertFalse(u.tieneMapa)
        assertNull(u.enlaceMapa())
        val media = UbicacionActividad.leer(JSONObject("""{"lugar":"Patio","latitud":-12.0}"""))
        assertFalse("una sola coordenada no sirve", media.tieneMapa)
        val mala = UbicacionActividad.leer(JSONObject("""{"lugar":"Patio","latitud":500,"longitud":10}"""))
        assertFalse(mala.tieneMapa)
    }

    @Test fun sinFichaUsaElNombreDelCatalogoOQuedaPorConfirmar() {
        assertEquals("Auditorio", UbicacionActividad.leer(null, "Auditorio").lugar)
        assertTrue(UbicacionActividad.leer(null, "Auditorio").confirmada)
        val nada = UbicacionActividad.leer(null, "")
        assertEquals(UbicacionActividad.POR_CONFIRMAR, nada.lugar)
        assertFalse(nada.confirmada)
        assertTrue(nada.filas().isEmpty())
    }

    @Test fun elCatalogoTraeLaReferenciaDelLugar() {
        val a = CatalogoApi.parsear(JSONObject("""{"id":"a1","nombre":"Charla","ubicacion":"Auditorio",
            "ubicacionReferencia":"2.º piso","ubicacionDetalle":{"lugar":"Auditorio","referencia":"2.º piso","confirmada":true}}"""))
        assertEquals("2.º piso", a.ubicacionReferencia)
        assertEquals("2.º piso", a.ubicacionDetalle.referencia)
        // Respuestas antiguas sin ficha siguen funcionando.
        val vieja = CatalogoApi.parsear(JSONObject("""{"id":"a1","nombre":"Charla","ubicacion":"Auditorio"}"""))
        assertEquals("", vieja.ubicacionReferencia)
        assertEquals("Auditorio", vieja.ubicacionDetalle.lugar)
    }

    // ---------- Respaldo por área (US-71 T05) ----------

    @Test fun elAreaDeTecsupMandaSobreElNombre() {
        assertEquals(RespaldoPortada.DIGITAL, respaldoPortada("Torneo de fútbol", "Tecnología Digital"))
        assertEquals(RespaldoPortada.ELECTRONICA, respaldoPortada("Charla", "Electricidad y Electrónica"))
        assertEquals(RespaldoPortada.MECANICA, respaldoPortada("Charla", "Mecánica"))
        assertEquals(RespaldoPortada.QUIMICA, respaldoPortada("Charla", "Minería y Procesos Químicos"))
        assertEquals(RespaldoPortada.GESTION, respaldoPortada("Charla", "Gestión y Producción"))
        assertEquals("sin tildes también", RespaldoPortada.DIGITAL, respaldoPortada("Charla", "tecnologia digital"))
    }

    @Test fun sinAreaSeDeduceDelNombreOLaCategoria() {
        assertEquals(RespaldoPortada.DEPORTES, respaldoPortada("Torneo de Fútbol 7", ""))
        assertEquals(RespaldoPortada.DEPORTES, respaldoPortada("Gran final", null, "deportes"))
        assertEquals(RespaldoPortada.DIGITAL, respaldoPortada("Charla de IA aplicada", "null"))
        assertEquals(RespaldoPortada.ELECTRONICA, respaldoPortada("Taller de robótica", ""))
        assertEquals(RespaldoPortada.QUIMICA, respaldoPortada("Visita al laboratorio", ""))
        assertEquals(RespaldoPortada.GENERAL, respaldoPortada("Recital de poesía", ""))
    }

    // ---------- Carga de portadas con alternativa local (US-71 T06) ----------

    @Test fun soloSeDescarganPortadasSeguras() {
        assertTrue(portadaRemotaSegura("https://upload.wikimedia.org/foto.jpg"))
        assertTrue(portadaRemotaSegura(Servidor.DIRECCION.trimEnd('/') + "/api/portadas/archivo/abc123"))
        assertFalse(portadaRemotaSegura("http://otro-sitio.com/foto.jpg"))
        assertFalse(portadaRemotaSegura("javascript:alert(1)"))
        assertFalse(portadaRemotaSegura(""))
        assertFalse(portadaRemotaSegura(null))
    }

    @Test fun elCatalogoSoloAceptaPortadasDeOrigenConfiable() {
        fun portada(json: String) = CatalogoApi.parsear(JSONObject(json)).portadaUrl
        assertEquals(Servidor.DIRECCION.trimEnd('/') + "/api/portadas/archivo/abc",
            portada("""{"id":"a","portadaUrl":"/api/portadas/archivo/abc","portada":{"tipo":"subida"}}"""))
        assertEquals("https://upload.wikimedia.org/x.jpg", portada("""{"id":"a","portadaUrl":"https://upload.wikimedia.org/x.jpg"}"""))
        assertEquals("https://cdn.tecsup.edu.pe/x.jpg",
            portada("""{"id":"a","portadaUrl":"https://cdn.tecsup.edu.pe/x.jpg","portada":{"tipo":"url"}}"""))
        // Sin portada, con http o con un enlace que no es del tipo «url»: se usa la ilustración local.
        assertEquals("", portada("""{"id":"a","portadaUrl":null}"""))
        assertEquals("", portada("""{"id":"a","portadaUrl":"http://x.com/a.jpg","portada":{"tipo":"url"}}"""))
        assertEquals("", portada("""{"id":"a","portadaUrl":"https://x.com/a.jpg","portada":{"tipo":"gemini"}}"""))
    }
}
