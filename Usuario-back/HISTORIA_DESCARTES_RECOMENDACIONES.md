# Historia: No me interesa — Backend Spring Boot

Responsables: Mishel (persistencia y filtrado), Luis (servicio y API), Todos (pruebas).

MongoDB: coleccion `recomendaciones_descartadas`, documentada por `RecomendacionDescartada`.
La clave `_id` es SHA-256 determinista del usuario y la actividad; un POST repetido actualiza sin duplicar.
La identidad proviene del JWT y nunca de un campo enviado por el cliente.

API:
- `GET /api/recomendaciones` devuelve sugerencias, excluyendo actividades descartadas.
- `POST /api/recomendaciones/{actividadId}/descartar` responde 204 y persiste la preferencia.
- `GET /api/agenda/sugerida` tambien excluye actividades descartadas.
- `GET /api/agenda/huecos` comparte el mismo servicio de recomendaciones.

Verificar con MongoDB real: iniciar sesion, consultar sugerencias, descartar un ID, volver a consultar y comprobar que desaparece; iniciar con otra cuenta y comprobar independencia.

Comando: `./mvnw test` en Linux / `mvnw.cmd test` en Windows.
Pruebas nuevas: `DescartesRecomendacionesServiceTest.java`.
