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
import pe.tecsup.vynk.model.AlertaUbicacion;
import pe.tecsup.vynk.model.DispositivoMovil;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.repository.AlertaUbicacionRepository;
import pe.tecsup.vynk.repository.DispositivoMovilRepository;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.service.NotificadorPush.Resultado;

class AlertasUbicacionTest {
    static final Instant AHORA = Instant.parse("2026-11-22T13:00:00Z");
    static final String CANCHA = "6730f1c2a1b2c3d4e5f60001";
    static final String AUDITORIO = "6730f1c2a1b2c3d4e5f60002";

    // ---------- Nombres de ubicación ----------

    @Test void traduceElIdentificadorAlNombre() {
        Map<String, String> nombres = Map.of(CANCHA, "Cancha 2");
        assertEquals("Cancha 2", NombresUbicacion.nombre(nombres, CANCHA));
        assertEquals("Cancha 2", NombresUbicacion.nombre(nombres, " " + CANCHA + " "));
    }

    @Test void unIdentificadorQueNoExisteOVacioQuedaPorConfirmar() {
        assertEquals(NombresUbicacion.POR_CONFIRMAR, NombresUbicacion.nombre(Map.of(), AUDITORIO));
        assertEquals(NombresUbicacion.POR_CONFIRMAR, NombresUbicacion.nombre(Map.of(), ""));
        assertEquals(NombresUbicacion.POR_CONFIRMAR, NombresUbicacion.nombre(Map.of(), null));
        assertEquals(NombresUbicacion.POR_CONFIRMAR, NombresUbicacion.nombre(Map.of(), "null"));
    }

    @Test void unTextoQueNoEsIdentificadorSeMuestraTalCual() {
        assertEquals("Laboratorio 3", NombresUbicacion.nombre(Map.of(), "Laboratorio 3"));
    }

    @Test void elRecordatorioMuestraElNombreDelLugarYNoSuIdentificador() {
        Inscripcion i = new Inscripcion();
        i.setId("i1"); i.setActividadId("act1"); i.setNombreActividad("Torneo"); i.setUbicacion(CANCHA);
        Instant inicio = Instant.parse("2026-11-22T15:00:00Z");
        assertEquals("Torneo empieza en 25 min (10:00) · Cancha 2",
                MensajeRecordatorio.datos(i, "Cancha 2", inicio, inicio.minusSeconds(1500)).get("cuerpo"));
        assertEquals("Torneo empieza en 25 min (10:00)",
                MensajeRecordatorio.datos(i, NombresUbicacion.POR_CONFIRMAR, inicio, inicio.minusSeconds(1500)).get("cuerpo"));
    }

    // ---------- Mensaje ----------

    static AlertaUbicacion alerta() {
        AlertaUbicacion a = new AlertaUbicacion();
        a.setId("al1"); a.setInscripcionId("i1"); a.setUsuarioId("u1"); a.setActividadId("act1");
        a.setNombreActividad("Torneo de fútbol");
        a.setUbicacionAnteriorId(CANCHA); a.setUbicacionAnterior("Cancha 2");
        a.setUbicacionNuevaId(AUDITORIO); a.setUbicacionNueva("Auditorio principal");
        a.setComienzaEn(Instant.parse("2026-11-23T20:00:00Z"));
        a.setCreadaEn(AHORA);
        return a;
    }

    @Test void elMensajeLlevaLaUbicacionAnteriorYLaNueva() {
        Map<String, String> d = MensajeCambioUbicacion.datos(alerta());
        assertEquals("cambio_ubicacion", d.get("tipo"));
        assertEquals("act1", d.get("actividadId"));
        assertEquals("Cambió el lugar de Torneo de fútbol", d.get("titulo"));
        assertEquals("Cancha 2", d.get("ubicacionAnterior"));
        assertEquals("Auditorio principal", d.get("ubicacionNueva"));
        assertEquals("Antes: Cancha 2\nAhora: Auditorio principal", d.get("cuerpo"));
    }

    @Test void elMensajeAguantaDatosFaltantesOLargos() {
        AlertaUbicacion a = alerta();
        a.setNombreActividad(null); a.setUbicacionAnterior(""); a.setUbicacionNueva("A".repeat(300));
        Map<String, String> d = MensajeCambioUbicacion.datos(a);
        assertEquals("Cambió el lugar de tu actividad", d.get("titulo"));
        assertEquals(NombresUbicacion.POR_CONFIRMAR, d.get("ubicacionAnterior"));
        assertEquals(60, d.get("ubicacionNueva").length());
    }

    // ---------- Envío ----------

    final AlertaUbicacionRepository alertas = mock(AlertaUbicacionRepository.class);
    final InscripcionRepository inscripciones = mock(InscripcionRepository.class);
    final DispositivoMovilRepository dispositivos = mock(DispositivoMovilRepository.class);
    final MongoTemplate mongo = mock(MongoTemplate.class);
    final NotificadorPush push = mock(NotificadorPush.class);
    final EnviadorAlertasUbicacion enviador = new EnviadorAlertasUbicacion(alertas, inscripciones, dispositivos, mongo, push);
    AlertaUbicacion pendiente;
    Inscripcion inscripcion;

    @BeforeEach void preparar() {
        pendiente = alerta();
        inscripcion = new Inscripcion();
        inscripcion.setId("i1"); inscripcion.setEstudianteId("u1"); inscripcion.setEstado("inscrita");
        inscripcion.setUbicacion(AUDITORIO);
        DispositivoMovil d = new DispositivoMovil();
        d.setUsuarioId("u1"); d.setToken("tokenA");
        when(push.disponible()).thenReturn(true);
        when(alertas.findTop200ByEstadoOrderByCreadaEnAsc(AlertaUbicacion.PENDIENTE)).thenReturn(List.of(pendiente));
        when(mongo.findAndModify(any(Query.class), any(UpdateDefinition.class), any(FindAndModifyOptions.class),
                eq(AlertaUbicacion.class))).thenReturn(pendiente);
        when(inscripciones.findById("i1")).thenReturn(Optional.of(inscripcion));
        when(dispositivos.findByUsuarioId("u1")).thenReturn(List.of(d));
        when(push.enviar(anyString(), any(), any())).thenReturn(Resultado.ENVIADO);
    }

    String ultimoEstado() {
        var cambios = ArgumentCaptor.forClass(Update.class);
        verify(mongo, atLeastOnce()).updateFirst(any(Query.class), cambios.capture(), eq(AlertaUbicacion.class));
        var set = (org.bson.Document) cambios.getValue().getUpdateObject().get("$set");
        return set.getString("estado");
    }

    @Test void enviaLaAlertaYLaGuardaHastaQueEmpieceLaActividad() {
        assertEquals(1, enviador.enviarPendientes(AHORA));
        verify(push).enviar(eq("tokenA"), any(), eq(Duration.between(AHORA, pendiente.getComienzaEn())));
        assertEquals(AlertaUbicacion.ENVIADA, ultimoEstado());
    }

    @Test void unaActividadSinFechaNoVenceYFcmLaGuardaUnaSemana() {
        pendiente.setComienzaEn(null);
        assertEquals(1, enviador.enviarPendientes(AHORA));
        verify(push).enviar(eq("tokenA"), any(), eq(Duration.ofDays(7)));
    }

    @Test void siElEstudianteCanceloNoSeEnvia() {
        inscripcion.setEstado("cancelada");
        assertEquals(0, enviador.enviarPendientes(AHORA));
        verify(push, never()).enviar(anyString(), any(), any());
        assertEquals(AlertaUbicacion.CANCELADA, ultimoEstado());
    }

    @Test void siElLugarVolvioACambiarEstaAlertaSeDescarta() {
        inscripcion.setUbicacion(CANCHA);
        assertEquals(0, enviador.enviarPendientes(AHORA));
        verify(push, never()).enviar(anyString(), any(), any());
        assertEquals(AlertaUbicacion.CANCELADA, ultimoEstado());
    }

    @Test void siLaActividadYaEmpezoLaAlertaVence() {
        pendiente.setComienzaEn(AHORA.minusSeconds(1));
        assertEquals(0, enviador.enviarPendientes(AHORA));
        verify(push, never()).enviar(anyString(), any(), any());
        assertEquals(AlertaUbicacion.VENCIDA, ultimoEstado());
    }

    @Test void sinCelularEsperaSinGastarIntentos() {
        when(dispositivos.findByUsuarioId("u1")).thenReturn(List.of());
        assertEquals(0, enviador.enviarPendientes(AHORA));
        assertEquals(AlertaUbicacion.PENDIENTE, ultimoEstado());
    }

    @Test void trasTresFallosSeDaPorFallida() {
        when(push.enviar(anyString(), any(), any())).thenReturn(Resultado.ERROR_TEMPORAL);
        pendiente.setIntentos(EnviadorAlertasUbicacion.MAX_INTENTOS - 1);
        assertEquals(0, enviador.enviarPendientes(AHORA));
        assertEquals(AlertaUbicacion.FALLIDA, ultimoEstado());
    }

    @Test void unCelularInvalidoSeOlvida() {
        when(push.enviar(anyString(), any(), any())).thenReturn(Resultado.TOKEN_INVALIDO);
        assertEquals(0, enviador.enviarPendientes(AHORA));
        verify(dispositivos).deleteByToken("tokenA");
        assertEquals(AlertaUbicacion.PENDIENTE, ultimoEstado());
    }

    @Test void sinCredencialesDeFirebaseNoTocaNada() {
        when(push.disponible()).thenReturn(false);
        assertEquals(0, enviador.enviarPendientes(AHORA));
        verifyNoInteractions(alertas, mongo, dispositivos);
    }
}
