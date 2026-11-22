# Historia: alerta cuando cambia el horario de una actividad

Como estudiante, quiero recibir una alerta cuando cambie el horario de una actividad.

## Cómo funciona

1. **Detectar el cambio (backend).** Cada inscripción guarda una copia del horario (fecha, inicio y fin) del momento
   en que el estudiante se inscribió. El administrador edita la actividad desde Django, que solo cambia la colección
   `actividades`. Cada 60 s `DetectorCambiosHorario` compara ambas: si no coinciden, registra una `AlertaHorario`
   (colección `alertas_horario`) con el horario anterior y el nuevo, y actualiza la copia de la inscripción. Así
   también quedan al día la agenda del estudiante y su recordatorio.
   - Cada cambio se detecta una sola vez, porque la copia se actualiza solo si seguía con el horario anterior.
   - No se avisa por actividades canceladas o finalizadas, por inscripciones canceladas ni si el horario nuevo está
     incompleto o ya pasó.
   - Si hay dos cambios antes de enviar la alerta, se junta todo en una sola alerta (horario original → el más
     reciente). Si el horario vuelve al de antes, la alerta se cancela.
2. **Enviar la alerta (backend).** `EnviadorAlertasHorario` la envía por Firebase Cloud Messaging a los celulares del
   inscrito. Si el celular está apagado, FCM la guarda hasta que empiece la actividad (mínimo 1 h, máximo 28 días).
   Se reintenta hasta 3 veces y vence si la actividad ya empezó.
3. **Mostrar la alerta (app).** La notificación aparece en el canal «Cambios de horario» con dos líneas:
   `Antes: dom 22/11 · 10:00–11:00` y `Ahora: lun 23/11 · 15:00–16:30`.
4. **Abrir la actividad (app).** Al tocarla se abre el detalle de la actividad con un recuadro que muestra el horario
   anterior y el nuevo.

La alerta se envía siempre que el horario cambie, aunque en el panel no se marque «avisar a inscritos». Esa opción
solo controla las notificaciones internas que ya registra Django.

Usa la misma configuración de Firebase que los recordatorios (`FCM_CREDENCIALES`, ver
HISTORIA_RECORDATORIO_ACTIVIDAD.md). Al instalar esta versión, si alguna actividad próxima ya tenía un horario
distinto del que guardan sus inscripciones, sus inscritos recibirán la alerta en el primer ciclo.

## Cómo probarlo

- Pruebas: `mvn test -Dtest=AlertasHorarioTest` y, con MongoDB,
  `MONGODB_E2E_URI=mongodb://localhost:27017 mvn test -Dtest=CambiosHorarioMongoE2ETest`.
- En un celular: inscríbete en una actividad futura, abre la app una vez y cambia la hora de esa actividad desde
  el panel de administración. La alerta llega en menos de un minuto.
