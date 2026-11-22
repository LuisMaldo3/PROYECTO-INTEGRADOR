package pe.tecsup.vynk.service;

import java.time.Duration;
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

    /** Aviso que FCM guarda hasta 30 min si el celular está apagado (sirve para recordatorios). */
    default Resultado enviar(String tokenDispositivo, Map<String, String> datos) {
        return enviar(tokenDispositivo, datos, Duration.ofMinutes(30));
    }

    /**
     * @param datos pares texto-texto que la app recibe tal cual (no se muestran solos).
     * @param vida  cuánto tiempo FCM intenta entregarlo si el celular está apagado (máximo 28 días).
     */
    Resultado enviar(String tokenDispositivo, Map<String, String> datos, Duration vida);
}
