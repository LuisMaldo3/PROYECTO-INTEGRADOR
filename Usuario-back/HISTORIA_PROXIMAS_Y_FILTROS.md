# Historias: próximas actividades (US-72) y filtros por horario y categoría (US-73)

## US-72 · Qué actividad comienza próximamente

- **T01 Servicio (backend).** `GET /api/proximas?limite=3` (`ProximasActividadesController` y
  `ProximasActividadesService`). Devuelve las actividades programadas que todavía no empiezan, de la más cercana a
  la más lejana (de 1 a 10; por defecto 3). Cada una trae lo mismo que el catálogo más:
  - `empiezaEn`: el momento exacto, en ISO.
  - `minutosParaEmpezar`: redondeado hacia arriba, nunca 0.
  - `inscrito`: si el estudiante está inscrito.

  No incluye canceladas, ni las que ya empezaron, ni las que no tienen fecha y hora válidas.
- **T02 Carga programada (app).** La tarjeta pide la lista al entrar al Inicio y cada 5 minutos. También la pide
  antes si la primera ya empezó o si llega una inscripción, una cancelación o un cambio. Entre recargas, el
  «Empieza en N min» se actualiza cada 30 s con el reloj del celular, sin gastar datos. Al salir del Inicio, la carga
  se detiene.
- **T03 Tarjeta «Próximamente» (app).** Está en el Inicio, debajo de la bienvenida. Muestra la actividad más cercana
  en grande, con «Empieza en 25 min» (resaltado si faltan 15 minutos o menos), su hora, su lugar y «Inscrito» si
  corresponde, y debajo las dos siguientes. Al tocar una se abre su detalle, desde donde se puede inscribir. Los
  textos son naturales: «Empieza en 2 h 15 min», «Mañana a las 10:00», «jue 26/11 a las 19:00», siempre en hora de
  Perú.

## US-73 · Filtrar actividades por horario y categoría

- **T01 Filtros en el servicio (backend).** `GET /api/catalogo` acepta estos parámetros opcionales:
  - `franja`: `manana` (antes de las 12:00), `tarde` (de 12:00 a 17:59) o `noche` (desde las 18:00).
  - `desde` y `hasta`: horas en formato HH:mm.
  - `fecha`: AAAA-MM-DD.
  - `categoria`: el id de la categoría, por ejemplo `deportes`.
  - `area`: una de las cinco áreas, con o sin tildes.

  Un valor mal escrito responde 400. La respuesta también trae `filtros` (opciones con su cantidad, contadas sobre
  todas las actividades) y `aplicados`. Sin parámetros responde igual que antes, así que la web no cambia. El
  listado interno que usan la agenda sugerida, el asistente y las recomendaciones sigue sin filtros.
- **T02 Filtros (app).** Hay tres filas de chips en Actividades: Horario, Categoría y Área. Cada chip muestra cuántas
  actividades tiene, y tocar un chip ya elegido lo quita. Con algún filtro activo aparece «Quitar filtros».
- **T03 Aplicación (app).** Cada cambio pide la lista filtrada al servidor y cancela el pedido anterior. Los filtros se
  conservan al girar la pantalla. Si no hay resultados aparece «Ninguna actividad coincide», con el botón para quitar
  los filtros.
- **T05 Las cinco áreas.** La fila Área siempre muestra las cinco áreas de Tecsup, cada una con su color, aunque
  alguna no tenga actividades.

## Cómo probarlo

- Backend: `mvn test -Dtest=FiltrosYProximasTest` y, con MongoDB,
  `MONGODB_E2E_URI=mongodb://localhost:27017 mvn test -Dtest=FiltrosYProximasMongoE2ETest`.
- App: `./gradlew testDebugUnitTest` (incluye `ProximasYFiltrosTest`).
