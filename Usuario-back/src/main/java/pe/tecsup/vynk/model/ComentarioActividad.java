package pe.tecsup.vynk.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/** Comentario individual de un estudiante inscrito; un registro por actividad y estudiante. */
@Document("comentarios_actividades")
@CompoundIndex(name = "comentario_actividad_estudiante", def = "{'actividadId': 1, 'estudianteId': 1}", unique = true)
public class ComentarioActividad {
    @Id private String id;
    private String actividadId;
    private String estudianteId;
    private String texto;
    private Instant actualizadoEn;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getActividadId() { return actividadId; }
    public void setActividadId(String actividadId) { this.actividadId = actividadId; }
    public String getEstudianteId() { return estudianteId; }
    public void setEstudianteId(String estudianteId) { this.estudianteId = estudianteId; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public Instant getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(Instant actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
