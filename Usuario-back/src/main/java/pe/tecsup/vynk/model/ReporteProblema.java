package pe.tecsup.vynk.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Reportes verificables, asociados a una actividad y a quien los envió. */
@Document("reportes_problemas")
public class ReporteProblema {
    @Id private String id;
    @Indexed private String actividadId;
    private String organizadorId;
    private String categoria;
    private String descripcion;
    private String estado = "pendiente";
    private Instant creadoEn;
    public String getId() { return id; }
    public void setId(String v) { id = v; }
    public String getActividadId() { return actividadId; }
    public void setActividadId(String v) { actividadId = v; }
    public String getOrganizadorId() { return organizadorId; }
    public void setOrganizadorId(String v) { organizadorId = v; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String v) { categoria = v; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { descripcion = v; }
    public String getEstado() { return estado; }
    public void setEstado(String v) { estado = v; }
    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant v) { creadoEn = v; }
}
