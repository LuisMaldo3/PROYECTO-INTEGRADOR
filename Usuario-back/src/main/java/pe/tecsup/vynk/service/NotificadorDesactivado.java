package pe.tecsup.vynk.service;

import java.util.Map;

/** Se usa cuando no hay credenciales de Firebase: el servidor arranca igual, pero no envía avisos. */
public class NotificadorDesactivado implements NotificadorPush {
    @Override public boolean disponible() { return false; }

    @Override public Resultado enviar(String tokenDispositivo, Map<String, String> datos) {
        return Resultado.ERROR_TEMPORAL;
    }
}
