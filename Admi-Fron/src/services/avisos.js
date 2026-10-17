import { pedir } from './usuariosService.js'

/** Se usa fecha local del navegador y se convierte a un instante UTC en la petición. */
export function validarAviso(valores, ahora = new Date()) {
  const titulo = typeof valores?.titulo === 'string' ? valores.titulo.trim() : ''
  const mensaje = typeof valores?.mensaje === 'string' ? valores.mensaje.trim() : ''
  const programar = valores?.programar === true
  const fechaLocal = typeof valores?.fechaLocal === 'string' ? valores.fechaLocal.trim() : ''
  const errores = {}
  if (!titulo) errores.titulo = 'Escribe un título.'
  else if (titulo.length > 100) errores.titulo = 'Máximo 100 caracteres.'
  if (!mensaje) errores.mensaje = 'Escribe el mensaje.'
  else if (mensaje.length > 2000) errores.mensaje = 'Máximo 2000 caracteres.'
  let publicarEn = null
  if (programar) {
    const fecha = fechaLocal ? new Date(fechaLocal) : null
    if (!fecha || Number.isNaN(fecha.getTime())) errores.publicarEn = 'Selecciona una fecha y hora válidas.'
    else if (fecha <= ahora) errores.publicarEn = 'Elige una fecha y hora futuras.'
    else publicarEn = fecha.toISOString()
  }
  return { titulo, mensaje, publicarEn, errores }
}

export async function listarAvisos(token) {
  const datos = await pedir('/api/avisos/', token)
  return Array.isArray(datos?.avisos) ? datos.avisos : []
}

export async function publicarAviso(token, valores) {
  // Permite enviar también el objeto ya validado y normalizado.
  const normalizado = valores?.publicarEn !== undefined && valores?.errores
    ? valores : validarAviso(valores)
  const { titulo, mensaje, publicarEn, errores } = normalizado
  if (Object.keys(errores).length) {
    const error = new Error('Revisa los campos del aviso.')
    error.errores = errores
    throw error
  }
  return pedir('/api/avisos/', token, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ titulo, mensaje, publicarEn }),
  })
}
