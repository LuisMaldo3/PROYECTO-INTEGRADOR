package com.tecsup.tcweek.data

import kotlinx.coroutines.CancellationException
import org.json.JSONObject

/*
 * US-75 T03: la conversación del chat, separada de la pantalla para poder probarla sin red.
 */

const val LARGO_MAXIMO_PREGUNTA = 600

/** Lo que necesita la conversación del servidor; en las pruebas se reemplaza por uno falso. */
interface ServicioChat {
    suspend fun preguntar(token: String, texto: String): RespuestaAsistente
    suspend fun historial(token: String): List<MensajeAsistente>
}

/** El servicio real: el backend de VYNK. */
object ServicioChatRemoto : ServicioChat {
    override suspend fun preguntar(token: String, texto: String) = AsistenteApi.preguntar(token, texto)
    override suspend fun historial(token: String) = AsistenteApi.consultarConversacion(token)
}

sealed interface ResultadoEnvio {
    /** [historial] viene solo si hubo que sincronizar: la pantalla lo usa en lugar de lo que tenía. */
    data class Respondida(val respuesta: RespuestaAsistente, val historial: List<MensajeAsistente>? = null) : ResultadoEnvio
    data class Fallida(val mensaje: String, val reintentable: Boolean) : ResultadoEnvio
}

/** null si la pregunta se puede enviar; si no, el motivo para mostrarlo. */
fun validarPregunta(texto: String): String? {
    val limpio = texto.trim()
    return when {
        limpio.isEmpty() -> "Escribe una pregunta."
        limpio.length > LARGO_MAXIMO_PREGUNTA -> "La pregunta puede tener hasta $LARGO_MAXIMO_PREGUNTA caracteres."
        else -> null
    }
}

class ConversacionChat(private val servicio: ServicioChat = ServicioChatRemoto) {
    /**
     * Envía la pregunta. Si otra sesión (la web u otro celular) cambió la conversación (HTTP 409), se trae el
     * historial actualizado y se reintenta una vez, sin molestar al estudiante. Los errores de red se pueden
     * reintentar con un toque; los de sesión o de una pregunta inválida no.
     */
    suspend fun enviar(token: String, texto: String): ResultadoEnvio {
        val pregunta = texto.trim().take(LARGO_MAXIMO_PREGUNTA)
        validarPregunta(pregunta)?.let { return ResultadoEnvio.Fallida(it, reintentable = false) }
        return try {
            ResultadoEnvio.Respondida(servicio.preguntar(token, pregunta))
        } catch (e: CancellationException) {
            throw e
        } catch (e: ErrorAsistente) {
            if (e.codigo == 409) sincronizarYReintentar(token, pregunta)
            else ResultadoEnvio.Fallida(e.message ?: "No se pudo consultar el asistente.", reintentable = e.codigo !in NO_REINTENTABLES)
        } catch (e: Exception) {
            ResultadoEnvio.Fallida(e.message ?: "No se pudo consultar el asistente.", reintentable = true)
        }
    }

    private suspend fun sincronizarYReintentar(token: String, pregunta: String): ResultadoEnvio = try {
        val historial = servicio.historial(token)
        ResultadoEnvio.Respondida(servicio.preguntar(token, pregunta), historial)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ResultadoEnvio.Fallida(e.message ?: "La conversación cambió en otro dispositivo. Intenta de nuevo.", reintentable = true)
    }

    private companion object {
        val NO_REINTENTABLES = setOf(400, 401, 403)
    }
}

/** Mensajes después de una respuesta: el historial sincronizado (si lo hubo) + la pregunta + la respuesta. */
fun mensajesTrasRespuesta(
    actuales: List<MensajeAsistente>, pregunta: String, resultado: ResultadoEnvio.Respondida
): List<MensajeAsistente> {
    val base = resultado.historial ?: actuales
    val r = resultado.respuesta
    return base + MensajeAsistente("usuario", pregunta.trim()) +
        MensajeAsistente("asistente", r.respuesta, r.origen, r.aviso, r.ubicaciones, r.tipoConsulta, r.actividades, r.requisitos)
}

/** Sugerencias del servidor, sin vacías ni repetidas y como máximo cinco; si no llega ninguna, las de ejemplo. */
fun leerSugerencias(json: JSONObject): List<String> {
    val arreglo = json.optJSONArray("sugerencias") ?: return EjemplosConsultasNaturales.preguntas
    val lista = (0 until arreglo.length()).mapNotNull { arreglo.optString(it).trim().takeIf { s -> s.isNotEmpty() && s.length <= 140 } }
        .distinct().take(5)
    return lista.ifEmpty { EjemplosConsultasNaturales.preguntas }
}

/**
 * Texto de la burbuja del asistente. Si la respuesta trae tarjetas (lugares, actividades o requisitos), la burbuja
 * muestra solo la introducción: el detalle ya está en las tarjetas y repetirlo alarga el chat sin aportar.
 */
fun textoVisible(m: MensajeAsistente): String {
    val conTarjetas = m.ubicaciones.isNotEmpty() ||
        (esConsultaDeActividades(m.tipoConsulta) && m.actividades.isNotEmpty()) ||
        (m.tipoConsulta == "requisitos" && m.requisitos.isNotEmpty())
    if (!conTarjetas) return m.contenido
    val corte = m.contenido.indexOf("\n\n•").takeIf { it > 0 } ?: m.contenido.indexOf("\n•").takeIf { it > 0 }
    return if (corte == null) m.contenido else m.contenido.substring(0, corte).trim()
}
