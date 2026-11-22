package pe.tecsup.vynk.config;

import java.io.IOException;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.tecsup.vynk.service.NotificadorDesactivado;
import pe.tecsup.vynk.service.NotificadorFcm;
import pe.tecsup.vynk.service.NotificadorPush;

@Configuration
public class PushConfig {
    private static final Logger log = LoggerFactory.getLogger(PushConfig.class);

    /**
     * Con FCM_CREDENCIALES (ruta al JSON de la cuenta de servicio de Firebase) se envían avisos de verdad.
     * Sin ella, o si el archivo no sirve, el servidor arranca igual y simplemente no envía.
     */
    @Bean
    NotificadorPush notificadorPush(@Value("${vynk.fcm.credenciales:}") String ruta,
                                    @Value("${vynk.fcm.proyecto:}") String proyecto) {
        if (ruta == null || ruta.isBlank()) {
            log.warn("FCM_CREDENCIALES no está definido: los recordatorios se programan, pero no se envían al celular.");
            return new NotificadorDesactivado();
        }
        try {
            NotificadorFcm fcm = NotificadorFcm.desdeCredenciales(Path.of(ruta.strip()), proyecto);
            log.info("Avisos al celular activos con Firebase Cloud Messaging.");
            return fcm;
        } catch (IOException | RuntimeException e) {
            log.error("No se pudieron leer las credenciales de Firebase ({}): {}. Los avisos quedan desactivados.",
                    ruta, e.getMessage());
            return new NotificadorDesactivado();
        }
    }
}
