/** Saca una lista de la respuesta del servidor ({ ubicaciones: [...] }); si falta o no es una lista devuelve [] para que la pantalla no se rompa. */
export function listaDe(datos, clave) {
  const lista = datos?.[clave]
  return Array.isArray(lista) ? lista : []
}
