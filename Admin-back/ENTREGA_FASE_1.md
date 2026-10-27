# ZIP 1 — Backend Django: avisos de ubicación y cancelación

Este ZIP contiene el proyecto Admin-back COMPLETO, listo para sustituir `Admin-back/` del repositorio. Se basa en el ZIP que proporcionaste y preserva las funciones anteriores. No incluye el ZIP 2 (programación de avisos).

## Implementado
- Detecta cambios reales de `ubicacionId` al editar una actividad, conservando el seguimiento de cambios de horario ya existente.
- Con `avisarInscritos: true` registra `CAMBIO_UBICACION` en la colección `notificaciones` para inscritos activos; no duplica usuarios ni notifica inscripciones canceladas.
- `PATCH /api/actividades/<id>/cancelar/` marca la actividad como cancelada mediante actualización condicional e inserta notificaciones privadas `CANCELACION_ACTIVIDAD`; mantiene las inscripciones y devuelve `avisosGenerados`.
- El backend reporta `avisosError` si MongoDB falla tras haber guardado cambios; no indica éxito falso del envío.
- Incluye `actividades/tests_notificaciones.py` con casos de cambio real, deduplicación y cancelación.

## Cómo aplicar
1. Guarda una copia del proyecto actual.
2. Copia la carpeta `Admin-back` de este ZIP sobre `PROYECTO99/Admin-back`.
3. **Conserva** el archivo `Admin-back/.env` del equipo original; NO está dentro de este ZIP.
4. Comprueba `MONGODB_URI`, `JWT_SECRET` (idéntico al de Spring Boot) y `CORS_ORIGINS=http://localhost:5174`.
5. Ejecuta `python manage.py test` desde `Admin-back` con el entorno virtual activo y luego `python manage.py runserver 8000`.
6. Confirma que los datos de la colección `inscripciones` contienen `actividadId` y `usuarioId`/`estudianteId`; la app de estudiantes debe leer sus notificaciones desde `notificaciones` con validación del usuario autenticado.

## Commit — desde la raíz PROYECTO99
*El commit solo describe la funcionalidad de este ZIP. No ejecutarlo antes de revisar `git diff` y `git status`.*

```powershell
git add Admin-back/actividades/cambios_ubicacion.py Admin-back/actividades/notificaciones_inscritos.py Admin-back/actividades/servicio.py Admin-back/actividades/views.py Admin-back/actividades/tests_notificaciones.py
git commit -m "feat(actividades): notificar cambios de ubicación y cancelaciones" -m "Luis: detectar modificaciones reales de la ubicación al editar actividades y generar avisos para inscritos activos previa confirmación. Jordi: marcar actividades como canceladas de forma segura, conservar inscripciones y registrar notificaciones de cancelación. Incluir validación de destinatarios, control de errores y pruebas unitarias."
```
