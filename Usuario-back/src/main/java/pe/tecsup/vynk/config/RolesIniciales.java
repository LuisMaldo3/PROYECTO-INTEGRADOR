package pe.tecsup.vynk.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import pe.tecsup.vynk.model.Rol;
import pe.tecsup.vynk.model.Roles;
import pe.tecsup.vynk.repository.RolRepository;

import java.util.List;

/**
 * Crea los roles en MongoDB al arrancar el backend. Es seguro repetirlo: solo agrega
 * los que faltan y nunca pisa los que ya existen.
 */
@Component
public class RolesIniciales implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RolesIniciales.class);

    private final RolRepository repo;

    public RolesIniciales(RolRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Rol> roles = List.of(
                new Rol(Roles.USUARIO, "Cuenta normal: todo el que entra con correo @tecsup.edu.pe"),
                new Rol(Roles.ORGANIZADOR, "Puede organizar actividades. Lo asigna el administrador"),
                new Rol(Roles.ADMINISTRADOR, "Gestiona usuarios y roles"));
        try {
            for (Rol rol : roles) {
                if (!repo.existsById(rol.getId())) {
                    repo.save(rol);
                    log.info("Rol creado: {}", rol.getId());
                }
            }
        } catch (Exception e) {
            // Si MongoDB no responde, el backend igual arranca; los roles se crean en el proximo inicio.
            log.warn("No se pudieron crear los roles iniciales (MongoDB no responde?): {}", e.getMessage());
        }
    }
}
