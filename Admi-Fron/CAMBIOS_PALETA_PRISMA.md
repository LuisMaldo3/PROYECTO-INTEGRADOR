# VYNK Admin · Rediseño de interfaz (paleta Prisma)

## Identidad visual
- Índigo principal `#5849DA` para navegación y acciones principales.
- Azul `#477DF2` para el degradado de bienvenida.
- Turquesa `#0A9C95` para progreso y cuentas activas.
- Ámbar `#F4AB4F` para métricas y avisos programados.
- Superficies claras y tipografía Plus Jakarta Sans.

## Cambios aplicados
- Menú lateral compacto de 210 px en escritorio; 198 px en portátiles; navegación horizontal de tres opciones en móviles.
- Encabezados de usuarios, actividades y avisos con degradado y decoración discreta.
- Tarjetas de resumen de tres colores y pequeños íconos.
- Unificación visual de tablas, botones, filtros, etiquetas de roles y estados.
- Vista de perfil independiente (no comparte el espacio de la lista). Pestañas Información, Rol y estado, Permisos.
- Color rojo reservado para errores efectivos y confirmación de acciones destructivas. Cancelar actividad desde la lista usa un aviso ámbar discreto.
- Login actualizado con diseño índigo/azul/turquesa.
- Ajustes de responsive hasta pantallas de 390 px, sin desplazamiento horizontal de la página.

## Estructura
- `src/palette.css`: paleta y ajustes visuales de esta versión, importada al final en `src/main.jsx`.
- Se conservaron servicios, rutas, modelos del backend y lógica de las pantallas.
- No se modificó el contrato de los endpoints.

## Arranque en Windows
```powershell
npm install
npm run dev
```

## Validación
- `npm run test`: 67 pruebas superadas.
- CSS analizado: sin errores de sintaxis en index.css, refresh.css ni palette.css.
- Capturas de previsualización visual a 1440 px y 390 px usando datos simulados.
- No fue posible completar `vite build` en el entorno Linux de revisión: las dependencias incluidas originalmente estaban preparadas para Windows y no contenían el binding nativo Linux de Rolldown. Ejecutar `npm install` en Windows antes de `npm run build`.
