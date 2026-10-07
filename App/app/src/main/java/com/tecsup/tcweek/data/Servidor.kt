package com.tecsup.tcweek.data

import com.tecsup.tcweek.BuildConfig

object Servidor {
    // Se define en app/build.gradle.kts (propiedad backend.url de local.properties).
    // Por defecto: http://10.0.2.2:8081, la PC vista desde el emulador.
    val DIRECCION: String = BuildConfig.BACKEND_URL
}