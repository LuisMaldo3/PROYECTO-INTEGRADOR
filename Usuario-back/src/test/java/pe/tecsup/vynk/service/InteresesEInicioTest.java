package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pe.tecsup.vynk.model.Inscripcion;

/** US-80 (catálogo por intereses) y US-81 (resumen de inicio). */
class InteresesEInicioTest {
    static final Instant AHORA = Instant.parse("2026-11-23T15:30:00Z"); // lunes 23/11, 10:30 en Lima

    static Map<String, Object> act(String id, String nombre, String categoria, String fecha, String ini, String fin) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("id", id); a.put("nombre", nombre); a.put("categoriaId", categoria); a.put("estado", "programada");
        a.put("fecha", fecha); a.put("horaInicio", ini); a.put("horaFin", fin); a.put("cuposDisponibles", 20);
        a.put("portadaUrl", null);
        return a;
    }

    static Inscripcion insc(String actividad) {
        Inscripcion i = new Inscripcion();
        i.setActividadId(actividad); i.setEstado("inscrita");
        return i;
    }

    // ---------- US-80 ----------

    @Test void lasParaTiVanPrimeroConMasAfinidadYElRestoConservaLaFecha() {
        var catalogo = List.of(
                act("a1", "Charla de cocina", "gastronomia", "2026-11-23", "09:00", "10:00"),
                act("a2", "Torneo de fútbol", "deportes", "2026-11-23", "11:00", "12:00"),
                act("a3", "Concierto", "cultural", "2026-11-24", "10:00", "11:00"),
                act("a4", "Videojuegos y deportes electrónicos", "deportes", "2026-11-25", "10:00", "11:00"),
                act("a5", "Charla de industria", "charlas", "2026-11-25", "12:00", "13:00"));
        var r = PersonalizacionCatalogo.ordenar(catalogo, List.of("deportes", "videojuegos", "ia"), true);
        assertEquals(List.of("a4", "a2", "a1", "a3", "a5"), r.stream().map(a -> a.get("id")).toList());
        assertEquals(2, r.get(0).get("afinidad"));
        assertEquals("Te interesa: deportes y videojuegos", r.get(0).get("motivoParaTi"));
        assertEquals(true, r.get(1).get("paraTi"));
        assertEquals(false, r.get(4).get("paraTi"), "«ia» no coincide con «industria»");
        assertNull(r.get(4).get("motivoParaTi"));
        var porFecha = PersonalizacionCatalogo.ordenar(catalogo, List.of("deportes"), false);
        assertEquals(List.of("a1", "a2", "a3", "a4", "a5"), porFecha.stream().map(a -> a.get("id")).toList());
        assertEquals(true, porFecha.get(1).get("paraTi"), "con orden por fecha igual se marcan");
    }

    @Test void unirTemasEnEspanol() {
        assertEquals("deportes", PersonalizacionCatalogo.unir(List.of("deportes")));
        assertEquals("deportes, música y teatro", PersonalizacionCatalogo.unir(List.of("deportes", "música", "teatro")));
    }

    // ---------- US-81 ----------

    @Test void elResumenTraeMisProximasEnOrdenSinLoTerminadoNiLoCancelado() {
        var catalogo = new ArrayList<>(List.of(
                act("p1", "Taller de IA", "charlas", "2026-11-23", "10:00", "11:00"),       // en curso
                act("p2", "Torneo de robots", "concursos", "2026-11-24", "10:00", "12:00"),
                act("p0", "Desayuno", "otros", "2026-11-23", "08:00", "09:00"),            // terminó
                act("p3", "Charla de hoy", "charlas", "2026-11-23", "15:00", "16:00")));
        catalogo.get(1).put("tipoActividad", "competencia");
        var mias = List.of(insc("p2"), insc("p1"), insc("p0"), insc("p3"), insc("cancelada"));
        var equipos = Map.of("p2", new EquiposAgenda.Equipo("e1", "Robots", "capitan", 3, 2));

        var r = InicioAppService.armar("Ana Torres", mias, equipos, catalogo, List.of(), List.of(), AHORA);
        @SuppressWarnings("unchecked") var proximas = (List<Map<String, Object>>) r.get("proximas");
        assertEquals(List.of("p1", "p3", "p2"), proximas.stream().map(a -> a.get("id")).toList());
        assertEquals(true, proximas.get(0).get("enCurso"));
        assertEquals(0L, proximas.get(0).get("minutosParaEmpezar"));
        assertEquals("hoy · 15:00", proximas.get(1).get("cuando"));
        assertEquals("mañana · 10:00", proximas.get(2).get("cuando"));
        @SuppressWarnings("unchecked") var eq = (Map<String, Object>) proximas.get(2).get("equipo");
        assertEquals(false, eq.get("confirmado"));
        @SuppressWarnings("unchecked") var resumen = (Map<String, Object>) r.get("resumen");
        assertEquals(3, resumen.get("inscritas"));
        assertEquals(2, resumen.get("hoy"));
        assertEquals(3, resumen.get("semana"));
        assertEquals(1, resumen.get("equiposPorConfirmar"));
        @SuppressWarnings("unchecked") var saludo = (Map<String, Object>) r.get("saludo");
        assertEquals("Ana", saludo.get("nombre"));
        assertEquals("Hoy tienes 2 actividades.", saludo.get("mensaje"));
    }

    @Test void destacaCompetenciasConPortadaYParaTiSinLasInscritasNiLasPasadas() {
        Map<String, Object> competencia = act("c1", "Hackathon", "concursos", "2026-11-25", "09:00", "13:00");
        competencia.put("tipoActividad", "competencia");
        Map<String, Object> conPortada = act("c2", "Feria", "gastronomia", "2026-11-24", "12:00", "13:00");
        conPortada.put("portadaUrl", "/api/portadas/archivo/abc");
        Map<String, Object> paraTi = act("c3", "Torneo de vóley", "deportes", "2026-11-26", "10:00", "11:00");
        Map<String, Object> comun = act("c4", "Charla", "charlas", "2026-11-24", "09:00", "10:00");
        Map<String, Object> inscrita = act("c5", "Mía", "deportes", "2026-11-24", "15:00", "16:00");
        Map<String, Object> pasada = act("c6", "Ya empezó", "deportes", "2026-11-23", "10:00", "12:00");
        Map<String, Object> llena = act("c7", "Llena", "deportes", "2026-11-24", "18:00", "19:00");
        llena.put("cuposDisponibles", 0);
        var r = InicioAppService.armar("Ana", List.of(insc("c5")), Map.of(),
                List.of(competencia, conPortada, paraTi, comun, inscrita, pasada, llena), List.of(), List.of("deportes"), AHORA);
        @SuppressWarnings("unchecked") var destacadas = (List<Map<String, Object>>) r.get("destacadas");
        assertEquals(List.of("c1", "c2", "c3", "c4"), destacadas.stream().map(a -> a.get("id")).toList());
        assertEquals("Competencia", destacadas.get(0).get("destacadaPor"));
        assertEquals(false, destacadas.get(0).get("tienePortada"), "sin portada publicada la app usa la ilustración");
        assertEquals(true, destacadas.get(1).get("tienePortada"));
        assertEquals("Para ti", destacadas.get(2).get("destacadaPor"));
        assertFalse(destacadas.get(0).containsKey("puntaje"));
        @SuppressWarnings("unchecked") var saludo = (Map<String, Object>) r.get("saludo");
        assertEquals("Tu próxima actividad es mañana a las 15:00.", saludo.get("mensaje"));
    }

    @Test void losAvisosTraenSuImagenListaYSiSonNuevos() {
        var avisos = List.<Map<String, Object>>of(
                Map.of("id", "v1", "titulo", "Feria", "mensaje", "Ven", "publicadoEn", "2026-11-22T10:00:00Z",
                        "portada", Map.of("tipo", "subida", "archivoId", "a".repeat(24))),
                Map.of("id", "v2", "titulo", "Antiguo", "mensaje", "Hace tiempo", "publicadoEn", "2026-11-01T10:00:00Z",
                        "portada", Map.of("tipo", "url", "url", "https://upload.wikimedia.org/x.jpg", "autor", "Juan")),
                Map.of("id", "v3", "titulo", "Sin imagen", "mensaje", "", "publicadoEn", "x",
                        "portada", Map.of("tipo", "url", "url", "http://inseguro/x.jpg")));
        var r = InicioAppService.armar("Ana", List.of(), Map.of(), List.of(), avisos, List.of(), AHORA);
        @SuppressWarnings("unchecked") var lista = (List<Map<String, Object>>) r.get("avisos");
        assertEquals("/api/portadas/archivo/" + "a".repeat(24), lista.get(0).get("imagenUrl"), "la subida ahora se ve");
        assertEquals(true, lista.get(0).get("nuevo"));
        assertEquals("https://upload.wikimedia.org/x.jpg", lista.get(1).get("imagenUrl"));
        assertEquals("Juan", lista.get(1).get("autor"));
        assertEquals(false, lista.get(1).get("nuevo"));
        assertNull(lista.get(2).get("imagenUrl"), "solo https");
        assertEquals(false, lista.get(2).get("nuevo"));
    }

    @Test void mensajeDeSaludoSegunLaAgenda() {
        assertEquals("Hoy tienes 1 actividad.", InicioAppService.mensaje(1, 3, List.of()));
        assertEquals("Tu próxima actividad es el martes 24 a las 10:00.",
                InicioAppService.mensaje(0, 1, List.of(Map.of("cuando", "el martes 24 · 10:00"))));
    }
}
