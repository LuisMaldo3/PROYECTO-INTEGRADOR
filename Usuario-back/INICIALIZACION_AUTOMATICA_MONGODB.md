# VYNK — Inicialización automática de MongoDB (Spring Boot)

## Qué ocurre al iniciar el backend

1. Spring Boot configura `MongoTemplate` con la conexión `spring.mongodb.uri` / `MONGODB_URI`.
2. `BaseDatosInicializador` se ejecuta **al arrancar** (`ApplicationRunner`, con prioridad), realiza `ping` a MongoDB y crea solo las colecciones que falten.
3. Mediante operaciones **upsert con `$setOnInsert`**, registra **las cinco áreas institucionales** en `areas_tecsup` y las categorías `actividad-cultural` / `concurso-tc-week` en `categorias`. No modifica los documentos ya existentes.
4. Las clases existentes `RolesIniciales` e `InteresesIniciales` conservan su función: crear únicamente los roles e intereses del catálogo que aún no existan.

Se puede iniciar nuevamente todas las veces que sea necesario: **no borra colecciones, no elimina registros, no reinicia puntos, no duplica los catálogos de IDs fijos**. No inventa estudiantes, actividades, inscritos, equipos, reemplazos, comentarios ni resultados; esos registros aparecen cuando los usuarios realizan las operaciones reales.

> **Atención:** una colección vacía no implica que su funcionalidad ya haya guardado datos. La inicialización crea la estructura; para garantizar cada historia de usuario se necesitan además API operativas, conexión a la misma base e integración probada.

## Colecciones preparadas

- **Usuarios y permisos:** `usuarios`, `roles`, `permisos_usuarios`, `tokens_revocados`.
- **Catálogos:** `areas_tecsup`, `categorias`, `intereses`, `intereses_usuario`.
- **Actividades:** `actividades`, `ubicaciones`, `avisos`, `notificaciones`.
- **Participación y resultados:** `inscripciones`, `participantes_equipo`, `reemplazos_inscripciones`, `resultados_competencias`.
- **Otras funciones:** `comentarios_actividades`, `reportes_problemas`, `recomendaciones_descartadas`, `conversaciones_asistente`, `disponibilidades_semanales`.

**No se crea `asistencias`**: esa función ya fue retirada del alcance de VYNK.

El resto de las áreas se siguen referenciando por su **nombre** en la lógica existente; `areas_tecsup` es un catálogo adicional de referencia con los mismos cinco nombres. No se han cambiado los formatos actuales de inscripciones o resultados.

## Ejecutar en Windows / IntelliJ

1. Instala e inicia MongoDB local o confirma que tu clúster MongoDB Atlas acepta la conexión.
2. Conserva tu `Usuario-back/.env` **local y privado**. Verifica que `MONGODB_URI` apunte a **la misma base `vynk`** que utiliza Django (`Admin-back`). No subas `.env` a Git.
3. Inicia Spring Boot como habitualmente: `./mvnw.cmd spring-boot:run` (PowerShell) o ejecuta `VynkApplication` en IntelliJ.
4. Revisa la consola. Debería mostrar un mensaje similar a `MongoDB preparado: X colecciones nuevas (21 verificadas); 5 areas y 2 categorias garantizadas...`.
5. Abre MongoDB Compass, entra en la misma base (`vynk` si así está configurada) y comprueba `areas_tecsup`, `categorias`, `roles`, `intereses`, `comentarios_actividades` e `inscripciones`.
6. Reinicia la aplicación: debería indicar `0 colecciones nuevas` y mantener intactos los documentos existentes.

Si MongoDB no está disponible, el inicializador **falla de forma visible** durante el arranque. Esto evita que la app parezca correctamente conectada cuando en realidad no se han preparado sus colecciones.

## Pruebas

```powershell
cd Usuario-back
.\mvnw.cmd -Dtest=BaseDatosInicializadorTest test
```

Incluye casos de creación inicial, repetición con colecciones existentes, estructura de cinco áreas, comportamiento no destructivo de `$setOnInsert` y desactivación explícita. Se necesitan dependencias descargadas por Maven para ejecutar JUnit/Mockito.

Para desactivar **solo** este inicializador en un entorno controlado, configura `vynk.mongodb.inicializar=false` en las propiedades de Spring. Por defecto está **activado**.

## Relación con Django

Django **no usa su ORM**: accede a las colecciones compartidas directamente con PyMongo. Este inicializador se incorpora a **Spring Boot**, no a Django. Si ejecutas Django sin arrancar Spring Boot antes, seguirás teniendo que ejecutar su comando de preparación o iniciar Spring Boot una vez para preparar los catálogos.
