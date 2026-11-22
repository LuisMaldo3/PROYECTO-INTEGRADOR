package pe.tecsup.vynk.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Notificación por enviar a los celulares de un estudiante (cancelaciones y recomendaciones).
 * Guarda el mensaje ya armado; «clave» impide repetirla (por ejemplo, la misma recomendación dos veces).
 */
@Document("avisos_push")
@CompoundIndexes({
        @CompoundIndex(name = "estado_creado", def = "{'estado': 1, 'creadoEn': 1}"),
        @CompoundIndex(name = "usuario_tipo_creado", def = "{'usuarioId': 1, 'tipo': 1, 'creadoEn': 1}")
})
public class AvisoPush {
    public static final String CANCELACION = "cancelacion_actividad";
    public static final String RECOMENDACION = "recomendacion_actividad";

    public static final String PENDIENTE = "pendiente";
    public static final String ENVIANDO = "enviando";
    public static final String ENVIADO = "enviado";
    /** Ya no corresponde enviarlo (por ejemplo, el estudiante se inscribió o apagó las recomendaciones). */
    public static final String DESCARTADO = "descartado";
    public static final String VENCIDO = "vencido";
    public static final String FALLIDO = "fallido";

    @Id private String id;
    @Indexed(unique = true) private String clave;
    private String tipo;
    private String usuarioId;
    private String actividadId;
    private String inscripcionId;
    private Map<String, String> datos = new LinkedHashMap<>();
    /** Después de este momento ya no sirve enviarlo. */
    private Instant expiraEn;
    private String estado = PENDIENTE;
    private int intentos;
    private Instant creadoEn;
    private Instant reclamadoEn;
    private Instant enviadoEn;

    public String getId() { return id; }
    public void setId(String v) { id = v; }
    public String getClave() { return clave; }
    public void setClave(String v) { clave = v; }
    public String getTipo() { return tipo; }
    public void setTipo(String v) { tipo = v; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String v) { usuarioId = v; }
    public String getActividadId() { return actividadId; }
    public void setActividadId(String v) { actividadId = v; }
    public String getInscripcionId() { return inscripcionId; }
    public void setInscripcionId(String v) { inscripcionId = v; }
    public Map<String, String> getDatos() { return datos; }
    public void setDatos(Map<String, String> v) { datos = v; }
    public Instant getExpiraEn() { return expiraEn; }
    public void setExpiraEn(Instant v) { expiraEn = v; }
    public String getEstado() { return estado; }
    public void setEstado(String v) { estado = v; }
    public int getIntentos() { return intentos; }
    public void setIntentos(int v) { intentos = v; }
    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant v) { creadoEn = v; }
    public Instant getReclamadoEn() { return reclamadoEn; }
    public void setReclamadoEn(Instant v) { reclamadoEn = v; }
    public Instant getEnviadoEn() { return enviadoEn; }
    public void setEnviadoEn(Instant v) { enviadoEn = v; }
}
