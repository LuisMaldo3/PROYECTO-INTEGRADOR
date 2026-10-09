package pe.tecsup.vynk.model;

import java.util.List;

/** Nombres de los roles de VYNK (se guardan tal cual en usuarios.rol y en la coleccion "roles"). */
public final class Roles {

    public static final String USUARIO = "usuario";
    public static final String ORGANIZADOR = "organizador";
    public static final String ADMINISTRADOR = "administrador";

    public static final List<String> TODOS = List.of(USUARIO, ORGANIZADOR, ADMINISTRADOR);

    private Roles() {
    }

    /** true si el texto es uno de los roles conocidos (sin distinguir mayusculas). */
    public static boolean esValido(String rol) {
        return rol != null && TODOS.contains(rol.trim().toLowerCase());
    }
}
