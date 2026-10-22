package com.tecsup.tcweek.data

import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class MensajeAsistente(
    val rol: String,
    val contenido: String,
    val origen: String = "",
    val aviso: String = "",
    val ubicaciones: List<UbicacionAsistente> = emptyList(),
    val tipoConsulta: String = "general",
    val actividades: List<ActividadMomentoAsistente> = emptyList(),
    val requisitos: List<RequisitosAsistente> = emptyList()
)

data class RespuestaAsistente(
    val respuesta: String, val origen: String, val aviso: String,
    val ubicaciones: List<UbicacionAsistente> = emptyList(),
    val tipoConsulta: String = "general", val actividades: List<ActividadMomentoAsistente> = emptyList(),
    val requisitos: List<RequisitosAsistente> = emptyList()
)

/** Jordi: consume /api/asistente/preguntar con la misma sesión JWT de VYNK. */
object AsistenteApi {
    /** Prueba de conectividad autenticada: estado real, no una suposición de la interfaz. */
    suspend fun verificar(token: String): String = withContext(Dispatchers.IO) {
        val conexion = URL(Servidor.DIRECCION.trimEnd('/') + "/api/estado").openConnection() as HttpURLConnection
        try {
            conexion.connectTimeout = 5000
            conexion.readTimeout = 8000
            conexion.setRequestProperty("Authorization", "Bearer $token")
            val status = conexion.responseCode
            if (status !in 200..299) return@withContext "No se pudo verificar el backend (HTTP $status)"
            val json = JSONObject(conexion.inputStream.bufferedReader().use { it.readText() })
            if (json.optString("mongodb") != "conectado") "⚠ MongoDB sin conexión"
            else "✓ MongoDB · ${json.optInt("actividadesPublicadas")} actividades publicadas · Gemini ${if (json.optBoolean("geminiConfigurado")) "configurado" else "no configurado"}"
        } catch (_: Exception) {
            "Sin conexión a Spring Boot: ${Servidor.DIRECCION}"
        } finally { conexion.disconnect() }
    }

    suspend fun preguntar(token: String, pregunta: String, historial: List<MensajeAsistente>): RespuestaAsistente = withContext(Dispatchers.IO) {
        val url = Servidor.DIRECCION.trimEnd('/') + "/api/asistente/preguntar"
        val conexion = URL(url).openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = "POST"
            conexion.connectTimeout = 8000
            conexion.readTimeout = 25000
            conexion.doOutput = true
            conexion.setRequestProperty("Authorization", "Bearer $token")
            conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conexion.setRequestProperty("Accept", "application/json")
            val contexto = JSONArray().apply {
                historial.takeLast(8).forEach { m ->
                    put(JSONObject().put("rol", m.rol).put("contenido", m.contenido.take(300)))
                }
            }
            val cuerpo = JSONObject().put("mensaje", pregunta.take(600)).put("historial", contexto).toString()
            conexion.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }
            val status = conexion.responseCode
            if (status !in 200..299) {
                val mensaje = when (status) {
                    400 -> "Revisa la pregunta. Escribe entre 1 y 600 caracteres."
                    401 -> "Tu sesión expiró. Vuelve a iniciar sesión."
                    403 -> "No tienes permisos para consultar el asistente."
                    404 -> "El servidor no tiene el nuevo servicio del asistente. Actualiza Spring Boot."
                    503 -> "MongoDB no está disponible. Comprueba la conexión del backend."
                    else -> "No se pudo consultar el asistente (HTTP $status)."
                }
                throw IllegalStateException(mensaje)
            }
            val respuesta = JSONObject(conexion.inputStream.bufferedReader().use { it.readText() })
            val texto = respuesta.optString("respuesta", "").trim()
            if (texto.isEmpty()) throw IllegalStateException("El backend envió una respuesta vacía.")
            RespuestaAsistente(texto, respuesta.optString("origen", "catalogo"), respuesta.optString("aviso", ""),
                leerUbicaciones(respuesta.optJSONArray("ubicaciones")),
                respuesta.optString("tipoConsulta", "general"), leerActividadesMomento(respuesta.optJSONArray("actividades")),
                leerRequisitosAsistente(respuesta.optJSONArray("requisitos")))
        } catch (e: SocketTimeoutException) {
            throw IllegalStateException("El servidor tardó demasiado. Comprueba Spring Boot y MongoDB.", e)
        } catch (e: java.net.ConnectException) {
            throw IllegalStateException("No se pudo conectar con $url. En el emulador usa 10.0.2.2:8081; en un celular, la IP local de tu PC.", e)
        } catch (e: CancellationException) {
            throw e
        } finally {
            conexion.disconnect()
        }
    }
}

/** Lee el contrato público del backend; las respuestas de versiones anteriores siguen funcionando. */
internal fun leerUbicaciones(array: JSONArray?): List<UbicacionAsistente> {
    if (array == null) return emptyList()
    return (0 until minOf(array.length(), 7)).mapNotNull { indice ->
        val dato = array.optJSONObject(indice) ?: return@mapNotNull null
        UbicacionAsistente(
            actividadId = dato.optString("actividadId"), nombre = dato.optString("nombre"),
            fecha = dato.optString("fecha"), horaInicio = dato.optString("horaInicio"),
            lugar = dato.optString("lugar").takeIf { it.isNotBlank() && it != "null" } ?: "Ubicación por confirmar",
            sede = dato.optString("sede"), pabellon = dato.optString("pabellon"),
            aula = dato.optString("aula"), direccion = dato.optString("direccion"),
            referencia = dato.optString("referencia"), confirmada = dato.optBoolean("confirmada", false)
        )
    }
}


/** Limita el número de resultados y valida datos del catálogo antes de mostrarlos. */
internal fun leerActividadesMomento(array: JSONArray?): List<ActividadMomentoAsistente> {
    if (array == null) return emptyList()
    return (0 until minOf(array.length(), 7)).mapNotNull { indice ->
        val dato = array.optJSONObject(indice) ?: return@mapNotNull null
        val cupos = if (dato.has("cuposDisponibles") && !dato.isNull("cuposDisponibles"))
            dato.optInt("cuposDisponibles", -1).takeIf { it >= 0 } else null
        ActividadMomentoAsistente(
            id = dato.optString("id").take(100), nombre = dato.optString("nombre").take(220),
            fecha = dato.optString("fecha").take(35), horaInicio = dato.optString("horaInicio").take(16),
            horaFin = dato.optString("horaFin").take(16), organizador = dato.optString("organizador").take(150),
            cuposDisponibles = cupos
        ).takeIf { it.esMostrable() }
    }
}


/** Solo lee elementos publicados y mantiene el contrato compatible con respuestas antiguas. */
internal fun leerRequisitosAsistente(array: JSONArray?): List<RequisitosAsistente> {
    if (array == null) return emptyList()
    return (0 until minOf(array.length(), 7)).mapNotNull { indice ->
        val dato = array.optJSONObject(indice) ?: return@mapNotNull null
        val nombre = dato.optString("nombre").trim().take(220)
        if (nombre.isBlank()) return@mapNotNull null
        val itemsArray = dato.optJSONArray("items")
        val items = if (itemsArray == null) emptyList() else (0 until minOf(itemsArray.length(), 12))
            .mapNotNull { i ->
                val texto = itemsArray.opt(i) as? String
                texto?.trim()?.takeIf { it.isNotEmpty() }?.take(300)
            }
        RequisitosAsistente(
            actividadId = dato.optString("actividadId").take(100),
            nombre = nombre, items = items,
            confirmados = dato.optBoolean("confirmados", false) && items.isNotEmpty()
        )
    }
}
