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

## Menú inferior

Después de iniciar sesión, la app muestra un menú inferior (Material 3) con las pantallas del estudiante. La app móvil es solo para el usuario (estudiante), así que el menú es el mismo para todos:

**Inicio · Catálogo · Agenda · Asistente · Perfil**

- Las pantallas se definen en el enum `Destino` de `ui/main/Destinos.kt` y se registran en `MainScreen.kt`.
- Inicio, Catálogo, Agenda y Asistente están **vacías**: cada una tiene su archivo en `ui/main/` (`InicioScreen.kt`, `CatalogoScreen.kt`, `AgendaScreen.kt`, `AsistenteScreen.kt`) para llenarlo cuando toque su historia.
- **Perfil** muestra los datos de la persona y el botón «Cerrar sesión».
- La lógica del menú se prueba con `ui/main/DestinosTest.kt` (`./gradlew testDebugUnitTest`).

## Cerrar sesión (app)

La opción «Cerrar sesión» está en **Perfil**, abajo, como botón de ancho completo. Al pulsarla, en este orden:

1. **Borra los datos de la sesión** del celular (token, nombre, correo y rol; archivo `vynk_sesion`). Se borra con `commit()` para que quede escrito en el disco al instante.
2. **Olvida la cuenta de Google** elegida (Credential Manager). Si esto falla no pasa nada: no impide continuar.
3. **Lleva al inicio de sesión**, sin posibilidad de volver atrás a las pantallas de la sesión.

La sesión **no se incluye en los respaldos** de Android (`res/xml/backup_rules.xml` y `data_extraction_rules.xml`): así no puede restaurarse en otro celular, ni por respaldo en la nube ni al transferir datos.

Por ahora la app no avisa al servidor al cerrar sesión; solo la web lo hace.

