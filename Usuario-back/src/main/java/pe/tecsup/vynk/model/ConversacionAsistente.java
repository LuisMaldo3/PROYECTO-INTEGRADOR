package pe.tecsup.vynk.model;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Una conversación privada por ID de usuario; la revisión protege los borrados. */
@Document(collection = "conversaciones_asistente")
public class ConversacionAsistente {
    @Id private String id;
    private String revision;
    private List<Mensaje> mensajes;

    public ConversacionAsistente() {}
    public String getId() { return id; }
    public String getRevision() { return revision; }
    public List<Mensaje> getMensajes() { return mensajes == null ? List.of() : mensajes; }

    public record Mensaje(String rol, String contenido, String origen, String aviso,
                          List<java.util.Map<String, Object>> ubicaciones, String tipoConsulta,
                          List<java.util.Map<String, Object>> actividades,
                          List<java.util.Map<String, Object>> requisitos) {
        public Mensaje {
            ubicaciones = ubicaciones == null ? List.of() : List.copyOf(ubicaciones);
            actividades = actividades == null ? List.of() : List.copyOf(actividades);
            requisitos = requisitos == null ? List.of() : List.copyOf(requisitos);
        }
        public static Mensaje usuario(String texto) {
            return new Mensaje("usuario", texto, "", "", List.of(), "general", List.of(), List.of());
        }
        public static Mensaje asistente(pe.tecsup.vynk.service.AsistenteHorariosService.Respuesta r) {
            return new Mensaje("asistente", r.respuesta(), r.origen(), r.aviso(),
                    r.ubicaciones(), r.tipoConsulta(), r.actividades(), r.requisitos());
        }
    }
}
