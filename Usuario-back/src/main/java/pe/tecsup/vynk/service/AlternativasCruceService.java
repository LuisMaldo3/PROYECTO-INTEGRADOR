package pe.tecsup.vynk.service;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.controller.CatalogoController;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;

/** Expone únicamente alternativas para cruces detectados del estudiante autenticado. */
@Service
public class AlternativasCruceService {
    private final UsuarioRepository usuarios;
    private final InscripcionRepository inscripciones;
    private final CruceHorarioService cruces;
    private final CatalogoController catalogo;
    private final BuscadorAlternativasCruce buscador;

    public AlternativasCruceService(UsuarioRepository usuarios, InscripcionRepository inscripciones,
            CruceHorarioService cruces, CatalogoController catalogo, BuscadorAlternativasCruce buscador) {
        this.usuarios = usuarios;
        this.inscripciones = inscripciones;
        this.cruces = cruces;
        this.catalogo = catalogo;
        this.buscador = buscador;
    }

    public Map<String, Object> obtener(String sujeto, String cruceId) {
        if (sujeto == null || sujeto.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var usuario = usuarios.findByCorreo(sujeto).or(() -> usuarios.findById(sujeto))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        if (!usuario.isActivo()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        var agenda = inscripciones.findByEstudianteId(usuario.getId());
        var cruce = cruces.detectar(agenda).stream().filter(c -> c.id().equals(cruceId))
                .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cruce no encontrado"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actividades = (List<Map<String, Object>>) catalogo.listar().get("actividades");
        var alternativas = buscador.buscar(cruce, agenda, actividades);
        return Map.of("cruceId", cruce.id(), "total", alternativas.size(), "alternativas", alternativas,
                "aviso", alternativas.isEmpty()
                        ? "No hay alternativas compatibles disponibles por el momento."
                        : "Estas actividades tienen cupos y no coinciden con tus inscripciones vigentes.");
    }
}
