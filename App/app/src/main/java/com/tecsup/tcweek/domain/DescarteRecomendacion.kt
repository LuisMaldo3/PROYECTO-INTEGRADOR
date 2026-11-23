package com.tecsup.tcweek.domain


import com.tecsup.tcweek.data.remote.Recomendacion
/** Reducir el listado local tras el descarte; el servidor aplica el filtro en sesiones futuras. */
fun quitarRecomendacion(lista: List<Recomendacion>, actividadId: String): List<Recomendacion> =
    lista.filterNot { it.actividad.id == actividadId }
