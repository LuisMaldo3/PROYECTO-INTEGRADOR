import { pedir } from './usuariosService.js'

export function validarAviso(valores) {
  const titulo = typeof valores?.titulo === 'string' ? valores.titulo.trim() : ''
  const mensaje = typeof valores?.mensaje === 'string' ? valores.mensaje.trim() : ''
  const errores = {}
  if (!titulo) errores.titulo = 'Escribe un título.'
  else if (titulo.length > 100) errores.titulo = 'Máximo 100 caracteres.'
  if (!mensaje) errores.mensaje = 'Escribe el mensaje.'
  else if (mensaje.length > 2000) errores.mensaje = 'Máximo 2000 caracteres.'
  return { titulo, mensaje, errores }
}

export async function listarAvisos(token) {
  const datos = await pedir('/api/avisos/', token)
  return Array.isArray(datos?.avisos) ? datos.avisos : []
}

export async function publicarAviso(token, valores) {
  const { titulo, mensaje, errores } = validarAviso(valores)
  if (Object.keys(errores).length) {
    const error = new Error('Revisa los campos del aviso.')
    error.errores = errores
    throw error
  }
  return pedir('/api/avisos/', token, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ titulo, mensaje }),
  })
}
