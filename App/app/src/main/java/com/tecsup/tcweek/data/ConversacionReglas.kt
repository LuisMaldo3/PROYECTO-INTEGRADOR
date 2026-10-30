package com.tecsup.tcweek.data

/** El reinicio solo se habilita cuando el cliente está sincronizado y sin petición pendiente. */
fun puedeIniciarNuevaConversacion(cantidadMensajes: Int, cargando: Boolean,
                                 sincronizando: Boolean, reiniciando: Boolean): Boolean =
    cantidadMensajes > 0 && !cargando && !sincronizando && !reiniciando
