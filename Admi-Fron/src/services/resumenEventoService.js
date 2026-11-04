import { pedir } from './usuariosService.js'
import { normalizarResumenEvento } from './resumenEvento.js'

/** Se piden datos reales; la IA se llama únicamente con el botón «Generar». */
export async function consultarResumenEvento(token, { conIA = false, signal } = {}) {
  const ruta = conIA ? '/api/estadisticas/resumen-evento/?ia=1' : '/api/estadisticas/resumen-evento/'
  return normalizarResumenEvento(await pedir(ruta, token, { signal }))
}
