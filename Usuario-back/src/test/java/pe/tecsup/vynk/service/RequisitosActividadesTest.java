package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import com.fasterxml.jackson.databind.ObjectMapper;
import pe.tecsup.vynk.controller.CatalogoController;

class RequisitosActividadesTest {
    private static Map<String, Object> actividad(String id, String nombre, Object requisitos) {
        return Map.of("id", id, "nombre", nombre, "requisitos", requisitos);
    }

    @Test void reconoceConsultasConYSinTildes() {
        assertTrue(RequisitosActividadesService.esConsultaDeRequisitos("¿Cuáles son los requisitos de Robótica?"));
        assertTrue(RequisitosActividadesService.esConsultaDeRequisitos("¿Qué debo llevar para robótica?"));
        assertTrue(RequisitosActividadesService.esConsultaDeRequisitos("¿Qué materiales necesito para programación?"));
        assertFalse(RequisitosActividadesService.esConsultaDeRequisitos("¿A qué hora empieza robótica?"));
    }

    @Test void entregaSoloRequisitosDeLaActividadPublicada() {
        var resultado = RequisitosActividadesService.responder("¿Qué requisitos tiene el taller de robótica?", List.of(
                actividad("1", "Taller de Robótica", List.of("Traer laptop", "Inscripción previa")),
                actividad("2", "Club de Música", List.of("Traer instrumento"))));
        assertEquals("requisitos", resultado.tipoConsulta());
        assertEquals("Taller de Robótica", resultado.requisitos().get(0).get("nombre"));
        assertEquals(List.of("Traer laptop", "Inscripción previa"), resultado.requisitos().get(0).get("items"));
        assertFalse(resultado.respuesta().contains("instrumento"));
    }

    @Test void siFaltaInformacionNoAfirmaQueNoHayaRequisitos() {
        var a = new java.util.LinkedHashMap<String, Object>();
        a.put("id", "1"); a.put("nombre", "Taller de Robótica"); a.put("requisitos", null);
        var r = RequisitosActividadesService.responder("¿Cuáles son los requisitos de robótica?", List.of(a));
        assertEquals(false, r.requisitos().get(0).get("confirmados"));
        assertTrue(r.respuesta().contains("no ha especificado"));
    }

    @Test void pideNombreExactoCuandoEsAmbiguo() {
        var datos = List.of(actividad("1", "Robótica Básica", "Laptop"), actividad("2", "Robótica Avanzada", "Experiencia"));
        assertTrue(RequisitosActividadesService.responder("¿Qué requisitos necesito?", datos).requisitos().isEmpty());
        assertTrue(RequisitosActividadesService.responder("¿Requisitos de robótica?", datos).requisitos().isEmpty());
        assertTrue(RequisitosActividadesService.responder("¿Requisitos de fotografía?", datos).requisitos().isEmpty());
    }

    @Test void separaFormatosDelAdminSinCrearRequisitos() {
        assertEquals(List.of("DNI", "Laptop"), RequisitosActividadesService.elementos("- DNI; Laptop"));
        assertEquals(List.of("DNI", "Laptop"), RequisitosActividadesService.elementos(List.of("DNI", "Laptop")));
        assertEquals(List.of(), RequisitosActividadesService.elementos(null));
    }

    @Test void integracionDelAsistenteLeeLosRequisitosDelCatalogo() {
        var catalogo = mock(CatalogoController.class);
        var actividad = new java.util.LinkedHashMap<String,Object>();
        actividad.put("id", "1"); actividad.put("nombre", "Feria de Robótica");
        actividad.put("requisitos", List.of("Estar inscrito"));
        when(catalogo.listar()).thenReturn(Map.of("actividades", List.of(actividad)));
        var servicio = new AsistenteHorariosService(catalogo, new ObjectMapper(),
                new UbicacionesActividadesService(mock(MongoTemplate.class)));
        var r = servicio.responder("¿Qué requisitos tiene la feria de robótica?", List.of());
        assertEquals("requisitos", r.tipoConsulta());
        assertTrue(r.respuesta().contains("Estar inscrito"));
    }
}
