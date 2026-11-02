package pe.tecsup.vynk.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

class ResultadosPublicosDetalleTest {
    @Test void noPublicaBorradoresNiParticipantesAntesDeAprobar() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.find(any(Query.class), eq(Document.class), eq("resultados_competencias")))
            .thenReturn(List.of());
        var controlador = new ResultadosPublicosController(mongo);
        var respuesta = controlador.resultadoPorActividad("actividad1");
        assertEquals("sin_resultados", respuesta.get("estado"));
    }
}
