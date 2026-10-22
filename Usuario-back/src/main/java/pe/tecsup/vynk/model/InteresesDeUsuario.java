package pe.tecsup.vynk.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Los intereses que eligió una persona. Hay un documento por usuario: su id es el id del usuario. */
@Document("intereses_usuario")
public class InteresesDeUsuario {

    @Id
    private String usuarioId;

    private List<String> intereses = new ArrayList<>();
    private Instant actualizadoEn;
    // Se almacena por persona para no repetir la invitación en otros dispositivos.
    private boolean omitido;

    public InteresesDeUsuario() {
    }

    public InteresesDeUsuario(String usuarioId, List<String> intereses, Instant actualizadoEn) {
        this.usuarioId = usuarioId;
        this.intereses = intereses;
        this.actualizadoEn = actualizadoEn;
    }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public List<String> getIntereses() { return intereses; }
    public void setIntereses(List<String> intereses) { this.intereses = intereses; }

    public boolean isOmitido() { return omitido; }
    public void setOmitido(boolean omitido) { this.omitido = omitido; }

    public Instant getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(Instant actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
