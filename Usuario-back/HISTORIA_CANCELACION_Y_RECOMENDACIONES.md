# Historias: notificación de cancelación y recomendaciones por notificación

## 1. Notificación cuando una actividad es cancelada

1. **Detectar (backend).** Django cancela la actividad (estado `cancelada`) pero deja las inscripciones como
   `inscrita`, por eso seguían en la agenda y con recordatorios. Cada 60 s `DetectorCancelaciones` busca las
   inscripciones vigentes de actividades canceladas y las pasa a `cancelada`, con `motivoCancelacion:
   actividad_cancelada` para distinguirlas de las que canceló el propio estudiante. Cada una se procesa una sola vez.
2. **Notificar (backend).** Si la actividad todavía no había empezado, se registra una notificación en la cola
   `avisos_push` y `EnviadorAvisosPush` la envía por Firebase Cloud Messaging:
   «Se canceló Torneo de fútbol — La actividad del lun 23/11 · 10:00–11:00 · Cancha 2 fue cancelada. Ya no está en
   tu agenda.»
3. **Mostrar (app).** Canal «Actividades canceladas». Al tocarla se abre la **Agenda** (la actividad ya no se puede
   abrir ni inscribir).
4. **Quitar de la agenda.** `/api/agenda` solo devuelve inscripciones vigentes, y la app filtra igual por si acaso.
   Al recibir una cancelación (o un cambio de horario o de lugar) la pantalla Agenda se vuelve a cargar sola.
   Además se frenan el recordatorio y las alertas pendientes de esa inscripción.

Corrección incluida: la agenda mostraba el identificador de la ubicación; ahora muestra su nombre.

## 2. Recomendaciones de actividades por notificación

1. **Elegir (backend).** `SelectorRecomendaciones` revisa cada 15 min, entre las 09:00 y las 20:00 (hora de Perú),
   a los estudiantes con la app instalada en algún celular. Elige como máximo **una recomendación al día** por
   estudiante: una actividad programada, con cupos, que empiece entre 2 horas y 7 días después, relacionada con sus
   intereses, en la que no esté inscrito ni la haya descartado, sin cruce con su agenda ni con su horario de clases
   (si lo confirmó) y que nunca se le haya recomendado antes. Usa las mismas reglas de relevancia y de cruces que la
   pantalla de recomendaciones; no llama a la IA.
2. **Enviar (backend).** Justo antes de enviarla se comprueba otra vez que siga teniendo sentido: recomendaciones
   activas, no se inscribió y la actividad sigue programada y con cupos. Si no, se descarta.
   Ejemplo: «Te puede interesar: Taller de IA — mar 24/11 · 15:00–16:00 · Laboratorio 3. Porque te interesa
   Inteligencia artificial. Hay cupos disponibles.»
3. **Mostrar (app).** Canal «Recomendaciones», con prioridad normal (no interrumpe como una cancelación). Al tocarla
   se abre el detalle de la actividad para inscribirse.
4. **Activar o desactivar (app y backend).** Interruptor «Recomendaciones de actividades» en Perfil →
   Notificaciones. Se guarda en el servidor (`GET/PUT /api/notificaciones/preferencias`, colección
   `preferencias_notificaciones`) y vale para todos los celulares de la cuenta. Por defecto están activas.
   Cancelaciones, cambios y recordatorios no se pueden apagar desde ahí, porque son de actividades en las que el
   estudiante ya se inscribió.

`RECOMENDACIONES_NOTIFICAR=false` apaga las recomendaciones por notificación en todo el servidor.

## Cómo probarlo

- Pruebas: `mvn test -Dtest=CancelacionYRecomendacionTest,PreferenciasNotificacionControllerTest` y, con MongoDB,
  `MONGODB_E2E_URI=mongodb://localhost:27017 mvn test -Dtest=CancelacionYRecomendacionMongoE2ETest`.
- Cancelación en un celular: inscríbete en una actividad futura, abre la app una vez y cancela la actividad desde el
  panel. La notificación llega en menos de un minuto y la actividad desaparece de la agenda.
- Recomendación en un celular: elige intereses en la app y crea una actividad relacionada para mañana. Entre las 09:00
  y las 20:00 llega en menos de 15 minutos.
