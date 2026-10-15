package pe.tecsup.vynk.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Diagnóstico autenticado; no publica secretos, URI de MongoDB ni claves Gemini. */
@RestController
@RequestMapping("/api/estado")
public class EstadoServicioController {
    private final MongoTemplate mongo;
    @Value("${GEMINI_API_KEY:}") private String clave;
    public EstadoServicioController(MongoTemplate mongo) { this.mongo = mongo; }

    @GetMapping
    public Map<String, Object> consultar(@AuthenticationPrincipal Jwt usuario) {
        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("backend", "conectado");
        estado.put("geminiConfigurado", clave != null && !clave.isBlank());
        try {
            mongo.executeCommand(new Document("ping", 1));
            estado.put("mongodb", "conectado");
            estado.put("actividadesPublicadas", mongo.getCollection("actividades").countDocuments(
                    new Document("estado", "programada")));
        } catch (Exception e) {
            estado.put("mongodb", "sin_conexion");
            estado.put("actividadesPublicadas", 0);
        }
        return estado;
    }
}
