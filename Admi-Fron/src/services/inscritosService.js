import { pedir } from './usuariosService'
import { normalizarRespuesta } from './inscritos.js'

/** US-35 · GET /api/estadisticas/inscritos/ → { generadoEn, totales, actividades } ya normalizados. */
export async function obtenerInscritosPorActividad(token, signal) {
  const datos = await pedir('/api/estadisticas/inscritos/', token, { signal })
  return normalizarRespuesta(datos)
}
