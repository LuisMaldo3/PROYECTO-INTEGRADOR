# «¿Qué hay ahora?» y «No puedo asistir» en la app

## US-78 · Como estudiante, quiero pedirle a la IA que busque actividades disponibles en ese momento

| Tarea | Responsable | Qué se hizo |
|---|---|---|
| 1. Buscar las actividades de este momento | Mishel | `ActividadesAhoraService`. Con la hora real de Lima busca en el catálogo publicado (programadas, con cupos, de hoy): **en curso** (si quedan al menos 10 min, para que valga la pena ir) y las que **empiezan en la próxima hora**. Cada una trae `estadoAhora`, `minutos`, `detalleAhora` («En curso · termina en 41 min (19:20)»), si ya estás `inscrito` y con qué inscripción tuya se cruza (`cruzaCon`). Si no hay nada, dice cuál es la siguiente de hoy. El chat la usa cuando la pregunta es «¿Qué hay ahora?» o similar («¿qué puedo hacer ahorita?», «actividades en este momento»); la respuesta se guarda en la conversación como cualquier otra. También está en `GET /api/app/ahora`. |
| 2. Botón «¿Qué hay ahora?» en el chat | Luis | Botón siempre visible encima de la barra para escribir y tarjeta destacada en la bienvenida. Envía la pregunta con un toque. |
| 3. Mostrar la lista de actividades de este momento | Luis | Tarjetas en el chat: verdes con punto vivo si están en curso («En curso · quedan 53 min»), azules si empiezan pronto, con horario, lugar y avisos («Ya estás inscrito», «Se cruza con …», «Quedan 3 cupos»). Al tocar una se abre su detalle con inscripción. La burbuja muestra solo la introducción (el detalle está en las tarjetas). |
| 4. Probar | Todos | Pruebas unitarias y E2E, y capturas reales del chat. |

## US-79 · Como estudiante, quiero consultar a la IA alternativas cuando no pueda asistir a una actividad

| Tarea | Responsable | Qué se hizo |
|---|---|---|
| 1. Buscar alternativas | Jordi | `GET /api/app/alternativas/{actividadId}?motivo=horario\|otro` (`AlternativasAsistenciaService`). Solo para una actividad en la que estás inscrito (si no, 404). Propone actividades programadas, con cupos, futuras, en las que no estás inscrito y que no se cruzan con tus otras inscripciones (el horario que dejas queda libre) ni con tus clases si confirmaste tu horario. Con «No puedo a esa hora» descarta las que coinciden con ese horario. Se ordenan por parecido (área, categoría, modalidad, tema) y cercanía; con clave de Gemini, la IA elige el orden y explica cada una (solo entre esas candidatas); sin IA se explican por reglas y se dice honestamente. El capitán de un equipo no puede cambiarse en un toque (cancelar retiraría al equipo). |
| 2. Opción «No puedo asistir» | Mishel | En el detalle de cualquier actividad inscrita (catálogo, agenda, recomendaciones, mi próxima) y en cada inscrita de la agenda inteligente. |
| 3. Mostrar las alternativas propuestas | Mishel | Hoja inferior: motivo (No puedo a esa hora / Otro motivo), origen (IA o por parecido), tarjetas con día, hora, lugar, etiquetas («Misma categoría», «El martes 24 a las 15:00», «Quedan 2 cupos») y el porqué. «Ver detalle» o «Cambiarme»: confirma y primero se inscribe en la nueva; solo si eso salió bien cancela la anterior (si la cancelación falla, lo avisa). |
| 4. Probar | Todos | Pruebas unitarias y E2E con Mongo real, y capturas reales. |

## Pruebas

- Backend: 365 pruebas; solo fallan las 3 de siempre (`BaseDatosInicializadorTest` ×2 y `VynkApplicationTests`, que necesita Atlas). Nuevas: `AhoraYAlternativasTest` (6) y `AhoraYAlternativasMongoE2ETest` (4, con `MONGODB_E2E_URI`).
- App: 151 pruebas unitarias pasan (nueva: `AhoraYAlternativasTest`, 7, incluido el orden «inscribir y luego cancelar») y el APK compila.
