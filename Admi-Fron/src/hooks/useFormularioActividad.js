import { useCallback, useReducer } from 'react'
import { ESTADO_INICIAL, erroresVisibles, puedeEnviar, reducir } from '../services/formularioActividad'
import { LIMITES } from '../services/validacionActividad'

/**
 * Tarea 3 · Revisiones del formulario «Nueva actividad».
 * valores / errores · cambiar(campo, valor) · tocar(campo) (al salir del campo)
 * revisar() marca todo como tocado y dice si se puede enviar · errorDelServidor({campo: mensaje}) · reiniciar()
 */
export function useFormularioActividad() {
  const [estado, despachar] = useReducer(reducir, ESTADO_INICIAL)

  const cambiar = useCallback((campo, valor) => despachar({ tipo: 'cambiar', campo, valor }), [])
  const tocar = useCallback((campo) => despachar({ tipo: 'tocar', campo }), [])
  const errorDelServidor = useCallback((errores) => despachar({ tipo: 'servidor', errores }), [])
  const reiniciar = useCallback(() => despachar({ tipo: 'reiniciar' }), [])
  const revisar = () => {
    despachar({ tipo: 'tocarTodo' })
    return puedeEnviar(estado)
  }

  return { valores: estado.valores, errores: erroresVisibles(estado), limites: LIMITES, cambiar, tocar, revisar, errorDelServidor, reiniciar }
}
