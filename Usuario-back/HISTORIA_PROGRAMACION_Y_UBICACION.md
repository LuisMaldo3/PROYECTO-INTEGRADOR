# Historias: programación del día (US-70) y ubicación de cada actividad (US-71)

## US-70 · Programación completa del día

- **T01 Servicio (backend).** `GET /api/programacion?fecha=AAAA-MM-DD` (`ProgramacionController` y
  `ProgramacionDiaService`). Devuelve todas las actividades publicadas de ese día (programadas, finalizadas y
  canceladas) ordenadas por hora, con lo mismo que el catálogo (portada, cupos, lugar y su ficha) más:
  - `inscrito`: si el estudiante está inscrito.
  - `estadoDia`: `proxima`, `en_curso`, `finalizada` o `cancelada`, calculado con la hora de Perú.
  - `dias`: los días que tienen actividades, con cuántas hay en cada uno.

  Sin `fecha` elige hoy; si hoy no hay actividades, el próximo día con actividades. Una fecha mal escrita responde 400.
- **T02 Conexión (app).** `ProgramacionApi` y `leerProgramacion`.
- **T03 Carga y cambio de día (app).** Al cambiar de día se cancela la carga anterior, así nunca se muestra un día
  viejo. Las flechas saltan al día anterior o siguiente con actividades y los días se nombran «Hoy», «Mañana» o
  «mié 25/11». La programación se recarga sola al inscribirse o al llegar una cancelación o un cambio.
- **T04 Pantalla (app).** `ProgramacionScreen`: selector de días y tarjetas por hora con portada, lugar, cupos y
  etiquetas «En curso», «Finalizada», «Cancelada» o «Inscrito». Se abre desde Inicio («Programación del día») y desde
  Actividades («Por día»).
- **T06 Portada y detalle.** Cada tarjeta muestra la portada (o la ilustración por área) y al tocarla abre el detalle
  de la actividad. Las canceladas no se abren.
- **T07 Inscripción desde el detalle.** El detalle permite inscribirse o cancelar. Al hacerlo, la agenda y la
  programación se actualizan solas.
- **T08 Contrato de inscripción.** `InscripcionContratoTest`: un servidor local responde como el backend y se
  comprueban el método, la ruta, el token, el cuerpo y cómo se interpretan las respuestas (incluidos «sin cupos»,
  la sesión vencida y los identificadores inválidos).

## US-71 · Ubicación de cada actividad

- **T01 Ubicación y referencia (backend).** El catálogo, el detalle, las similares y la programación incluyen
  `ubicacion` (nombre), `ubicacionReferencia` y `ubicacionDetalle` (sede, pabellón, aula, dirección, referencia y,
  solo si están cargadas en la colección `ubicaciones`, `latitud` y `longitud`). No se inventa ninguna dirección.
  Para agregar referencia o coordenadas basta con añadir esos campos al documento de la ubicación en MongoDB.
- **T02 Lugar en cada tarjeta (app).** Catálogo y programación muestran el lugar y su referencia. En la programación,
  tocar el lugar abre su pantalla, y el detalle tiene el botón «Ver ubicación».
- **T03 Pantalla de ubicación (app).** `UbicacionScreen`: lugar, ficha (solo los datos que existen), aviso si el
  lugar no está confirmado, «Abrir en el mapa» solo si hay coordenadas reales, y las otras actividades del mismo día
  en ese lugar.
- **T05 Imágenes de respaldo por área.** `respaldoPortada`: primero manda el área de Tecsup (las cinco áreas, con o
  sin tildes); si no tiene área, se deduce del nombre o la categoría (deportes, IA, robótica, laboratorio…).
- **T06 Carga de portadas con alternativa local.** Solo se descargan enlaces https o del propio backend. Mientras
  carga, o si no se puede descargar, se muestra la ilustración local.

## Cómo probarlo

- Backend: `mvn test -Dtest=ProgramacionDiaTest` y, con MongoDB,
  `MONGODB_E2E_URI=mongodb://localhost:27017 mvn test -Dtest=ProgramacionDiaMongoE2ETest`.
- App: `./gradlew testDebugUnitTest` (incluye `ProgramacionDiaTest`, `UbicacionYPortadaTest` e
  `InscripcionContratoTest`).
