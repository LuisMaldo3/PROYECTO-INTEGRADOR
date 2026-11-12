package pe.tecsup.vynk.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bson.Document;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.service.ContextoSugerenciasActividadService;
import pe.tecsup.vynk.service.GeminiResumenInscripcionesService;
import pe.tecsup.vynk.service.GeminiSugerenciasActividadService;
import pe.tecsup.vynk.service.OrganizadorAccessService;
import pe.tecsup.vynk.service.OrganizadorParticipacionService;

/** US-60 y US-61: solo el organizador titular puede consultar datos y generar la explicación. */
@RestController
@RequestMapping("/api/organizador/actividades/{actividadId}")
public class OrganizadorAnaliticaController {
    private final OrganizadorAccessService acceso;
    private final OrganizadorParticipacionService participacion;
    private final GeminiResumenInscripcionesService gemini;
    private final ContextoSugerenciasActividadService contextoSugerencias;
    private final GeminiSugerenciasActividadService geminiSugerencias;

    public OrganizadorAnaliticaController(OrganizadorAccessService acceso,
            OrganizadorParticipacionService participacion, GeminiResumenInscripcionesService gemini,
            ContextoSugerenciasActividadService contextoSugerencias, GeminiSugerenciasActividadService geminiSugerencias) {
        this.acceso = acceso;
        this.participacion = participacion;
        this.gemini = gemini;
        this.contextoSugerencias = contextoSugerencias;
        this.geminiSugerencias = geminiSugerencias;
    }

    @GetMapping("/participacion")
    public Map<String, Object> obtenerParticipacion(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String actividadId) {
        Usuario organizador = acceso.exigirOrganizador(jwt);
        Document actividad = acceso.exigirActividadPropia(organizador, actividadId);
        return participacion.calcular(actividad);
    }

    @PostMapping("/resumen-inscripciones-ia")
    public Map<String, Object> generarResumen(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String actividadId) {
        Usuario organizador = acceso.exigirOrganizador(jwt);
        Document actividad = acceso.exigirActividadPropia(organizador, actividadId);
        Map<String, Object> cifras = participacion.calcular(actividad);
        GeminiResumenInscripcionesService.Respuesta resumen = gemini.resumir(cifras);
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("datos", cifras);
        respuesta.put("resumen", resumen.texto());
        respuesta.put("origen", resumen.origen());
        respuesta.put("aviso", resumen.aviso());
        return respuesta;
    }

    // --- Tareas 1 y 2 de la US «Como organizador, quiero recibir sugerencias basadas en los datos
    // recopilados para futuras actividades» ------------------------------------------------------

    /** Tarea 1 (Luis): los datos reunidos, de solo lectura (para mostrar qué se le pasará a la IA). */
    @GetMapping("/datos-sugerencias")
    public Map<String, Object> obtenerDatosSugerencias(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String actividadId) {
        Usuario organizador = acceso.exigirOrganizador(jwt);
        Document actividad = acceso.exigirActividadAsignada(organizador, actividadId);
        return contextoSugerencias.reunir(actividad);
    }

    /** Tarea 2 (Mishel): junta los datos (tarea 1) y le pide a Gemini sugerencias de mejora. */
    @PostMapping("/sugerencias-futuras-ia")
    public Map<String, Object> generarSugerenciasFuturas(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String actividadId) {
        Usuario organizador = acceso.exigirOrganizador(jwt);
        Document actividad = acceso.exigirActividadAsignada(organizador, actividadId);
        Map<String, Object> datos = contextoSugerencias.reunir(actividad);
        GeminiSugerenciasActividadService.Respuesta ia = geminiSugerencias.generar(datos);
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("datos", datos);
        respuesta.put("sugerencias", ia.sugerencias());
        respuesta.put("resumen", ia.resumen());
        respuesta.put("origen", ia.origen());
        respuesta.put("aviso", ia.aviso());
        return respuesta;
    }
}
