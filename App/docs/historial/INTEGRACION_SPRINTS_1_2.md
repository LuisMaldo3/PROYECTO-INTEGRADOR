Guía de integración disponible en esta carpeta.

# VYNK — Entrega de cambios (solo sprints 1 y 2)

Esta entrega integra el código de las tareas adicionales de US-28, US-31, US-32, US-41, US-07, US-51 (Sprint 1) y US-47 (Sprint 2). No incorpora las nuevas tareas de los sprints 3 y 4.

## Estructura e integración
- `Admin-back` (Django): datos de modalidad, reglas, cupos y revisión/publicación de posiciones de competencias; avisos de ganadores.
- `Admi-Fron` (React): formulario de competencia y panel de resultados.
- `Usuario-back` (Spring Boot): inscripción individual o por equipo con comprobación de cupos, consulta pública de resultados, detalle autorizado de organizador.
- `Usuario-fron` (React): inscripción, estado, cancelación, consulta de resultados y sección de clasificación pendiente.
- `App` (Kotlin): código de la app sin cambios funcionales nuevos en esta entrega; se reserva para Sprint 4.

## Base de datos
Los servicios Django y Spring Boot deben conectarse a **la misma MongoDB**. Las colecciones `actividades`, `inscripciones`, `usuarios`, `avisos` ya existían; se agregan `resultados_competencias` y `participantes_equipo`. No se borran ni migran colecciones existentes.

Para resultados de T&C Week los nombres oficiales de áreas son Gestión y Producción, Tecnología Digital, Minería y Procesos Químicos, Electricidad y Electrónica, Mecánica. `usuarios.areaTecsup` solo debe ser asignada y validada por un operador autorizado, junto con `areaVerificada=true`. No se permite que el alumno elija el área con fines de puntaje. No se atribuyen puntos al área mientras no esté verificada.

**Importante:** Los resultados publicados de una competencia aparecen al estudiante; la **suma de puntos por área** es una tarea posterior del Sprint 3, por eso el endpoint de clasificación devuelve `pendiente_sprint_3` y no cifras ficticias. La presentación móvil de estos datos también queda pendiente.

## Ejecución local
1. Define MongoDB, las variables de OAuth/JWT y las URLs de frontend en los `.env` locales de ambos backends usando los `.env.example` originales. Se excluyeron credenciales y archivos `.env.local` de los ZIP.
2. Arranca Django con `python manage.py runserver` e inicia Spring Boot desde IntelliJ o Maven con JDK 17.
3. En cada React ejecuta `npm install` y `npm run dev`. El backend de estudiante usa el puerto 8081 por defecto; revisa las variables VITE correspondientes de cada frontend.
4. Usa una cuenta real de Tecsup para probar. Crea primero la competencia desde el administrador y verifica que aparece en el catálogo.

## Recorrido para probar
- Crear una actividad tipo `competencia`, configurar modalidad y reglas; los cupos se cuentan por estudiantes o por equipos.
- Ingresar como estudiante, inscribirse, confirmar que cambia el contador; cancelar y reinscribirse.
- En modalidad equipos crear equipo y copiar el código mostrado una sola vez; otro estudiante puede unirse con él; el límite de integrantes debe respetarse.
- Ingresar como organizador asignado y revisar el detalle, reglamento, inscritos y equipos.
- Desde el administrador abrir `Resultados`, ingresar participantes y puestos, guardar propuesta, validar y publicar. Confirmar que el estudiante ve los resultados.
- Con permiso de avisos, el administrador puede anunciar al ganador.

## Validación y límites de esta entrega
- 139 pruebas JS de `Usuario-fron` y 70 de `Admi-Fron`: aprobadas en el entorno de generación.
- Django: comprobación estática de sintaxis Python (`compileall`).
- La compilación completa de Spring Boot y el arranque con MongoDB no pudieron ejecutarse en este entorno por falta de Maven/dependencias y servidor MongoDB; son validaciones imprescindibles en tu equipo.
- Los dos frontends usan dependencias que estaban empaquetadas para otra plataforma; la compilación Vite aquí quedó bloqueada por ausencia del binario nativo de rolldown. Se comprobó la sintaxis JS/JSX con el parser de TypeScript.
- No se ha comprobado la app Android, porque no hay modificaciones de Sprint 1/2 destinadas a su interfaz.
