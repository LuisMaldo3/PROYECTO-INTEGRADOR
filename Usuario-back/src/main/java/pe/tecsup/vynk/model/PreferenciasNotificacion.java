package pe.tecsup.vynk.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Qué notificaciones opcionales quiere recibir un estudiante. Un documento por usuario (su id es el del usuario).
 * Sin documento, las recomendaciones están activas. Cancelaciones, cambios y recordatorios no se pueden apagar aquí:
 * afectan actividades en las que el estudiante ya se inscribió.
 */
@Document("preferencias_notificaciones")
public class PreferenciasNotificacion {
    @Id private String usuarioId;
    private boolean recomendaciones = true;
    private Instant actualizadoEn;

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String v) { usuarioId = v; }
    public boolean isRecomendaciones() { return recomendaciones; }
    public void setRecomendaciones(boolean v) { recomendaciones = v; }
    public Instant getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(Instant v) { actualizadoEn = v; }
}
