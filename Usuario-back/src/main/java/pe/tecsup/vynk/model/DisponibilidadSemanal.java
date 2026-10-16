package pe.tecsup.vynk.model;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Horario semanal confirmado por la persona; la imagen original no se almacena. */
@Document("disponibilidades_semanales")
public class DisponibilidadSemanal {
    @Id private String id;
    @Indexed(unique = true) private String usuarioId;
    private List<BloqueOcupado> bloques = new ArrayList<>();
    private boolean confirmado;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    public List<BloqueOcupado> getBloques() { return bloques; }
    public void setBloques(List<BloqueOcupado> bloques) { this.bloques = bloques; }
    public boolean isConfirmado() { return confirmado; }
    public void setConfirmado(boolean confirmado) { this.confirmado = confirmado; }
    /** dia: 1=lunes ... 7=domingo, franjas locales HH:mm. */
    public record BloqueOcupado(int dia, String inicio, String fin, String nombre) {}
}
