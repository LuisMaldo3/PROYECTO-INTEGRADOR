# VYNK — Personalizar intereses en el primer ingreso

## Historia de usuario

Como estudiante, quiero configurar mis intereses al ingresar para recibir recomendaciones personalizadas mediante IA.

Alcance: **7 tareas · 3 horas**. El backend, la web React y la app Kotlin ya tenían la selección de intereses y un catálogo. Esta entrega **no reestructura** los proyectos: conecta el primer ingreso con las pantallas existentes y permite posponer la selección.

## Contrato único entre web, app y backend

Las tres operaciones exigen `Authorization: Bearer <jwt>`.

| Operación | URL | Resultado |
| --- | --- | --- |
| Consultar estado | `GET /api/intereses/mios` | `{ "intereses": [], "actualizadoEn": null, "configurados": false, "omitido": false }` |
| Elegir intereses | `PUT /api/intereses/mios` | Body: `{ "intereses": ["teatro", "tecnologia"] }`; devuelve `configurados: true`, `omitido: false` |
| Elegir más tarde | `POST /api/intereses/omitir` | Devuelve `configurados: false`, `omitido: true` cuando aún no eligió intereses |

**Definiciones:** `configurados` indica una selección guardada y no vacía; `omitido` indica que el estudiante pospuso la elección. La omisión se guarda en MongoDB dentro del documento existente `intereses_usuario`. Una persona puede volver a **Perfil → Mis intereses** y guardar su elección más adelante. Esto reemplaza la omisión anterior.

Para documentos creados antes de esta historia, `omitido` se interpreta como `false`; la marca `actualizadoEn` y la lista existente determinan si ya estaban configurados.

## Flujo de aceptación

1. **Nuevo estudiante:** inicia sesión (Google o contraseña). Web y app consultan el estado, y si ambas marcas son falsas abren automáticamente la pantalla existente «Mis intereses».
2. **Guardar:** selecciona uno o más temas y guarda; el backend confirma, el estado pasa a configurado y se muestra el inicio. La elección ya alimenta los servicios de recomendaciones existentes.
3. **Omitir:** pulsa «Omitir por ahora». La app envía POST y solo tras confirmación continúa al inicio. Puede usar el catálogo y la agenda normalmente. En otro inicio de sesión o dispositivo, no se repite la invitación.
4. **Elegir después:** abre Perfil → Mis intereses, elige temas y guarda. Al consultar otra vez debe verse `configurados: true` y `omitido: false`.
5. **Estudiante con selección previa:** entra directamente a Inicio y sigue conservando sus temas guardados.
6. **Fallo de red:** las opciones fallidas de guardar/omitir muestran un error y no presentan como exitoso el cambio. La comprobación inicial no bloquea indefinidamente el resto de la aplicación.
7. **Sesión inválida:** al recibir 401, se cierra la sesión o se solicita volver a autenticarse según el comportamiento preexistente.

## Pruebas

- **React:** `npm install` y `npm test` en `Usuario-fron`. También ejecutar `npm run build` para validar el bundle (se necesitan las dependencias de la plataforma actual).
- **Spring Boot:** `./mvnw test` o `mvn test` en `Usuario-back`. Las pruebas nuevas usan Mockito para no depender de un MongoDB local.
- **Android Studio:** abrir `App`, sincronizar Gradle y ejecutar `:app:testDebugUnitTest`, además de probar en un emulador/dispositivo.
- **Pruebas integradas manuales (todos):** realizar los siete pasos anteriores en web y móvil sobre la **misma** cuenta y verificar que omitir en uno no vuelve a abrir el onboarding en el otro.

## Separación de tareas y autoría

| Autor | Cambio | Archivos principales |
| --- | --- | --- |
| Jordi | Estado de elección/omisión en Spring Boot | `InteresesController`, `InteresesService`, `InteresesDeUsuario`, `MisInteresesDto` |
| Jordi | Primer ingreso en app | `MainScreen`, `InteresesApi`, `Intereses` |
| Jordi | Pruebas backend de estado | `InteresesOnboardingTest` |
| Mishel | Primer ingreso en web | `App.jsx`, `interesesService.js` |
| Mishel | Omitir y guardar desde app | `InteresesScreen`, `MainScreen`, `InteresesApi` |
| Mishel | Pruebas unitarias en app | `InteresesTest` |
| Luis | Omitir y guardar desde web | `Intereses.jsx`, `useIntereses`, `Intereses.css`, `interesesService.js` |
| Luis | Pruebas de API y onboarding web | `intereses.test.js`, `interesesService.test.js`, `App.jsx` |
| Luis | Guía de integración y aceptación | `docs/HISTORIA_INTERESES.md` |

> **Orden de despliegue:** primero actualizar Spring Boot, luego web y móvil, porque los clientes nuevos requieren los campos `configurados` y `omitido`.
> **Autoría GitHub:** al hacer los commits, configurar el correo real verificado de cada estudiante. El paquete de parches incluye un script PowerShell que solicita esos correos y genera nueve commits separados, sin alterar el historial previo.
