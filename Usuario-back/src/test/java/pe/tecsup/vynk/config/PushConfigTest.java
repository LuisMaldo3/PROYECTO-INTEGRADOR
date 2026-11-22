package pe.tecsup.vynk.config;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.tecsup.vynk.service.NotificadorDesactivado;
import pe.tecsup.vynk.service.NotificadorFcm;

class PushConfigTest {
    final PushConfig config = new PushConfig();

    /** JSON con la forma de la cuenta de servicio de Firebase, con una llave generada solo para la prueba. */
    static String cuentaDeServicio(String proyecto) throws Exception {
        var generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        String llave = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(generador.generateKeyPair().getPrivate().getEncoded());
        return "{\"type\":\"service_account\"," + (proyecto == null ? "" : "\"project_id\":\"" + proyecto + "\",")
                + "\"private_key_id\":\"abc\",\"private_key\":\"-----BEGIN PRIVATE KEY-----\\n"
                + llave.replace("\n", "\\n") + "\\n-----END PRIVATE KEY-----\\n\","
                + "\"client_email\":\"fcm@prueba.iam.gserviceaccount.com\",\"client_id\":\"1\","
                + "\"token_uri\":\"https://oauth2.googleapis.com/token\"}";
    }

    @Test void sinCredencialesElServidorArrancaPeroNoEnvia() {
        assertInstanceOf(NotificadorDesactivado.class, config.notificadorPush("", ""));
        assertInstanceOf(NotificadorDesactivado.class, config.notificadorPush("   ", null));
        assertFalse(config.notificadorPush("", "").disponible());
    }

    @Test void conUnaRutaQueNoExisteElServidorArrancaPeroNoEnvia(@TempDir Path carpeta) {
        assertInstanceOf(NotificadorDesactivado.class,
                config.notificadorPush(carpeta.resolve("no-existe.json").toString(), ""));
    }

    @Test void conUnArchivoQueNoEsUnaCuentaDeServicioNoEnvia(@TempDir Path carpeta) throws Exception {
        Path basura = Files.writeString(carpeta.resolve("basura.json"), "esto no es json");
        assertInstanceOf(NotificadorDesactivado.class, config.notificadorPush(basura.toString(), ""));
    }

    @Test void conLaCuentaDeServicioDeFirebaseEnviaAvisos(@TempDir Path carpeta) throws Exception {
        Path json = Files.writeString(carpeta.resolve("firebase.json"), cuentaDeServicio("vynk-e8ac4"));
        var notificador = config.notificadorPush(json.toString(), "");
        assertInstanceOf(NotificadorFcm.class, notificador);
        assertTrue(notificador.disponible());
    }

    @Test void siElJsonNoTraeProyectoSeExigeIndicarloConFcmProyecto(@TempDir Path carpeta) throws Exception {
        Path json = Files.writeString(carpeta.resolve("firebase.json"), cuentaDeServicio(null));
        assertInstanceOf(NotificadorDesactivado.class, config.notificadorPush(json.toString(), ""));
        assertInstanceOf(NotificadorFcm.class, config.notificadorPush(json.toString(), "vynk-e8ac4"));
    }
}
