# VYNK — Rediseño institucional del frontend

Actualización visual aplicada al frontend React, sin reemplazar las rutas ni los servicios del backend.

## Cambios de interfaz

- **Acceso:** nueva composición con fotografía original de Tecsup, mensaje institucional, formulario y autenticación Google originales. Se conservan validación de dominio, alertas y manejo de sesión.
- **Estructura global:** navegación lateral más clara, barra superior, usuario con iniciales, rol y nombre procedentes de la sesión verificada por el servidor, títulos contextuales, navegación móvil desplazable.
- **Inicio:** portada con fotografía original del campus, saludo usando el nombre de sesión, métricas obtenidas de `/api/agenda` y `/api/catalogo`, eventos de agenda visibles y manejo honesto de errores parciales. Si falla un servicio, **no se muestra 0 como si fuera un dato real**.
- **Catálogo:** tarjetas legibles con categorías, cupos, descripciones, horarios y organizador; ubicación solo cuando el backend la proporciona. Se preservan filtros, carga y reintentos.
- **Agenda, horarios libres y sugerencias:** jerarquía tipográfica, distinción de estado y conflictos, formularios legibles en móvil; mismas llamadas a los servicios existentes.
- **Recomendaciones, intereses y asistente:** diseño consistente, preguntas, resultados, opciones y estados conservados.
- **Organizador:** métricas reales, tarjetas, filtros y subpantallas (detalle, inscritos, asistencia, reportes) con estilo unificado; sin modificar endpoints ni validaciones.
- **Perfil:** panel de identidad con nombre/correo/rol de sesión, datos institucionales, contraseña e intereses existentes.

## ¿De dónde salen los datos?

**No se agregaron tarjetas ni contadores con datos de prueba.** Las actividades, inscripciones, cupos, usuarios y recomendaciones siguen llegando de los servicios en `src/services/`. El nuevo código de UI no agrega peticiones a APIs ficticias.

Se añadieron solamente la nueva hoja `src/components/Institucional.css` (importada al final de `src/App.jsx`) y mejoras puntuales en componentes para enriquecer la presentación. Los recursos visuales son imágenes que ya venían dentro del ZIP.

## Ejecutar en Windows

1. Extrae el ZIP.
2. Abre la carpeta `Usuario-fron` en VS Code.
3. Comprueba que `.env.local` tiene **tu URL de backend y tu ID de Google** correctos. No subas `.env.local` a un repositorio público.
4. Ejecuta `npm install`.
5. Ejecuta `npm run dev`.
6. Accede a la URL local que muestre Vite.

`npm test` ejecuta pruebas de los servicios y utilidades. `npm run build` compila la aplicación para producción cuando hayas instalado las dependencias.

> La interfaz depende de los endpoints originales: para comprobar datos reales, ejecuta también tu backend. Si una ruta falla por CORS, token expirado o servidor fuera de línea, debe corregirse allí; los estilos no sustituyen esas conexiones.

## Verificación realizada en esta entrega

- Los **125 tests de servicios** de la carpeta `src/services` pasaron.
- Los **87 archivos JS/JSX** de `src/` se comprobaron sintácticamente, sin diagnósticos.
- La hoja CSS principal se analizó y no presentó errores de sintaxis.
- No fue posible ejecutar `npm run build` ni una prueba visual de navegador en el entorno de preparación. Se recomienda ejecutar el build localmente antes de desplegar.
