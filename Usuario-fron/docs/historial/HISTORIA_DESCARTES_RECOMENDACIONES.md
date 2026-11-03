# Historia: No me interesa — Frontend React

Responsable: Jordi (boton y actualizacion), Todos (pruebas).

La pantalla `Recomendaciones.jsx` muestra un boton por actividad; elimina la tarjeta de forma optimista con `retirarTarjetaDescartada`. Si falla el servidor, la restaura y muestra el motivo.
El servicio `recomendacionesService.js` envia POST con JWT al backend. El descarte es persistente, no depende del almacenamiento local.

Pruebas nuevas: `recomendacionesService.test.js`.
`npm test` paso con 137 pruebas en el entorno de entrega.

Abrir con `npm install` y `npm run dev`. Ajustar VITE_API_URL si corresponde.
Los estilos nuevos se agrupan en `VynkProfessional2026.css`, cargados al final desde `App.jsx`.
