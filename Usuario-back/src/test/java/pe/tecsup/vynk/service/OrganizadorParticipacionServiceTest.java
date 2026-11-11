package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.repository.InscripcionRepository;

class OrganizadorParticipacionServiceTest {
    private final OrganizadorAccessService acceso = mock(OrganizadorAccessService.class);
    private final OrganizadorCuposService cupos = mock(OrganizadorCuposService.class);
    private final InscripcionRepository inscripciones = mock(InscripcionRepository.class);
    private final OrganizadorParticipacionService servicio =
            new OrganizadorParticipacionService(acceso, cupos, inscripciones);

    @Test void calculaParticipacionPorPersonasYReemplazosDesdeHistorial() {
        Document actividad = new Document("_id", "a1");
        var base = Map.<String, Object>of("id", "a1", "nombre", "Taller", "maximoParticipantes", 10);
        when(acceso.resumen(actividad)).thenReturn(base);
        when(cupos.enriquecer(base)).thenReturn(Map.of("id", "a1", "nombre", "Taller",
                "maximoParticipantes", 10, "unidadCupo", "estudiantes", "cuposOcupados", 6L,
                "estudiantesInscritos", 6L, "cuposDisponibles", 4L));
        Inscripcion i = new Inscripcion();
        i.setHistorialReemplazos(List.of(Map.of("motivo", "salud"), Map.of("motivo", "retiro")));
        when(inscripciones.findByActividadId("a1")).thenReturn(List.of(i));
        var respuesta = servicio.calcular(actividad);
        assertEquals(60L, respuesta.get("porcentajeParticipacion"));
        assertEquals("medio", respuesta.get("nivel"));
        assertEquals(2L, respuesta.get("reemplazosRegistrados"));
        assertEquals("estudiantes", respuesta.get("unidadCupo"));
    }

    @Test void separaEquiposDeEstudiantesYSeñalaExceso() {
        Document actividad = new Document("_id", "a1");
        var base = Map.<String, Object>of("id", "a1");
        when(acceso.resumen(actividad)).thenReturn(base);
        when(cupos.enriquecer(base)).thenReturn(Map.of("id", "a1", "unidadCupo", "equipos",
                "maximoParticipantes", 4, "cuposOcupados", 5L, "cuposDisponibles", 0L,
                "estudiantesInscritos", 15L, "equiposRegistrados", 5L, "equiposConfirmados", 3L,
                "equiposPendientes", 2L));
        when(inscripciones.findByActividadId("a1")).thenReturn(List.of());
        var respuesta = servicio.calcular(actividad);
        assertEquals(125L, respuesta.get("porcentajeParticipacion"));
        assertEquals(60L, respuesta.get("porcentajeEquiposCompletos"));
        assertEquals(15L, respuesta.get("estudiantesInscritos"));
        assertEquals(true, respuesta.get("excedeLimite"));
    }

    @Test void noInventaPorcentajeSiNoHayCapacidad() {
        Document actividad = new Document("_id", "a1");
        var base = Map.<String, Object>of("id", "a1");
        when(acceso.resumen(actividad)).thenReturn(base);
        when(cupos.enriquecer(base)).thenReturn(Map.of("id", "a1", "unidadCupo", "estudiantes",
                "cuposOcupados", 2L, "estudiantesInscritos", 2L));
        when(inscripciones.findByActividadId("a1")).thenReturn(List.of());
        var respuesta = servicio.calcular(actividad);
        assertNull(respuesta.get("porcentajeParticipacion"));
        assertEquals("sin_capacidad", respuesta.get("nivel"));
    }
}
