# VYNK — Complemento de tareas pendientes de Sprint 1 y Sprint 2

**Base:** cinco proyectos VYNK integrados del 8 de noviembre de 2026. Se conservaron las funciones anteriores, las 84 historias y las cinco áreas Tecsup. Se modificaron solamente las implementaciones relacionadas con los sprints 1 y 2; la aplicación Kotlin no recibe cambios nuevos en esta entrega.

## Sprint 1: ampliaciones implementadas o reforzadas
- **US-28-T16:** filtro de licencias reutilizables de fotografías de Wikimedia Commons en Spring Boot; validación adicional en Django de la atribución y las licencias declaradas, más tres pruebas unitarias independientes.
- **US-31-T10 y US-51-T09:** separación real entre *alumnos inscritos*, *equipos registrados*, *equipos habilitados* (mínimo dos integrantes con inscripción personal activa) y *equipos pendientes*. El contador de cupos del organizador utiliza equipos en modalidad grupal y estudiantes en modalidad individual; no cuenta dos veces los integrantes.
- **US-07, US-31:** se preservan la inscripción individual por JWT, la unión con código privado y las validaciones de cupos de la entrega previa. Se añaden pruebas unitarias a los contadores para evitar regresiones.
- **US-34:** se preserva el organizador principal con permisos exclusivos y los cinco coordinadores por área. Sus nombres ahora aparecen en la pantalla de detalle del organizador.
- **US-41:** se conserva el flujo de avisos publicados de más nuevo a más antiguo y las imágenes opcionales del proyecto anterior.

## Sprint 2: ampliaciones implementadas o reforzadas
- **US-16-T07:** el asistente explica, usando los datos publicados, que cada integrante debe confirmar su inscripción personal en competencias por equipo; también muestra el reglamento existente cuando fue configurado.
- **US-20-T07:** comprobación determinista en Spring Boot para excluir de recomendaciones actividades canceladas, finalizadas, sin cupos o con áreas no reconocidas (sin sustituir datos oficiales por IA).
- **US-47-T07/T08 y US-49-T05:** el detalle de la actividad asignada incluye responsable principal y los coordinadores de Gestión y Producción, Tecnología Digital, Minería y Procesos Químicos, Electricidad y Electrónica, y Mecánica.
- **US-52-T05:** indicadores de estudiantes inscritos, equipos registrados, equipos habilitados y pendientes, según la modalidad.
- **US-53-T05:** la búsqueda del organizador ahora encuentra por nombre, correo, equipo o área.

## Pruebas ejecutadas aquí
- React estudiante: **147 pruebas aprobadas** mediante `npm test`.
- React administrador: **70 pruebas aprobadas** mediante `npm test`.
- Django: validación de sintaxis Python con `compileall` y **3 pruebas unitarias independientes de licencias aprobadas**.
- Java: se añadieron pruebas JUnit de conteo de cupos por equipos, pero **no se pudieron ejecutar ni compilar Maven en este entorno**.

## Pendientes que no se deben marcar como concluidos
- Pruebas reales de integración con MongoDB Atlas, sesión de usuarios, cupos concurrentes, confirmaciones individuales desde Android y autorización de roles.
- Generación efectiva de portadas con una clave Gemini real, búsqueda de Wikimedia Commons con red, fechas de avisos programados y pruebas de extremo a extremo.
- No se reasignaron horas o responsables del Word, ni se modificó la funcionalidad futura de los sprints 3 y 4.

## Puesta en marcha
Extrae cada archivo en su propia carpeta. Copia los archivos `.env` y configuraciones locales **de tu copia anterior**, nunca desde un ZIP recibido de terceros. Ejecuta Spring Boot (`.\\mvnw.cmd spring-boot:run`), Django (`py manage.py runserver 8000`), y React (`npm install; npm run dev`). Abre Kotlin en Android Studio para probarlo.
