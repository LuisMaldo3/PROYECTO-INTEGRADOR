import { pedir } from './usuariosService.js'
import { normalizarIntereses } from './intereses.js'

/** Los conteos provienen de MongoDB. IA solo al pulsar el botón del panel. */
export async function consultarIntereses(token, { conIA = false, signal } = {}) {
  const url = conIA ? '/api/estadisticas/intereses/?ia=1' : '/api/estadisticas/intereses/'
  return normalizarIntereses(await pedir(url, token, { signal }))
}
