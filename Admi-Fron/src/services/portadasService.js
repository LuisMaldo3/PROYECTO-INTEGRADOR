import { AUTH_URL } from '../config.js'

function urlPortada(portada) {
  if (!portada?.url) return ''
  return portada.url.startsWith('/') ? `${AUTH_URL}${portada.url}` : portada.url
}
export { urlPortada }

async function obtener(ruta, token, opciones = {}) {
  let respuesta
  try {
    respuesta = await fetch(`${AUTH_URL}${ruta}`, {
      ...opciones,
      headers: { Authorization: `Bearer ${token}`, ...(opciones.headers || {}) },
    })
  } catch {
    throw new Error('No se pudo conectar con Spring Boot. Inícialo en el puerto 8081.')
  }
  const datos = await respuesta.json().catch(() => null)
  if (!respuesta.ok) {
    throw new Error(datos?.detail || datos?.mensaje || `La solicitud de portada falló (HTTP ${respuesta.status}). Revisa Gemini o tu conexión.`)
  }
  return datos
}

export async function buscarPortadas(token, nombre) {
  const datos = await obtener(`/api/portadas/buscar?q=${encodeURIComponent(nombre.trim())}`, token)
  return datos.fotos || []
}

export async function generarPortadaGemini(token, nombre) {
  const datos = await obtener('/api/portadas/generar', token, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ nombre: nombre.trim() }),
  })
  return datos.portada
}

/** Envia un archivo de imagen autorizado sin exponer claves en React. */
export async function subirPortada(token, archivo) {
  if (!archivo || !['image/jpeg', 'image/png'].includes(archivo.type) || archivo.size > 5 * 1024 * 1024)
    throw new Error('Selecciona una imagen JPG o PNG de hasta 5 MB.')
  const cuerpo = new FormData()
  cuerpo.append('archivo', archivo)
  const datos = await obtener('/api/portadas/subir', token, { method: 'POST', body: cuerpo })
  return datos.portada
}
