import { pedir } from './usuariosService.js'
import { normalizarComparacion } from './comparacionCategorias.js'

/** API de administrador protegida por JWT; no recibe tokens ni claves Gemini de usuario. */
export async function consultarComparacionCategorias(token, { signal } = {}) {
  return normalizarComparacion(await pedir('/api/estadisticas/comparacion-categorias/', token, { signal }))
}
