package com.tecsup.tcweek.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ResultadoTorneo(val modo: String, val a: Int?, val b: Int?, val setsA: Int?, val setsB: Int?, val ganadorPenales: String?)

data class PartidoTorneo(
    val id: String, val ronda: Int, val posicion: Int, val nombreRonda: String,
    val a: String?, val b: String?, val estado: String, val descanso: Boolean,
    val fecha: String, val hora: String, val lugar: String, val ganador: String?,
    val provisional: Boolean, val resultado: ResultadoTorneo?
)

/** Un equipo (o alumno) del torneo, con su área y el departamento al que pertenece. */
data class EquipoTorneo(val id: String, val nombre: String, val area: String, val departamento: String?)

data class Torneo(
    val id: String, val tipo: String, val deporte: String, val modoResultado: String, val estado: String,
    val participantes: List<String>, val campeon: String?, val subcampeon: String?, val partidos: List<PartidoTorneo>,
    val equipos: Map<String, EquipoTorneo> = emptyMap()
)

/** T&C ARENA y Karadúo: consulta de solo lectura. Los torneos los administra el panel, nunca el teléfono. */
object TorneosApi {
    suspend fun consultar(token: String): List<Torneo> = withContext(Dispatchers.IO) {
        val conexion = URL(Servidor.DIRECCION.trimEnd('/') + "/api/competencias/torneos").openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = "GET"
            conexion.connectTimeout = 8000
            conexion.readTimeout = 12000
            conexion.setRequestProperty("Authorization", "Bearer $token")
            conexion.setRequestProperty("Accept", "application/json")
            if (conexion.responseCode !in 200..299) throw IllegalStateException("No se pudieron consultar los torneos (HTTP ${conexion.responseCode}).")
            leer(conexion.inputStream.bufferedReader().use { it.readText() })
        } finally { conexion.disconnect() }
    }

    private fun JSONObject.texto(campo: String): String? = if (isNull(campo)) null else optString(campo).ifBlank { null }
    private fun JSONObject.entero(campo: String): Int? = if (!has(campo) || isNull(campo)) null else optInt(campo)

    fun leer(texto: String): List<Torneo> {
        val lista = JSONObject(texto).optJSONArray("torneos") ?: return emptyList()
        return (0 until lista.length()).map { i ->
            val t = lista.getJSONObject(i)
            val ps = t.optJSONArray("partidos")
            val participantes = t.optJSONArray("participantes")
            // Con «equipos», los lados y el campeón se identifican por equipo («aId», «bId», «campeonId»…);
            // sin ellos (torneos antiguos) se usan los departamentos de siempre.
            val listaEquipos = t.optJSONArray("equipos")
            val equipos = (0 until (listaEquipos?.length() ?: 0)).map { listaEquipos!!.getJSONObject(it) }.associate { e ->
                e.optString("id") to EquipoTorneo(e.optString("id"), e.optString("nombre"), e.optString("area"), e.texto("departamento"))
            }
            val porEquipo = equipos.isNotEmpty()
            Torneo(
                equipos = equipos,
                id = t.optString("id"), tipo = t.optString("tipo"), deporte = t.optString("deporte"),
                modoResultado = t.optString("modoResultado"), estado = t.optString("estado"),
                participantes = if (porEquipo) equipos.keys.toList() else (0 until (participantes?.length() ?: 0)).map { participantes!!.getString(it) },
                campeon = t.texto(if (porEquipo) "campeonId" else "campeon"), subcampeon = t.texto(if (porEquipo) "subcampeonId" else "subcampeon"),
                partidos = (0 until (ps?.length() ?: 0)).map { j ->
                    val p = ps!!.getJSONObject(j)
                    val r = p.optJSONObject("resultado")
                    PartidoTorneo(
                        id = p.optString("id"), ronda = p.optInt("ronda", 1), posicion = p.optInt("posicion", j + 1),
                        nombreRonda = p.optString("nombreRonda"), a = p.texto(if (porEquipo) "aId" else "a"), b = p.texto(if (porEquipo) "bId" else "b"),
                        estado = p.optString("estado"), descanso = p.optBoolean("descanso"),
                        fecha = p.optString("fecha"), hora = p.optString("hora"), lugar = p.optString("lugar"),
                        ganador = p.texto(if (porEquipo) "ganadorId" else "ganador"), provisional = p.optBoolean("provisional"),
                        resultado = r?.let { ResultadoTorneo(it.optString("modo"), it.entero("a"), it.entero("b"), it.entero("setsA"), it.entero("setsB"), it.texto("ganadorPenales")) }
                    )
                }
            )
        }
    }
}
