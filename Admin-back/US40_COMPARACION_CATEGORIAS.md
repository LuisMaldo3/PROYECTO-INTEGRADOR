# US-40 · Comparación de categorías y clasificación oficial (Sprint 3)

## Implementado

1. `GET /api/estadisticas/comparacion-categorias/` (JWT administrador). Calcula participaciones por categoría con `categorias`, `actividades`, `inscripciones` y US-35; excluye cancelaciones.
2. `Admi-Fron` incorpora **Comparación de categorías** con gráfico, selección múltiple y indicadores de equipos.
3. Clasificación y desglose de las **cinco áreas institucionales** desde resultados oficialmente publicados, sin duplicar puntajes. Se reutiliza exactamente el servicio US-39 para la tabla administrativa.
4. Filtro de competencias por categoría; filtro de área y desglose por competencia. Un resultado aún pendiente nunca suma puntos.
5. Consulta desde estudiante autenticado en `Usuario-back`: `GET /api/competencias/clasificacion`. Se actualizó la deduplicación de publicaciones y la validación de puestos/puntos para igualar la consulta administrativa.
6. Se añadieron pruebas puras Python, pruebas de servicio Gemini con respuesta simulada, tests Java y tests Node.

## Gemini, sin publicar la clave

**US-40 no necesita IA para las cifras**; el análisis con IA de US-37/US-38/US-39 sigue disponible. La API de Google acepta el encabezado `x-goog-api-key` y la ruta `models/{modelo}:generateContent`.

La configuración de Gemini en Django se resuelve en este orden:

1. `GEMINI_API_KEY` (y `GEMINI_MODEL`) del entorno/Django `.env`.
2. En desarrollo local, si está vacía, **lee** `../Usuario-back/.env` como carpeta hermana de `Admin-back`.

No copia la clave, no crea credenciales, no la añade a `.env.example`, ni la envía a React. Ambos proyectos deben mantener la misma configuración de MongoDB. En despliegues separados conviene inyectar la variable mediante secretos del servidor, ya que el archivo del backend usuario no será accesible.

El `.env` personal se excluye de los ZIP y debe mantenerse en tu equipo. Si una clave ya se incluyó en un commit, revócala y crea una nueva.

## Comprobación local

```powershell
# Ejecuta desde la raíz de PROYECTO99
cd Admin-back
python -m unittest actividades.tests_comparacion_categorias actividades.tests_reporte_evento actividades.tests_gemini_config_us40 actividades.tests_gemini_rest_us40 -v
python manage.py runserver

# En otra consola
cd Admi-Fron
npm install
npm test
npm run build
npm run dev

# En otra consola
cd Usuario-back
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Accede con administrador a **Comparación de categorías**; desde la sesión de estudiante usa la API `GET /api/competencias/clasificacion` con el JWT del estudiante.

Para probar Gemini de verdad, abre «Resumen del evento» → «Generar con IA» con la clave válida y conectividad. Los tests Python de esta entrega **simulan la respuesta del proveedor**; no demuestran que tu clave real esté vigente.

## Qué no se hace

- No modifica cupos, usuarios, resultados ni valores en MongoDB.
- No cuenta asistentes ni accesos/visitas no medidos.
- No suma personas y equipos como si consumieran la misma unidad de cupo.
- No presenta resultados pendientes ni declara ganador general sin validación oficial.
