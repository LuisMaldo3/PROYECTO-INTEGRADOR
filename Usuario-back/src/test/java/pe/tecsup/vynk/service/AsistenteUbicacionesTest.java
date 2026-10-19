package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import pe.tecsup.vynk.controller.CatalogoController;

class AsistenteUbicacionesTest {
    private static Map<String, Object> actividad(String nombre, String ubicacionId) {
        return Map.of("id", "act1", "nombre", nombre, "fecha", "2026-11-10",
                "horaInicio", "14:00", "horaFin", "15:00", "ubicacionId", ubicacionId);
    }

    @Test void detectaPreguntasDeLugarConTildesYNombresDeAulas() {
        assertTrue(AsistenteHorariosService.preguntaUbicacion("¿Dónde será el taller de robótica?"));
        assertTrue(AsistenteHorariosService.preguntaUbicacion("¿En qué aula se realizará la charla?"));
        assertTrue(AsistenteHorariosService.preguntaUbicacion("¿Cuál es la ubicación de la feria?"));
        assertFalse(AsistenteHorariosService.preguntaUbicacion("¿A qué hora comienza la feria?"));
    }

    @Test void resuelveNombreYDetallesGuardadosPorAdministracion() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.findById("u1", Document.class, "ubicaciones"))
                .thenReturn(new Document("nombre", "Auditorio central").append("pabellon", "B").append("aula", "201"));
        var encontrados = new UbicacionesActividadesService(mongo).resolver(List.of(actividad("Robótica", "u1")));
        assertEquals("Auditorio central", encontrados.get(0).get("lugar"));
        assertEquals("201", encontrados.get(0).get("aula"));
        assertEquals(true, encontrados.get(0).get("confirmada"));
    }

    @Test void noConvierteElIdDeMongoEnUnaUbicacionInventada() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        String id = "507f1f77bcf86cd799439011";
        var lugar = new UbicacionesActividadesService(mongo).resolver(List.of(actividad("Charla", id))).get(0);
        assertEquals("Ubicación por confirmar", lugar.get("lugar"));
        assertEquals(false, lugar.get("confirmada"));
    }

    @Test void respondeConUbicacionRegistradaSinNecesitarGemini() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.findById("u1", Document.class, "ubicaciones"))
                .thenReturn(new Document("nombre", "Auditorio central").append("sede", "Campus principal"));
        CatalogoController catalogo = mock(CatalogoController.class);
        when(catalogo.listar()).thenReturn(Map.of("actividades", List.of(actividad("Robótica", "u1"))));
        AsistenteHorariosService servicio = new AsistenteHorariosService(catalogo, new ObjectMapper(), new UbicacionesActividadesService(mongo));
        var respuesta = servicio.responder("¿Dónde es la actividad de robótica?", List.of());
        assertTrue(respuesta.respuesta().contains("Auditorio central"));
        assertEquals("Auditorio central", respuesta.ubicaciones().get(0).get("lugar"));
    }

    @Test void respondePorConfirmarSinEnviarIdsComoAulas() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        CatalogoController catalogo = mock(CatalogoController.class);
        when(catalogo.listar()).thenReturn(Map.of("actividades", List.of(actividad("Robótica", "507f1f77bcf86cd799439011"))));
        AsistenteHorariosService servicio = new AsistenteHorariosService(catalogo, new ObjectMapper(), new UbicacionesActividadesService(mongo));
        var respuesta = servicio.responder("¿Dónde será la actividad de robótica?", List.of());
        assertTrue(respuesta.respuesta().contains("Ubicación por confirmar"));
        assertFalse(respuesta.respuesta().contains("507f1f77"));
    }
}
