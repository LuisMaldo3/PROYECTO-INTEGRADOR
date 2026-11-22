package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class FiltrosYProximasTest {
    static Map<String, Object> a(String id, String fecha, String inicio, String categoria, String area) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id); m.put("nombre", "Actividad " + id); m.put("estado", "programada"); m.put("fecha", fecha);
        m.put("horaInicio", inicio); m.put("horaFin", ""); m.put("categoriaId", categoria); m.put("areaTecsup", area);
        m.put("categoria", categoria == null ? null : categoria.substring(0, 1).toUpperCase() + categoria.substring(1));
        return m;
    }

    final List<Map<String, Object>> todas = List.of(
            a("m", "2026-11-23", "09:00", "deportes", "Mecánica"),
            a("t", "2026-11-23", "12:00", "charlas-talleres", "Tecnología Digital"),
            a("t2", "2026-11-24", "17:59", "deportes", null),
            a("n", "2026-11-24", "18:00", "videojuegos", "Tecnología Digital"),
            a("x", "2026-11-24", "", "deportes", "Mecánica"));

    List<String> ids(FiltrosCatalogo f) { return f.aplicar(todas).stream().map(x -> (String) x.get("id")).toList(); }

    // ---------- US-73 filtros ----------

    @Test void sinFiltrosDevuelveTodo() {
        assertEquals(5, ids(FiltrosCatalogo.desde(null, null, null, null, null, null)).size());
        assertTrue(FiltrosCatalogo.desde("", " ", null, "", "", "").vacio());
    }

    @Test void filtraPorFranjaDeLaManianaTardeYNoche() {
        assertEquals(List.of("m"), ids(FiltrosCatalogo.desde("manana", null, null, null, null, null)));
        assertEquals(List.of("t", "t2"), ids(FiltrosCatalogo.desde("tarde", null, null, null, null, null)));
        assertEquals(List.of("n"), ids(FiltrosCatalogo.desde("noche", null, null, null, null, null)));
        assertEquals(List.of("m"), ids(FiltrosCatalogo.desde("Mañana", null, null, null, null, null)), "acepta la ñ");
    }

    @Test void filtraPorRangoDeHorasYFecha() {
        assertEquals(List.of("t", "t2"), ids(FiltrosCatalogo.desde(null, "10:00", "18:00", null, null, null)));
        assertEquals(List.of("t2", "n"), ids(FiltrosCatalogo.desde(null, "13:00", null, "2026-11-24", null, null)));
        assertEquals(List.of("m", "t"), ids(FiltrosCatalogo.desde(null, null, null, "2026-11-23", null, null)));
    }

    @Test void filtraPorCategoria() {
        assertEquals(List.of("m", "t2", "x"), ids(FiltrosCatalogo.desde(null, null, null, null, "deportes", null)));
    }

    @Test void filtraPorLasCincoAreasConOSinTildes() {
        assertEquals(List.of("t", "n"), ids(FiltrosCatalogo.desde(null, null, null, null, null, "tecnologia digital")));
        assertEquals(List.of("m", "x"), ids(FiltrosCatalogo.desde(null, null, null, null, null, "Mecánica")));
        assertEquals(List.of(), ids(FiltrosCatalogo.desde(null, null, null, null, null, "Gestión y Producción")));
    }

    @Test void combinaFiltrosYLasSinHoraNoEntranEnUnFiltroDeHorario() {
        assertEquals(List.of("m"), ids(FiltrosCatalogo.desde("manana", null, null, null, "deportes", "Mecánica")));
        assertFalse(ids(FiltrosCatalogo.desde(null, "00:00", null, null, null, null)).contains("x"));
    }

    @Test void rechazaValoresMalEscritos() {
        for (String[] malo : new String[][]{{"madrugada", null, null, null, null}, {null, "9am", null, null, null},
                {null, "15:00", "10:00", null, null}, {null, null, null, "24/11/2026", null}, {null, null, null, null, "Arte"}}) {
            assertThrows(ResponseStatusException.class, () -> FiltrosCatalogo.desde(malo[0], malo[1], malo[2], malo[3], null, malo[4]));
        }
    }

    @Test void lasOpcionesCuentanSobreTodasLasActividades() {
        Map<String, Object> o = FiltrosCatalogo.opciones(todas);
        @SuppressWarnings("unchecked") var categorias = (List<Map<String, Object>>) o.get("categorias");
        assertEquals(Map.of("id", "deportes", "nombre", "Deportes", "total", 3), categorias.stream().filter(c -> c.get("id").equals("deportes")).findFirst().orElseThrow());
        @SuppressWarnings("unchecked") var areas = (List<Map<String, Object>>) o.get("areas");
        assertEquals(5, areas.size(), "siempre las cinco áreas, aunque no tengan actividades");
        assertEquals(2, areas.stream().filter(x -> x.get("nombre").equals("Tecnología Digital")).findFirst().orElseThrow().get("total"));
        @SuppressWarnings("unchecked") var franjas = (List<Map<String, Object>>) o.get("franjas");
        assertEquals(List.of(Map.of("id", "manana", "total", 1), Map.of("id", "tarde", "total", 2), Map.of("id", "noche", "total", 1)), franjas);
    }

    // ---------- US-72 próximas ----------

    @Test void eligeLasQueEmpiezanAntesSinContarLasQueYaEmpezaron() {
        Instant ahora = Instant.parse("2026-11-23T14:30:00Z"); // 09:30 en Lima
        var elegidas = ProximasActividadesService.elegir(todas, ahora, 3);
        assertEquals(List.of("t", "t2", "n"), elegidas.stream().map(x -> x.get("id")).toList());
        assertEquals(List.of("t"), ProximasActividadesService.elegir(todas, ahora, 1).stream().map(x -> x.get("id")).toList());
    }

    @Test void noIncluyeCanceladasNiSinHora() {
        var cancelada = a("c", "2026-11-23", "10:00", "deportes", null);
        cancelada.put("estado", "cancelada");
        var elegidas = ProximasActividadesService.elegir(List.of(cancelada, todas.get(4)), Instant.parse("2026-11-23T12:00:00Z"), 3);
        assertTrue(elegidas.isEmpty());
        assertTrue(ProximasActividadesService.elegir(null, Instant.now(), 3).isEmpty());
    }

    @Test void losMinutosSeRedondeanHaciaArribaYNuncaSonCero() {
        Instant inicio = Instant.parse("2026-11-23T15:00:00Z");
        assertEquals(30, ProximasActividadesService.minutos(inicio.minusSeconds(1800), inicio));
        assertEquals(30, ProximasActividadesService.minutos(inicio.minusSeconds(1790), inicio));
        assertEquals(1, ProximasActividadesService.minutos(inicio.minusSeconds(5), inicio));
    }
}
