import { pedir } from './usuariosService'
import { armarCuerpo } from './actividades.js'

/** GET /api/categorias/ → [{ id, nombre }] */
export async function listarCategorias(token) {
  const datos = await pedir('/api/categorias/', token)
  return datos?.categorias ?? []
}

/** GET /api/actividades/ → [{ id, nombre, descripcion, categoria: { id, nombre }, creadaPor, creadaEn }] (la más nueva primero) */
export async function listarActividades(token) {
  const datos = await pedir('/api/actividades/', token)
  return datos?.actividades ?? []
}

/** POST /api/actividades/ → la actividad creada. Si el servidor rechaza algo, el error trae `errores` por campo. */
export function crearActividad(token, valores) {
  return pedir('/api/actividades/', token, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(armarCuerpo(valores)),
  })
}
