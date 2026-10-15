package com.tecsup.tcweek.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Historial local por cuenta. Nunca persiste JWT ni claves de servicios. */
class HistorialAsistente(context: Context, correo: String) {
    private val prefs = context.applicationContext.getSharedPreferences("vynk_historial_chat", Context.MODE_PRIVATE)
    private val clave = "mensajes_" + correo.trim().lowercase()

    fun cargar(): List<MensajeAsistente> = try {
        val json = JSONArray(prefs.getString(clave, "[]"))
        (0 until json.length()).mapNotNull { n ->
            val v = json.optJSONObject(n) ?: return@mapNotNull null
            val rol = v.optString("rol")
            if (rol != "usuario" && rol != "asistente") return@mapNotNull null
            MensajeAsistente(rol, v.optString("contenido"), v.optString("origen"), v.optString("aviso"))
        }.takeLast(30)
    } catch (_: Exception) { emptyList() }

    fun guardar(mensajes: List<MensajeAsistente>) {
        val array = JSONArray()
        mensajes.takeLast(30).forEach { m ->
            array.put(JSONObject().put("rol", m.rol).put("contenido", m.contenido)
                .put("origen", m.origen).put("aviso", m.aviso))
        }
        prefs.edit().putString(clave, array.toString()).apply()
    }

    fun borrar() { prefs.edit().remove(clave).apply() }
}
