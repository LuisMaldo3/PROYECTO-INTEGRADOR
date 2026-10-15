package pe.tecsup.vynk.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/** MongoDB: una marca por estudiante y actividad (se actualiza, no se duplica). */
@Document("asistencias")
@CompoundIndex(name = "actividad_alumno_asistencia", def = "{'actividadId':1,'estudianteId':1}", unique = true)
public class Asistencia {
    @Id private String id;
    private String actividadId;
    private String estudianteId;
    private boolean presente;
    private Instant actualizadoEn;
    private String registradoPor;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getActividadId() { return actividadId; }
    public void setActividadId(String v) { actividadId = v; }
    public String getEstudianteId() { return estudianteId; }
    public void setEstudianteId(String v) { estudianteId = v; }
    public boolean isPresente() { return presente; }
    public void setPresente(boolean v) { presente = v; }
    public Instant getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(Instant v) { actualizadoEn = v; }
    public String getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(String v) { registradoPor = v; }
}
