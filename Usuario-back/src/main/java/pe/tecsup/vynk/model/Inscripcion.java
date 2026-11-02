package pe.tecsup.vynk.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("inscripciones")
@CompoundIndex(name = "alumno_actividad_unica", def = "{'estudianteId': 1, 'actividadId': 1}", unique = true)
public class Inscripcion {
    @Id private String id;
    private String estudianteId;
    private String nombreEstudiante;
    private String nombreEquipo;
    private String codigoEquipoHash;
    // Cada integrante de un equipo confirma una inscripción personal, enlazada al equipo del capitán.
    private String equipoId;
    private String rolEquipo;
    private java.util.List<String> integrantes = new java.util.ArrayList<>();
    private String areaTecsup;
    private boolean areaVerificada;
    // Auditoría de sustituciones: nunca modifica el límite ni el conteo de cupos.
    private java.util.List<java.util.Map<String, Object>> historialReemplazos = new java.util.ArrayList<>();
    private String actividadId;
    private String nombreActividad;
    private String descripcion;
    private String categoria;
    private String ubicacion;
    private String fecha; // yyyy-MM-dd
    private String horaInicio; // HH:mm
    private String horaFin;
    private String estado = "inscrita";
    public String getEquipoId() { return equipoId; }
    public void setEquipoId(String id) { equipoId = id; }
    public String getRolEquipo() { return rolEquipo; }
    public void setRolEquipo(String rol) { rolEquipo = rol; }
    public String getNombreEstudiante() { return nombreEstudiante; }
    public void setNombreEstudiante(String v) { nombreEstudiante = v; }
    public String getNombreEquipo() { return nombreEquipo; }
    public void setNombreEquipo(String v) { nombreEquipo = v; }
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getCodigoEquipoHash() { return codigoEquipoHash; }
    public void setCodigoEquipoHash(String v) { codigoEquipoHash = v; }
    @com.fasterxml.jackson.annotation.JsonIgnore
    public java.util.List<String> getIntegrantes() { return integrantes == null ? java.util.List.of() : integrantes; }
    public void setIntegrantes(java.util.List<String> v) { integrantes = v; }
    public String getAreaTecsup() { return areaTecsup; }
    public void setAreaTecsup(String v) { areaTecsup = v; }
    public boolean isAreaVerificada() { return areaVerificada; }
    public void setAreaVerificada(boolean v) { areaVerificada = v; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEstudianteId() { return estudianteId; }
    public void setEstudianteId(String v) { estudianteId = v; }
    @com.fasterxml.jackson.annotation.JsonIgnore
    public java.util.List<java.util.Map<String, Object>> getHistorialReemplazos() { return historialReemplazos; }
    public void setHistorialReemplazos(java.util.List<java.util.Map<String, Object>> historial) { historialReemplazos = historial; }
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
