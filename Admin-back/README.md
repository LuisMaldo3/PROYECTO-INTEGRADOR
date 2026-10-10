# Admin-back (Django) — VYNK

Backend del administrador. Lee la **misma MongoDB** que `Usuario-back` y valida los
**JWT que emite Spring Boot** (mismo `JWT_SECRET`). No tiene login propio.

## Endpoints (US-23)

| Metodo | Ruta | Tarea |
|---|---|---|
| GET | `/api/usuarios/` | 1 · lista de usuarios |
| GET | `/api/usuarios/<id>/` | 2 · detalle de un usuario |

Ambos exigen `Authorization: Bearer <token>` de un usuario **activo con rol `administrador`**.
Solo devuelven `id, nombre, correo, rol, activo` (nunca `passwordHash`).

## Ejecutar (PowerShell)

```powershell
cd Admin-back
python -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r requirements.txt
Copy-Item .env.example .env     # y pon el MISMO JWT_SECRET y MONGODB_URI de Usuario-back
python manage.py runserver 8000
```

## Probar (tarea 7)

1. Tener un usuario administrador en Mongo:
   `db.usuarios.updateOne({correo:"tu@tecsup.edu.pe"},{$set:{rol:"administrador"}})`
2. Pedir token a Spring Boot (el usuario debe tener contraseña definida):
   ```powershell
   curl -X POST http://localhost:8081/auth/login-password -H "Content-Type: application/json" -d '{\"correo\":\"tu@tecsup.edu.pe\",\"password\":\"TU_CLAVE\"}'
   ```
3. Usar el token:
   ```powershell
   curl http://localhost:8000/api/usuarios/ -H "Authorization: Bearer TOKEN"
   curl http://localhost:8000/api/usuarios/ID/ -H "Authorization: Bearer TOKEN"
   ```
   Sin token → 401. Con token de un usuario normal → 403. Id inexistente → 404.

## Pruebas automáticas

```powershell
python manage.py test usuarios
```
No necesitan MongoDB (usan colecciones falsas). Cubren: sin token (401), token con otro secreto o vencido (401),
sesión cerrada (401), usuario normal (403), admin desactivado (401), lista sin `passwordHash`, detalle (200 y 404).

Los errores usan el mismo formato que Usuario-back: `{ "codigo": "...", "mensaje": "..." }`.
