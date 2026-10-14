package pe.tecsup.vynk.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import pe.tecsup.vynk.model.Interes;
import pe.tecsup.vynk.repository.InteresRepository;

import java.util.List;

/**
 * Crea los intereses disponibles en MongoDB al arrancar el backend. Es seguro repetirlo: solo agrega
 * los que faltan y nunca pisa los que ya existen (si alguien edita un nombre en la base, se respeta).
 * Salen de las actividades del programa del T&C Week.
 */
@Component
public class InteresesIniciales implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InteresesIniciales.class);

    /** (id, nombre, descripción); el orden de la lista es el orden en pantalla. */
    public static final List<Interes> CATALOGO = List.of(
            new Interes("teatro", "Teatro y artes escénicas", "Obras, presentaciones teatrales y cuentacuentos.", 1),
            new Interes("musica", "Música", "Presentaciones musicales y conciertos.", 2),
            new Interes("danza", "Danza", "Concursos y presentaciones de baile.", 3),
            new Interes("libros", "Libros y literatura", "Presentaciones de libros y charlas con autores.", 4),
            new Interes("tecnologia", "Tecnología e innovación", "Charlas TEC y novedades de la industria digital.", 5),
            new Interes("ia", "Inteligencia artificial", "IA aplicada, ciencia de datos y su impacto.", 6),
            new Interes("emprendimiento", "Emprendimiento", "Ideas, negocios y experiencias de emprendedores.", 7),
            new Interes("videojuegos", "Videojuegos", "Torneos y concursos de videojuegos.", 8),
            new Interes("deportes", "Deportes", "Gymkana, competencias y actividades deportivas.", 9),
            new Interes("gastronomia", "Gastronomía", "Ferias y concursos de comida.", 10),
            new Interes("concursos", "Concursos y talento", "Karaoke, freestyle, moda y más.", 11));

    private final InteresRepository repo;

    public InteresesIniciales(InteresRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            for (Interes interes : CATALOGO) {
                if (!repo.existsById(interes.getId())) {
                    repo.save(interes);
                    log.info("Interés creado: {}", interes.getId());
                }
            }
        } catch (Exception e) {
            // Si MongoDB no responde, el backend igual arranca; los intereses se crean en el próximo inicio.
            log.warn("No se pudieron crear los intereses iniciales (MongoDB no responde?): {}", e.getMessage());
        }
    }
}
