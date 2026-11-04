# US-38 · Intereses de estudiantes (React)

La nueva opción **Intereses** en el menú lateral consulta `GET /api/estadisticas/intereses/` en Admin-back Django y muestra estudiantes activos, estudiantes con/sin intereses, categorías, porcentajes y número de selecciones.

El botón **Resumir con IA** solicita `GET /api/estadisticas/intereses/?ia=1`. No se envía la clave Gemini a React. El panel conserva estadísticas si la IA falla o no está configurada.

Continúa incluidas las pantallas y correcciones de US-35, US-36 y US-37. Ejecuta `npm install`, `npm test`, `npm run build` y `npm run dev` en el frontend tras copiar el proyecto.
