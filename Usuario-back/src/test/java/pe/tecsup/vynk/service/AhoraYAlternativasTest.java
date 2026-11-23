package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pe.tecsup.vynk.model.Inscripcion;

/** US-78 («¿Qué hay ahora?») y US-79 («No puedo asistir»). */
class AhoraYAlternativasTest {
    static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 23, 10, 30); // lunes, hora de Lima

    static Map<String, Object> act(String id, String nombre, String fecha, String ini, String fin) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("id", id); a.put("nombre", nombre); a.put("estado", "programada"); a.put("fecha", fecha);
        a.put("horaInicio", ini); a.put("horaFin", fin); a.put("cuposDisponibles", 10); a.put("ubicacion", "Auditorio");
        return a;
    }

    static Inscripcion insc(String actividad, String nombre, String fecha, String ini, String fin) {
        Inscripcion i = new Inscripcion();
        i.setActividadId(actividad); i.setNombreActividad(nombre); i.setEstado("inscrita");
        i.setFecha(fecha); i.setHoraInicio(ini); i.setHoraFin(fin);
        return i;
    }

    // ---------- US-78 ----------

    @Test void reconoceLaPreguntaDelBotonYSusVariantesPeroNoLasQueTienenTemaOFecha() {
        for (String p : List.of("¿Qué hay ahora?", "que hay ahorita", "¿Qué actividades hay en este momento?",
                "¿Qué puedo hacer ahora mismo?", "Búscame actividades disponibles ahora"))
            assertTrue(ActividadesAhoraService.esPreguntaAhora(p), p);
        for (String p : List.of("¿Qué deportes hay ahora?", "¿Dónde es la charla ahora?", "¿Qué hay mañana?",
                "¿Qué hay a las 3 pm?", "hola"))
            assertFalse(ActividadesAhoraService.esPreguntaAhora(p), p);
    }

    @Test void separaLoQueEstaEnCursoDeLoQueEmpiezaProntoYMarcaInscritasYCruces() {
        var catalogo = List.of(
                act("a1", "Taller de IA", "2026-11-23", "10:00", "12:00"),          // en curso, 90 min
                act("a2", "Charla corta", "2026-11-23", "10:00", "10:35"),          // termina en 5: no vale la pena
                act("a3", "Feria", "2026-11-23", "09:00", "11:00"),                  // en curso, 30 min
                act("a4", "Torneo de fútbol", "2026-11-23", "11:00", "12:00"),       // pronto, 30 min
                act("a5", "Concierto", "2026-11-23", "13:00", "14:00"),              // más tarde: la siguiente
                act("a6", "Mañana", "2026-11-24", "10:00", "12:00"),                 // otro día
                act("a7", "Pronto 2", "2026-11-23", "11:30", "12:30"));              // pronto, 60 min
        Map<String, Object> lleno = act("a8", "Lleno", "2026-11-23", "10:00", "12:00");
        lleno.put("cuposDisponibles", 0);
        Map<String, Object> cancelada = act("a9", "Cancelada", "2026-11-23", "10:00", "12:00");
        cancelada.put("estado", "cancelada");
        var todo = new java.util.ArrayList<>(catalogo);
        todo.add(lleno); todo.add(cancelada);
        var mias = List.of(insc("a3", "Feria", "2026-11-23", "09:00", "11:00"));

        var r = ActividadesAhoraService.armar(todo, mias, AHORA);
        @SuppressWarnings("unchecked") var lista = (List<Map<String, Object>>) r.get("actividades");
        assertEquals(List.of("a1", "a3", "a4", "a7"), lista.stream().map(a -> a.get("id")).toList());
        assertEquals("en_curso", lista.get(0).get("estadoAhora"));
        assertEquals(90L, lista.get(0).get("minutos"));
        assertEquals("En curso · termina en 1 h 30 min (12:00)", lista.get(0).get("detalleAhora"));
        assertEquals(true, lista.get(1).get("inscrito"));
        assertEquals(false, lista.get(1).get("inscripcionDirecta"), "ya está inscrito");
        assertEquals("Feria", lista.get(0).get("cruzaCon"), "el taller se cruza con su Feria");
        assertEquals("pronto", lista.get(2).get("estadoAhora"));
        assertEquals("Empieza en 30 min (11:00)", lista.get(2).get("detalleAhora"));
        assertNull(lista.get(2).get("cruzaCon"), "la Feria termina a las 11:00 y el fútbol empieza a las 11:00");
        assertEquals("10:30", r.get("hora"));
        assertEquals(2, r.get("enCurso"));
        assertEquals(2, r.get("pronto"));
        assertTrue(String.valueOf(r.get("mensaje")).startsWith(
                "Ahora mismo (10:30) hay 2 actividades en curso y 2 empiezan dentro de la próxima hora:\n\n• Taller de IA"));
    }

    @Test void sinNadaAhoraDiceCualEsLaSiguienteDeHoy() {
        var r = ActividadesAhoraService.armar(List.of(act("a5", "Concierto", "2026-11-23", "15:00", "16:00")), List.of(), AHORA);
        assertEquals(List.of(), r.get("actividades"));
        assertEquals("Ahora mismo (10:30) no hay actividades en curso ni empieza ninguna en la próxima hora. "
                + "La siguiente de hoy es «Concierto» a las 15:00.", r.get("mensaje"));
        var vacio = ActividadesAhoraService.armar(List.of(), List.of(), AHORA);
        assertTrue(String.valueOf(vacio.get("mensaje")).endsWith("Por hoy ya no quedan actividades con cupos."));
        assertNull(vacio.get("siguiente"));
    }

    @Test void lasDeEquiposSeInscribenDesdeElDetalle() {
        Map<String, Object> equipos = act("e1", "Hackathon", "2026-11-23", "10:00", "13:00");
        equipos.put("tipoActividad", "competencia"); equipos.put("modalidad", "equipos");
        @SuppressWarnings("unchecked") var lista = (List<Map<String, Object>>) ActividadesAhoraService
                .armar(List.of(equipos), List.of(), AHORA).get("actividades");
        assertEquals(false, lista.get(0).get("inscripcionDirecta"));
    }

    // ---------- US-79 ----------

    @Test void lasAlternativasNoChocanConMisOtrasInscripcionesYElHorarioQueDejoQuedaLibre() {
        Map<String, Object> original = act("o1", "Taller de robótica", "2026-11-24", "10:00", "12:00");
        original.put("categoriaId", "charlas-talleres");
        Inscripcion dejada = insc("o1", "Taller de robótica", "2026-11-24", "10:00", "12:00");
        Inscripcion otra = insc("x1", "Charla de IA", "2026-11-24", "15:00", "16:00");
        Map<String, Object> mismaHora = act("c1", "Robótica avanzada", "2026-11-24", "10:30", "11:30");
        mismaHora.put("categoriaId", "charlas-talleres");
        Map<String, Object> parecida = act("c2", "Robótica móvil", "2026-11-25", "10:00", "12:00");
        parecida.put("categoriaId", "charlas-talleres");
        Map<String, Object> chocaConOtra = act("c3", "Taller de drones", "2026-11-24", "15:30", "16:30");
        chocaConOtra.put("categoriaId", "charlas-talleres");
        Map<String, Object> distinta = act("c4", "Feria gastronómica", "2026-11-24", "13:00", "14:00");
        Map<String, Object> pasada = act("c5", "Ya pasó", "2026-11-23", "08:00", "09:00");
        var catalogo = List.of(original, mismaHora, parecida, chocaConOtra, distinta, pasada);

        var porHorario = AlternativasAsistenciaService.candidatas(original, dejada, List.of(dejada, otra), catalogo, true, AHORA);
        assertEquals(List.of("c2", "c4"), porHorario.stream().map(c -> c.get("id")).toList(),
                "sin la de su mismo horario, sin la que choca con su charla, sin la pasada; primero la parecida");
        var otroMotivo = AlternativasAsistenciaService.candidatas(original, dejada, List.of(dejada, otra), catalogo, false, AHORA);
        assertEquals("c1", otroMotivo.get(0).get("id"), "si el motivo no es el horario, la de la misma hora vale (su horario queda libre)");
    }

    @Test void explicaCadaAlternativaSinIa() {
        Map<String, Object> original = act("o1", "Taller de robótica", "2026-11-24", "10:00", "12:00");
        original.put("categoriaId", "charlas-talleres");
        Map<String, Object> parecida = act("c2", "Robótica móvil", "2026-11-25", "10:00", "12:00");
        parecida.put("categoriaId", "charlas-talleres");
        parecida.put("cuposDisponibles", 2);
        assertEquals("Se parece a la que dejas: es de la misma categoría y puedes ir el miércoles 25 a las 10:00.",
                AlternativasAsistenciaService.motivoReglas(original, parecida, AHORA));
        assertEquals(List.of("Misma categoría", "El miércoles 25 a las 10:00", "Quedan 2 cupos"),
                AlternativasAsistenciaService.etiquetas(original, parecida, AHORA));
        Map<String, Object> hoy = act("c9", "Otra cosa", "2026-11-23", "16:00", "17:00");
        assertEquals("Encaja en tu agenda y puedes ir hoy a las 16:00.", AlternativasAsistenciaService.motivoReglas(original, hoy, AHORA));
        Map<String, Object> mismoTema = act("c8", "Robótica para todos", "2026-11-24", "16:00", "17:00");
        assertEquals("Se parece a la que dejas: tiene un tema parecido y puedes ir mañana a las 16:00.",
                AlternativasAsistenciaService.motivoReglas(original, mismoTema, AHORA));
    }
}
