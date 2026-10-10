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

Todas las cuentas `@tecsup.edu.pe` entran; el rol no bloquea el ingreso, solo puede cambiar lo que se ve después (ver «Menú y pantallas»). El rol de organizador solo lo asigna el administrador.
Pruebas automáticas: `npm test`.

## Menú y pantallas

Después de iniciar sesión, la web ocupa **toda la pantalla**: una columna lateral izquierda con el logo y el menú (como en Canvas), y a la derecha la cabecera con el título de la sección y el contenido. En celular el menú pasa a una barra fija abajo, con íconos. Los datos de la persona y «Cerrar sesión» están dentro de Perfil. El login mantiene su diseño con la foto de Tecsup.

Las pantallas son las del usuario (estudiante). La lista está en un solo lugar: `PANTALLAS` en `src/services/permisos.js` (ruta, título, descripción y roles); la estructura está en `src/components/Shell.jsx` y `Shell.css`.

| Pantalla | Dirección | Estado |
|---|---|---|
| Inicio | `#/inicio` | vacía (US-04, US-19) |
| Catálogo | `#/catalogo` | vacía (US-08, US-14) |
| Agenda | `#/agenda` | vacía (US-07, US-09) |
| Asistente | `#/asistente` | vacía (US-12) |
| Perfil | `#/perfil` | datos, contraseña opcional y **Cerrar sesión** |

- Cada pantalla vacía tiene su propio archivo en `src/components/` (`Inicio.jsx`, `Catalogo.jsx`, `Agenda.jsx`, `Asistente.jsx`): se llena cuando toque su historia.
- **Guardar el rol:** el rol viaja en `usuario.rol` y se guarda en `localStorage` con la sesión, siempre en minúsculas. Un rol que no se reconoce (o que falta) se guarda como `usuario`.
- **Bloqueo:** una pantalla puede limitarse a ciertos roles con `roles: [...]`. Si alguien abre la dirección de una pantalla que no le toca, aparece «Acceso no autorizado»; si la dirección no existe, «Pantalla no encontrada».
- **Por ahora solo hay pantallas de usuario**, y las ven todos los roles. Las pantallas del organizador se agregan después a `PANTALLAS` con `roles: ['organizador']` y aparecen solas en el menú de quien tenga ese rol. El administrador usa su propia web (React + Django).

> **Esto no es seguridad.** Ocultar o bloquear pantallas en la web solo mejora lo que ve la persona: cualquiera puede alterar el navegador. La protección de verdad está en el backend, que ya convierte el rol del token en `ROLE_*` para exigirlo en cada endpoint (`hasRole`).

## Rol y «quién soy»

Al abrir la web con una sesión guardada, `App.jsx` llama a `GET /auth/me` (función `quienSoy` en `authService.js`) y actualiza el usuario guardado, así el **rol mostrado es el actual**. Si el backend responde 401 (token vencido o cuenta desactivada) se cierra la sesión; si no hay conexión o el servidor falla, se mantiene lo guardado. La decisión está en `decidirQuienSoy` (`acceso.js`) y se prueba con `npm test`.

## Cerrar sesión (web)

El botón «Cerrar sesión» está en **Perfil**. Al pulsarlo, en este orden:

1. **Avisa al servidor** (`POST /auth/logout`) para que el token deje de servir. No espera la respuesta: si el servidor falla o tarda, igual se cierra la sesión aquí.
2. **Borra los datos de la sesión** (tarea 3): todo lo que VYNK guardó en el navegador, es decir, toda clave que empiece con `vynk.` en `localStorage` y `sessionStorage` (token, usuario y lo que se agregue después, como la conversación del asistente). Lo que no es de VYNK no se toca. Está en `src/services/sesionLocal.js`.
3. **Lleva al inicio de sesión** (tarea 4): vuelve al login con la dirección limpia. Si hay **otras pestañas abiertas**, también vuelven al login solas.

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
