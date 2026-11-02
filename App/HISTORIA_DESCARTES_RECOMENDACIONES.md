# Historia: No me interesa — Android Kotlin

Responsable: Luis (boton y descarte), Todos (pruebas).

`RecomendacionesApi.descartar` envia POST autenticado. `RecomendacionesScreen` retira la tarjeta inmediatamente y la restituye ante errores.
`DescarteRecomendacion.kt` implementa una regla pura para retirar un ID; `DescarteRecomendacionTest.kt` incluye pruebas de eliminacion, idempotencia y preservacion de otros IDs.

Se actualizo la paleta de `ui/theme` y se agregaron accesos funcionales a `InicioScreen`.

Abrir en Android Studio, sincronizar Gradle y ejecutar `gradlew.bat :app:testDebugUnitTest`.
Usa la misma base URL y backend de Spring Boot que el proyecto original.
