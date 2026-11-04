package pe.tecsup.vynk.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

/** US-40-T09/T10: misma muestra de 25 puntos que el backend de administración. */
class ResultadosPublicosControllerTest {
    @Test
    void clasificacionNoDuplicaNiAceptaCompetenciasCanceladas() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        String tecnologia = "Tecnología Digital";
        String gestion = "Gestión y Producción";
        Document antigua = new Document("_id", "r1").append("actividadId", "a")
                .append("estado", "publicado").append("publicadoEn", "2026-11-09T10:00:00")
                .append("posiciones", List.of(new Document("inscripcionId", "x")
                        .append("puesto", 1).append("puntos", 10).append("areaTecsup", gestion)));
        Document vigente = new Document("_id", "r2").append("actividadId", "a")
                .append("estado", "publicado").append("publicadoEn", "2026-11-10T10:00:00")
                .append("posiciones", List.of(
                        new Document("inscripcionId", "x").append("puesto", 1).append("puntos", 20).append("areaTecsup", gestion),
                        new Document("inscripcionId", "y").append("puesto", 2).append("puntos", 5).append("areaTecsup", tecnologia),
                        new Document("inscripcionId", "x").append("puesto", 3).append("puntos", 900).append("areaTecsup", gestion)));
        Document cancelada = new Document("actividadId", "c").append("estado", "publicado")
                .append("posiciones", List.of(new Document("inscripcionId", "z").append("puesto", 1)
                        .append("puntos", 999).append("areaTecsup", gestion)));
        when(mongo.find(any(Query.class), eq(Document.class), eq("resultados_competencias")))
                .thenReturn(List.of(antigua, vigente, cancelada));
        when(mongo.findOne(any(Query.class), eq(Document.class), eq("actividades")))
                .thenAnswer(invocacion -> {
                    Query query = invocacion.getArgument(0);
                    Object id = query.getQueryObject().get("_id");
                    if ("a".equals(id)) return new Document("nombre", "Fútbol")
                            .append("tipoActividad", "competencia").append("estado", "programada");
                    if ("c".equals(id)) return new Document("nombre", "Cancelada")
                            .append("tipoActividad", "competencia").append("estado", "cancelada");
                    return null;
                });
        Map<String, Object> resultado = new ResultadosPublicosController(mongo).clasificacion();
        assertEquals(1, resultado.get("totalCompetencias"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> areas = (List<Map<String, Object>>) resultado.get("areas");
        assertEquals(5, areas.size());
        assertEquals(20L, areas.stream().filter(a -> gestion.equals(a.get("area"))).findFirst().orElseThrow().get("puntos"));
        assertEquals(5L, areas.stream().filter(a -> tecnologia.equals(a.get("area"))).findFirst().orElseThrow().get("puntos"));
        assertNotNull(areas.stream().filter(a -> gestion.equals(a.get("area"))).findFirst().orElseThrow().get("desglose"));
    }
}
