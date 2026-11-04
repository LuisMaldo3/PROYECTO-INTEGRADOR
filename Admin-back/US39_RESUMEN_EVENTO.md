# VYNK · Sprint 3 · US-39 (Backend Django)

**Historia:** Como administrador, quiero obtener un resumen automático del desarrollo del T&C Week.

## Implementación

- `actividades/reporte_evento.py`: reúne actividades, estadísticas reales de inscritos de US-35 y resultados oficiales publicados; recalcula los puntajes acumulados de las 5 áreas sin guardar un acumulado adicional.
- `actividades/urls.py`: `GET /api/estadisticas/resumen-evento/` (estadísticas) y `GET /api/estadisticas/resumen-evento/?ia=1` (estadísticas + narración Gemini opcional).
- `actividades/views.py`: endpoint de **solo lectura protegido con JWT para administradores**.
- `actividades/tests_reporte_evento.py`: 7 pruebas de cálculo puro, incluyendo duplicados, resultados pendientes, competencias canceladas, cinco áreas, ganador del primer puesto y consistencia del recálculo.

La API omite resultados pendientes o de competencias canceladas. Nunca inventa puntos ni considera el evento finalizado por la sola publicación de resultados. Inscripciones activas representa participaciones por actividad, no personas únicas del evento. Los puntos se atribuyen al `areaTecsup` validado y ya guardado en el resultado oficial.

## Variables

`GEMINI_API_KEY` en el entorno Django para activar la generación de texto (sin clave, la parte numérica funciona). El navegador no recibe esta clave.

## Pruebas

```bash
cd Admin-back
python -m unittest actividades.tests_reporte_evento -v
python manage.py test actividades.tests_reporte_evento
```

En el entorno de generación, las 7 pruebas de cálculo pasan. La prueba Django completa y la consulta a MongoDB necesitan el entorno local configurado.

## Tareas

- US-39-T01: Reunir los datos del evento — Jordi.
- US-39-T02: Pedir a la IA el resumen del evento — Luis.
- US-39-T05: Calcular acumulado de las 5 áreas — Pendiente de asignación.
- US-39-T06: Incluir ganadores y puntajes — Pendiente de asignación.
- US-39-T07: Acumular solo cinco áreas — Pendiente de asignación.
- US-39-T08: Evitar duplicados al recalcular — Pendiente de asignación.
- US-39-T09: Resultados por competencia — Pendiente de asignación.
- US-39-T10: Verificar la atribución de puntos — Pendiente de asignación.
