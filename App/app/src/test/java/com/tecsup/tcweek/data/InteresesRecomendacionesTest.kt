package com.tecsup.tcweek.data

import com.tecsup.tcweek.data.remote.leerResultadoGuardado
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Al guardar intereses la app sabe si debe olvidar las recomendaciones que tenía guardadas. */
class InteresesRecomendacionesTest {

    @Test fun lee_los_intereses_y_que_las_recomendaciones_cambiaron() {
        val r = leerResultadoGuardado(JSONObject("""{"intereses":["musica","teatro"],"configurados":true,"recomendacionesActualizadas":true}"""))
        assertEquals(listOf("musica", "teatro"), r.intereses)
        assertTrue(r.recomendacionesActualizadas)
    }

    @Test fun sin_cambios_las_recomendaciones_se_conservan() {
        val r = leerResultadoGuardado(JSONObject("""{"intereses":["musica"],"recomendacionesActualizadas":false}"""))
        assertFalse(r.recomendacionesActualizadas)
    }

    @Test fun un_servidor_anterior_que_no_envia_el_dato_no_rompe_nada() {
        val r = leerResultadoGuardado(JSONObject("""{"intereses":["deportes"]}"""))
        assertEquals(listOf("deportes"), r.intereses)
        assertFalse(r.recomendacionesActualizadas)
    }

    @Test fun una_respuesta_vacia_no_deja_intereses() {
        assertEquals(emptyList<String>(), leerResultadoGuardado(JSONObject("{}")).intereses)
    }
}
