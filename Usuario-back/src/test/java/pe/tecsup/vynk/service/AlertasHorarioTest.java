package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
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
import pe.tecsup.vynk.model.AlertaHorario;
import pe.tecsup.vynk.model.DispositivoMovil;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.repository.AlertaHorarioRepository;
import pe.tecsup.vynk.repository.DispositivoMovilRepository;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.service.NotificadorPush.Resultado;

class AlertasHorarioTest {
    static final Instant AHORA = Instant.parse("2026-11-22T13:00:00Z"); // 08:00 en Lima

    // ---------- Horario ----------

    @Test void nullYVacioSonElMismoHorario() {
        assertEquals(new Horario("2026-11-22", "10:00", ""), Horario.de("2026-11-22", "10:00", null));
        assertEquals(new Horario("2026-11-22", "10:00", ""), new Horario(" 2026-11-22 ", "10:00", "null"));
        assertNotEquals(new Horario("2026-11-22", "10:00", ""), new Horario("2026-11-22", "10:30", ""));
    }

    @Test void calculaElComienzoEnHoraDePeru() {
        assertEquals(Instant.parse("2026-11-22T15:00:00Z"), new Horario("2026-11-22", "10:00", "11:00").comienzo());
        assertNull(new Horario("", "10:00", "").comienzo());
        assertNull(new Horario("2026-11-22", "", "").comienzo());
        assertFalse(new Horario("22/11/2026", "10:00", "").completo());
    }

    @Test void muestraElHorarioConDiaFechaYHoras() {
        assertEquals("sáb 21/11 · 10:00–11:30", new Horario("2026-11-21", "10:00", "11:30").paraMostrar());
        assertEquals("dom 22/11 · 09:00", new Horario("2026-11-22", "09:00", "").paraMostrar());
        assertEquals("Fecha por confirmar · hora por confirmar", new Horario("", "", "").paraMostrar());
    }

    // ---------- Mensaje ----------

    static AlertaHorario alerta() {
        AlertaHorario a = new AlertaHorario();
        a.setId("al1"); a.setInscripcionId("i1"); a.setUsuarioId("u1"); a.setActividadId("act1");
        a.setNombreActividad("Torneo de fútbol");
        a.setFechaAnterior("2026-11-22"); a.setInicioAnterior("10:00"); a.setFinAnterior("11:00");
        a.setFechaNueva("2026-11-23"); a.setInicioNuevo("15:00"); a.setFinNuevo("16:30");
        a.setComienzaEn(Instant.parse("2026-11-23T20:00:00Z"));
        a.setCreadaEn(AHORA);
        return a;
    }

    @Test void elMensajeLlevaElHorarioAnteriorYElNuevo() {
        Map<String, String> d = MensajeCambioHorario.datos(alerta());
        assertEquals("cambio_horario", d.get("tipo"));
        assertEquals("act1", d.get("actividadId"));
        assertEquals("Cambió el horario de Torneo de fútbol", d.get("titulo"));
        assertEquals("dom 22/11 · 10:00–11:00", d.get("horarioAnterior"));
        assertEquals("lun 23/11 · 15:00–16:30", d.get("horarioNuevo"));
        assertEquals("Antes: dom 22/11 · 10:00–11:00\nAhora: lun 23/11 · 15:00–16:30", d.get("cuerpo"));
    }

    @Test void elMensajeAguantaNombresVaciosOLargos() {
        AlertaHorario a = alerta();
        a.setNombreActividad(null);
        assertEquals("Cambió el horario de tu actividad", MensajeCambioHorario.datos(a).get("titulo"));
        a.setNombreActividad("X".repeat(300));
        assertTrue(MensajeCambioHorario.datos(a).get("titulo").length() < 110);
    }

    // ---------- Envío ----------

    final AlertaHorarioRepository alertas = mock(AlertaHorarioRepository.class);
    final InscripcionRepository inscripciones = mock(InscripcionRepository.class);
    final DispositivoMovilRepository dispositivos = mock(DispositivoMovilRepository.class);
    final MongoTemplate mongo = mock(MongoTemplate.class);
    final NotificadorPush push = mock(NotificadorPush.class);
    final EnviadorAlertasHorario enviador = new EnviadorAlertasHorario(alertas, inscripciones, dispositivos, mongo, push);
    AlertaHorario pendiente;
    Inscripcion inscripcion;

    @BeforeEach void preparar() {
        pendiente = alerta();
        inscripcion = new Inscripcion();
        inscripcion.setId("i1"); inscripcion.setEstudianteId("u1"); inscripcion.setEstado("inscrita");
        inscripcion.setFecha("2026-11-23"); inscripcion.setHoraInicio("15:00"); inscripcion.setHoraFin("16:30");
        DispositivoMovil d = new DispositivoMovil();
        d.setUsuarioId("u1"); d.setToken("tokenA");
        when(push.disponible()).thenReturn(true);
        when(alertas.findTop200ByEstadoOrderByCreadaEnAsc(AlertaHorario.PENDIENTE)).thenReturn(List.of(pendiente));
        when(mongo.findAndModify(any(Query.class), any(UpdateDefinition.class), any(FindAndModifyOptions.class),
                eq(AlertaHorario.class))).thenReturn(pendiente);
        when(inscripciones.findById("i1")).thenReturn(Optional.of(inscripcion));
        when(dispositivos.findByUsuarioId("u1")).thenReturn(List.of(d));
        when(push.enviar(anyString(), any(), any())).thenReturn(Resultado.ENVIADO);
    }

    String ultimoEstado() {
        var cambios = ArgumentCaptor.forClass(Update.class);
        verify(mongo, atLeastOnce()).updateFirst(any(Query.class), cambios.capture(), eq(AlertaHorario.class));
        var set = (org.bson.Document) cambios.getValue().getUpdateObject().get("$set");
        return set.getString("estado");
    }

    @Test void enviaLaAlertaYLaGuardaHastaQueEmpieceLaActividad() {
        assertEquals(1, enviador.enviarPendientes(AHORA));
        var vida = ArgumentCaptor.forClass(Duration.class);
        verify(push).enviar(eq("tokenA"), any(), vida.capture());
        assertEquals(Duration.between(AHORA, pendiente.getComienzaEn()), vida.getValue());
        assertEquals(AlertaHorario.ENVIADA, ultimoEstado());
    }

    @Test void siFaltaPocoParaEmpezarFcmLaGuardaAlMenosUnaHora() {
        pendiente.setComienzaEn(AHORA.plusSeconds(600));
        enviador.enviarPendientes(AHORA);
        verify(push).enviar(eq("tokenA"), any(), eq(Duration.ofHours(1)));
    }

    @Test void siElEstudianteCanceloNoSeEnvia() {
        inscripcion.setEstado("cancelada");
        assertEquals(0, enviador.enviarPendientes(AHORA));
        verify(push, never()).enviar(anyString(), any(), any());
        assertEquals(AlertaHorario.CANCELADA, ultimoEstado());
    }

    @Test void siElHorarioVolvioACambiarEstaAlertaSeDescarta() {
        inscripcion.setHoraInicio("17:00");
        assertEquals(0, enviador.enviarPendientes(AHORA));
        verify(push, never()).enviar(anyString(), any(), any());
        assertEquals(AlertaHorario.CANCELADA, ultimoEstado());
    }

    @Test void siLaActividadYaEmpezoLaAlertaVence() {
        pendiente.setComienzaEn(AHORA.minusSeconds(1));
        assertEquals(0, enviador.enviarPendientes(AHORA));
        verify(push, never()).enviar(anyString(), any(), any());
        assertEquals(AlertaHorario.VENCIDA, ultimoEstado());
    }

    @Test void sinCelularEsperaSinGastarIntentos() {
        when(dispositivos.findByUsuarioId("u1")).thenReturn(List.of());
        assertEquals(0, enviador.enviarPendientes(AHORA));
        assertEquals(AlertaHorario.PENDIENTE, ultimoEstado());
    }

    @Test void trasTresFallosSeDaPorFallida() {
        when(push.enviar(anyString(), any(), any())).thenReturn(Resultado.ERROR_TEMPORAL);
        pendiente.setIntentos(EnviadorAlertasHorario.MAX_INTENTOS - 1);
        assertEquals(0, enviador.enviarPendientes(AHORA));
        assertEquals(AlertaHorario.FALLIDA, ultimoEstado());
    }

    @Test void unCelularInvalidoSeOlvida() {
        when(push.enviar(anyString(), any(), any())).thenReturn(Resultado.TOKEN_INVALIDO);
        assertEquals(0, enviador.enviarPendientes(AHORA));
        verify(dispositivos).deleteByToken("tokenA");
        assertEquals(AlertaHorario.PENDIENTE, ultimoEstado());
    }

    @Test void sinCredencialesDeFirebaseNoTocaNada() {
        when(push.disponible()).thenReturn(false);
        assertEquals(0, enviador.enviarPendientes(AHORA));
        verifyNoInteractions(alertas, mongo, dispositivos);
    }
}
