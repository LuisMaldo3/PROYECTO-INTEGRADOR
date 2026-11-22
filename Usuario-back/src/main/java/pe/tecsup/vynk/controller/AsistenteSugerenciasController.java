package pe.tecsup.vynk.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.service.SugerenciasAsistenteService;

/**
 * US-75 T01: preguntas sugeridas para el chat del celular.
 *
 *  GET /api/asistente/sugerencias  ->  { sugerencias: ["¿Dónde se realiza Charla IA?", …] }  (máximo 5)
 */
@RestController
@RequestMapping("/api/asistente/sugerencias")
public class AsistenteSugerenciasController {
    private final SugerenciasAsistenteService sugerencias;

    public AsistenteSugerenciasController(SugerenciasAsistenteService sugerencias) {
        this.sugerencias = sugerencias;
    }

    @GetMapping
    public Map<String, Object> listar(@AuthenticationPrincipal Jwt jwt) {
        return sugerencias.sugerir(jwt == null ? null : jwt.getSubject(), Instant.now());
    }
}
