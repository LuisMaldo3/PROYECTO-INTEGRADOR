package pe.tecsup.vynk.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import pe.tecsup.vynk.model.DispositivoMovil;
import pe.tecsup.vynk.repository.DispositivoMovilRepository;

/** Envía un aviso a todos los celulares de un estudiante y olvida los que ya no existen. Lo usan las alertas. */
class EntregaCelulares {
    enum Resultado {
        /** Al menos un celular lo recibió. */
        ENTREGADA,
        /** No tiene celulares registrados (o todos eran inválidos y se olvidaron): esperar a que abra la app. */
        SIN_CELULAR,
        /** Ningún celular lo recibió por un error: vale la pena reintentar. */
        FALLO
    }

    private final DispositivoMovilRepository dispositivos;
    private final NotificadorPush push;

    EntregaCelulares(DispositivoMovilRepository dispositivos, NotificadorPush push) {
        this.dispositivos = dispositivos;
        this.push = push;
    }

    Resultado entregar(String usuarioId, Map<String, String> datos, Duration vida) {
        List<DispositivoMovil> celulares = dispositivos.findByUsuarioId(usuarioId);
        int entregadas = 0;
        boolean fallo = false;
        for (DispositivoMovil celular : celulares) {
            switch (push.enviar(celular.getToken(), datos, vida)) {
                case ENVIADO -> entregadas++;
                case TOKEN_INVALIDO -> dispositivos.deleteByToken(celular.getToken());
                case ERROR_TEMPORAL, ERROR_PERMANENTE -> fallo = true;
            }
        }
        if (entregadas > 0) return Resultado.ENTREGADA;
        return fallo ? Resultado.FALLO : Resultado.SIN_CELULAR;
    }
}
