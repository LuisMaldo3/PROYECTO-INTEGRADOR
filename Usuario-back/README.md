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

Si existe `mvnw.cmd` en Windows, ejecutar:

```powershell
.\mvnw.cmd clean install
.\mvnw.cmd spring-boot:run
```

Si no existe el wrapper, pero Maven está instalado:

```bash
mvn clean install
mvn spring-boot:run
```

El backend se ejecutará en el puerto configurado en el proyecto.

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

## 10. Solución de problemas

**Error de Java:** verificar que la versión del JDK sea compatible con el proyecto.

**Error de Maven:** revisar que Maven esté instalado o utilizar `mvnw.cmd`, si existe.

**Error del frontend:** ejecutar `npm install` dentro de `Usuario-fron`.

**Error de conexión:** comprobar que el backend esté iniciado, que la base de datos esté disponible y que la dirección configurada en el frontend sea correcta.

**Variables faltantes:** revisar `.env.example` y la configuración del backend.
