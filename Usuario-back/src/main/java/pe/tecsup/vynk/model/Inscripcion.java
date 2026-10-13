package pe.tecsup.vynk.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("inscripciones")
@CompoundIndex(name = "alumno_actividad_unica", def = "{'estudianteId': 1, 'actividadId': 1}", unique = true)
public class Inscripcion {
    @Id private String id;
    private String estudianteId;
    private String actividadId;
    private String nombreActividad;
    private String descripcion;
    private String categoria;
    private String ubicacion;
    private String fecha; // yyyy-MM-dd
    private String horaInicio; // HH:mm
    private String horaFin;
    private String estado = "inscrita";
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEstudianteId() { return estudianteId; }
    public void setEstudianteId(String v) { estudianteId = v; }
    public String getActividadId() { return actividadId; }
    public void setActividadId(String v) { actividadId = v; }
    public String getNombreActividad() { return nombreActividad; }
    public void setNombreActividad(String v) { nombreActividad = v; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { descripcion = v; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String v) { categoria = v; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String v) { ubicacion = v; }
    public String getFecha() { return fecha; }
    public void setFecha(String v) { fecha = v; }
    public String getHoraInicio() { return horaInicio; }
    public void setHoraInicio(String v) { horaInicio = v; }
    public String getHoraFin() { return horaFin; }
    public void setHoraFin(String v) { horaFin = v; }
    public String getEstado() { return estado; }
    public void setEstado(String v) { estado = v; }
}
