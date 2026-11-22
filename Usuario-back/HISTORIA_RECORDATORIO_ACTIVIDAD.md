# Historia: recordatorio antes de que comience una actividad inscrita

Como estudiante, quiero recibir un recordatorio antes de que comience una actividad inscrita.

## Cómo funciona

1. **Programar (backend).** Cada 60 s `TareaRecordatorios` ejecuta `ProgramadorRecordatorios`: busca las inscripciones
   `inscrita` cuya actividad empieza en las próximas 48 h y guarda un `RecordatorioActividad` (colección
   `recordatorios_actividad`, uno por inscripción) para enviarlo **30 min antes** del inicio. Si el organizador cambia la
   hora, el recordatorio se reprograma solo. Las horas se interpretan en hora de Perú (`America/Lima`).
2. **Enviar (backend).** `EnviadorRecordatorios` toma los recordatorios que ya tocan, comprueba que la inscripción siga
   vigente y que la hora no haya cambiado, y envía el aviso con Firebase Cloud Messaging a los celulares del estudiante.
   Un recordatorio se entrega una sola vez (aunque se reinicie el servidor o haya dos copias corriendo); un fallo de red
   se reintenta hasta 3 veces; si la actividad ya empezó, el aviso vence y no se envía.
3. **Registrar el celular (app → backend).** Al entrar a la app, esta envía el token de Firebase a
   `PUT /api/dispositivos`; al cerrar sesión llama a `POST /api/dispositivos/baja`.
4. **Recibir y mostrar (app).** `ServicioMensajes` recibe el aviso (llega con la app abierta, en segundo plano o cerrada)
   y `NotificadorAvisos` lo muestra como notificación. En Android 13+ la app pide el permiso de notificaciones.
5. **Abrir la actividad (app).** Al tocar la notificación se abre el detalle de la actividad (el mismo diálogo del
   catálogo). Si hay que iniciar sesión primero, se abre al terminar de entrar.

## Configuración (obligatoria para que los avisos lleguen)

El servidor necesita la **cuenta de servicio de Firebase** del proyecto `vynk-e8ac4`:

1. Firebase Console → Configuración del proyecto → Cuentas de servicio → *Generar nueva clave privada*.
2. Guarda el JSON **fuera del repositorio** y apunta a él en `.env`: `FCM_CREDENCIALES=/ruta/al/archivo.json`
   (opcional: `FCM_PROYECTO=vynk-e8ac4` si el JSON no trae `project_id`).
3. Reinicia el backend. En el arranque debe verse: `Avisos al celular activos con Firebase Cloud Messaging.`

Sin `FCM_CREDENCIALES` el servidor arranca igual y programa los recordatorios, pero no envía nada.

Otras variables (opcionales): `RECORDATORIOS_ACTIVO=false` apaga el proceso (por ejemplo en una segunda copia del
servidor) y `RECORDATORIOS_ANTELACION_MIN=30` cambia con cuántos minutos de anticipación se avisa.

## Cómo probarlo

- Pruebas unitarias: `mvn test` en `Usuario-back` y `./gradlew testDebugUnitTest` en `App`.
- Prueba contra MongoDB real (opcional): `MONGODB_E2E_URI=mongodb://localhost:27017 mvn test -Dtest=RecordatoriosMongoE2ETest`
  (usa una base temporal y la borra al terminar).
- En un celular: inscríbete en una actividad que empiece en unos 35 min, abre la app una vez (registra el celular) y
  espera al aviso. Para no esperar, pon `RECORDATORIOS_ANTELACION_MIN=60`.
