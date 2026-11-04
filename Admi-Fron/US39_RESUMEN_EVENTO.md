# VYNK · Sprint 3 · US-39 (Frontend administrador)

**Tarea US-39-T03 (Mishel):** pantalla «Resumen del T&C Week» y botón «Generar con IA».

El menú `MenuAdmin.jsx` tiene un acceso a `PantallaResumenEvento.jsx`.
Muestra indicadores (actividades, competencias, inscripciones por actividad y puntos oficiales), ranking acumulado de las cinco áreas, ganadores y posiciones de las competencias publicadas, y un buscador. Los datos proceden del backend y no se incluyen cifras ficticias. El botón consulta `GET /api/estadisticas/resumen-evento/?ia=1`; al cargar la pantalla se usa `GET /api/estadisticas/resumen-evento/`, sin llamada automática a Gemini.

Archivos: `src/components/PantallaResumenEvento.jsx`, `.css`, `src/services/resumenEvento.js`, `resumenEventoService.js`, `resumenEvento.test.js`.

**US-39-T04:** pruebas del resumen — Todos. Se agregan 5 pruebas de normalización y filtros. El resto de la suite Node también se ejecutó.

### Ejecución

```bash
cd Admi-Fron
npm ci
npm test
npm run dev
```

Necesita el backend Django y la sesión administrador. Con `GEMINI_API_KEY` en Django, el botón «Generar con IA» puede producir texto; sin la clave se ve el mensaje correspondiente sin perder los datos numéricos.
