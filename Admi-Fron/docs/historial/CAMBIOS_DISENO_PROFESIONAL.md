# VYNK Admin — diseño institucional profesional

Cambios de diseño (sin cambiar endpoints, modelos, autorización ni lógica de backend):

- Paleta consistente, **un color sólido por tarjeta de métrica**:
  - Índigo `#4F46B8`: comunidad / acciones principales.
  - Turquesa `#087D76`: organizadores / publicados / activos.
  - Ocre `#A4681E`: indicadores secundarios y programaciones.
  - Azul profundo `#34448D`: encabezado de perfiles.
  - Azul institucional `#215F91`: actividades.
  - Verde petróleo `#106C71`: comunicados.
- Menú lateral compacto a 218 px en escritorio, con navegación activa de un solo color y logo VYNK original.
- Encabezados de usuarios, actividades, avisos y perfiles con **fondos sólidos**, sin mezcla de gradientes intensos.
- Tabla con jerarquía tipográfica, badges semánticos suaves, alineación y espacios unificados.
- Perfil como pantalla independiente; pestañas de información, rol/estado y permisos intactas.
- Vista móvil con métricas en dos columnas y la tercera a todo el ancho para evitar texto apretado.
- Rojo únicamente para errores y confirmaciones destructivas, no para estados informativos.

## Archivos
- `src/professional.css`: última capa de estilos, importada después de `palette.css` en `src/main.jsx`.
- `ListaUsuarios.jsx` / `ListaActividades.jsx`: clases semánticas para diferenciar color de encabezado por sección.

## Ejecutar

```sh
npm install
npm run dev
```

Ajustar variables de entorno copiando `.env.example` a `.env.local` para conectar los dos backends.
