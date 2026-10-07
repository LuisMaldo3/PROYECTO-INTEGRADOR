# VYNK – App Android (TC Week)

## Requisitos
* Android Studio reciente (trae su propio JDK) y **Android SDK 37** (Studio ofrece instalarlo al sincronizar).
* El backend **Usuario-back** corriendo en el puerto 8081 (ver su README; necesita su archivo `.env`).

## Conectar con el backend (emulador y celular, sin IP)
Una sola configuración sirve para ambos. En `local.properties` (local, no se sube a Git):

```properties
backend.url=http://localhost:8081
```

Luego *Sync Project*, instalar la app y, con el emulador abierto o el celular conectado por USB
(depuración USB activada), ejecutar **`conectar-backend.bat`** (doble clic).
Repetirlo cada vez que se reinicie el emulador o se reconecte el cable.

Si no se configura nada, la app usa `http://10.0.2.2:8081`, que solo funciona en el emulador.
Para probar sin cable, el backend tiene que estar en un servidor con URL pública.

## Login con Google
Google solo acepta la app si el **SHA-1 de la llave de depuración** de quien compila está registrado.
Cada integrante tiene uno distinto:

1. Obtenerlo: `Gradle > app > Tasks > android > signingReport` (o `./gradlew signingReport`).
2. Registrarlo: Firebase Console > proyecto `vynk-e8ac4` > app `com.tecsup.tcweek` > *Add fingerprint*.
3. Si la pantalla de consentimiento de Google está en modo "Testing", agregar tu correo como usuario de prueba.

Si falla, el detalle real aparece en Logcat filtrando por `LoginViewModel`.
