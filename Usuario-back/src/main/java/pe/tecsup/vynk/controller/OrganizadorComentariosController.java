package pe.tecsup.vynk.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.service.ComentariosActividadService;
import pe.tecsup.vynk.service.GeminiComentariosService;
import pe.tecsup.vynk.service.OrganizadorAccessService;

/** US-62/63: solo el responsable de la actividad accede a su analítica de comentarios. */
@RestController
@RequestMapping("/api/organizador/actividades/{actividadId}/comentarios")
public class OrganizadorComentariosController {
    private final OrganizadorAccessService acceso;
    private final ComentariosActividadService comentarios;
    private final GeminiComentariosService ia;

    public OrganizadorComentariosController(OrganizadorAccessService acceso,
            ComentariosActividadService comentarios, GeminiComentariosService ia) {
        this.acceso = acceso;
        this.comentarios = comentarios;
        this.ia = ia;
    }

    @GetMapping("/estadisticas")
    public Map<String, Object> estadisticas(@AuthenticationPrincipal Jwt jwt, @PathVariable String actividadId) {
        Usuario organizador = acceso.exigirOrganizador(jwt);
        acceso.exigirActividadPropia(organizador, actividadId);
        return Map.of("totalComentarios", comentarios.total(actividadId));
    }

    @PostMapping("/analisis-ia")
    public Map<String, Object> analizar(@AuthenticationPrincipal Jwt jwt, @PathVariable String actividadId) {
        Usuario organizador = acceso.exigirOrganizador(jwt);
        acceso.exigirActividadPropia(organizador, actividadId);
        var todos = comentarios.listar(actividadId);
        GeminiComentariosService.Respuesta respuesta = ia.analizar(todos);
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("totalComentarios", todos.size());
        datos.put("comentariosAnalizados", respuesta.analizados());
        datos.put("resumen", respuesta.resumen());
        datos.put("temas", respuesta.temas());
        datos.put("origen", respuesta.origen());
        datos.put("aviso", respuesta.aviso());
        return datos;
    }
}
