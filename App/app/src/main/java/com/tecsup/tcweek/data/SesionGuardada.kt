package com.tecsup.tcweek.data

import android.content.Context

data class DatosDeEntrada(
    val llave: String,
    val nombre: String,
    val correo: String,
    val rol: String
)

class SesionGuardada(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(NOMBRE, Context.MODE_PRIVATE)

    fun guardar(datos: DatosDeEntrada) {
        prefs.edit()
            .putString(LLAVE, datos.llave)
            .putString(NOMBRE_PERSONA, datos.nombre)
            .putString(CORREO, datos.correo)
            .putString(ROL, datos.rol)
            .apply()
    }

    fun leer(): DatosDeEntrada? {
        val llave = prefs.getString(LLAVE, null) ?: return null
        if (llave.isBlank()) return null
        return DatosDeEntrada(
            llave = llave,
            nombre = prefs.getString(NOMBRE_PERSONA, "").orEmpty(),
            correo = prefs.getString(CORREO, "").orEmpty(),
            rol = prefs.getString(ROL, "usuario").orEmpty()
        )
    }

    fun borrar() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val NOMBRE = "vynk_sesion"
        const val LLAVE = "llave"
        const val NOMBRE_PERSONA = "nombre"
        const val CORREO = "correo"
        const val ROL = "rol"
    }
}