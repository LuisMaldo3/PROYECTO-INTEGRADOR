package pe.tecsup.vynk.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Un interés que la persona puede elegir para recibir recomendaciones. El id es un texto fijo y legible ("tecnologia"). */
@Document("intereses")
public class Interes {

    @Id
    private String id;

    private String nombre;
    private String descripcion;
    private int orden;

    public Interes() {
    }

    public Interes(String id, String nombre, String descripcion, int orden) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.orden = orden;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
}
