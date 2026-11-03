# VYNK · Sprint 3 · US-37: demanda de actividades y análisis con IA

## Backend Django

`GET /api/estadisticas/demanda/` devuelve un ranking real de ocupación de cupos según inscripciones activas, sin alterar inscripciones ni límites. Está protegido con el JWT del administrador.

`GET /api/estadisticas/demanda/?ia=1` añade una explicación breve de Gemini. El ranking no depende de Gemini y sigue funcionando si falta la clave o si el proveedor no responde. Configuración opcional **solo en `Admin-back/.env`**:

```env
GEMINI_API_KEY=TU_CLAVE_PRIVADA
```

No agregar `VITE_GEMINI_API_KEY` ni enviar la clave al navegador. La llamada se realiza desde Django y el endpoint no devuelve la clave.

### Reglas del ranking
- Se excluyen actividades canceladas.
- Solo se cuentan inscripciones activas.
- En competencias por equipos, los cupos ocupados son **equipos registrados**, no el número de integrantes; los equipos completos también se muestran por separado.
- Primero se ordena por el porcentaje de ocupación del cupo y se desempata por cupos usados. Actividades sin límite definido quedan después.
- No se fabrican visitas ni causas de la demanda, porque el sistema actual no las mide.

### Archivos
- `actividades/demanda.py`: ranking y Gemini opcional.
- `actividades/views.py`: endpoint administrado.
- `actividades/urls.py`: ruta REST.
- `actividades/tests_demanda.py`: pruebas Django con MongoDB simulado.

### Pruebas
```powershell
cd Admin-back
python manage.py test actividades.tests_demanda
```

> Las pruebas Django requieren las dependencias instaladas de `requirements.txt`.
