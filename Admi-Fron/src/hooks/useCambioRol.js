import { useCallback, useReducer } from 'react'
import { ESTADO_INICIAL, reducir } from '../services/cambioRol'
import { cambiarRol } from '../services/usuariosService'

/**
 * Tarea 3 · Cambio de rol con confirmación.
 * pedir(rol) abre la confirmación, confirmar() guarda en Admin-back, cancelar() la cierra.
 * onCambiado(usuarioActualizado) se llama cuando el servidor ya guardó el rol.
 */
export function useCambioRol({ token, usuarioId, onCambiado, onSesionExpirada }) {
  const [estado, despachar] = useReducer(reducir, ESTADO_INICIAL)

  const pedir = useCallback((rol) => despachar({ tipo: 'pedir', rol }), [])
  const cancelar = useCallback(() => despachar({ tipo: 'cancelar' }), [])

  const confirmar = useCallback(async () => {
    if (!estado.pendiente || estado.enviando) return
    despachar({ tipo: 'enviando' })
    try {
      const actualizado = await cambiarRol(token, usuarioId, estado.pendiente)
      despachar({ tipo: 'listo' })
      onCambiado(actualizado)
    } catch (e) {
      if (e.estado === 401) return onSesionExpirada(e.detalle)
      despachar({ tipo: 'fallo', mensaje: e.message })
    }
  }, [estado.pendiente, estado.enviando, token, usuarioId, onCambiado, onSesionExpirada])

  return { ...estado, pedir, cancelar, confirmar }
}
