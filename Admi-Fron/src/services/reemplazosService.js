import { pedir } from './usuariosService'
import { normalizarReemplazos } from './reemplazos'

/** Consulta administrativa de solo lectura. */
export async function consultarReemplazos(token, signal) {
  return normalizarReemplazos(await pedir('/api/estadisticas/reemplazos/', token, { signal }))
}
