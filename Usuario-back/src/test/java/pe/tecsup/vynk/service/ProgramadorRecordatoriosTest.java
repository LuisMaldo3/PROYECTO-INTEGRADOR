package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.model.RecordatorioActividad;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.RecordatorioActividadRepository;

class ProgramadorRecordatoriosTest {
    // 2026-11-22 08:00 en Lima (UTC-5) = 13:00 UTC
    static final Instant AHORA = Instant.parse("2026-11-22T13:00:00Z");

    final InscripcionRepository inscripciones = mock(InscripcionRepository.class);
    final RecordatorioActividadRepository recordatorios = mock(RecordatorioActividadRepository.class);
    final ProgramadorRecordatorios programador = new ProgramadorRecordatorios(inscripciones, recordatorios, 30, 48);

    static Inscripcion inscripcion(String id, String estudiante, String fecha, String hora) {
        Inscripcion i = new Inscripcion();
        i.setId(id); i.setEstudianteId(estudiante); i.setActividadId("act-" + id);
        i.setFecha(fecha); i.setHoraInicio(hora); i.setEstado("inscrita");
        return i;
    }

    @Test void interpretaLaHoraDeLaActividadEnHoraDePeru() {
        // 10:00 en Lima son las 15:00 UTC.
        assertEquals(Instant.parse("2026-11-22T15:00:00Z"),
                ProgramadorRecordatorios.inicioDe(inscripcion("1", "u1", "2026-11-22", "10:00")));
    }

    @Test void fechasOHorasInvalidasNoTienenInicio() {
        assertNull(ProgramadorRecordatorios.inicioDe(inscripcion("1", "u1", "", "10:00")));
        assertNull(ProgramadorRecordatorios.inicioDe(inscripcion("1", "u1", "2026-11-22", "mañana")));
        assertNull(ProgramadorRecordatorios.inicioDe(inscripcion("1", "u1", null, null)));
        assertNull(ProgramadorRecordatorios.inicioDe(null));
    }

    @Test void consultaLasFechasDeHoyYDeLosDosDiasSiguientes() {
        // 48 h desde el 22/11 08:00 Lima llega hasta el 24/11 08:00 Lima.
        assertEquals(List.of("2026-11-22", "2026-11-23", "2026-11-24"),
                ProgramadorRecordatorios.fechasEntre(AHORA, AHORA.plusSeconds(48 * 3600)));
    }

    @Test void usaElDiaDePeruYNoElDiaUtcParaLasFechas() {
        // 02:00 UTC del 23/11 todavía es 21:00 del 22/11 en Lima.
        Instant tarde = Instant.parse("2026-11-23T02:00:00Z");
        assertEquals("2026-11-22", ProgramadorRecordatorios.fechasEntre(tarde, tarde).get(0));
    }

    @Test void programaElAvisoTreintaMinutosAntesDelInicio() {
        when(inscripciones.findByEstadoAndFechaIn(eq("inscrita"), anyCollection()))
                .thenReturn(List.of(inscripcion("i1", "u1", "2026-11-22", "10:00")));
        when(recordatorios.findByInscripcionId("i1")).thenReturn(Optional.empty());

        assertEquals(1, programador.programar(AHORA));

        var captura = ArgumentCaptor.forClass(RecordatorioActividad.class);
        verify(recordatorios).save(captura.capture());
        RecordatorioActividad r = captura.getValue();
        assertEquals("i1", r.getInscripcionId());
        assertEquals("u1", r.getUsuarioId());
        assertEquals("act-i1", r.getActividadId());
        assertEquals(Instant.parse("2026-11-22T15:00:00Z"), r.getInicio());
        assertEquals(Instant.parse("2026-11-22T14:30:00Z"), r.getEnviarEn());
        assertEquals(RecordatorioActividad.PENDIENTE, r.getEstado());
    }

    @Test void noProgramaActividadesQueYaEmpezaronNiLasMuyLejanas() {
        when(inscripciones.findByEstadoAndFechaIn(eq("inscrita"), anyCollection())).thenReturn(List.of(
                inscripcion("pasada", "u1", "2026-11-22", "07:59"),
                inscripcion("justoAhora", "u1", "2026-11-22", "08:00"),
                inscripcion("lejana", "u1", "2026-11-24", "09:00")));

        assertEquals(0, programador.programar(AHORA));
        verify(recordatorios, never()).save(any());
    }

    @Test void ignoraInscripcionesConFechaOHoraInvalida() {
        when(inscripciones.findByEstadoAndFechaIn(eq("inscrita"), anyCollection())).thenReturn(List.of(
                inscripcion("mala1", "u1", "2026-11-22", "sin hora"),
                inscripcion("mala2", "u1", "22/11/2026", "10:00")));

        assertEquals(0, programador.programar(AHORA));
        verify(recordatorios, never()).save(any());
    }

    @Test void noDuplicaUnAvisoQueYaEstaProgramadoConLaMismaHora() {
        RecordatorioActividad existente = new RecordatorioActividad();
        existente.setInscripcionId("i1"); existente.setUsuarioId("u1");
        existente.setInicio(Instant.parse("2026-11-22T15:00:00Z"));
        existente.setEstado(RecordatorioActividad.ENVIADO);
        when(inscripciones.findByEstadoAndFechaIn(eq("inscrita"), anyCollection()))
                .thenReturn(List.of(inscripcion("i1", "u1", "2026-11-22", "10:00")));
        when(recordatorios.findByInscripcionId("i1")).thenReturn(Optional.of(existente));

        assertEquals(0, programador.programar(AHORA));
        verify(recordatorios, never()).save(any());
        assertEquals(RecordatorioActividad.ENVIADO, existente.getEstado());
    }

    @Test void siElOrganizadorCambiaLaHoraSeVuelveAAvisarConLaHoraNueva() {
        RecordatorioActividad existente = new RecordatorioActividad();
        existente.setInscripcionId("i1"); existente.setUsuarioId("u1");
        existente.setInicio(Instant.parse("2026-11-22T15:00:00Z"));
        existente.setEstado(RecordatorioActividad.ENVIADO);
        existente.setIntentos(2);
        existente.setEnviadoEn(AHORA);
        when(inscripciones.findByEstadoAndFechaIn(eq("inscrita"), anyCollection()))
                .thenReturn(List.of(inscripcion("i1", "u1", "2026-11-22", "11:00")));
        when(recordatorios.findByInscripcionId("i1")).thenReturn(Optional.of(existente));

        assertEquals(1, programador.programar(AHORA));

        verify(recordatorios).save(existente);
        assertEquals(Instant.parse("2026-11-22T16:00:00Z"), existente.getInicio());
        assertEquals(Instant.parse("2026-11-22T15:30:00Z"), existente.getEnviarEn());
        assertEquals(RecordatorioActividad.PENDIENTE, existente.getEstado());
        assertEquals(0, existente.getIntentos());
        assertNull(existente.getEnviadoEn());
    }

    @Test void siOtraEjecucionLoCreoPrimeroSeSigueConLasDemasInscripciones() {
        when(inscripciones.findByEstadoAndFechaIn(eq("inscrita"), anyCollection())).thenReturn(List.of(
                inscripcion("i1", "u1", "2026-11-22", "10:00"),
                inscripcion("i2", "u2", "2026-11-22", "10:00")));
        when(recordatorios.findByInscripcionId(any())).thenReturn(Optional.empty());
        when(recordatorios.save(argThat(r -> "i1".equals(r.getInscripcionId()))))
                .thenThrow(new DuplicateKeyException("duplicado"));

        assertEquals(1, programador.programar(AHORA));
        verify(recordatorios, times(2)).save(any());
    }
}
