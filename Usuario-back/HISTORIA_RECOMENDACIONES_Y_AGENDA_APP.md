# Recomendaciones de IA y agenda inteligente en la app

## US-76 · Como estudiante, quiero recibir recomendaciones de IA desde la aplicación móvil

| Tarea | Responsable | Qué se hizo |
|---|---|---|
| 1. Preparar el servicio de recomendaciones para la app | Mishel | `GET /api/app/recomendaciones` (`AppRecomendacionesController` + `RecomendacionesAppService`). Con horario confirmado usa las recomendaciones que caben en las horas libres; sin horario ya no queda vacío: recomienda por intereses y sin cruces con las inscripciones, e invita a subir el horario. Cada recomendación trae la actividad completa del catálogo (portada, lugar, modalidad), `motivo`, `etiquetas` comprobables («Te interesa: deportes», «Mañana», «Libre de 13:00 a 21:00», «Quedan 3 cupos», «Por equipos») e `inscripcionDirecta`. `vigenteHasta` (15 min) le dice a la app hasta cuándo reutilizarla. El motivo nombra el interés como lo diría el estudiante («inteligencia artificial», no «ia»), lo busca como palabra («ia» ya no coincide con «industria») y usa la fecha legible. |
| 2. Conectar la app con el servicio | Luis | `RecomendacionesAppApi` + `leerRecomendacionesApp`: reutiliza el lector del catálogo, quita repetidas y sin id, y da errores claros (sin conexión, sesión vencida, servidor desactualizado). |
| 3. Programar la carga de recomendaciones | Jordi | `CargaProgramada` + `rememberCargaProgramada`: lo último cargado se ve al instante (memoria y teléfono, por cuenta) y se actualiza solo al vencer, al inscribirse o cancelar (`CambiosAgenda`) o al tocar actualizar. Sin conexión se muestra lo guardado con aviso y se reintenta cada minuto. Al cerrar sesión se borra. |
| 4. Crear la pantalla de recomendaciones | Luis | Nueva pantalla «Para ti»: cabecera animada con el origen honesto (IA o reglas) y «Actualizado hace X», tarjetas con portada, puesto (#1, #2…), día, hora, lugar, etiquetas, «Por qué te la recomendamos», «Ver detalle», «Inscribirme» y descartar. Carga con esqueletos y animación de entrada. |
| 5. Probar | Todos | Pruebas unitarias y E2E (abajo) y capturas de la app conectada al backend. |
| T06. Abrir detalle e inscripción desde recomendaciones | — | «Ver detalle» abre el detalle completo (con inscripción individual o por equipos). «Inscribirme» pide confirmación y se inscribe en un toque; si es por equipos abre el detalle para poner el nombre o el código. Al inscribirse la tarjeta sale y la agenda se actualiza. |

## US-77 · Como estudiante, quiero consultar mi agenda inteligente desde el celular

| Tarea | Responsable | Qué se hizo |
|---|---|---|
| 1. Preparar el servicio de agenda sugerida para la app | Mishel | `GET /api/app/agenda-inteligente` (`AgendaInteligenteController` + `AgendaInteligenteService`): una línea de tiempo por día con lo inscrito, lo sugerido sin cruces y las clases del horario confirmado (como referencia). Con horario confirmado tampoco sugiere nada en horas de clase. Lo que ya terminó no aparece. Trae resumen (inscritas, sugeridas, en equipo, equipos por confirmar) y avisos. |
| 2. Conectar la app con la agenda inteligente | Jordi | `AgendaInteligenteApi` + `leerAgendaInteligente` (descarta bloques sin hora válida). |
| 3. Programar la carga | Jordi | La misma carga programada (vigencia 5 min). Con el reloj del teléfono se marca lo que está «Ahora» y se quita lo que ya pasó, también en la agenda guardada sin conexión. |
| 4. Crear la pantalla de agenda inteligente | Mishel | Cabecera con contadores animados, filtros (Todo, Mis actividades, Sugeridas por IA), línea de tiempo con hora y riel: inscritas en tarjeta blanca (verde si está en curso), sugeridas con borde punteado, motivo, «Ver detalle» e «Inscribirme», clases en gris. Reemplaza a la antigua «Agenda sugerida». |
| 5. Probar | Todos | Pruebas unitarias y E2E con Mongo real. |
| T06. Sincronizar agenda individual y equipo confirmado | — | `EquiposAgenda`: cada competencia por equipos muestra el estado del equipo, igual para capitán e integrantes («Equipo Los Circuitos · confirmado (3/3)» o «falta 1 por confirmar (2/3)»), con aviso si falta alguien. Si el capitán cancela, el integrante deja de tenerla en su agenda y ese horario queda libre para sugerencias. **Se corrigió un error que ya existía:** `GET /api/agenda` mostraba dos veces la competencia de un integrante (su inscripción y la del capitán); ahora sale una sola vez, y el «Tienes N actividades» de Inicio cuenta bien. |

## Pruebas

- Backend: 355 pruebas; solo fallan las 3 de siempre (`BaseDatosInicializadorTest` ×2 y `VynkApplicationTests`, que necesita Atlas). Nuevas: `RecomendacionesYAgendaAppTest` (9), `RecomendacionesYAgendaAppMongoE2ETest` (5, con `MONGODB_E2E_URI`), `AgendaEquiposTest` (2) y una más en `MotivosHorarioServiceTest`.
- App: 144 pruebas unitarias pasan (nueva: `RecomendacionesYAgendaTest`, 9) y el APK compila. Capturas reales de «Para ti», la inscripción rápida y la agenda inteligente contra el backend con datos de demo.

## Nota para el equipo

Se eliminó `App/.../ui/main/AgendaSugeridaScreen.kt` (la reemplaza `AgendaInteligenteScreen.kt`). Si copian los archivos a mano, bórrenlo; con `git add App` el borrado se sube solo.
