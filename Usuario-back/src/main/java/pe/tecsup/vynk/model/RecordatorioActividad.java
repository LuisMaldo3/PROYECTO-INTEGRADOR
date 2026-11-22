package pe.tecsup.vynk.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Aviso programado para una inscripción: se envía al celular del estudiante poco antes de que empiece
 * la actividad. Hay uno solo por inscripción, así que un reinicio del servidor no repite avisos.
 */
@Document("recordatorios_actividad")
@CompoundIndex(name = "estado_envio", def = "{'estado': 1, 'enviarEn': 1}")
public class RecordatorioActividad {
    public static final String PENDIENTE = "pendiente";
    public static final String ENVIANDO = "enviando";
    public static final String ENVIADO = "enviado";
    /** La inscripción se canceló antes del aviso. */
    public static final String CANCELADO = "cancelado";
    /** La actividad ya empezó sin que el aviso pudiera salir (por ejemplo, el celular no estaba registrado). */
    public static final String VENCIDO = "vencido";
    /** Se agotaron los reintentos de envío. */
    public static final String FALLIDO = "fallido";

    @Id private String id;
    @Indexed(unique = true) private String inscripcionId;
    private String usuarioId;
    private String actividadId;
    private Instant inicio;
    private Instant enviarEn;
    private String estado = PENDIENTE;
    private int intentos;
    private Instant reclamadoEn;
    private Instant enviadoEn;

    public String getId() { return id; }
    public void setId(String v) { id = v; }
    public String getInscripcionId() { return inscripcionId; }
    public void setInscripcionId(String v) { inscripcionId = v; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String v) { usuarioId = v; }
    public String getActividadId() { return actividadId; }
    public void setActividadId(String v) { actividadId = v; }
    public Instant getInicio() { return inicio; }
    public void setInicio(Instant v) { inicio = v; }
    public Instant getEnviarEn() { return enviarEn; }
    public void setEnviarEn(Instant v) { enviarEn = v; }
    public String getEstado() { return estado; }
    public void setEstado(String v) { estado = v; }
    public int getIntentos() { return intentos; }
    public void setIntentos(int v) { intentos = v; }
    public Instant getReclamadoEn() { return reclamadoEn; }
    public void setReclamadoEn(Instant v) { reclamadoEn = v; }
    public Instant getEnviadoEn() { return enviadoEn; }
    public void setEnviadoEn(Instant v) { enviadoEn = v; }
}
