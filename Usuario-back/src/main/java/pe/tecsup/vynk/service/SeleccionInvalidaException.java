package pe.tecsup.vynk.service;

/** La lista de intereses enviada no se puede guardar. El mensaje está listo para mostrarse a la persona. */
public class SeleccionInvalidaException extends RuntimeException {

    public SeleccionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
