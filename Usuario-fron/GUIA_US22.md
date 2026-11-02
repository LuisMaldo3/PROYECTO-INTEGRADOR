# VYNK — Historia US-22 / 8 tareas

## Tareas originales (T01–T06)
1. **BACKEND Spring Boot — Mishel:** `ActividadesSimilaresService` compara área, categoría, modalidad y palabras del título; excluye el mismo evento.
2. **BACKEND Spring Boot — Jordi:** `GET /api/catalogo/{id}/similares` devuelve hasta cuatro actividades programadas relacionadas, junto con información de portada.
3. **FRONTEND React — Luis:** en `DetalleActividad.jsx`, sección *Actividades similares*, portadas y enlaces a las actividades recomendadas.
4. **FRONTEND Kotlin — Mishel:** en `CatalogoScreen.kt`, el diálogo de actividad consulta las sugerencias y permite abrir otra actividad.
5. **PRUEBAS web — Todos:** pruebas de lógica React y `ActividadesSimilaresServiceTest` en backend.
6. **PRUEBAS app — Todos:** pendiente de ejecución con Android Studio y un dispositivo/emulador.

## Ampliaciones
7. **US-22-T07 — Resultados en competencias finalizadas:** `GET /api/catalogo/{id}` permite abrir el detalle de la actividad finalizada; `GET /api/competencias/resultados/{id}` entrega únicamente resultados oficiales. React los presenta en la ficha; Kotlin los ofrece en *Resultados y clasificación*.
8. **US-22-T08 — Ganadores con imágenes:** portada de la competencia al lado de los ganadores, puestos y puntos oficiales. No se inventan fotografías personales. La portada oficial está registrada con la actividad. React y Android incluyen el detalle correspondiente.

## Prueba manual
1. Iniciar Spring Boot y MongoDB, ingresar con cuenta de Tecsup válida.
2. Crear dos actividades relacionadas con estado `programada` y comprobar `GET /api/catalogo/{id}/similares`.
3. Publicar un resultado de competencia desde administrador. Ver que aparece en web `#/resultados`, abrir *Ver competencia y sus ganadores* y revisar el detalle.
4. Confirmar que un resultado pendiente **no aparece**.
5. En Android, entrar en *Resultados y clasificación*, abrir una competencia publicada y consultar las posiciones, la imagen de portada y actividades similares.

## Restricciones
- Los cupos los define exclusivamente el administrador.
- Se conservan inscripciones personales para los equipos; aquí no se modifica esa lógica.
- Los resultados los registra el organizador y los valida el administrador.
- No se cambió el acceso a datos de Django ni se añadieron credenciales.

## Validación disponible en este entorno
- Se ejecutaron 149 pruebas Node del frontend sin fallos.
- Se validó la sintaxis del JSX/JS mediante el compilador TypeScript.
- No fue posible ejecutar Maven / Android Studio / MongoDB aquí. Ejecuta `./mvnw test` y `./gradlew testDebugUnitTest` en tu PC, con las dependencias instaladas.
