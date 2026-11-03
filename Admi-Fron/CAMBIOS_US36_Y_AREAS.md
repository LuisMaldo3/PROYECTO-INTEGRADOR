# VYNK Admin React · Sprint 3 · US-36 + corrección de áreas

- Se incorpora la pantalla **Reemplazos y cupos**, en el menú lateral.
- Visualiza por actividad cupos ocupados, disponibles, límite establecido por el administrador, equipos pendientes e historial confirmado.
- Los excesos se indican sin alterar inscripciones ni aumentar límites.
- El historial depende de que el backend del organizador persista la auditoría en MongoDB. Este ZIP no incluye Spring Boot.
- El detalle de usuario ahora presenta los errores de asignación de área como errores y mantiene separados los mensajes de confirmación.
- Backend Django requerido: el ZIP `US36_Admin_Backend_Areas_OK.zip` de la misma entrega.

Pruebas: `npm test` · Desarrollo: `npm install && npm run dev`. Mantén tus propios archivos `.env.local` y variables de entorno; no se incluyen credenciales.
