# Historia: alerta cuando cambia la ubicación de una actividad

Como estudiante, quiero recibir una alerta cuando cambie la ubicación.

## Cómo funciona

1. **Detectar el cambio (backend).** Cada inscripción guarda el identificador de la ubicación del momento en que el
   estudiante se inscribió. El administrador cambia el lugar desde Django, que solo modifica `ubicacionId` en la
   actividad. Cada 60 s `DetectorCambiosUbicacion` compara ambos. Si no coinciden, registra una `AlertaUbicacion`
   (colección `alertas_ubicacion`) con los **nombres** de la ubicación anterior y la nueva (de la colección
   `ubicaciones`) y actualiza la inscripción. Cada cambio se detecta una sola vez.
   - No se avisa por actividades canceladas o finalizadas, por inscripciones canceladas, si la actividad se quedó sin
     ubicación ni si ya empezó.
   - Dos cambios antes de enviar se juntan en una sola alerta; si vuelve al lugar de antes, la alerta se cancela.
   - Si una ubicación se borró, se muestra «Ubicación por confirmar».
2. **Enviar la alerta (backend).** `EnviadorAlertasUbicacion` la envía por Firebase Cloud Messaging a los celulares
   del inscrito. FCM la guarda hasta que empiece la actividad (una semana si no tiene fecha). Reintenta hasta 3 veces.
3. **Mostrar la alerta (app).** Canal «Cambios de ubicación», con dos líneas: `Antes: Cancha 2` y
   `Ahora: Auditorio principal`.
4. **Abrir la actividad (app).** Al tocarla se abre el detalle con un recuadro que indica el lugar anterior y el nuevo.

Corrección incluida: el recordatorio antes de la actividad mostraba el identificador de la ubicación en lugar de su
nombre. Ahora muestra el nombre (por ejemplo «· Auditorio principal»).

Usa la misma configuración de Firebase que las demás alertas (`FCM_CREDENCIALES`).

## Cómo probarlo

- Pruebas: `mvn test -Dtest=AlertasUbicacionTest` y, con MongoDB,
  `MONGODB_E2E_URI=mongodb://localhost:27017 mvn test -Dtest=CambiosUbicacionMongoE2ETest`.
- En un celular: inscríbete en una actividad futura, abre la app una vez y cambia su ubicación desde el panel de
  administración. La alerta llega en menos de un minuto.
