package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import pe.tecsup.vynk.model.AvisoPush;
import pe.tecsup.vynk.model.DisponibilidadSemanal.BloqueOcupado;
import pe.tecsup.vynk.model.DispositivoMovil;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.repository.AvisoPushRepository;
import pe.tecsup.vynk.repository.DispositivoMovilRepository;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.service.NotificadorPush.Resultado;

class CancelacionYRecomendacionTest {
    // Domingo 22/11/2026, 10:00 en Lima.
    static final Instant AHORA = Instant.parse("2026-11-22T15:00:00Z");

    // ---------- Mensaje de cancelación ----------

    @Test void laCancelacionDiceQueActividadCuandoYDondeYQueSalioDeLaAgenda() {
        Map<String, String> d = MensajeCancelacion.datos("act1", "Torneo de fútbol",
                new Horario("2026-11-23", "10:00", "11:00"), "Cancha 2");
        assertEquals("cancelacion_actividad", d.get("tipo"));
        assertEquals("act1", d.get("actividadId"));
        assertEquals("Se canceló Torneo de fútbol", d.get("titulo"));
        assertEquals("lun 23/11 · 10:00–11:00 · Cancha 2", d.get("detalle"));
        assertEquals("La actividad del lun 23/11 · 10:00–11:00 · Cancha 2 fue cancelada. Ya no está en tu agenda.", d.get("cuerpo"));
    }

    @Test void laCancelacionAguantaDatosFaltantes() {
        Map<String, String> d = MensajeCancelacion.datos("act1", null, new Horario("", "", ""), NombresUbicacion.POR_CONFIRMAR);
        assertEquals("Se canceló Tu actividad", d.get("titulo"));
        assertEquals("La actividad fue cancelada. Ya no está en tu agenda.", d.get("cuerpo"));
    }

    // ---------- Elección de recomendaciones ----------

    static Map<String, Object> actividad(String id, String nombre, String fecha, String inicio, String fin) {
        Map<String, Object> a = new HashMap<>();
        a.put("id", id); a.put("nombre", nombre); a.put("descripcion", ""); a.put("categoriaId", "");
        a.put("estado", "programada"); a.put("fecha", fecha); a.put("horaInicio", inicio); a.put("horaFin", fin);
        a.put("cuposDisponibles", 10); a.put("ubicacion", "Auditorio principal");
        return a;
    }

    static Inscripcion inscrita(String actividadId, String fecha, String inicio, String fin) {
        Inscripcion i = new Inscripcion();
        i.setActividadId(actividadId); i.setFecha(fecha); i.setHoraInicio(inicio); i.setHoraFin(fin); i.setEstado("inscrita");
        return i;
    }

    Optional<Map<String, Object>> elegir(List<Map<String, Object>> publicadas, List<Inscripcion> inscritas,
                                         Set<String> descartadas, Set<String> notificadas, List<BloqueOcupado> clases) {
        return SelectorRecomendaciones.elegirPara(publicadas, List.of("tecnologia", "deportes"), inscritas,
                descartadas, notificadas, clases, AHORA);
    }

    @Test void eligeLaActividadMasRelacionadaConSusIntereses() {
        var poco = actividad("a1", "Charla de tecnologia", "2026-11-23", "10:00", "11:00");
        var mucho = actividad("a2", "Torneo de deportes y tecnologia", "2026-11-24", "10:00", "11:00");
        var nada = actividad("a3", "Recital de poesía", "2026-11-23", "12:00", "13:00");
        assertEquals("a2", elegir(List.of(poco, mucho, nada), List.of(), Set.of(), Set.of(), List.of()).orElseThrow().get("id"));
    }

    @Test void aIgualRelevanciaEligeLaMasProxima() {
        var despues = actividad("a1", "Charla de tecnologia", "2026-11-25", "10:00", "11:00");
        var antes = actividad("a2", "Taller de tecnologia", "2026-11-23", "10:00", "11:00");
        assertEquals("a2", elegir(List.of(despues, antes), List.of(), Set.of(), Set.of(), List.of()).orElseThrow().get("id"));
    }

    @Test void sinRelacionConSusInteresesNoRecomiendaNada() {
        var a = actividad("a1", "Recital de poesía", "2026-11-23", "10:00", "11:00");
        assertTrue(elegir(List.of(a), List.of(), Set.of(), Set.of(), List.of()).isEmpty());
    }

    @Test void noRecomiendaInscritasDescartadasNiYaNotificadas() {
        var a1 = actividad("a1", "Charla de tecnologia", "2026-11-23", "10:00", "11:00");
        var a2 = actividad("a2", "Taller de tecnologia", "2026-11-23", "12:00", "13:00");
        var a3 = actividad("a3", "Feria de tecnologia", "2026-11-23", "15:00", "16:00");
        assertTrue(elegir(List.of(a1, a2, a3), List.of(inscrita("a1", "2026-11-24", "08:00", "09:00")),
                Set.of("a2"), Set.of("a3"), List.of()).isEmpty());
    }

    @Test void soloRecomiendaEntreDosHorasYSieteDiasDesdeAhora() {
        var prontisimo = actividad("a1", "Charla de tecnologia", "2026-11-22", "11:30", "12:30"); // en 1,5 h
        var lejana = actividad("a2", "Taller de tecnologia", "2026-12-05", "10:00", "11:00");
        var bien = actividad("a3", "Feria de tecnologia", "2026-11-22", "12:30", "13:30"); // en 2,5 h
        assertEquals("a3", elegir(List.of(prontisimo, lejana, bien), List.of(), Set.of(), Set.of(), List.of()).orElseThrow().get("id"));
    }

    @Test void noRecomiendaLlenasCanceladasNiSinHorario() {
        var llena = actividad("a1", "Charla de tecnologia", "2026-11-23", "10:00", "11:00");
        llena.put("cuposDisponibles", 0);
        var cancelada = actividad("a2", "Taller de tecnologia", "2026-11-23", "12:00", "13:00");
        cancelada.put("estado", "cancelada");
        var sinHora = actividad("a3", "Feria de tecnologia", "2026-11-23", "", "");
        assertTrue(elegir(List.of(llena, cancelada, sinHora), List.of(), Set.of(), Set.of(), List.of()).isEmpty());
    }

    @Test void noRecomiendaLoQueSeCruzaConSuAgendaOConSusClases() {
        var cruceAgenda = actividad("a1", "Charla de tecnologia", "2026-11-23", "10:00", "11:00");
        var cruceClase = actividad("a2", "Taller de tecnologia", "2026-11-23", "14:00", "15:00");
        var libre = actividad("a3", "Feria de tecnologia", "2026-11-23", "17:00", "18:00");
        // 23/11/2026 es lunes (día 1).
        var clases = List.of(new BloqueOcupado(1, "13:00", "16:00", "Matemática"));
        var agenda = List.of(inscrita("otra", "2026-11-23", "09:30", "10:30"));
        assertEquals("a3", elegir(List.of(cruceAgenda, cruceClase, libre), agenda, Set.of(), Set.of(), clases).orElseThrow().get("id"));
    }

    @Test void soloSeNotificaEntreLasNueveYLasOchoDeLaNoche() {
        assertFalse(SelectorRecomendaciones.enHorario(Instant.parse("2026-11-22T13:59:00Z"))); // 08:59
        assertTrue(SelectorRecomendaciones.enHorario(Instant.parse("2026-11-22T14:00:00Z")));  // 09:00
        assertTrue(SelectorRecomendaciones.enHorario(Instant.parse("2026-11-23T00:59:00Z")));  // 19:59
        assertFalse(SelectorRecomendaciones.enHorario(Instant.parse("2026-11-23T01:00:00Z"))); // 20:00
    }

    @Test void elMotivoUsaElNombreLegibleDelInteres() {
        var a = actividad("a1", "Torneo de deportes", "2026-11-23", "10:00", "11:00");
        assertEquals("Porque te interesa Deportes", SelectorRecomendaciones.motivo(a, List.of("tecnologia", "deportes")));
    }

    @Test void laRecomendacionMuestraCuandoDondePorQueYQueHayCupos() {
        var a = actividad("a1", "Torneo de deportes", "2026-11-23", "10:00", "11:00");
        Map<String, String> d = MensajeRecomendacion.datos(a, "Porque te interesa Deportes");
        assertEquals("recomendacion_actividad", d.get("tipo"));
        assertEquals("a1", d.get("actividadId"));
        assertEquals("Te puede interesar: Torneo de deportes", d.get("titulo"));
        assertEquals("lun 23/11 · 10:00–11:00 · Auditorio principal\nPorque te interesa Deportes\nHay cupos disponibles.", d.get("cuerpo"));
    }

    // ---------- Envío ----------

    final AvisoPushRepository avisos = mock(AvisoPushRepository.class);
    final InscripcionRepository inscripciones = mock(InscripcionRepository.class);
    final DispositivoMovilRepository dispositivos = mock(DispositivoMovilRepository.class);
    final PreferenciasNotificacionService preferencias = mock(PreferenciasNotificacionService.class);
    final MongoTemplate mongo = mock(MongoTemplate.class);
    final NotificadorPush push = mock(NotificadorPush.class);
    final EnviadorAvisosPush enviador = new EnviadorAvisosPush(avisos, inscripciones, dispositivos, preferencias, mongo, push);
    AvisoPush aviso;

    @BeforeEach void preparar() {
        aviso = new AvisoPush();
        aviso.setId("p1"); aviso.setTipo(AvisoPush.RECOMENDACION); aviso.setUsuarioId("u1"); aviso.setActividadId("act1");
        aviso.setDatos(Map.of("tipo", AvisoPush.RECOMENDACION, "actividadId", "act1"));
        aviso.setExpiraEn(AHORA.plusSeconds(86400)); aviso.setCreadoEn(AHORA);
        DispositivoMovil d = new DispositivoMovil();
        d.setUsuarioId("u1"); d.setToken("tokenA");
        when(push.disponible()).thenReturn(true);
        when(avisos.findTop200ByEstadoOrderByCreadoEnAsc(AvisoPush.PENDIENTE)).thenReturn(List.of(aviso));
        when(mongo.findAndModify(any(Query.class), any(UpdateDefinition.class), any(FindAndModifyOptions.class),
                eq(AvisoPush.class))).thenReturn(aviso);
        when(dispositivos.findByUsuarioId("u1")).thenReturn(List.of(d));
        when(push.enviar(anyString(), any(), any())).thenReturn(Resultado.ENVIADO);
        when(preferencias.recomendacionesActivas("u1")).thenReturn(true);
        when(inscripciones.findByEstudianteIdAndActividadId("u1", "act1")).thenReturn(Optional.empty());
        when(mongo.findOne(any(Query.class), eq(Document.class), eq("actividades")))
                .thenReturn(new Document("_id", "act1").append("estado", "programada")
                        .append("maximoParticipantes", 10).append("participantesInscritos", 3));
    }

    List<String> estados() {
        var cambios = ArgumentCaptor.forClass(Update.class);
        verify(mongo, atLeastOnce()).updateFirst(any(Query.class), cambios.capture(), eq(AvisoPush.class));
        List<String> lista = new ArrayList<>();
        for (Update u : cambios.getAllValues()) lista.add(((Document) u.getUpdateObject().get("$set")).getString("estado"));
        return lista;
    }

    @Test void enviaLaRecomendacionSiSigueVigente() {
        assertEquals(1, enviador.enviarPendientes(AHORA));
        verify(push).enviar(eq("tokenA"), eq(aviso.getDatos()), any());
        assertEquals(List.of(AvisoPush.ENVIADO), estados());
    }

    @Test void siApagoLasRecomendacionesNoSeEnvia() {
        when(preferencias.recomendacionesActivas("u1")).thenReturn(false);
        assertEquals(0, enviador.enviarPendientes(AHORA));
        verify(push, never()).enviar(anyString(), any(), any());
        assertEquals(List.of(AvisoPush.DESCARTADO), estados());
    }

    @Test void siYaSeInscribioNoSeEnvia() {
        Inscripcion i = new Inscripcion(); i.setEstado("inscrita");
        when(inscripciones.findByEstudianteIdAndActividadId("u1", "act1")).thenReturn(Optional.of(i));
        assertEquals(0, enviador.enviarPendientes(AHORA));
        assertEquals(List.of(AvisoPush.DESCARTADO), estados());
    }

    @Test void siLaActividadSeLlenoOSeCanceloNoSeEnvia() {
        when(mongo.findOne(any(Query.class), eq(Document.class), eq("actividades")))
                .thenReturn(new Document("estado", "programada").append("maximoParticipantes", 5).append("participantesInscritos", 5));
        assertEquals(0, enviador.enviarPendientes(AHORA));
        when(mongo.findOne(any(Query.class), eq(Document.class), eq("actividades")))
                .thenReturn(new Document("estado", "cancelada"));
        assertEquals(0, enviador.enviarPendientes(AHORA));
        verify(push, never()).enviar(anyString(), any(), any());
    }

    @Test void laCancelacionSeEnviaSinMasComprobaciones() {
        aviso.setTipo(AvisoPush.CANCELACION);
        assertEquals(1, enviador.enviarPendientes(AHORA));
        verifyNoInteractions(preferencias);
        assertEquals(List.of(AvisoPush.ENVIADO), estados());
    }

    @Test void unAvisoVencidoNoSeEnvia() {
        aviso.setExpiraEn(AHORA);
        assertEquals(0, enviador.enviarPendientes(AHORA));
        verify(push, never()).enviar(anyString(), any(), any());
        assertEquals(List.of(AvisoPush.VENCIDO), estados());
    }

    @Test void sinCelularEsperaYTrasTresFallosSeDaPorFallido() {
        when(dispositivos.findByUsuarioId("u1")).thenReturn(List.of());
        enviador.enviarPendientes(AHORA);
        assertEquals(List.of(AvisoPush.PENDIENTE), estados());

        clearInvocations(mongo);
        DispositivoMovil d = new DispositivoMovil(); d.setToken("tokenA");
        when(dispositivos.findByUsuarioId("u1")).thenReturn(List.of(d));
        when(push.enviar(anyString(), any(), any())).thenReturn(Resultado.ERROR_TEMPORAL);
        aviso.setIntentos(EnviadorAvisosPush.MAX_INTENTOS - 1);
        enviador.enviarPendientes(AHORA);
        assertEquals(List.of(AvisoPush.FALLIDO), estados());
    }
}
