package pe.tecsup.vynk.service;

import java.util.Map;

/** Envía un aviso a un celular. Hoy lo hace Firebase Cloud Messaging; las pruebas usan uno falso. */
public interface NotificadorPush {
    enum Resultado {
        ENVIADO,
        /** El celular ya no tiene la app o cambió de token: hay que olvidarlo. */
        TOKEN_INVALIDO,
        /** Falló la red o el servicio: conviene reintentar. */
        ERROR_TEMPORAL,
        /** El servicio rechazó el aviso: reintentar no ayuda. */
        ERROR_PERMANENTE
    }

    /** Falso cuando el servidor no tiene credenciales de Firebase: no se intenta enviar nada. */
    boolean disponible();

    /** @param datos pares texto-texto que la app recibe tal cual (no se muestran solos). */
    Resultado enviar(String tokenDispositivo, Map<String, String> datos);
}
