# ZIP 2 — Backend Django: publicación programada de avisos

Este ZIP contiene **todo Admin-back**, tanto el código del ZIP 1 como las nuevas tareas del ZIP 2. **Al usar ZIP 2 sobre ZIP 1 no se pierde ninguna funcionalidad:** la carpeta final completa es la del ZIP 2. El proyecto comparte la misma base Mongo de Usuario-back.

## Implementado
- `POST /api/avisos/`: acepta `{ "titulo", "mensaje", "publicarEn": "2026-11-02T16:00:00-05:00" }`. `publicarEn` opcional/null publica ahora; cuando existe requiere ISO 8601 con zona horaria y fecha FUTURA.
- Persistencia: `estado: programado | publicado`, `publicarEn` en UTC, `publicadoEn`, `creadoEn`, `autorId`.
- `GET /api/avisos/` (requiere admin) incluye publicados y programados para la lista del panel.
- `GET /api/avisos/publicados/` solo devuelve comunicados publicados, incluso datos antiguos sin el campo `estado`; nunca incluye programados ni devuelve `autorId` público.
- `python manage.py publicar_avisos` pasa a publicados los avisos cuyo instante UTC ya llegó, con actualización atómica. Las consultas también recuperan programaciones vencidas si el planificador no se ejecutó.
- Pruebas: `actividades/tests_avisos_programados.py` valida fechas, zonas horarias, creación y filtro de publicación.

## IMPORTANTE: ejecución puntual
**Para publicar a la hora establecida sin depender de que alguien abra el panel es obligatorio ejecutar `python manage.py publicar_avisos` con un planificador cada minuto.** Django no mantiene trabajos automáticos por sí solo. En una computadora Windows, usa Programador de tareas (tarea repetitiva cada minuto) con el Python del entorno virtual y argumento `manage.py publicar_avisos` en la carpeta `Admin-back`. En producción (VPS) un cron `* * * * *` o trabajador programado equivalente. No uses un proceso que solo corre cuando hay visitas.

```powershell
cd Admin-back
.venv\Scripts\Activate.ps1
python manage.py test
python manage.py publicar_avisos
python manage.py runserver 8000
```

**Config:** preservar tu `.env` original (no se distribuye); `JWT_SECRET` DEBE coincidir con Usuario-back y `CORS_ORIGINS=http://localhost:5174`. La notificación que recibe cada estudiante requiere integración con la app de estudiantes; este ZIP registra los eventos en MongoDB.

## Commit — desde la raíz PROYECTO99
El mensaje se limita a las funcionalidades del ZIP 2; debe realizarse **después** del commit del ZIP 1. `git diff` antes de confirmar.

```powershell
git add Admin-back/actividades/avisos.py Admin-back/actividades/views.py Admin-back/actividades/urls.py Admin-back/actividades/management/commands/publicar_avisos.py Admin-back/actividades/tests_avisos_programados.py
git commit -m "feat(avisos): permitir publicaciones programadas por fecha y hora" -m "Mishel: validar y guardar la fecha y hora de publicación con conversión a UTC; diferenciar avisos programados y publicados; ejecutar publicación de avisos pendientes mediante comando programable; incorporar listado administrativo, consulta pública de avisos publicados y pruebas de programación."
```
