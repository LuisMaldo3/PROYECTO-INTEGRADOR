package pe.tecsup.vynk.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Límite para fotos, evitando guardar archivos de tamaño excesivo. */
@Configuration
public class MultipartConfig {
    @Bean
    MultipartConfigElement limiteImagenes() {
        return new MultipartConfigElement("", 5L * 1024 * 1024, 6L * 1024 * 1024, 0);
    }
}
