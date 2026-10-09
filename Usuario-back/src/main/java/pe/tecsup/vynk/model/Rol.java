package pe.tecsup.vynk.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Un rol disponible en VYNK. El id es el nombre del rol: "usuario", "organizador", "administrador". */
@Document("roles")
public class Rol {

    @Id
    private String id;

    private String descripcion;

    public Rol() {
    }

    public Rol(String id, String descripcion) {
        this.id = id;
        this.descripcion = descripcion;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
