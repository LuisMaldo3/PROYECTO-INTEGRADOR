/**
 * Tarea 3 (web): borrar los datos de la sesión.
 *
 * Todo lo que VYNK guarda en el navegador lleva el prefijo "vynk." (token, usuario y, más adelante,
 * cosas como la conversación del asistente). Al cerrar sesión se borra todo lo que empiece así,
 * para que no quede nada de una persona en una computadora compartida.
 *
 * Es lógica pura (recibe los almacenes) para poder probarla con `npm test`.
 */

export const PREFIJO_SESION = 'vynk.'

/**
 * Borra de cada almacén (localStorage, sessionStorage...) las claves que empiezan con el prefijo.
 * @returns {number} cuántas claves se borraron
 */
export function borrarDatosDeSesion(almacenes, prefijo = PREFIJO_SESION) {
  let borradas = 0
  for (const almacen of almacenes.filter(Boolean)) {
    try {
      const claves = []
      for (let i = 0; i < almacen.length; i++) {
        const clave = almacen.key(i)
        if (clave?.startsWith(prefijo)) claves.push(clave)
      }
      for (const clave of claves) {
        almacen.removeItem(clave)
        borradas++
      }
    } catch {
      // el navegador bloquea este almacenamiento: no hay nada que borrar
    }
  }
  return borradas
}

/**
 * true si en el almacén ya no hay token de sesión (otra pestaña cerró la sesión).
 * SOLO LEE: nunca borra nada. Así, si otra pestaña está en pleno inicio de sesión
 * (escribiendo el token y luego el usuario), esta no destruye lo que se está guardando.
 * Si el almacén está bloqueado no se puede saber: devuelve false (no se cierra nada).
 */
export function sinTokenGuardado(almacen, clave = 'vynk.token') {
  try {
    return almacen.getItem(clave) === null
  } catch {
    return false
  }
}
