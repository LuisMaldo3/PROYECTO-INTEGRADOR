# VYNK · Frontend (React + Vite)

Login con Google (cuentas `@tecsup.edu.pe`) conectado al backend `Usuario-back`.

## Cómo funciona

1. El botón oficial de Google (Google Identity Services) devuelve un **ID token**.
2. El frontend lo envía a `POST {VITE_API_URL}/auth/login` con el cuerpo `{ "idToken": "..." }`.
3. El backend valida el token con Google, verifica el dominio y responde `{ token, usuario }`.
4. El `token` (JWT de VYNK) se guarda en `localStorage` para las siguientes peticiones.

## Formas de entrar

- **Google** (cuentas `@tecsup.edu.pe`): funciona desde la primera vez; crea el usuario en MongoDB.
- **Correo y contraseña**: solo para quien ya entró con Google y creó su contraseña desde su perfil (`PUT /auth/password`). Así nadie puede registrar un correo ajeno sin verificarlo.

## Permitir o bloquear el acceso (tarea 4)

`src/services/acceso.js` decide, según la respuesta del backend, si se **permite** o se **bloquea** el acceso:

| Respuesta del backend | Resultado |
|---|---|
| 200 con `token` y `usuario` | **Permite**: se guarda la sesión y se muestra la pantalla de inicio. |
| 401 / 403 (correo que no es de Tecsup, contraseña incorrecta, token inválido) | **Bloquea**: sin sesión, se muestra el mensaje del backend. |
| 5xx, servidor apagado o 200 incompleto | **Bloquea** con un mensaje genérico. |

El rol no interviene: todas las cuentas entran como "usuario"; el rol de organizador solo lo asigna el administrador.
Pruebas automáticas: `npm test`.

## Configuración

1. **Google Cloud Console** → APIs y servicios → Credenciales → *Crear ID de cliente de OAuth* → tipo **Aplicación web**.
   - Orígenes autorizados de JavaScript: `http://localhost:5173`
   - (No hace falta URI de redirección.)
2. Copia `.env.example` como `.env.local` y pega el Client ID:
   ```
   VITE_API_URL=http://localhost:8081
   VITE_GOOGLE_CLIENT_ID=xxxxxxxx.apps.googleusercontent.com
   ```
3. En el backend, copia `.env.example` como `.env` y usa **el mismo** Client ID en `OAUTH_CLIENT_ID`.
4. Arranca ambos:
   ```
   # backend (puerto 8081)
   ./mvnw spring-boot:run
   # frontend (puerto 5173)
   npm install
   npm run dev
   ```

> Si cambias algún valor de `.env.local`, reinicia `npm run dev`.

## Errores comunes

| Síntoma | Causa |
|---|---|
| Google muestra `Error 400: origin_mismatch` | Falta `http://localhost:5173` en *Orígenes autorizados de JavaScript*. |
| Mensaje "No se pudo verificar tu cuenta" (401) | El Client ID del front y el del back (`OAUTH_CLIENT_ID`) no coinciden. |
| Mensaje "No pudimos conectar con el servidor" | Backend apagado, o `VITE_API_URL` apunta a otro puerto. |
| Error de CORS en la consola | El front no corre en `http://localhost:5173` (ver `CORS_ORIGINS` en el backend). |
