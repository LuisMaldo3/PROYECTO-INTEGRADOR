package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.RecomendacionDescartada;
import pe.tecsup.vynk.repository.RecomendacionDescartadaRepository;

class DescartesRecomendacionesServiceTest {
    @Test void guardaUnaPreferenciaIdempotentePorUsuario() {
        var repositorio = mock(RecomendacionDescartadaRepository.class);
        var servicio = new DescartesRecomendacionesService(repositorio);
        servicio.descartar("estudiante-1", "actividad-99");
        servicio.descartar("estudiante-1", "actividad-99");
        var captor = ArgumentCaptor.forClass(RecomendacionDescartada.class);
        verify(repositorio, times(2)).save(captor.capture());
        assertEquals(captor.getAllValues().get(0).getId(), captor.getAllValues().get(1).getId());
        assertEquals("estudiante-1", captor.getValue().getUsuarioId());
        assertEquals("actividad-99", captor.getValue().getActividadId());
    }

    @Test void noMezclaLasPreferenciasDeUsuariosDistintos() {
        assertNotEquals(RecomendacionDescartada.generarId("usuario-1", "actividad-1"),
                RecomendacionDescartada.generarId("usuario-2", "actividad-1"));
        var repo = mock(RecomendacionDescartadaRepository.class);
        when(repo.findByUsuarioId("estudiante-A")).thenReturn(List.of(new RecomendacionDescartada("estudiante-A", "a2")));
        when(repo.findByUsuarioId("estudiante-B")).thenReturn(List.of());
        var servicio = new DescartesRecomendacionesService(repo);
        assertEquals(Set.of("a2"), servicio.idsDescartados("estudiante-A"));
        assertTrue(servicio.idsDescartados("estudiante-B").isEmpty());
    }

    @Test void excluyeDescartadasAntesDeOrdenarConIaYSinIa() {
        var candidatas = List.of(Map.<String, Object>of("id", "a1", "nombre", "Taller"),
                Map.<String, Object>of("id", "a2", "nombre", "Deportes"),
                Map.<String, Object>of("id", "a3", "nombre", "Robótica"));
        var filtradas = DescartesRecomendacionesService.excluir(candidatas, Set.of("a2"));
        assertEquals(List.of("a1", "a3"), filtradas.stream().map(a -> a.get("id")).toList());
        assertEquals(3, candidatas.size());
        assertEquals(3, DescartesRecomendacionesService.excluir(candidatas, Set.of()).size());
    }

    @Test void rechazaIdentificadoresInvalidosSinPersistir() {
        var repo = mock(RecomendacionDescartadaRepository.class);
        var servicio = new DescartesRecomendacionesService(repo);
        assertThrows(ResponseStatusException.class, () -> servicio.descartar("u", "../otro"));
        assertThrows(ResponseStatusException.class, () -> servicio.descartar("u", ""));
        assertThrows(ResponseStatusException.class, () -> servicio.descartar("", "a1"));
        verify(repo, never()).save(any());
    }
}
