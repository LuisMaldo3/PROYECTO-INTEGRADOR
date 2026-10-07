/**
 * Guardar el motivo del rechazo (web).
 *
 * Cuando el acceso se bloquea (correo que no es de Tecsup, contraseña incorrecta,
 * servidor caído...), se guarda el motivo en la pestaña (sessionStorage) para que
 * el aviso siga visible si la persona recarga la página. Se borra al intentar de
 * nuevo, al iniciar sesión bien o al cerrar la pestaña.
 *
 * Solo se guarda el código, el mensaje y la fecha: nunca el correo ni la contraseña.
 */

const CLAVE = 'vynk.motivoRechazo'

function almacenDeSesion() {
  try {
    return globalThis.sessionStorage ?? null
  } catch {
    return null // el navegador bloquea el almacenamiento: no pasa nada
  }
}

/** @returns {{ codigo: string, mensaje: string, fecha: string } | null} lo que se guardó */
export function guardarMotivo({ codigo, mensaje }, almacen = almacenDeSesion(), ahora = Date.now()) {
  if (!almacen || !mensaje) return null
  const motivo = { codigo: codigo || 'ERROR', mensaje, fecha: new Date(ahora).toISOString() }
  try {
    almacen.setItem(CLAVE, JSON.stringify(motivo))
    return motivo
  } catch {
    return null
  }
}

/** @returns {{ codigo: string, mensaje: string, fecha: string } | null} */
export function leerMotivo(almacen = almacenDeSesion()) {
  try {
    const motivo = JSON.parse(almacen?.getItem(CLAVE) ?? 'null')
    return motivo && typeof motivo.mensaje === 'string' && motivo.mensaje ? motivo : null
  } catch {
    return null
  }
}

export function borrarMotivo(almacen = almacenDeSesion()) {
  try {
    almacen?.removeItem(CLAVE)
  } catch {
    // nada que borrar
  }
}
