# Mi próxima actividad + Asistente en el celular + rediseño de la app

## US-74 · Como estudiante, quiero consultar rápidamente dónde se realizará mi próxima actividad

| Tarea | Responsable | Qué se hizo |
|---|---|---|
| 1. Backend: entregar la ubicación de mi próxima actividad | Luis | `GET /api/mi-proxima` (`MiProximaActividadController` + `MiProximaActividadService`). Toma las inscripciones del estudiante, descarta las que ya terminaron (sin hora de fin se asume 1 h), las canceladas y las no visibles, y ordena por inicio. Devuelve `tiene`, `actividad` (detalle completo con ubicación: lugar, sede, pabellón, aula, referencia) más `empiezaEn`, `terminaEn`, `enCurso`, `minutosParaEmpezar`, y `despues` (la siguiente). Zona horaria America/Lima. |
| 2. Acceso rápido «Mi próxima actividad» | Jordi | `TarjetaMiProxima` en Inicio, montada sobre la portada: estado ("Empieza en 25 min" / "Ahora mismo · termina 11:00" con punto vivo), el lugar en grande con pabellón, aula y referencia, botón «Cómo llegar» (abre la pantalla de ubicación) y «Después: …». Si no hay inscripciones invita a explorar. |
| 3. Programar la carga de mi próxima actividad | Luis | `MiProximaApi` + `debeRecargarMiProxima`: se recarga cada 5 minutos, cuando la actividad termina y cuando llega una alerta de cambio (horario, ubicación o cancelación) por `CambiosAgenda`. El estado se recalcula cada 30 s sin volver a pedir datos. |
| 4. Probar | Todos | `MiProximaYSugerenciasTest` (7), `MiProximaYAsistenteMongoE2ETest` (4, Mongo real) y `MiProximaYChatTest` (11 en la app). |

## US-75 · Como estudiante, quiero utilizar el asistente virtual desde mi celular

| Tarea | Responsable | Qué se hizo |
|---|---|---|
| 1. Preparar el servicio del asistente para la app | Mishel | `GET /api/asistente/sugerencias`: hasta 5 preguntas sugeridas armadas con la próxima actividad del estudiante ("¿Dónde se realiza X?"), otra inscrita ("¿Qué requisitos tiene Y?"), sus intereses ("¿Qué actividades de inteligencia artificial hay mañana?") y preguntas generales. |
| 2. Conectar la app con el asistente | Luis | `AsistenteApi`: errores HTTP tipados (`ErrorAsistente` con código), `sugerencias(token)` con respaldo local si falla la red. |
| 3. Programar la conversación | Luis | `ConversacionChat`: valida la pregunta (vacía o > 600 caracteres), envía, si el historial quedó desfasado (409) lo sincroniza y reintenta una vez; 400/401/403 no se reintentan. Los mensajes fallidos quedan con «Reintentar» y «Editar». |
| 4. Crear la pantalla del chat | Jordi | Nueva pantalla: cabecera con avatar y estado en línea, bienvenida con tarjetas de sugerencias, chips de sugerencias (no repite lo ya preguntado), burbujas asimétricas con degradado, indicador «escribiendo…», barra de texto tipo píldora con botón animado. Si la respuesta trae tarjetas (lugares, actividades, requisitos) la burbuja muestra solo la introducción y no repite el detalle; al llegar la respuesta se ve la pregunta arriba y la respuesta desde el principio. |
| 5. Probar | Todos | Pruebas unitarias de conversación, validación, sugerencias y texto visible; E2E que envía las sugerencias al asistente real. |

## Rediseño visual (mismos colores)

- `ui/comun/Movimiento.kt`: sistema de movimiento — aparición escalonada (`aparecer`), presión con resorte (`presionable`), sombra suave, esqueletos de carga con brillo, punto vivo, fondo degradado animado, íconos con degradado.
- Inicio: portada animada con saludo y fecha, tarjeta «Mi próxima actividad» superpuesta, «Próximamente» sin repetir la próxima, cuadrícula de 6 accesos rápidos.
- Barra inferior flotante con píldora animada en la pestaña activa.
- Tarjetas con esquinas 18–22 dp, sombra, borde suave y velo degradado en las portadas (Catálogo, Programación, Próximamente).
- Esqueletos en vez de spinners mientras carga.

## Pruebas

- Backend: 338 pruebas; solo fallan las 3 que ya fallaban antes (`BaseDatosInicializadorTest` ×2 y `VynkApplicationTests`, que necesita Atlas).
- App: todas las pruebas unitarias pasan y el APK compila. Las pantallas se renderizaron contra el backend real con datos de demo para revisar el diseño.
