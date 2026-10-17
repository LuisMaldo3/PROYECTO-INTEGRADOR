# ZIP 3 — Frontend React: administración de actividades y avisos

Proyecto `Admi-Fron/` COMPLETO (sin `node_modules` ni `.env.local`). Combinado con el backend completo del ZIP 2. Preserva la sesión contra Usuario-back (Spring Boot) y las consultas contra Admin-back (Django).

## Funcionalidades
- Al editar una actividad y cambiar **ubicación o horario**, pregunta «¿Avisar a los inscritos?». Puedes elegir guardar con avisos o sin avisos; se envía `avisarInscritos` en la edición.
- Al cancelar una actividad pide confirmación, llama `PATCH /api/actividades/:id/cancelar/` e informa la cantidad de notificaciones registradas para inscritos.
- El formulario de avisos permite elegir entre **publicar ahora** o **programar publicación**; `datetime-local` se convierte a ISO UTC antes de enviar al servidor. Rechaza fechas anteriores a la actual.
- El historial muestra las pestañas Todos / Publicados / Programados, estados y fechas en formato local. El botón Actualizar refresca las programaciones.
- Mejoras visuales en login, menú, gestión de usuarios, actividades, avisos, modales, filtros y diseño adaptable a móvil, incluidas tarjetas para tablas en pantallas pequeñas.

## Ejecutar
```powershell
cd Admi-Fron
Copy-Item .env.example .env.local
# Confirma VITE_AUTH_URL=http://localhost:8081 y VITE_ADMIN_URL=http://localhost:8000
npm ci
npm test
npm run dev
```

Abrir `http://localhost:5174/`. El origen del frontend (`http://localhost:5174`) debe figurar en CORS de Django y Spring Boot. Si aparece `TOKEN_INVALIDO`, alinea el `JWT_SECRET` de Django con Spring Boot y vuelve a iniciar sesión; la interfaz no puede corregir credenciales desalineadas por sí misma.

## Commit — desde la raíz PROYECTO99
El mensaje describe **únicamente** las tareas funcionales solicitadas, no los cambios visuales. Las mejoras de estilo van incluidas en el ZIP. Revisa `git status` antes de confirmar.

```powershell
git add Admi-Fron/src Admi-Fron/ENTREGA_FRONTEND.md
git commit -m "feat(admin-web): confirmar notificaciones y programar avisos" -m "Mishel: agregar confirmación de aviso por cambio de horario o ubicación y enviar la elección al backend. Luis: confirmar cancelación de actividad, ejecutar la petición y mostrar resultado de notificaciones. Jordi: agregar selector de fecha y hora para publicaciones programadas y mostrar avisos programados y publicados con filtros y fechas. Incluir validaciones de formularios y pruebas."
```
