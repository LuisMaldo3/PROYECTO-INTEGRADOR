package com.tecsup.tcweek.data

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * US-76 T3 y US-77 T3: carga programada de recomendaciones y de la agenda inteligente.
 *
 * - Lo último que llegó se guarda (en memoria y en el teléfono), así la pantalla se ve al instante y sin conexión.
 * - Se vuelve a pedir cuando vence ([Marca.vigenteHasta]), cuando cambió la agenda (inscripción, cancelación o
 *   alerta de cambio: [CambiosAgenda]) o cuando el estudiante lo pide.
 */
data class Marca(val cargadoEn: Long, val vigenteHasta: Long, val versionAgenda: Int)

object CargaProgramada {
    const val VIGENCIA_RECOMENDACIONES_MS = 15 * 60_000L
    const val VIGENCIA_AGENDA_MS = 5 * 60_000L

    /** ¿Hay que volver a pedir los datos? */
    fun debeRecargar(marca: Marca?, ahora: Long, versionAgenda: Int): Boolean =
        marca == null || ahora >= marca.vigenteHasta || versionAgenda != marca.versionAgenda

    /** Cuánto esperar antes de la siguiente recarga automática (nunca menos de 30 s). */
    fun esperaHastaVencer(marca: Marca, ahora: Long): Long = maxOf(30_000L, marca.vigenteHasta - ahora)
}

/** «hace un momento», «hace 5 min», «hace 2 h»: para decir qué tan reciente es lo que se ve. */
fun haceCuanto(desde: Long, ahora: Long): String {
    val minutos = (ahora - desde).coerceAtLeast(0) / 60_000
    return when {
        minutos < 1 -> "hace un momento"
        minutos < 60 -> "hace $minutos min"
        minutos < 24 * 60 -> "hace ${minutos / 60} h"
        else -> "hace ${minutos / (24 * 60)} d"
    }
}

/**
 * Última respuesta guardada en el teléfono, por cuenta (otra persona que entre en el mismo celular no ve lo
 * de la anterior). Se borra al cerrar sesión junto con el resto (ver [borrarTodo]).
 */
class CacheRespuestas(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    fun guardar(clave: String, cuenta: String, json: String, ahora: Long) {
        prefs.edit().putString(llave(clave, cuenta), json).putLong(llave(clave, cuenta) + "_en", ahora).apply()
    }

    /** El JSON guardado y cuándo se guardó, o null. */
    fun leer(clave: String, cuenta: String): Pair<String, Long>? {
        val json = prefs.getString(llave(clave, cuenta), null) ?: return null
        return json to prefs.getLong(llave(clave, cuenta) + "_en", 0L)
    }

    fun borrarTodo() { prefs.edit().clear().apply() }

    private fun llave(clave: String, cuenta: String) = clave + "_" + cuenta.lowercase().hashCode()

    companion object { private const val ARCHIVO = "vynk_respuestas" }
}

/** Error al pedir datos: [codigo] HTTP (0 si no hubo conexión) para decidir si mostrar lo guardado. */
class ErrorCarga(val codigo: Int, mensaje: String) : IllegalStateException(mensaje) {
    val sinConexion get() = codigo == 0
}

/** GET autenticado que devuelve el cuerpo tal cual (para guardarlo) o lanza [ErrorCarga] con un mensaje claro. */
internal suspend fun pedirJson(ruta: String, token: String, que: String, lectura: Int = 28_000): String =
    withContext(Dispatchers.IO) {
        val conn = URL(Servidor.DIRECCION.trimEnd('/') + ruta).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = lectura
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/json")
            val codigo = conn.responseCode
            if (codigo !in 200..299) {
                val detalle = runCatching {
                    val cuerpo = JSONObject(conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty())
                    cuerpo.optString("mensaje").ifBlank { cuerpo.optString("message") }
                }.getOrDefault("")
                throw ErrorCarga(codigo, when (codigo) {
                    401 -> "Tu sesión expiró. Vuelve a iniciar sesión."
                    403 -> "Tu cuenta no tiene acceso a $que."
                    404 -> detalle.ifBlank { "El servidor todavía no tiene $que. Actualiza Spring Boot." }
                    503 -> "El servidor no pudo conectarse con la base de datos."
                    else -> detalle.ifBlank { "No se pudo cargar $que (HTTP $codigo)." }
                })
            }
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: java.net.ConnectException) {
            throw ErrorCarga(0, "Sin conexión con el servidor.")
        } catch (e: java.net.UnknownHostException) {
            throw ErrorCarga(0, "Sin conexión a internet.")
        } catch (e: java.net.SocketTimeoutException) {
            throw ErrorCarga(0, "El servidor tardó demasiado en responder.")
        } finally {
            conn.disconnect()
        }
    }

/** Lo último cargado mientras la app está abierta: volver a una pantalla no la deja en blanco. */
object MemoriaPantallas {
    @Volatile var recomendaciones: Pair<PaqueteRecomendaciones, Marca>? = null
    @Volatile var agenda: Pair<AgendaInteligente, Marca>? = null
    @Volatile var inicio: Pair<ResumenInicio, Marca>? = null

    fun olvidar() {
        recomendaciones = null
        agenda = null
        inicio = null
    }
}
