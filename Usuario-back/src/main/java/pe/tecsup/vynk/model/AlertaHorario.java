package pe.tecsup.vynk.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

/** Aviso al celular de un inscrito porque la actividad cambió de fecha u hora. Guarda el horario anterior y el nuevo. */
@Document("alertas_horario")
@CompoundIndexes({
        @CompoundIndex(name = "estado_creada", def = "{'estado': 1, 'creadaEn': 1}"),
        @CompoundIndex(name = "inscripcion_estado", def = "{'inscripcionId': 1, 'estado': 1}")
})
public class AlertaHorario {
    public static final String PENDIENTE = "pendiente";
    public static final String ENVIANDO = "enviando";
    public static final String ENVIADA = "enviada";
    /** El horario volvió al de antes, el estudiante canceló o llegó un cambio más nuevo. */
    public static final String CANCELADA = "cancelada";
    /** La actividad ya empezó sin que la alerta pudiera salir. */
    public static final String VENCIDA = "vencida";
    public static final String FALLIDA = "fallida";

    @Id private String id;
    private String inscripcionId;
    private String usuarioId;
    private String actividadId;
    private String nombreActividad;
    private String fechaAnterior;
    private String inicioAnterior;
    private String finAnterior;
    private String fechaNueva;
    private String inicioNuevo;
    private String finNuevo;
    /** Momento en que empieza la actividad con el horario nuevo: después de eso la alerta ya no sirve. */
    private Instant comienzaEn;
    private String estado = PENDIENTE;
    private int intentos;
    private Instant creadaEn;
    private Instant reclamadaEn;
    private Instant enviadaEn;

    public String getId() { return id; }
    public void setId(String v) { id = v; }
    public String getInscripcionId() { return inscripcionId; }
    public void setInscripcionId(String v) { inscripcionId = v; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String v) { usuarioId = v; }
    public String getActividadId() { return actividadId; }
    public void setActividadId(String v) { actividadId = v; }
    public String getNombreActividad() { return nombreActividad; }
    public void setNombreActividad(String v) { nombreActividad = v; }
    public String getFechaAnterior() { return fechaAnterior; }
    public void setFechaAnterior(String v) { fechaAnterior = v; }
    public String getInicioAnterior() { return inicioAnterior; }
    public void setInicioAnterior(String v) { inicioAnterior = v; }
    public String getFinAnterior() { return finAnterior; }
    public void setFinAnterior(String v) { finAnterior = v; }
    public String getFechaNueva() { return fechaNueva; }
    public void setFechaNueva(String v) { fechaNueva = v; }
    public String getInicioNuevo() { return inicioNuevo; }
    public void setInicioNuevo(String v) { inicioNuevo = v; }
    public String getFinNuevo() { return finNuevo; }
    public void setFinNuevo(String v) { finNuevo = v; }
    public Instant getComienzaEn() { return comienzaEn; }
    public void setComienzaEn(Instant v) { comienzaEn = v; }
    public String getEstado() { return estado; }
    public void setEstado(String v) { estado = v; }
    public int getIntentos() { return intentos; }
    public void setIntentos(int v) { intentos = v; }
    public Instant getCreadaEn() { return creadaEn; }
    public void setCreadaEn(Instant v) { creadaEn = v; }
    public Instant getReclamadaEn() { return reclamadaEn; }
    public void setReclamadaEn(Instant v) { reclamadaEn = v; }
    public Instant getEnviadaEn() { return enviadaEn; }
    public void setEnviadaEn(Instant v) { enviadaEn = v; }
}
