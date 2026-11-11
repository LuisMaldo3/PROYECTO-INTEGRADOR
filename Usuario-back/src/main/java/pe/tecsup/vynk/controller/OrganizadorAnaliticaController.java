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
import pe.tecsup.vynk.service.GeminiResumenInscripcionesService;
import pe.tecsup.vynk.service.OrganizadorAccessService;
import pe.tecsup.vynk.service.OrganizadorParticipacionService;

/** US-60 y US-61: solo el organizador titular puede consultar datos y generar la explicación. */
@RestController
@RequestMapping("/api/organizador/actividades/{actividadId}")
public class OrganizadorAnaliticaController {
    private final OrganizadorAccessService acceso;
    private final OrganizadorParticipacionService participacion;
    private final GeminiResumenInscripcionesService gemini;

    public OrganizadorAnaliticaController(OrganizadorAccessService acceso,
            OrganizadorParticipacionService participacion, GeminiResumenInscripcionesService gemini) {
        this.acceso = acceso;
        this.participacion = participacion;
        this.gemini = gemini;
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
}
