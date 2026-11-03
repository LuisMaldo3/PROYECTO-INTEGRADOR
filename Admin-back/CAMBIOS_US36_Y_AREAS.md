# VYNK · Sprint 3 · US-36 + corrección de asignación de áreas

## Corrección de áreas de usuario
- `usuarios/mongo.py`: se agrega `inscripciones()` para acceder a la colección compartida.
- `PATCH /api/usuarios/{id}/area/`: mantiene la validación de las cinco áreas oficiales, actualiza la cuenta y propaga el área verificada a las inscripciones activas. Antes la vista llamaba un método inexistente (`mongo.inscripciones()`), que ocasionaba respuesta HTTP 500 **después de actualizar el usuario**.
- Pruebas: `usuarios/tests_area_usuario.py`.

## US-36 · Supervisión de reemplazos y cupos (SOLO LECTURA)
- `GET /api/estadisticas/reemplazos/` devuelve cupos definidos, cupos ocupados, personas/equipos, alertas por sobrecupo e historial confirmado.
- Protegido por JWT de administrador y permiso `actividades.gestionar`.
- El conteo usa inscripciones activas y respeta que una competencia por equipos consume **un cupo por equipo**, no uno por integrante.
- **No permite al administrador sustituir participantes ni cambiar el máximo desde este endpoint.**
- Pruebas: `actividades/tests_reemplazos.py`.

### Contrato de auditoría de sustituciones
El backend del organizador (Spring Boot), **no incluido en estos ZIP**, debe persistir un registro cuando el cambio de estudiante se confirme; este backend administrador SOLO consulta esa colección.

Colección MongoDB: `reemplazos_inscripciones`. Ejemplo de campos:

```json
{
  "actividadId": "id-de-actividad",
  "estudianteSalienteId": "id-de-estudiante-saliente",
  "estudianteEntranteId": "id-de-estudiante-entrante",
  "organizadorId": "id-de-organizador",
  "fecha": "2026-11-11T04:00:00Z",
  "estado": "realizado"
}
```

Opcionales: `nombreSaliente`, `nombreEntrante`. Se ignoran estados pendientes/fallidos y registros incompletos. **No se interpreta una cancelación como un reemplazo**. Si todavía no se escribe esta auditoría desde Spring Boot, se verá historial vacío y el conteo real de cupos igualmente funcionará.

Para ejecutar: instalar `requirements.txt` en un entorno virtual, configurar `.env` en Django (no se distribuyen credenciales), iniciar MongoDB y Spring Boot; luego `python manage.py test usuarios.tests_area_usuario actividades.tests_reemplazos`.
