package pe.tecsup.vynk.controller;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.service.AlternativasCruceService;

@RestController
@RequestMapping("/api/agenda/cruces")
public class AlternativasCruceController {
    private final AlternativasCruceService alternativas;

    public AlternativasCruceController(AlternativasCruceService alternativas) {
        this.alternativas = alternativas;
    }

    @GetMapping("/{id}/alternativas")
    public Map<String, Object> listar(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        if (jwt == null || jwt.getSubject() == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para consultar alternativas.");
        return alternativas.obtener(jwt.getSubject(), id);
    }
}
