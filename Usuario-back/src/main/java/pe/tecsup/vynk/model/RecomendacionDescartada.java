package pe.tecsup.vynk.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Marca de desinteres por usuario y actividad. El ID compuesto impide duplicados. */
@Document(collection = "recomendaciones_descartadas")
public class RecomendacionDescartada {
    @Id private String id;
    @Indexed private String usuarioId;
    private String actividadId;
    private Instant descartadaEn;

    public RecomendacionDescartada() {}
    public RecomendacionDescartada(String usuarioId, String actividadId) {
        this.id = generarId(usuarioId, actividadId);
        this.usuarioId = usuarioId;
        this.actividadId = actividadId;
        this.descartadaEn = Instant.now();
    }

    public static String generarId(String usuarioId, String actividadId) {
        try {
            byte[] clave = (usuarioId.length() + "#" + usuarioId + actividadId).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(clave));
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("SHA-256 no disponible", imposible);
        }
    }
    public String getId() { return id; }
    public String getUsuarioId() { return usuarioId; }
    public String getActividadId() { return actividadId; }
    public Instant getDescartadaEn() { return descartadaEn; }
}
