# VYNK · Sprint 3 · US-37: pantalla Demanda

La opción **Demanda** está en el menú del administrador. Muestra el ranking real de actividades por ocupación de cupos y permite filtrar por tipo y nombre/área. El botón **Analizar con IA** es opcional y solicita una explicación a Django, sin enviar la clave Gemini al navegador.

Archivos nuevos: `src/components/PantallaDemanda.jsx`, `PantallaDemanda.css`, `src/services/demanda.js`, `demandaService.js` y `demanda.test.js`.

Conexión: `VITE_ADMIN_URL=http://localhost:8000` y una sesión de administrador válida.

Pruebas:
```powershell
cd Admi-Fron
npm ci
npm test
npm run build
```
