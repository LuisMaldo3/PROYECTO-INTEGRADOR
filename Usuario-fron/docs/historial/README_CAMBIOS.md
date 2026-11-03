# VYNK — Actualización de los cuatro sprints · 8 de noviembre de 2026

Esta entrega parte de los ZIP `VYNK_*_Actualizado_*` suministrados en esta conversación. Se preservan las funciones anteriores y se aplican las correcciones vinculadas al Word consolidado de 84 historias, sin dar por completadas automáticamente las 553 tareas documentadas.

## Cambios nuevos de esta versión

- **Registro individual obligatorio:** al crear un equipo el capitán registra su propia inscripción; al unirse, cada miembro debe autenticarse e inscribirse personalmente. Cada inscripción queda registrada con `equipoId` y `rolEquipo`. El equipo existe como inscripción del capitán y no se confunde con las inscripciones personales de sus miembros. Un integrante puede cancelar solo su inscripción, mientras que el capitán cancela el equipo entero. El backend impide inscribir al mismo estudiante en dos equipos para una competencia y valida el máximo de integrantes.
- **Resultados y equipos:** el organizador principal registra propuestas con posiciones y puntajes. Solo se permite registrar resultados para equipos que tengan al menos dos integrantes y cuyas inscripciones personales estén confirmadas. El administrador los valida y publica. Antes de publicarlos se verifica de nuevo que no se cancelaron inscripciones y que todos los participantes tienen una de las cinco áreas verificadas. No se permite a la web asignar el área de los puntos.
- **Áreas institucionales:** el administrador puede verificar una de las cinco áreas en el perfil de usuario, sin que el estudiante pueda alterarla. Una verificación actualiza la afiliación de las inscripciones activas del estudiante. Las áreas: Gestión y Producción, Tecnología Digital, Minería y Procesos Químicos, Electricidad y Electrónica, Mecánica.
- **Clasificación general:** Spring Boot y Django recalculan la clasificación desde los resultados oficialmente publicados de la sede, sin contabilizar resultados pendientes ni duplicar competencias. La clasificación pública contiene puntos acumulados, primeros puestos y desglose por actividad. Se muestra en React estudiante, panel administrador y app Kotlin. Empates: puntos, primeros puestos y luego nombre del área.
- **UX:** en la web del estudiante el formulario explica que cada integrante se inscribe por separado, muestra la cantidad de confirmaciones y diferencia equipos habilitados. Android cambia Catálogo por Actividades y ofrece acceso a resultados oficiales desde Inicio.
- **Portadas y avisos:** se conservan las cuatro opciones previas: buscar fotografía real por nombre, generar con Gemini, URL HTTPS y subir JPG/PNG. Continúan las imágenes en tarjetas y las portadas temáticas de respaldo; avisos publicados de más reciente a antiguo.

## Rutas de API añadidas o ampliadas

- `PATCH /api/usuarios/{id}/area/` (Django, permiso `usuarios.editar_rol`; body `{"areaTecsup":"Tecnología Digital"}` o `null`).
- `GET /api/clasificacion/` (Django, administrador autorizado).
- `GET /api/competencias/clasificacion` (Spring Boot, usuario autenticado).
- `GET /api/actividades/{id}/mi-inscripcion` incluye confirmación personal, miembros confirmados y estado del equipo.
- `POST /api/actividades/{id}/inscripcion` crea inscripción individual (y equipo del capitán si procede).
- `POST /api/actividades/{id}/unirse-equipo` requiere código privado e identidad propia; registra una inscripción individual.

## Configuración y puesta en marcha

1. Descomprime los cinco ZIP en carpetas distintas. Haz una copia de seguridad de tu repositorio y de la base de datos antes de reemplazar el proyecto actual.
2. Copia manualmente tus variables privadas anteriores; no vienen en los ZIP. Spring Boot necesita `MONGODB_URI`, `JWT_SECRET`, `CORS_ORIGINS`; Gemini necesita `GEMINI_API_KEY` y un modelo con capacidad de imágenes (opcional `GEMINI_IMAGE_MODEL=gemini-2.5-flash-image`). Admin Django debe apuntar **a la misma base de datos** y utilizar la verificación JWT compatible.
3. Inicia Django y Spring Boot y comprueba sus URLs. En las webs React ejecuta `npm install` y `npm run dev`. Abre `App` con Android Studio y revisa la dirección del backend.
4. Administra el área institucional verificada de cada estudiante en Usuarios > Perfil > Rol y estado. Sin esta verificación no deben publicarse sus puntos como oficiales.
5. Prueba: crea una competencia por equipos (capacidad 2 equipos, máximo 3 personas por equipo); un alumno crea equipo y comparte el código; un segundo alumno se une con su cuenta; el organizador registra puestos; el administrador valida; los puntos deben actualizarse en las dos webs y la app. Repite con un integrante que cancela, cupos llenos y una cuenta de otra área.

## Qué se verificó aquí y qué NO

- 70 pruebas Node del frontend administrador aprobadas; 146 del frontend estudiante aprobadas.
- Los 159 archivos `.js` y `.jsx` analizados por el analizador de TypeScript no presentan errores de sintaxis.
- Python `compileall` terminó sin errores de sintaxis.
- **Sin verificación de integración real:** esta sesión no cuenta con una conexión MongoDB configurada ni con Gemini autorizado. No se pudo ejecutar el proyecto Spring Boot con sus dependencias Maven ni compilar Android con Gradle. Los endpoints nuevos y las pantallas necesitan verificarse con la configuración real antes de su uso en producción.
- La base original no incluía migraciones de datos para inscripciones grupales antiguas. Los equipos previos que guardaban integrantes solo en un arreglo deberán revalidarse; el backend ahora exige confirmaciones personales para sus resultados.
- Los resultados publicados siguen siendo inmutables por las rutas normales; para corregir un resultado oficial es necesario un flujo de revisión adicional con historial y autorización.

Mantén las tareas nuevas del Word sin amarillo hasta pasar pruebas de extremo a extremo.
