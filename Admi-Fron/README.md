# Admi-Fron (React + Vite) — Web de administrador de VYNK

Consume dos backends:
- **Usuario-back** (Spring Boot, `:8081`): inicio de sesión (`/auth/login-password`) y cierre (`/auth/logout`).
- **Admin-back** (Django, `:8000`): `GET /api/usuarios/` y `GET /api/usuarios/<id>/` (requieren token de administrador).

## Qué hay (US-23)

| Tarea | Archivo |
|---|---|
| 3 · Cargar la lista de usuarios | `src/services/usuariosService.js` (`listarUsuarios`) |
| 4 · Pantalla con la lista | `src/components/ListaUsuarios.jsx` |
| 5 · Panel con el detalle | `src/components/DetalleUsuario.jsx` |
| 6 · Menú del panel | `src/components/MenuAdmin.jsx` |
| (necesario) Login de administrador | `src/components/Login.jsx`, `src/services/authService.js` |

La lógica sin red (token, roles, búsqueda, mensajes) está en `src/services/sesion.js` y `usuarios.js`, con sus pruebas.

## Ejecutar (PowerShell)

```powershell
cd Admi-Fron
npm install
Copy-Item .env.example .env.local
npm run dev        # http://localhost:5174
npm test           # pruebas
```

Antes: en `Usuario-back/.env` agrega `http://localhost:5174` a `CORS_ORIGINS` (separado por coma) y reinicia el backend.

---

## Parte de este zip — Jordi: tareas 3 (carga de la lista) y 4 (pantalla de la lista)

- **Tuyo:** src/services/usuariosService.js (listarUsuarios), src/components/ListaUsuarios.jsx
- **Placeholder (es de tu compañero/a, no lo edites):** src/components/DetalleUsuario.jsx, src/components/MenuAdmin.jsx (y obtenerUsuario en usuariosService.js)
- Al integrar, cada quien entrega solo sus archivos. `usuariosService.js` lo comparten: Jordi aporta `listarUsuarios`, Mishel `obtenerUsuario`.
- `.env.local` no se incluye: `Copy-Item .env.example .env.local`.
