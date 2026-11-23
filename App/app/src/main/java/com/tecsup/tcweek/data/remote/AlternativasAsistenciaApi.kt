package com.tecsup.tcweek.data.remote

import com.tecsup.tcweek.data.repository.ErrorCarga
import com.tecsup.tcweek.data.repository.pedirJson
import org.json.JSONObject

/** US-79: una alternativa propuesta para la actividad a la que no puedo asistir. */
data class AlternativaAsistencia(
    val actividad: ActividadCatalogo,
    val motivo: String,
    val etiquetas: List<String> = emptyList(),
    val inscripcionDirecta: Boolean = true
) {
    val porEquipos get() = actividad.tipoActividad == "competencia" && actividad.modalidad == "equipos"
}

data class RespuestaAlternativas(
    val actividadId: String,
    val nombre: String,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val motivo: String,
    val origen: String,
    /** El capitán de un equipo no se cambia en un toque: cancelar retiraría a su equipo. */
    val puedeCambiar: Boolean,
    val aviso: String,
    val alternativas: List<AlternativaAsistencia>
) {
    val conIa get() = origen == "ia"
}

/** Por qué no puede ir: «horario» descarta lo que coincide con ese horario; «otro» no. */
enum class MotivoInasistencia(val clave: String, val texto: String) {
    HORARIO("horario", "No puedo a esa hora"),
    OTRO("otro", "Otro motivo")
}

/** US-79 T2: GET /api/app/alternativas/{actividadId}?motivo=horario|otro. */
object AlternativasAsistenciaApi {
    suspend fun cargar(token: String, actividadId: String, motivo: MotivoInasistencia): RespuestaAlternativas {
        require(actividadId.matches(Regex("[A-Za-z0-9_-]{1,128}"))) { "Identificador de actividad inválido" }
        val cuerpo = try {
            pedirJson("/api/app/alternativas/$actividadId?motivo=${motivo.clave}", token, "las alternativas")
        } catch (e: ErrorCarga) {
            if (e.codigo == 404) throw ErrorCarga(404, "Ya no estás inscrito en esta actividad.") else throw e
        }
        return leerAlternativas(JSONObject(cuerpo))
    }

    /**
     * «Cambiarme a esta»: primero se inscribe en la nueva y solo si eso funcionó cancela la que deja. Así nunca
     * se queda sin ninguna de las dos por un error a medio camino.
     */
    suspend fun cambiar(token: String, nuevaId: String, dejadaId: String, servicio: ServicioInscripcion = ServicioInscripcionRemoto): ResultadoCambio {
        try {
            servicio.inscribir(token, nuevaId)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            return ResultadoCambio.NoInscrito(e.message ?: "No se pudo inscribir en la nueva actividad.")
        }
        return try {
            servicio.cancelar(token, dejadaId)
            ResultadoCambio.Cambiado
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            ResultadoCambio.SinCancelar(e.message ?: "No se pudo cancelar la inscripción anterior.")
        }
    }
}

/** Para probar el cambio sin red. */
interface ServicioInscripcion {
    suspend fun inscribir(token: String, id: String)
    suspend fun cancelar(token: String, id: String)
}

object ServicioInscripcionRemoto : ServicioInscripcion {
    override suspend fun inscribir(token: String, id: String) { InscripcionApi.inscribir(token, id) }
    override suspend fun cancelar(token: String, id: String) { InscripcionApi.cancelar(token, id) }
}

sealed class ResultadoCambio {
    object Cambiado : ResultadoCambio()
    /** No se inscribió en la nueva: la anterior sigue intacta. */
    data class NoInscrito(val mensaje: String) : ResultadoCambio()
    /** Se inscribió en la nueva pero la anterior sigue activa. */
    data class SinCancelar(val mensaje: String) : ResultadoCambio()
}

/** Lógica pura (probada). */
fun leerAlternativas(json: JSONObject): RespuestaAlternativas {
    val act = json.optJSONObject("actividad") ?: JSONObject()
    fun t(o: JSONObject, k: String) = o.optString(k).takeUnless { it.isBlank() || it == "null" }.orEmpty()
    val arr = json.optJSONArray("alternativas")
    val lista = (0 until (arr?.length() ?: 0)).mapNotNull { i ->
        val a = arr!!.optJSONObject(i) ?: return@mapNotNull null
        val actividad = CatalogoApi.parsear(a)
        if (actividad.id.isBlank() || actividad.id == "null") return@mapNotNull null
        val etiquetas = a.optJSONArray("etiquetas")
        AlternativaAsistencia(
            actividad = actividad,
            motivo = t(a, "motivo").ifBlank { "Encaja en tu agenda." },
            etiquetas = (0 until (etiquetas?.length() ?: 0)).map { etiquetas!!.optString(it) }.filter { it.isNotBlank() },
            inscripcionDirecta = a.optBoolean("inscripcionDirecta", false)
        )
    }.distinctBy { it.actividad.id }
    return RespuestaAlternativas(
        actividadId = t(act, "id"), nombre = t(act, "nombre").ifBlank { "Actividad" },
        fecha = t(act, "fecha"), horaInicio = t(act, "horaInicio"), horaFin = t(act, "horaFin"),
        motivo = t(json, "motivo").ifBlank { "horario" }, origen = t(json, "origen").ifBlank { "reglas" },
        puedeCambiar = json.optBoolean("puedeCambiar", false), aviso = t(json, "aviso"), alternativas = lista
    )
}

/** Origen honesto: solo se dice «IA» si la IA eligió. */
fun origenAlternativas(origen: String): String = when (origen) {
    "ia" -> "Propuestas por la IA (Gemini)"
    "reglas" -> "Por parecido · IA no disponible"
    else -> "Sin alternativas por ahora"
}
