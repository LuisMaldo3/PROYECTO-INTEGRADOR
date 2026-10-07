# Proyecto — Sistema de autenticación TECSUP

## 1. Descripción

Proyecto compuesto por un backend desarrollado con Spring Boot y Maven, y un frontend desarrollado con React y Vite.

El sistema valida el acceso con correo institucional de TECSUP (`@tecsup.edu.pe`).

## 2. Estructura del proyecto

```text
55444/
├── README.md
├── Usuario-back/
│   ├── pom.xml
│   └── src/
└── Usuario-fron/
    ├── package.json
    ├── package-lock.json
    ├── .env.example
    ├── public/
    └── src/
```

## 3. Requisitos previos

Instalar los siguientes programas:

* **Git:** para descargar y actualizar el proyecto.
* **Java JDK:** versión compatible con el proyecto Spring Boot.
* **Maven:** si el proyecto no incluye el wrapper de Maven.
* **Node.js LTS y npm:** para instalar y ejecutar el frontend.
* **Base de datos:** configurar el servicio que utiliza el backend, según los valores de su configuración.

Verificar las instalaciones:

```bash
git --version
java -version
mvn -version
node -v
npm -v
```

## 4. Descargar el proyecto

Si todavía no tienes el repositorio:

```bash
git clone URL_DEL_REPOSITORIO
cd 55444
```

Reemplaza `URL_DEL_REPOSITORIO` por la dirección real del repositorio.

Si ya descargaste el proyecto, entra directamente en la carpeta `55444`.

## 5. Configurar y ejecutar el backend

Entrar en la carpeta:

```bash
cd Usuario-back
```

**Paso obligatorio: crear el archivo `.env`.** Está en `.gitignore`, así que *no viene en el repositorio*; sin él el backend no arranca (falta `JWT_SECRET`).

```powershell
Copy-Item .env.example .env      # Windows (PowerShell)
```
```bash
cp .env.example .env             # Mac / Linux
```

Abrir `.env` y revisar `MONGODB_URI`:

* **MongoDB local:** debe estar instalado y corriendo (valor por defecto del ejemplo).
* **MongoDB Atlas:** pedir usuario/clave al dueño del cluster y, en Atlas > Network Access, agregar tu IP. Si no, el backend no puede conectarse.

Ejecutar (requiere JDK 17 o superior):

```powershell
.\mvnw.cmd spring-boot:run
```

Si no hay wrapper pero Maven está instalado: `mvn spring-boot:run`.

El backend queda en **http://localhost:8081**. Comprobación rápida (debe responder 401 o 403, no "connection refused"):

```bash
curl -i -X POST http://localhost:8081/auth/login-password -H "Content-Type: application/json" -d "{\"correo\":\"a@tecsup.edu.pe\",\"password\":\"x\"}"
```

## 6. Configurar y ejecutar el frontend

Abrir otra terminal y ejecutar:

```bash
cd Usuario-fron
npm install
```

Si existe `.env.example`, crear una copia local:

```powershell
Copy-Item .env.example .env.local
```

Revisar `.env.local` y completar las variables necesarias, como la dirección del backend, utilizando los valores correctos del proyecto.

Después, ejecutar:

```bash
npm run dev
```

Abrir en el navegador la dirección que muestre Vite, normalmente:

```text
http://localhost:5173/
```

## 7. Configuración manual necesaria

Cada integrante debe revisar y completar:

1. Las variables de entorno requeridas por el frontend.
2. Las credenciales y la conexión a la base de datos del backend.
3. Las variables de entorno requeridas por Spring Boot.
4. Las direcciones y puertos utilizados para conectar el frontend con el backend.
5. Las credenciales de Google, si la autenticación de Google requiere configuración local.

No copiar credenciales personales de otro integrante.

## 8. Validación del correo institucional

El sistema debe permitir únicamente los correos institucionales autorizados de TECSUP.

Los correos que no cumplan con el dominio `@tecsup.edu.pe` deben ser rechazados por el backend y mostrar un mensaje comprensible en el frontend.

La validación del backend es obligatoria y no debe depender únicamente de la interfaz.

## 9. Archivos que no deben subirse a Git

No subir:

* `node_modules/`
* `.env.local`
* Contraseñas, tokens o claves privadas.
* Archivos de compilación generados.
* Configuraciones personales del IDE.
* Archivos ZIP innecesarios.

El archivo `.env.example` debe contener únicamente nombres de variables y valores ficticios, nunca credenciales reales.

## 10. App Android (carpeta `App`)

1. Abrir `App` con Android Studio y esperar el *Gradle sync*. Hace falta el **Android SDK 37** (Studio ofrece instalarlo).
2. **Dirección del backend.** Por defecto la app usa `http://10.0.2.2:8081`, que solo sirve en el **emulador**. Para que funcione igual en emulador y celular, agregar en `App/local.properties` (local, no se sube a Git):

   ```properties
   backend.url=http://localhost:8081
   ```

   Después de un *Sync*, y con el emulador abierto o el celular conectado por USB, ejecutar `App/conectar-backend.bat` (hace `adb reverse tcp:8081 tcp:8081`). Repetirlo al reiniciar el emulador o reconectar el cable.
3. **Login con Google.** Google solo acepta la app si el **SHA-1 de tu llave de depuración** está registrado. Cada integrante tiene un SHA-1 distinto: obtenerlo con `Gradle > app > Tasks > android > signingReport` (o `gradlew signingReport`) y agregarlo en Firebase Console > proyecto `vynk-e8ac4` > app `com.tecsup.tcweek` > *Add fingerprint*. Si la pantalla de consentimiento de Google está en modo "Testing", agregar también tu correo como usuario de prueba.

## 11. Solución de problemas

**El backend no arranca con `Could not resolve placeholder 'JWT_SECRET'`:** falta el archivo `.env` (ver sección 5).

**El backend tarda y termina con error de MongoDB:** MongoDB local apagado, o tu IP no está permitida en Atlas.

**Error de Java:** verificar que el JDK sea 17 o superior.

**La app dice "No se pudo conectar con el servidor":** el backend no está corriendo, o la dirección es incorrecta (ver punto 2 de la sección 10).

**La app dice "No se pudo abrir el acceso con Google":** casi siempre es el SHA-1 no registrado (punto 3 de la sección 10). Ver el detalle con Logcat filtrando por `LoginViewModel`.

**Error del frontend:** ejecutar `npm install` dentro de `Usuario-fron`.
