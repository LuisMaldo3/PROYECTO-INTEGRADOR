# US-40 · Clasificación oficial disponible para estudiantes

- Consulta autenticada de solo lectura: `GET /api/competencias/clasificacion` (JWT de estudiante).
- Muestra las cinco áreas institucionales, puntajes acumulados, primeros puestos y desglose por competencia.
- Recalcula a partir de `resultados_competencias` publicados y competencias vigentes; no modifica MongoDB.
- En registros duplicados usa la publicación más reciente, evita inscripciones/puestos repetidos y descarta áreas o puntajes inválidos, de acuerdo con el cálculo administrativo de US-39.
- Las categorías y los filtros comparativos de US-40 están en el panel administrador (`Admi-Fron`).
- Verificación: comparar las cinco áreas y los puntos totales de `/api/competencias/clasificacion` frente a `/api/clasificacion/` y `/api/estadisticas/comparacion-categorias/` con el mismo MongoDB.
