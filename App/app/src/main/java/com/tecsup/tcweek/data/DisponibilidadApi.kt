package com.tecsup.tcweek.data

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class BloqueHorario(val dia: Int, val inicio: String, val fin: String, val nombre: String)
data class LecturaHorario(val bloques: List<BloqueHorario>, val avisos: List<String>)
data class HorarioConfirmado(val confirmado: Boolean, val bloques: List<BloqueHorario>)
data class RecomendacionLibre(val id: String, val nombre: String, val fecha: String, val inicio: String, val fin: String, val huecoInicio: String, val huecoFin: String, val motivo: String)
data class SugerenciasLibres(val origen: String, val aviso: String, val configurado: Boolean, val actividades: List<RecomendacionLibre>)

object DisponibilidadApi {
    private fun bloques(json: JSONArray?): List<BloqueHorario> = (0 until (json?.length() ?: 0)).map { i ->
        val j = json!!.getJSONObject(i)
        BloqueHorario(j.optInt("dia"), j.optString("inicio"), j.optString("fin"), j.optString("nombre"))
    }
    fun validar(items: List<BloqueHorario>): String? = ReglasHorario.validar(items)
    private suspend fun pedir(token: String, path: String, metodo: String = "GET", json: String? = null, imagen: Pair<String, ByteArray>? = null): JSONObject = withContext(Dispatchers.IO) {
        val conn = URL(Servidor.DIRECCION.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = metodo
            conn.connectTimeout = 8000
            conn.readTimeout = 42000
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/json")
            if (json != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            }
            if (imagen != null) {
                val boundary = "Vynk${UUID.randomUUID()}"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                conn.outputStream.use { out ->
                    out.write("--$boundary\r\nContent-Disposition: form-data; name=\"imagen\"; filename=\"horario\"\r\nContent-Type: ${imagen.first}\r\n\r\n".toByteArray())
                    out.write(imagen.second)
                    out.write("\r\n--$boundary--\r\n".toByteArray())
                }
            }
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException(when (code) {
                401 -> "Tu sesión expiró. Inicia sesión nuevamente."
                404 -> "Actualiza Spring Boot para gestionar tu disponibilidad."
                413 -> "La foto supera el máximo de 5 MB."
                503 -> "Gemini no está disponible. Revisa GEMINI_API_KEY."
                else -> "Error HTTP $code. Revisa tu horario o la conexión."
            })
            JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally { conn.disconnect() }
    }
    suspend fun obtener(token: String): HorarioConfirmado {
        val json = pedir(token, "/api/disponibilidad")
        return HorarioConfirmado(json.optBoolean("confirmado"), bloques(json.optJSONArray("bloques")))
    }
    suspend fun interpretar(context: Context, token: String, uri: Uri): LecturaHorario {
        val mime = context.contentResolver.getType(uri) ?: ""
        if (mime !in listOf("image/jpeg", "image/png", "image/webp")) throw IllegalArgumentException("Selecciona una foto JPG, PNG o WEBP.")
        val bytes = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val result = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val n = input.read(buffer)
                    if (n == -1) break
                    if (result.size() + n > 5 * 1024 * 1024) throw IllegalArgumentException("La imagen supera 5 MB.")
                    result.write(buffer, 0, n)
                }
                result.toByteArray()
            } ?: throw IllegalArgumentException("No se pudo abrir la imagen.")
        }
        if (bytes.isEmpty()) throw IllegalArgumentException("La imagen está vacía.")
        val j = pedir(token, "/api/disponibilidad/interpretar", "POST", imagen = mime to bytes)
        val warnings = j.optJSONArray("advertencias")
        return LecturaHorario(bloques(j.optJSONArray("bloques")), (0 until (warnings?.length() ?: 0)).map { warnings!!.optString(it) })
    }
    suspend fun guardar(token: String, items: List<BloqueHorario>): HorarioConfirmado {
        validar(items)?.let { throw IllegalArgumentException(it) }
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("dia", it.dia).put("inicio", it.inicio).put("fin", it.fin).put("nombre", it.nombre)) }
        val j = pedir(token, "/api/disponibilidad", "PUT", JSONObject().put("bloques", arr).toString())
        return HorarioConfirmado(j.optBoolean("confirmado"), bloques(j.optJSONArray("bloques")))
    }
    suspend fun sugerencias(token: String): SugerenciasLibres {
        val j = pedir(token, "/api/agenda/huecos")
        val arr = j.optJSONArray("actividades")
        val items = (0 until (arr?.length() ?: 0)).map { i ->
            val a = arr!!.getJSONObject(i)
            RecomendacionLibre(a.optString("id"), a.optString("nombre", "Actividad"), a.optString("fecha"),
                a.optString("horaInicio"), a.optString("horaFin"), a.optString("huecoInicio"), a.optString("huecoFin"), a.optString("motivo"))
        }
        return SugerenciasLibres(j.optString("origen"), j.optString("aviso"), j.optBoolean("configurado"), items)
    }
}
