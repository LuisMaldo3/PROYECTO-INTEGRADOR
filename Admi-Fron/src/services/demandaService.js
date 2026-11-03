import { pedir } from './usuariosService'
import { normalizarDemanda } from './demanda'

/** El servidor decide el ranking. Gemini solo se invoca si el usuario pulsa Analizar con IA. */
export async function consultarDemanda(token, { conIA = false, signal } = {}) {
  const url = conIA ? '/api/estadisticas/demanda/?ia=1' : '/api/estadisticas/demanda/'
  return normalizarDemanda(await pedir(url, token, { signal }))
}
