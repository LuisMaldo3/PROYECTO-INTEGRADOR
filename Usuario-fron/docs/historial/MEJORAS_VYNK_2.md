# VYNK 2.0 · Diseño de campus durante todo el año

## Problemas corregidos

1. **Disponibilidad ocupaba la pantalla:** en *Mi agenda → Mis horas libres* se ve un resumen pequeño con el número real de bloques ocupados y días registrados. El botón accesible «Mostrar y editar horario» / «Ocultar horario» controla el formulario completo. Si falta configurar el horario, el editor se abre; al confirmar y guardar, vuelve a plegarse. Si falla cargarlo, se ve el error y se abre el editor. El botón **no borra los datos ni hace peticiones adicionales**.
2. **T&C Week aparecía como identidad permanente:** inicio, portada, login, menú y descripciones ya no giran alrededor de esa semana. La etiqueta de concurso ahora dice «Concurso», no «Concurso T&C Week». La pantalla de inicio solo promociona T&C Week cuando el backend contiene actividades con una denominación explícita y fechas agrupadas dentro de un máximo de siete días. Después de finalizar esas fechas, desaparece la promoción, **no el catálogo**.
3. **Catálogo útil todo el año:** pestañas «Próximas», «Todas» y «Finalizadas». No se eliminan datos del backend; las pestañas filtran la lista recibida por fecha local. Actividades sin fecha todavía se mantienen bajo Próximas como «Fecha por confirmar».
4. **Inicio más útil:** bloque de «Mis inscripciones» separado de «Para descubrir»; ambos se llenan con API. Si el backend falla no se presenta 0 como si fuese real. No se inventaron avisos, usuarios, plazas ni eventos.
5. **Jerarquía visual corregida:** menos banners inmensos, títulos moderados, tarjetas compactas, más contraste, espacios y estados consistentes, paleta azul marino + verde petróleo. Optimización a 1100, 760 y 480 px; la barra móvil conserva la navegación original.
6. **Otras pantallas:** unificación visual en agenda, asistente, recomendaciones, intereses, perfil, organizador, inscritos, asistencia e incidencias sin alterar sus servicios.

## Archivos relevantes

- `src/components/HorarioLibre.jsx`: control real para mostrar u ocultar el horario; `HorarioLibre.css` y `VynkCampus2026.css` lo presentan.
- `src/services/campanas.js`: identifica campañas solo si la API aporta el nombre; distingue la edición temporal de actividades corrientes.
- `src/components/Catalogo.jsx`: filtros por fechas, etiquetas de campaña específicas y estados vacíos.
- `src/components/Inicio.jsx`: dos listas reales y promoción condicional.
- `src/components/Shell.jsx`, `src/components/Portada.jsx`, `src/components/Fondo.jsx`, `src/services/permisos.js`: textos institucionales permanentes.
- `src/components/VynkCampus2026.css`: capa de diseño al final de `App.jsx`.

## Referencias consultadas (guías de organización, no copia del diseño)

- Tecsup: https://eventos.tecsup.edu.pe/events — buscador de eventos futuros y anteriores.
- UTEC: https://bienestarestudiantil.utec.edu.pe/eventos — presentación por actividades de bienestar.
- Universidad de Lima: https://www.ulima.edu.pe/agenda — agenda por fechas y eventos a lo largo del año.
- UPC: https://www.upc.edu.pe/servicios/vida-universitaria/viernes-culturales/ — actividades regulares distintas de una semana de campaña.
- Stanford: https://events.stanford.edu/about/ — eventos de campus por fechas, organizaciones y categorías.
- MIT: https://wikis.mit.edu/confluence/display/LMODHelp/Calendar — vistas de mes, semana y agenda.

## Límites de esta entrega

No existe en el ZIP un servicio de **avisos institucionales para estudiantes** con contrato de API identificado. Por eso no se muestran comunicados ficticios ni se inventa un endpoint; las novedades que se ven son actividades reales del catálogo. Si el backend publica posteriormente `avisos`, integrar esa sección requerirá su contrato autenticado. Este ZIP contiene código fuente de React, no un despliegue. La compilación de Vite no se pudo ejecutar en el entorno de revisión por la falta de un binding nativo Linux de Rolldown; se verificaron sintaxis JSX/JS, CSS nuevo y pruebas puras.

## Ejecutar

En Windows, desde `Usuario-fron`:

```powershell
npm install
npm test
npm run build
npm run dev
```

Para probar con datos reales, debe estar disponible el backend indicado en `.env.local`. No publiques dicho archivo en repositorios.
