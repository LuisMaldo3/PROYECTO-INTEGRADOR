package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.tecsup.vynk.model.Inscripcion;

/** US-76 y US-77: recomendaciones para la app, agenda inteligente y sincronización con equipos. */
class RecomendacionesYAgendaAppTest {
    static final Instant AHORA = Instant.parse("2026-11-23T15:30:00Z"); // lunes 23/11, 10:30 en Lima
    static final LocalDate HOY = LocalDate.of(2026, 11, 23);

    final Map<String, Inscripcion> porId = new HashMap<>();
    final List<Inscripcion> todas = new ArrayList<>();

    Inscripcion insc(String id, String estudiante, String actividad, String nombre, String fecha, String ini, String fin) {
        Inscripcion i = new Inscripcion();
        i.setId(id); i.setEstudianteId(estudiante); i.setActividadId(actividad); i.setNombreActividad(nombre);
        i.setFecha(fecha); i.setHoraInicio(ini); i.setHoraFin(fin); i.setEstado("inscrita"); i.setUbicacion("lab");
        porId.put(id, i); todas.add(i);
        return i;
    }

    EquiposAgenda.Resultado sincronizar(List<Inscripcion> mias) {
        return EquiposAgenda.sincronizar(mias, id -> Optional.ofNullable(porId.get(id)),
                (est, act) -> todas.stream().filter(x -> est.equals(x.getEstudianteId()) && act.equals(x.getActividadId())).findFirst());
    }

    /** Equipo «Robots» de la competencia c1: capitán u1 e integrante u2 (u3 figura en la lista sin confirmar). */
    Inscripcion[] equipo(boolean conPendiente) {
        Inscripcion cap = insc("e1", "u1", "c1", "Competencia de robots", "2026-11-24", "10:00", "12:00");
        cap.setRolEquipo("capitan"); cap.setEquipoId("e1"); cap.setNombreEquipo("Robots");
        cap.setIntegrantes(new ArrayList<>(conPendiente ? List.of("u1", "u2", "u3") : List.of("u1", "u2")));
        Inscripcion miembro = insc("m2", "u2", "c1", "Competencia de robots", "2026-11-24", "10:00", "12:00");
        miembro.setRolEquipo("integrante"); miembro.setEquipoId("e1"); miembro.setNombreEquipo("Robots");
        return new Inscripcion[] {cap, miembro};
    }

    @Test void equipoCompletoQuedaConfirmadoParaCapitanEIntegrante() {
        var e = equipo(false);
        var capitan = sincronizar(List.of(e[0])).entradas().get(0).equipo();
        var integrante = sincronizar(List.of(e[1])).entradas().get(0).equipo();
        assertEquals("capitan", capitan.rol());
        assertEquals("integrante", integrante.rol());
        for (var eq : List.of(capitan, integrante)) {
            assertEquals("Robots", eq.nombre());
            assertEquals(2, eq.integrantes());
            assertEquals(2, eq.confirmados());
            assertTrue(eq.confirmado());
        }
    }

    @Test void siFaltaUnIntegranteElEquipoNoEstaConfirmado() {
        var e = equipo(true);
        var eq = sincronizar(List.of(e[1])).entradas().get(0).equipo();
        assertEquals(3, eq.integrantes());
        assertEquals(2, eq.confirmados());
        assertFalse(eq.confirmado());
        assertEquals(1, eq.pendientes());
    }

    @Test void siElCapitanCancelaElIntegranteQuedaSinEquipoYLasIndividualesSiguen() {
        var e = equipo(false);
        e[0].setEstado("cancelada");
        Inscripcion individual = insc("i1", "u2", "a1", "Taller", "2026-11-24", "15:00", "16:00");
        Inscripcion cancelada = insc("i2", "u2", "a2", "Feria", "2026-11-24", "17:00", "18:00");
        cancelada.setEstado("cancelada");
        var r = sincronizar(List.of(e[1], individual, cancelada));
        assertEquals(List.of(e[1]), r.sinEquipo());
        assertEquals(1, r.entradas().size());
        assertSame(individual, r.entradas().get(0).inscripcion());
        assertNull(r.entradas().get(0).equipo());
    }

    @Test void laAgendaJuntaInscritasYSugeridasPorDiaEnOrdenYOmiteLoTerminado() {
        var e = equipo(true);
        Inscripcion hoyTemprano = insc("i1", "u2", "a1", "Desayuno", "2026-11-23", "08:00", "09:00");   // ya terminó
        Inscripcion enCurso = insc("i2", "u2", "a2", "Taller de IA", "2026-11-23", "10:00", "11:00");
        Map<String, Object> sugerida = new LinkedHashMap<>(Map.of("id", "s1", "nombre", "Charla de drones",
                "fecha", "2026-11-24", "horaInicio", "08:00", "horaFin", "09:00", "ubicacion", "Auditorio",
                "motivo", "Te gustan los drones", "cuposDisponibles", 4));
        Map<String, Object> yaEmpezo = new LinkedHashMap<>(Map.of("id", "s2", "nombre", "Pasada",
                "fecha", "2026-11-23", "horaInicio", "10:00", "horaFin", "12:00"));

        var r = AgendaInteligenteService.armar(sincronizar(List.of(e[1], hoyTemprano, enCurso)),
                List.of(sugerida, yaEmpezo), Map.of("lab", "Laboratorio 4"), AHORA);

        @SuppressWarnings("unchecked") var dias = (List<Map<String, Object>>) r.get("dias");
        assertEquals(2, dias.size());
        assertEquals("2026-11-23", dias.get(0).get("fecha"));
        assertTrue(String.valueOf(dias.get(0).get("etiqueta")).startsWith("Hoy · lunes 23"));
        assertTrue(String.valueOf(dias.get(1).get("etiqueta")).startsWith("Mañana · martes 24"));
        @SuppressWarnings("unchecked") var hoy = (List<Map<String, Object>>) dias.get(0).get("bloques");
        assertEquals(1, hoy.size(), "lo terminado y la sugerida que ya empezó no se muestran");
        assertEquals("Taller de IA", hoy.get(0).get("nombre"));
        assertEquals(true, hoy.get(0).get("enCurso"));
        assertEquals("Laboratorio 4", hoy.get(0).get("ubicacion"));
        @SuppressWarnings("unchecked") var manana = (List<Map<String, Object>>) dias.get(1).get("bloques");
        assertEquals(List.of("Charla de drones", "Competencia de robots"), manana.stream().map(b -> b.get("nombre")).toList());
        assertEquals("sugerida", manana.get(0).get("tipo"));
        assertEquals(true, manana.get(0).get("inscripcionDirecta"));
        assertEquals("Te gustan los drones", manana.get(0).get("motivo"));
        assertEquals("equipo", manana.get(1).get("modalidad"));
        @SuppressWarnings("unchecked") var eq = (Map<String, Object>) manana.get(1).get("equipo");
        assertEquals(false, eq.get("confirmado"));
        assertEquals(2, eq.get("confirmados"));

        @SuppressWarnings("unchecked") var resumen = (Map<String, Object>) r.get("resumen");
        assertEquals(2, resumen.get("inscritas"));
        assertEquals(1, resumen.get("sugeridas"));
        assertEquals(1, resumen.get("enEquipo"));
        assertEquals(1, resumen.get("equiposPorConfirmar"));
        @SuppressWarnings("unchecked") var avisos = (List<String>) r.get("avisos");
        assertEquals(List.of("Tu equipo «Robots» en «Competencia de robots» tiene 2 de 3 integrantes confirmados; falta 1."), avisos);
    }

    @Test void avisaCuandoElEquipoYaNoEstaActivoYCuandoFaltanIntegrantes() {
        var e = equipo(false);
        e[0].setIntegrantes(new ArrayList<>(List.of("u1")));
        var soloCapitan = AgendaInteligenteService.armar(sincronizar(List.of(e[0])), List.of(), Map.of(), AHORA);
        assertEquals(List.of("Tu equipo «Robots» en «Competencia de robots» necesita al menos 2 integrantes para quedar confirmado."),
                soloCapitan.get("avisos"));
        e[0].setEstado("cancelada");
        var huerfano = AgendaInteligenteService.armar(sincronizar(List.of(e[1])), List.of(), Map.of(), AHORA);
        assertEquals(List.of(), huerfano.get("dias"));
        assertTrue(String.valueOf(huerfano.get("avisos")).contains("ya no está activo"));
    }

    @Test void lasRecomendacionesTraenEtiquetasComprobablesYSiSeInscribeEnUnToque() {
        Map<String, Object> a = new LinkedHashMap<>(Map.of("id", "a1", "nombre", "Torneo de fútbol 7",
                "descripcion", "Campeonato relámpago", "categoriaId", "deportes", "fecha", "2026-11-24",
                "horaInicio", "16:00", "horaFin", "17:00"));
        a.putAll(Map.of("cuposDisponibles", 3, "cabeEnHorario", true, "huecoInicio", "15:00", "huecoFin", "18:00"));
        a.put("motivo", "");
        var item = RecomendacionesAppService.preparar(a, List.of("deportes"), HOY);
        assertEquals(List.of("Te interesa: deportes", "Mañana", "Libre de 15:00 a 18:00", "Quedan 3 cupos"), item.get("etiquetas"));
        assertEquals(true, item.get("inscripcionDirecta"));
        assertEquals("individual", item.get("modalidadInscripcion"));
        assertEquals("Actividad disponible que encaja con tu agenda.", item.get("motivo"));
        assertEquals("Torneo de fútbol 7", item.get("nombre"), "conserva los datos del catálogo");
    }

    @Test void porEquiposSeInscribeDesdeElDetalleYSinCuposNoHayInscripcionDirecta() {
        Map<String, Object> equipos = new LinkedHashMap<>(Map.of("nombre", "Hackathon", "tipoActividad", "competencia",
                "modalidad", "equipos", "fecha", "2026-11-23", "cuposDisponibles", 1));
        var item = RecomendacionesAppService.preparar(equipos, List.of(), HOY);
        assertEquals(false, item.get("inscripcionDirecta"));
        assertEquals("equipos", item.get("modalidadInscripcion"));
        assertEquals(List.of("Hoy", "Queda 1 equipo", "Por equipos"), item.get("etiquetas"));
        Map<String, Object> lleno = new LinkedHashMap<>(Map.of("nombre", "Taller", "cuposDisponibles", 0));
        assertEquals(false, RecomendacionesAppService.preparar(lleno, List.of(), HOY).get("inscripcionDirecta"));
    }

    @Test void elInteresSeBuscaComoPalabraYSinTildes() {
        Map<String, Object> industria = Map.of("nombre", "Charla: robótica en la industria", "categoriaId", "charlas");
        assertNull(RecomendacionesAppService.interesCoincidente(industria, List.of("ia")), "«ia» no es «industria»");
        Map<String, Object> ia = Map.of("nombre", "Taller de IA generativa");
        assertEquals("inteligencia artificial", RecomendacionesAppService.interesCoincidente(ia, List.of("ia")));
        Map<String, Object> robotica = Map.of("nombre", "Concurso de ROBÓTICA");
        assertEquals("robotica", RecomendacionesAppService.interesCoincidente(robotica, List.of("robotica")));
    }

    @Test void lasClasesSeVenEnLosDiasConActividadesYLasSugeridasRespetanElHorario() {
        Inscripcion taller = insc("i1", "u2", "a1", "Taller", "2026-11-24", "15:00", "16:00");
        var clases = List.of(new pe.tecsup.vynk.model.DisponibilidadSemanal.BloqueOcupado(2, "08:00", "13:00", "Clases"),
                new pe.tecsup.vynk.model.DisponibilidadSemanal.BloqueOcupado(3, "08:00", "13:00", "Clases"));
        Map<String, Object> enClase = new LinkedHashMap<>(Map.of("id", "s1", "nombre", "En clase", "fecha", "2026-11-24", "horaInicio", "09:00", "horaFin", "10:00"));
        Map<String, Object> libre = new LinkedHashMap<>(Map.of("id", "s2", "nombre", "Libre", "fecha", "2026-11-24", "horaInicio", "13:30", "horaFin", "14:30"));
        Map<String, Object> cruzaTaller = new LinkedHashMap<>(Map.of("id", "s3", "nombre", "Cruza", "fecha", "2026-11-24", "horaInicio", "15:30", "horaFin", "16:30"));
        var sugeridas = AgendaInteligenteService.enHorasLibres(List.of(enClase, libre, cruzaTaller), clases, List.of(taller));
        assertEquals(List.of("Libre"), sugeridas.stream().map(a -> a.get("nombre")).toList());

        var r = AgendaInteligenteService.armar(sincronizar(List.of(taller)), sugeridas, clases, Map.of(), AHORA);
        @SuppressWarnings("unchecked") var dias = (List<Map<String, Object>>) r.get("dias");
        assertEquals(1, dias.size(), "el miércoles no tiene actividades: no se muestra solo por las clases");
        @SuppressWarnings("unchecked") var bloques = (List<Map<String, Object>>) dias.get(0).get("bloques");
        assertEquals(List.of("clase", "sugerida", "inscrita"), bloques.stream().map(b -> b.get("tipo")).toList());
        assertEquals(1, ((Map<?, ?>) r.get("resumen")).get("inscritas"), "las clases no cuentan como inscripciones");
    }
}
