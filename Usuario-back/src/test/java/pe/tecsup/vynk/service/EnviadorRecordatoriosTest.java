package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import pe.tecsup.vynk.model.DispositivoMovil;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.model.RecordatorioActividad;
import pe.tecsup.vynk.repository.DispositivoMovilRepository;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.RecordatorioActividadRepository;
import pe.tecsup.vynk.service.NotificadorPush.Resultado;

class EnviadorRecordatoriosTest {
    // 2026-11-22 09:35 en Lima; la actividad empieza a las 10:00 (15:00 UTC).
    static final Instant AHORA = Instant.parse("2026-11-22T14:35:00Z");
    static final Instant INICIO = Instant.parse("2026-11-22T15:00:00Z");

    final RecordatorioActividadRepository recordatorios = mock(RecordatorioActividadRepository.class);
    final InscripcionRepository inscripciones = mock(InscripcionRepository.class);
    final DispositivoMovilRepository dispositivos = mock(DispositivoMovilRepository.class);
    final MongoTemplate mongo = mock(MongoTemplate.class);
    final NotificadorPush push = mock(NotificadorPush.class);
    final EnviadorRecordatorios enviador = new EnviadorRecordatorios(recordatorios, inscripciones, dispositivos, mongo, push,
            new NombresUbicacion(mongo));

    RecordatorioActividad recordatorio;
    Inscripcion inscripcion;

    @BeforeEach void preparar() {
        recordatorio = new RecordatorioActividad();
        recordatorio.setId("r1"); recordatorio.setInscripcionId("i1"); recordatorio.setUsuarioId("u1");
        recordatorio.setInicio(INICIO); recordatorio.setEnviarEn(INICIO.minusSeconds(1800));

        inscripcion = new Inscripcion();
        inscripcion.setId("i1"); inscripcion.setEstudianteId("u1"); inscripcion.setActividadId("act1");
        inscripcion.setNombreActividad("Torneo de fútbol"); inscripcion.setUbicacion("Cancha 2");
        inscripcion.setFecha("2026-11-22"); inscripcion.setHoraInicio("10:00"); inscripcion.setEstado("inscrita");

        when(push.disponible()).thenReturn(true);
        when(recordatorios.findTop200ByEstadoAndEnviarEnLessThanEqualOrderByEnviarEnAsc(
                RecordatorioActividad.PENDIENTE, AHORA)).thenReturn(List.of(recordatorio));
        when(mongo.findAndModify(any(Query.class), any(UpdateDefinition.class), any(FindAndModifyOptions.class),
                eq(RecordatorioActividad.class))).thenReturn(recordatorio);
        when(inscripciones.findById("i1")).thenReturn(Optional.of(inscripcion));
        when(dispositivos.findByUsuarioId("u1")).thenReturn(List.of(celular("tokenA")));
        when(push.enviar(anyString(), any())).thenReturn(Resultado.ENVIADO);
    }

    static DispositivoMovil celular(String token) {
        DispositivoMovil d = new DispositivoMovil();
        d.setId("d-" + token); d.setUsuarioId("u1"); d.setToken(token);
        return d;
    }

    /** Estados a los que el servicio pasó el aviso, en orden, leídos de las actualizaciones a Mongo. */
    List<String> estadosGuardados() {
        var cambios = ArgumentCaptor.forClass(Update.class);
        verify(mongo, atLeast(0)).updateFirst(any(Query.class), cambios.capture(), eq(RecordatorioActividad.class));
        List<String> estados = new ArrayList<>();
        for (Update u : cambios.getAllValues()) {
            Object set = u.getUpdateObject().get("$set");
            if (set instanceof org.bson.Document d && d.get("estado") != null) estados.add(d.getString("estado"));
        }
        return estados;
    }

    @Test void enviaElAvisoYLoMarcaComoEnviado() {
        assertEquals(1, enviador.enviarPendientes(AHORA));

        var datos = ArgumentCaptor.forClass(Map.class);
        verify(push).enviar(eq("tokenA"), datos.capture());
        Map<?, ?> enviado = datos.getValue();
        assertEquals("recordatorio_actividad", enviado.get("tipo"));
        assertEquals("act1", enviado.get("actividadId"));
        assertEquals("i1", enviado.get("inscripcionId"));
        assertEquals(List.of(RecordatorioActividad.ENVIADO), estadosGuardados());
    }

    @Test void siElCelularEsInvalidoLoOlvidaYDejaElAvisoPendiente() {
        when(push.enviar(eq("tokenA"), any())).thenReturn(Resultado.TOKEN_INVALIDO);

        assertEquals(0, enviador.enviarPendientes(AHORA));

        verify(dispositivos).deleteByToken("tokenA");
        assertEquals(List.of(RecordatorioActividad.PENDIENTE), estadosGuardados());
    }

    @Test void conVariosCelularesBastaConQueUnoRecibaElAviso() {
        when(dispositivos.findByUsuarioId("u1")).thenReturn(List.of(celular("tokenA"), celular("tokenB")));
        when(push.enviar(eq("tokenA"), any())).thenReturn(Resultado.ERROR_TEMPORAL);
        when(push.enviar(eq("tokenB"), any())).thenReturn(Resultado.ENVIADO);

        assertEquals(1, enviador.enviarPendientes(AHORA));
        assertEquals(List.of(RecordatorioActividad.ENVIADO), estadosGuardados());
    }

    @Test void unFalloTemporalReintentaYTrasTresIntentosSeDaPorFallido() {
        when(push.enviar(anyString(), any())).thenReturn(Resultado.ERROR_TEMPORAL);

        recordatorio.setIntentos(0);
        assertEquals(0, enviador.enviarPendientes(AHORA));
        assertEquals(List.of(RecordatorioActividad.PENDIENTE), estadosGuardados());

        clearInvocations(mongo);
        recordatorio.setIntentos(EnviadorRecordatorios.MAX_INTENTOS - 1);
        assertEquals(0, enviador.enviarPendientes(AHORA));
        assertEquals(List.of(RecordatorioActividad.FALLIDO), estadosGuardados());
    }

    @Test void siLaInscripcionSeCanceloNoEnviaNada() {
        inscripcion.setEstado("cancelada");

        assertEquals(0, enviador.enviarPendientes(AHORA));

        verify(push, never()).enviar(anyString(), any());
        assertEquals(List.of(RecordatorioActividad.CANCELADO), estadosGuardados());
    }

    @Test void siLaInscripcionYaNoExisteNoEnviaNada() {
        when(inscripciones.findById("i1")).thenReturn(Optional.empty());

        assertEquals(0, enviador.enviarPendientes(AHORA));
        verify(push, never()).enviar(anyString(), any());
        assertEquals(List.of(RecordatorioActividad.CANCELADO), estadosGuardados());
    }

    @Test void siLaActividadCambioDeHoraNoEnviaLaHoraVieja() {
        inscripcion.setHoraInicio("11:00");

        assertEquals(0, enviador.enviarPendientes(AHORA));

        verify(push, never()).enviar(anyString(), any());
        assertEquals(List.of(RecordatorioActividad.PENDIENTE), estadosGuardados());
    }

    @Test void sinCelularRegistradoEsperaYNoGastaIntentos() {
        when(dispositivos.findByUsuarioId("u1")).thenReturn(List.of());

        assertEquals(0, enviador.enviarPendientes(AHORA));

        verify(push, never()).enviar(anyString(), any());
        assertEquals(List.of(RecordatorioActividad.PENDIENTE), estadosGuardados());
    }

    @Test void siLaActividadYaEmpezoElAvisoVence() {
        recordatorio.setInicio(AHORA.minusSeconds(60));

        assertEquals(0, enviador.enviarPendientes(AHORA));

        verify(push, never()).enviar(anyString(), any());
        verify(mongo, never()).findAndModify(any(Query.class), any(UpdateDefinition.class),
                any(FindAndModifyOptions.class), eq(RecordatorioActividad.class));
        assertEquals(List.of(RecordatorioActividad.VENCIDO), estadosGuardados());
    }

    @Test void siOtraEjecucionYaTomoElAvisoNoLoEnviaDeNuevo() {
        when(mongo.findAndModify(any(Query.class), any(UpdateDefinition.class), any(FindAndModifyOptions.class),
                eq(RecordatorioActividad.class))).thenReturn(null);

        assertEquals(0, enviador.enviarPendientes(AHORA));
        verify(push, never()).enviar(anyString(), any());
    }

    @Test void sinCredencialesDeFirebaseNoTocaNingunAviso() {
        when(push.disponible()).thenReturn(false);

        assertEquals(0, enviador.enviarPendientes(AHORA));

        verifyNoInteractions(recordatorios, dispositivos, mongo);
        verify(push, never()).enviar(anyString(), any());
    }

    @Test void unErrorInesperadoNoImpideProcesarLosDemasAvisos() {
        RecordatorioActividad segundo = new RecordatorioActividad();
        segundo.setId("r2"); segundo.setInscripcionId("i2"); segundo.setUsuarioId("u1");
        segundo.setInicio(INICIO);
        when(recordatorios.findTop200ByEstadoAndEnviarEnLessThanEqualOrderByEnviarEnAsc(
                RecordatorioActividad.PENDIENTE, AHORA)).thenReturn(List.of(segundo, recordatorio));
        when(mongo.findAndModify(any(Query.class), any(UpdateDefinition.class), any(FindAndModifyOptions.class),
                eq(RecordatorioActividad.class))).thenReturn(segundo, recordatorio);
        when(inscripciones.findById("i2")).thenThrow(new IllegalStateException("base de datos caída"));

        assertEquals(1, enviador.enviarPendientes(AHORA));
        verify(push, times(1)).enviar(eq("tokenA"), any());
    }

    @Test void elTextoDelAvisoIndicaActividadMinutosHoraYLugar() {
        Map<String, String> datos = MensajeRecordatorio.datos(inscripcion, inscripcion.getUbicacion(), INICIO, AHORA);

        assertEquals("Tu actividad empieza pronto", datos.get("titulo"));
        assertEquals("Torneo de fútbol empieza en 25 min (10:00) · Cancha 2", datos.get("cuerpo"));
        assertEquals("2026-11-22T15:00:00Z", datos.get("inicio"));
    }

    @Test void elTextoNuncaDiceCeroMinutosYAguantaDatosFaltantes() {
        inscripcion.setNombreActividad(null);
        inscripcion.setUbicacion("  ");

        Map<String, String> datos = MensajeRecordatorio.datos(inscripcion, inscripcion.getUbicacion(), INICIO, INICIO.minusSeconds(10));

        assertEquals("Tu actividad empieza en 1 min (10:00)", datos.get("cuerpo"));
    }

    @Test void recortaNombresLargosParaNoPasarseDelLimiteDeFcm() {
        inscripcion.setNombreActividad("A".repeat(500));

        String cuerpo = MensajeRecordatorio.datos(inscripcion, inscripcion.getUbicacion(), INICIO, AHORA).get("cuerpo");

        assertTrue(cuerpo.length() < 160, cuerpo);
        assertTrue(cuerpo.contains("…"));
    }
}
