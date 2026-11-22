package com.tecsup.tcweek.data

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * US-70 T08: contrato de inscripción entre la app y el backend. Un servidor local responde con la misma forma que
 * InscripcionesCompetenciaController y se comprueba qué envía la app (método, ruta, token y cuerpo) y cómo
 * interpreta cada respuesta, incluidos los errores.
 */
class InscripcionContratoTest {
    data class Peticion(val metodo: String, val ruta: String, val autorizacion: String, val cuerpo: String)

    private lateinit var servidor: ServerSocket
    private val recibidas = CopyOnWriteArrayList<Peticion>()
    @Volatile private var estado = 200
    @Volatile private var respuesta = "{}"
    private val direccionOriginal = InscripcionApi.direccion

    @Before fun iniciar() {
        servidor = ServerSocket(0)
        thread(isDaemon = true) {
            while (!servidor.isClosed) {
                val socket = try { servidor.accept() } catch (e: Exception) { break }
                socket.use { s ->
                    val lector = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
                    val linea = lector.readLine() ?: return@use
                    val (metodo, ruta) = linea.split(" ").let { it[0] to it[1] }
                    var largo = 0
                    var autorizacion = ""
                    while (true) {
                        val h = lector.readLine() ?: break
                        if (h.isEmpty()) break
                        val (k, v) = h.split(":", limit = 2).let { it[0].trim().lowercase() to it.getOrElse(1) { "" }.trim() }
                        if (k == "content-length") largo = v.toInt()
                        if (k == "authorization") autorizacion = v
                    }
                    val cuerpo = CharArray(largo).also { var leidos = 0; while (leidos < largo) { val n = lector.read(it, leidos, largo - leidos); if (n < 0) break; leidos += n } }
                    recibidas.add(Peticion(metodo, ruta, autorizacion, String(cuerpo)))
                    val bytes = respuesta.toByteArray(Charsets.UTF_8)
                    val salida = s.getOutputStream()
                    salida.write(("HTTP/1.1 $estado X\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n").toByteArray())
                    salida.write(bytes)
                    salida.flush()
                }
            }
        }
        InscripcionApi.direccion = "http://127.0.0.1:${servidor.localPort}"
    }

    @After fun detener() {
        InscripcionApi.direccion = direccionOriginal
        servidor.close()
    }

    @Test fun consultaElEstadoConElTokenYLaRutaCorrecta() = runBlocking {
        respuesta = """{"inscrito":false}"""
        val r = InscripcionApi.estado("llave-123", "6730f1c2a1b2c3d4e5f60718")
        assertFalse(r.optBoolean("inscrito", true))
        val p = recibidas.single()
        assertEquals("GET", p.metodo)
        assertEquals("/api/actividades/6730f1c2a1b2c3d4e5f60718/mi-inscripcion", p.ruta)
        assertEquals("Bearer llave-123", p.autorizacion)
    }

    @Test fun laInscripcionIndividualEnviaElCuerpoEsperadoYLeeLaRespuestaDelBackend() = runBlocking {
        respuesta = """{"inscripcionId":"i1","actividadId":"a1","estado":"inscrita","modalidad":"individual",
            "nombreEquipo":null,"integrantes":0,"areaTecsup":null,"areaVerificada":false,"equipoId":null,"rolEquipo":null}"""
        val r = InscripcionApi.inscribir("llave-123", "a1")
        val p = recibidas.single()
        assertEquals("POST", p.metodo)
        assertEquals("/api/actividades/a1/inscripcion", p.ruta)
        assertEquals("", JSONObject(p.cuerpo).getString("nombreEquipo"))
        assertEquals("inscrita", r.getString("estado"))
        assertEquals("individual", r.getString("modalidad"))
        assertEquals("una inscripción individual no trae código de equipo", "", r.optString("codigoEquipo"))
    }

    @Test fun cancelarUsaDeleteYDevuelveNoInscrito() = runBlocking {
        respuesta = """{"inscrito":false}"""
        val r = InscripcionApi.cancelar("llave-123", "a1")
        assertEquals("DELETE", recibidas.single().metodo)
        assertEquals("/api/actividades/a1/inscripcion", recibidas.single().ruta)
        assertFalse(r.getBoolean("inscrito"))
    }

    @Test fun unirseAUnEquipoEnviaElCodigo() = runBlocking {
        respuesta = """{"inscripcionId":"i2","estado":"inscrita","modalidad":"equipos"}"""
        InscripcionApi.unirse("llave-123", "a1", "Xy7-codigo")
        assertEquals("/api/actividades/a1/unirse-equipo", recibidas.single().ruta)
        assertEquals("Xy7-codigo", JSONObject(recibidas.single().cuerpo).getString("codigoEquipo"))
    }

    @Test fun sinCuposMuestraElMensajeDelBackend() = runBlocking {
        estado = 409
        respuesta = """{"status":409,"mensaje":"No quedan cupos disponibles"}"""
        try {
            InscripcionApi.inscribir("llave-123", "a1")
            fail("debía fallar")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!, e.message!!.contains("HTTP 409"))
            assertTrue(e.message!!, e.message!!.contains("No quedan cupos disponibles"))
        }
    }

    @Test fun unaSesionVencidaPideVolverAEntrar() = runBlocking {
        estado = 401
        respuesta = ""
        try {
            InscripcionApi.estado("llave-vieja", "a1")
            fail("debía fallar")
        } catch (e: IllegalStateException) {
            assertEquals("Tu sesión expiró. Vuelve a ingresar.", e.message)
        }
    }

    @Test fun unIdentificadorInvalidoNiSiquieraLlegaAlServidor() = runBlocking {
        for (malo in listOf("../admin", "a b", "", "x".repeat(101))) {
            try {
                InscripcionApi.inscribir("llave-123", malo)
                fail("debía rechazar $malo")
            } catch (e: IllegalArgumentException) { /* esperado */ }
        }
        assertTrue(recibidas.isEmpty())
    }
}
