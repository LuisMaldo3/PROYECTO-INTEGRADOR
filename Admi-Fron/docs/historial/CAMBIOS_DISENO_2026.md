# Mejoras del frontend administrativo VYNK

## 1. Problema que se corrigió
Anteriormente `App.jsx` pintaba la tabla de usuarios y la ficha `DetalleUsuario` en un mismo contenedor `flex`. En tamaños estrechos ambas se apilaban y al entrar al perfil seguía apareciendo el listado completo.

Ahora `DetalleUsuario` ocupa su propia vista de página. La lista se oculta, sin desmontarse, para conservar los filtros, el texto de búsqueda y el estado del listado. El botón **Volver a usuarios** restaura la vista y el scroll.

## 2. Perfil del usuario
- Cabecera de identidad, rol, estado y correo.
- Pestaña **Información**: datos del usuario y acceso a la administración.
- Pestaña **Rol y estado**: selector de rol con confirmación y activar/desactivar con confirmación.
- Pestaña **Permisos**: permisos granulares y guardar cambios previa confirmación.
- Las operaciones siguen utilizando los endpoints existentes de Django.
- Nombre y correo son de solo lectura: no existe un endpoint frontend configurado para editarlos.

## 3. Rediseño visual
- Fondo cálido y claro y navegación lateral blanca; se retiran fondos azul oscuro excesivos.
- Nueva tipografía **Plus Jakarta Sans** (con fallback del sistema).
- Jerarquía tipográfica uniforme, botones legibles y tablas con mejor separación.
- Métricas sin tarjetas grandes ni gráficos decorativos.
- Modales, avisos, actividades y pantalla de inicio de sesión unificados.
- Reglas responsive para escritorio, tablet y móvil.

## 4. Archivos clave
- `src/App.jsx`: navegación entre listado y perfil.
- `src/components/DetalleUsuario.jsx`: nueva vista con pestañas.
- `src/components/ListaUsuarios.jsx`: tabla con botón explícito de perfil.
- `src/components/Iconos.jsx`: flechas de navegación.
- `src/refresh.css`: nueva capa visual.
- `src/main.jsx`: importación del tema.
- `index.html`: fuente y color del navegador.

## 5. Verificación
- Se ejecutaron 67 pruebas unitarias de Node: todas pasaron.
- El JSX y el CSS se verificaron sintácticamente, sin errores.
- La compilación Vite de producción no pudo comprobarse en este contenedor Linux porque el ZIP de origen traía el ejecutable nativo de Rolldown para Windows. En el equipo Windows se resuelve con `npm install`.
