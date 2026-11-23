# Catálogo por intereses y pantalla de inicio

## US-80 · Como estudiante, quiero visualizar primero las actividades relacionadas con mis intereses

| Tarea | Responsable | Qué se hizo |
|---|---|---|
| 1. Ordenar el catálogo según los intereses | Jordi | `GET /api/catalogo` (`PersonalizacionCatalogo`): con sesión y con intereses elegidos, primero van las actividades «Para ti» (las que coinciden con más intereses arriba) y después el resto; dentro de cada grupo se mantiene el orden por fecha y hora. Cada actividad trae `paraTi`, `afinidad` y `motivoParaTi` («Te interesa: deportes y música»). El interés se busca como palabra y sin tildes en nombre, descripción, categoría y área («ia» no coincide con «industria»). `orden=fecha` lo desactiva. Funciona junto con los filtros de horario, categoría y área. La respuesta dice el `orden` usado, cuántas son `paraTi` y los `intereses`. Sin sesión o sin intereses responde como antes. |
| 2. Programar el orden del catálogo | Luis | Selector «Para ti primero / Por fecha» que se recuerda entre aperturas; viaja al servidor como `orden=fecha` (no cuenta como filtro). El catálogo arma una sección «Para ti» arriba y debajo «Más actividades» por día. |
| 3. Marcar las actividades «Para ti» | Luis | Insignia «✦ Para ti» sobre la portada y el motivo («Te interesa: deportes») en la tarjeta. Encabezado «Para ti · N actividades — Según tus intereses: …». |
| 4. Probar | Todos | Pruebas unitarias y E2E (abajo) y captura real del catálogo. |

## US-81 · Como estudiante, quiero tener una pantalla de inicio con mis próximas actividades

| Tarea | Responsable | Qué se hizo |
|---|---|---|
| 1. Servicio de resumen de inicio | Mishel | `GET /api/app/inicio` (`InicioAppService`) en una sola llamada: saludo con mensaje («Hoy tienes 1 actividad.» / «Tu próxima actividad es mañana a las 14:00.»), resumen (hoy, esta semana, inscritas, equipos por confirmar), mis próximas actividades (individuales y de equipo, una vez cada una, sin las canceladas ni las que terminaron) con portada, lugar, «cuándo» y estado del equipo, destacadas y avisos. `vigenteHasta` = 2 min. |
| 2. Conectar la app | Jordi | `InicioApi` + `leerResumenInicio`. |
| 3. Programar la carga | Jordi | La carga programada de la app: se ve al instante con lo guardado, se actualiza cada 2 min, al inscribirse o cancelar, y sin conexión muestra lo último con aviso. Reemplaza las consultas separadas de agenda y avisos. |
| 4. Pantalla de inicio con las próximas actividades | Luis | Portada con el saludo y las cifras, «Mi próxima actividad», carrusel «Tus próximas actividades» (portada, «Empieza en…»/«Ahora», lugar y equipo), accesos rápidos, destacadas, avisos y «Próximamente». |
| 5. Probar | Todos | Pruebas y capturas reales. |
| T06. Avisos recientes con imágenes | — | Carrusel de avisos con su imagen (o una ilustración si no tiene), «Nuevo» si es de los últimos 3 días; al tocar se lee completo. **Se corrigió:** las imágenes subidas o generadas de los avisos solo traen `archivoId` y la app no las mostraba; ahora se sirven desde `/api/portadas/archivo/{id}` (también en `AvisosApi`). |
| T07. Portada en actividades y competencias destacadas | — | Destacadas: por venir, con cupos y en las que no estás inscrito, priorizando competencias, las que tienen portada, las «Para ti» y las de últimos cupos. Siempre con portada: la publicada o, si no tiene, la ilustración de su tema. Etiqueta «Competencia», «Para ti», «Últimos cupos». |

## Pruebas

- Backend: 373 pruebas; solo fallan las 3 de siempre (`BaseDatosInicializadorTest` ×2 y `VynkApplicationTests`, que necesita Atlas). Nuevas: `InteresesEInicioTest` (6) y `InteresesEInicioMongoE2ETest` (2, con `MONGODB_E2E_URI`). Las pruebas anteriores del catálogo con filtros siguen pasando.
- App: 157 pruebas unitarias pasan (nueva: `InteresesEInicioTest`, 8) y el APK compila.
