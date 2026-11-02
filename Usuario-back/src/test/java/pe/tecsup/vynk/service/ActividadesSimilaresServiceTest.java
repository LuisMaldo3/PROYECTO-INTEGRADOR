package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

class ActividadesSimilaresServiceTest {
    @Test void priorizaAreaYCategoriaYTema() {
        Document origen = new Document("_id", "a")
            .append("nombre", "Campeonato de fútbol")
            .append("areaTecsup", "Tecnología Digital").append("categoriaId", "deportes")
            .append("tipoActividad", "competencia");
        Document relacionada = new Document("_id", "b")
            .append("nombre", "Torneo de fútbol universitario")
            .append("areaTecsup", "Tecnología Digital").append("categoriaId", "deportes")
            .append("tipoActividad", "competencia");
        Document otra = new Document("_id", "c").append("nombre", "Charla sobre emprendimiento");
        assertTrue(ActividadesSimilaresService.puntuacion(origen, relacionada) >
                   ActividadesSimilaresService.puntuacion(origen, otra));
    }
    @Test void evitaLaMismaActividadYLimitaLaRespuesta() {
        Document origen = new Document("_id", "a").append("nombre", "Robótica")
                .append("areaTecsup", "Mecánica");
        Document otra = new Document("_id", "b").append("nombre", "Torneo de robótica")
                .append("areaTecsup", "Mecánica");
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.find(any(Query.class), eq(Document.class), eq("actividades")))
                .thenReturn(List.of(origen, otra));
        var similares = new ActividadesSimilaresService(mongo).buscar(origen, 4);
        assertEquals(1, similares.size());
        assertEquals("b", similares.get(0).getString("_id"));
    }
}
