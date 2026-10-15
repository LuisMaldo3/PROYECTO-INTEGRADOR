package com.tecsup.tcweek.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class Recomendacion(val actividad: ActividadCatalogo, val motivo: String)
data class RespuestaRecomendaciones(val origen: String, val aviso: String, val actividades: List<Recomendacion>)
object RecomendacionesApi {
    suspend fun cargar(token: String): RespuestaRecomendaciones = withContext(Dispatchers.IO) {
        val conn = URL(Servidor.DIRECCION.trimEnd('/') + "/api/recomendaciones").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"; conn.connectTimeout = 8000; conn.readTimeout = 24000
            conn.setRequestProperty("Authorization", "Bearer $token"); conn.setRequestProperty("Accept", "application/json")
            val status = conn.responseCode
            if(status !in 200..299) throw IllegalStateException(if(status == 401) "Sesión expirada. Inicia sesión." else "Error al cargar recomendaciones (HTTP $status)")
            val obj = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val arr = obj.optJSONArray("actividades")
            val lista = (0 until (arr?.length() ?: 0)).map { i ->
                val a = arr!!.getJSONObject(i)
                Recomendacion(ActividadCatalogo(
                    id=a.optString("id"), nombre=a.optString("nombre"), descripcion=a.optString("descripcion"),
                    fecha=a.optString("fecha"), horaInicio=a.optString("horaInicio"), horaFin=a.optString("horaFin"),
                    cupos=if(a.isNull("cuposDisponibles")) null else a.optInt("cuposDisponibles"),
                    maximo=if(a.isNull("maximoParticipantes")) null else a.optInt("maximoParticipantes"),
                    organizador=a.optString("organizador"), categoriaId=a.optString("categoriaId")
                ), a.optString("motivo"))
            }
            RespuestaRecomendaciones(obj.optString("origen"), obj.optString("aviso"), lista)
        } finally { conn.disconnect() }
    }
}
