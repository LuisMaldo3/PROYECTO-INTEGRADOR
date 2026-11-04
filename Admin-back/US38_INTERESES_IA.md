# US-38 · Intereses predominantes de estudiantes

## Tareas del Sprint 3
- **US-38-T01 (Jordi):** contar intereses guardados por estudiante activo desde MongoDB.
- **US-38-T02 (Luis):** resumen de intereses con Gemini únicamente al solicitarlo.
- **US-38-T03 (Mishel):** pantalla «Intereses» con gráfico de barras y filtros en el panel React.
- **US-38-T04 (Todos):** pruebas de conteo, porcentajes, duplicados, categorías desconocidas y datos que se comparten con IA.

## API protegida (requiere token JWT de administrador)

`GET /api/estadisticas/intereses/`: conteos y ranking de preferencias reales. No utiliza IA por defecto.

`GET /api/estadisticas/intereses/?ia=1`: mismo conteo y resumen opcional con Gemini.

Las colecciones son **`intereses`** (catálogo, `id/nombre/orden`), **`intereses_usuario`** (preferencias por `_id` de usuario) y **`usuarios`** (estado/rol). MongoDB es compartida con Usuario-back de Spring Boot. Django usa PyMongo, **no ORM**.

Se excluyen cuentas inactivas, administradores, selecciones inexistentes y duplicados por usuario. Los organizadores activos se incluyen porque su cuenta continúa siendo la de un estudiante. «Sin intereses» comprende quienes no configuraron o decidieron omitir la selección.

**Porcentajes:** estudiantes activos con intereses elegidos que seleccionaron cada categoría / estudiantes activos con alguna categoría válida. Como se pueden seleccionar varias, los porcentajes no necesariamente suman 100 %. La IA solo recibe cifras agregadas; nunca nombres, correos ni identificadores.

## Variables

Configura `GEMINI_API_KEY` únicamente en `Admin-back/.env`. Sin clave, el gráfico y estadísticas siguen funcionando y el resumen informa que la IA no está configurada.

## Pruebas

- `python -m unittest actividades.tests_intereses -v` — pruebas puras sin DB ni Django.
- `python manage.py test actividades.tests_intereses_api` — requiere dependencias de Django instaladas.
- Frontend: `npm test`, después `npm install` y `npm run build`.

Para pruebas end-to-end se requiere un MongoDB conectado que contenga documentos reales en las tres colecciones y JWT administrador vigente.
