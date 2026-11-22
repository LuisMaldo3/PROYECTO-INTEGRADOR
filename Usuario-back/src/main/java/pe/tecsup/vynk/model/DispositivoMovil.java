package pe.tecsup.vynk.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Celular de un estudiante al que se le pueden enviar avisos (token de Firebase Cloud Messaging). */
@Document("dispositivos_moviles")
public class DispositivoMovil {
    @Id private String id;
    @Indexed private String usuarioId;
    // Un token identifica a un solo celular: si otra cuenta entra en él, el token pasa a esa cuenta.
    @Indexed(unique = true) private String token;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public String getId() { return id; }
    public void setId(String v) { id = v; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String v) { usuarioId = v; }
    public String getToken() { return token; }
    public void setToken(String v) { token = v; }
    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant v) { creadoEn = v; }
    public Instant getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(Instant v) { actualizadoEn = v; }
}
