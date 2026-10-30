# Admi-Fron (React + Vite) — Web de administrador de VYNK

Consume dos backends:
- **Usuario-back** (Spring Boot, `:8081`): inicio de sesión (`/auth/login-password`) y cierre (`/auth/logout`).
- **Admin-back** (Django, `:8000`): `GET /api/usuarios/` y `GET /api/usuarios/<id>/` (requieren token de administrador).

## Qué hay (US-23)

| Tarea | Archivo |
|---|---|
| 3 · Cargar la lista de usuarios | `src/services/usuariosService.js` (`listarUsuarios`) |
| 4 · Pantalla con la lista | `src/components/ListaUsuarios.jsx` |
| 5 · Vista independiente del perfil con pestañas | `src/components/DetalleUsuario.jsx` |
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


## Renovación de interfaz (noviembre de 2026)

- Diseño institucional claro, con tipografía Plus Jakarta Sans, barra lateral blanca y espacios coherentes.
- La lista de usuarios permanece intacta mientras se abre **solo el perfil seleccionado** en una vista dedicada. Al regresar se conserva el filtro/búsqueda y se restaura el desplazamiento.
- Perfiles con pestañas: **Información**, **Rol y estado** y **Permisos**. Se conservan las llamadas existentes al backend Django.
- La tabla ofrece una acción explícita **Ver perfil**, además de permitir seleccionar la fila.
- Actividades, avisos, formularios, diálogos y acceso mantienen la misma guía de estilo.
- No se agregaron campos falsos de edición: el nombre y el correo son de solo lectura porque el frontend no cuenta con un endpoint de actualización de esos datos.
- Los nuevos estilos están en `src/refresh.css`, importados después de `src/index.css`.

**Instalación recomendada (Windows, PowerShell):**

```powershell
cd Admi-Fron
npm install
npm run dev
```

El ZIP no incluye `node_modules` porque npm instala automáticamente las dependencias adecuadas al sistema operativo.
