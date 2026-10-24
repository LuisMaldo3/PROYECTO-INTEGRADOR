# Admin-back (Django) — VYNK

Backend del administrador. Lee la **misma MongoDB** que `Usuario-back` y valida los
**JWT que emite Spring Boot** (mismo `JWT_SECRET`). No tiene login propio.

## Endpoints (US-23)

| Metodo | Ruta | Tarea |
|---|---|---|
| GET | `/api/usuarios/` | 1 · lista de usuarios |
| GET | `/api/usuarios/<id>/` | 2 · detalle de un usuario |
| PATCH | `/api/usuarios/<id>/rol/` | US-24 · cambiar el rol (cuerpo `{"rol": "organizador"}`) |

Todos exigen `Authorization: Bearer <token>` de un usuario **activo con rol `administrador`**.
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
python manage.py test
```
No necesitan MongoDB (usan colecciones falsas). Cubren: sin token (401), token con otro secreto o vencido (401),
sesión cerrada (401), usuario normal (403), admin desactivado (401), lista sin `passwordHash`, detalle (200 y 404).

Los errores usan el mismo formato que Usuario-back: `{ "codigo": "...", "mensaje": "..." }`.

## Cambio de rol (US-24)

`PATCH /api/usuarios/<id>/rol/` con `{"rol": "usuario" | "organizador" | "administrador"}`.
Responde `200` con el usuario actualizado (`id, nombre, correo, rol, activo`). Errores:
`400 JSON_INVALIDO`, `400 ROL_INVALIDO` (rol vacío o fuera de la lista), `401`, `403 SOLO_ADMIN`,
`404 USUARIO_NO_ENCONTRADO`. Los roles válidos están en `usuarios/roles.py` y deben coincidir con
`Roles.java` de Usuario-back.

## Actividades (US-25)

Primero, una sola vez (y se puede repetir sin problema), crea las colecciones y las categorías iniciales:

```powershell
python manage.py preparar_actividades
```

| Metodo | Ruta | Para qué |
|---|---|---|
| GET | `/api/categorias/` | categorías del formulario (`{ categorias: [{ id, nombre }] }`) |
| GET | `/api/ubicaciones/` | ubicaciones del formulario (`{ ubicaciones: [{ id, nombre }] }`) |
| GET | `/api/actividades/` | lista de actividades por fecha y hora de inicio |
| POST | `/api/actividades/` | crea una actividad con `{ "nombre", "descripcion", "categoriaId", "ubicacionId", "fecha", "horaInicio", "horaFin" }` |

Todos exigen token de administrador. `POST` responde `201` con la actividad creada
(`id, nombre, descripcion, categoria {id, nombre}, ubicacion {id, nombre}, fecha, horaInicio, horaFin, creadaPor, creadaEn`). Si algo no pasa la revisión responde
`400 DATOS_INVALIDOS` con `errores` por campo (`nombre`, `descripcion`, `categoriaId`, `ubicacionId`, `fecha`, `horaInicio`, `horaFin`);
límites: nombre 3–80, descripción 10–500 caracteres y categoría existente. Los mismos límites están en la web (`validacionActividad.js`).

### Fecha y horario (US-26)

`fecha` va como `AAAA-MM-DD` (un día real del calendario) y es obligatoria; `horaInicio` como `HH:MM` en 24 h y es
obligatoria; `horaFin` también `HH:MM`, es opcional y, si viene, debe ser **después** de la de inicio. La lista sale
ordenada por fecha y hora; las actividades creadas antes de esta versión (sin fecha) quedan al final.

### Ubicación (US-27)

Cada actividad nueva lleva una `ubicacionId` que debe existir en la colección `ubicaciones` (Auditorio, Patio central y
Polideportivo vienen de `python manage.py preparar_actividades`). En las respuestas sale como `ubicacion: { id, nombre }`;
las actividades creadas antes de esta versión devuelven `ubicacion: null`. Para agregar más lugares, súmalos a
`UBICACIONES_INICIALES` en `actividades/ubicaciones.py` y vuelve a correr el comando.


## Solucion a HTTP 401 / TOKEN_INVALIDO (Oct 2026)

El login de la web se hace en **Usuario-back (Spring Boot, puerto 8081)**. El JWT
emitido por Spring Boot es verificado por **Admin-back (Django, puerto 8000)**.
Ambos deben usar **la misma clave REAL de firma HMAC HS256** (incluido el
mismo modo de interpretar la clave: texto UTF-8 o Base64).

1. Deja encendidos Usuario-back y Admin-back.
2. En el backend **Usuario-back** localiza la clave usada realmente para FIRMAR
   el JWT (`JWT_SECRET`, variable de entorno, application.properties/yml o
   configuración del proveedor de tokens). Ese archivo NO viene en estos ZIP.
3. En `Admin-back/.env` configura `JWT_SECRET` para que sea exactamente la
   MISMA clave en bytes. NO cambies el secreto a ciegas; si la implementacion
   de Spring Boot decodifica Base64, el código de verificación deberá utilizar
   ese formato también. No copies ni publiques esta clave en React.
4. Reinicia Django para que relea su `.env`.
5. Vuelve a iniciar sesión en la web para obtener un JWT fresco.
6. En PowerShell, ejecuta:

   ```powershell
   cd Admin-back
   .\.venv\Scripts\Activate.ps1
   python manage.py diagnosticar_token
   ```

   Pega el token reciente en la consola **local**; no aparecerá al escribirlo.
   Puedes copiarlo desde las herramientas de desarrollador de tu navegador,
   en la respuesta de `POST http://localhost:8081/auth/login-password`.
   **No lo compartas por chat, repositorios o capturas públicas.**

   - `La firma NO coincide`: comprueba la clave o la codificación del JWT.
   - `La firma se valido, pero ... expiro`: inicia sesión nuevamente.
   - `JWT VALIDO`: la firma y claims son correctos; si la API da 401 después,
     comprueba el mismo `MONGODB_URI` en ambos backends, el usuario activo, su
     rol `administrador` y los tokens revocados.

El backend conserva los controles de **firma, expiración, revocación, cuenta
activa y rol administrador**. Se añadieron mensajes de log para distinguir
fallos sin mostrar tokens ni secretos. El frontend valida el JWT con Django
**antes** de mostrar el panel; ya no abre una sesión que Django rechaza.

**Nota sobre este ZIP:** no incluye `.env` ni `.venv`. Conserva tu `.env` local
actual al copiar el proyecto; nunca subas credenciales a GitHub.
