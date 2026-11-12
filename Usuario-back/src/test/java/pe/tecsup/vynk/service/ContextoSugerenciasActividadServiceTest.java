package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import pe.tecsup.vynk.model.ComentarioActividad;

/** Tarea 1 (Luis): junta participación, comentarios y resultado oficial de UNA actividad. */
class ContextoSugerenciasActividadServiceTest {
    private final OrganizadorParticipacionService participacion = mock(OrganizadorParticipacionService.class);
    private final ComentariosActividadService comentarios = mock(ComentariosActividadService.class);
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final ContextoSugerenciasActividadService servicio =
            new ContextoSugerenciasActividadService(participacion, comentarios, mongo);

    private static Document actividad(String id, String tipo) {
        Document d = new Document("_id", id).append("nombre", "Taller de robótica")
                .append("tipoActividad", tipo).append("categoria", "Taller").append("fecha", "2026-11-17");
        return d;
    }

    private static ComentarioActividad comentario(String texto) {
        ComentarioActividad c = new ComentarioActividad();
        c.setTexto(texto);
        return c;
    }

    @Test void juntaNombreCategoriaYParticipacion() {
        Document act = actividad("a1", "actividad");
        when(participacion.calcular(act)).thenReturn(Map.of("estudiantesInscritos", 12L));
        when(comentarios.listar("a1")).thenReturn(List.of());

        Map<String, Object> datos = servicio.reunir(act);

        assertEquals("Taller de robótica", datos.get("nombreActividad"));
        assertEquals("actividad", datos.get("tipoActividad"));
        assertEquals("Taller", datos.get("categoria"));
        assertEquals(Map.of("estudiantesInscritos", 12L), datos.get("participacion"));
        assertEquals(0, datos.get("totalComentarios"));
        assertEquals(List.of(), datos.get("comentarios"));
        assertNull(datos.get("resultadoOficial"), "una actividad que no es competencia no lleva resultado");
    }

    @Test void losComentariosLleganAnonimizadosYLimitadosA40() {
        Document act = actividad("a2", "actividad");
        when(participacion.calcular(act)).thenReturn(Map.of());
        List<ComentarioActividad> cincuenta = new java.util.ArrayList<>();
        for (int i = 0; i < 50; i++) cincuenta.add(comentario("Comentario " + i + ", escríbeme a ana" + i + "@tecsup.edu.pe"));
        when(comentarios.listar("a2")).thenReturn(cincuenta);

        Map<String, Object> datos = servicio.reunir(act);

        assertEquals(50, datos.get("totalComentarios"));
        @SuppressWarnings("unchecked")
        List<String> textos = (List<String>) datos.get("comentarios");
        assertEquals(40, textos.size(), "no se mandan más de 40 comentarios a la IA");
        assertFalse(textos.get(0).contains("@"), "los correos se anonimizan antes de guardarse en el contexto");
    }

    @Test void losComentariosEnBlancoSeDescartan() {
        Document act = actividad("a3", "actividad");
        when(participacion.calcular(act)).thenReturn(Map.of());
        when(comentarios.listar("a3")).thenReturn(List.of(comentario(""), comentario("   "), comentario("Bien organizado")));

        Map<String, Object> datos = servicio.reunir(act);

        assertEquals(3, datos.get("totalComentarios"), "el total cuenta todos los registros");
        assertEquals(List.of("Bien organizado"), datos.get("comentarios"), "pero solo se envían los que tienen texto");
    }

    @Test void sinResultadoPublicadoNoSeIncluyeResultadoOficial() {
        Document act = actividad("a4", "competencia");
        when(participacion.calcular(act)).thenReturn(Map.of());
        when(comentarios.listar("a4")).thenReturn(List.of());
        when(mongo.findOne(org.mockito.ArgumentMatchers.any(Query.class), org.mockito.ArgumentMatchers.eq(Document.class),
                org.mockito.ArgumentMatchers.eq("resultados_competencias"))).thenReturn(null);

        assertNull(servicio.reunir(act).get("resultadoOficial"));
    }

    @Test void unaPropuestaPendienteNoCuentaComoResultadoOficial() {
        Document act = actividad("a5", "competencia");
        when(participacion.calcular(act)).thenReturn(Map.of());
        when(comentarios.listar("a5")).thenReturn(List.of());
        Document pendiente = new Document("estado", "pendiente").append("posiciones",
                List.of(new Document("nombre", "Equipo A").append("puesto", 1)));
        when(mongo.findOne(org.mockito.ArgumentMatchers.any(Query.class), org.mockito.ArgumentMatchers.eq(Document.class),
                org.mockito.ArgumentMatchers.eq("resultados_competencias"))).thenReturn(pendiente);

        assertNull(servicio.reunir(act).get("resultadoOficial"), "solo se usa un resultado ya publicado");
    }

    @Test void conResultadoPublicadoSeIncluyeElGanadorYElTotalDePuestos() {
        Document act = actividad("a6", "competencia");
        when(participacion.calcular(act)).thenReturn(Map.of());
        when(comentarios.listar("a6")).thenReturn(List.of());
        Document publicado = new Document("estado", "publicado").append("posiciones", List.of(
                new Document("nombre", "Equipo Digital").append("puesto", 1),
                new Document("nombre", "Equipo Mecánico").append("puesto", 2)));
        when(mongo.findOne(org.mockito.ArgumentMatchers.any(Query.class), org.mockito.ArgumentMatchers.eq(Document.class),
                org.mockito.ArgumentMatchers.eq("resultados_competencias"))).thenReturn(publicado);

        Map<String, Object> resultado = (Map<String, Object>) servicio.reunir(act).get("resultadoOficial");

        assertEquals("Equipo Digital", resultado.get("ganador"));
        assertEquals(2, resultado.get("totalPuestos"));
    }
}
