/** Estado puro del flujo "elegir rol → confirmar → guardar" (sin React ni red, para probarlo con `npm test`). */

export const ESTADO_INICIAL = { pendiente: null, enviando: false, error: '' }

export function reducir(estado, accion) {
  switch (accion.tipo) {
    case 'pedir': // se eligió un rol en la lista: se abre la confirmación
      return estado.enviando ? estado : { pendiente: accion.rol, enviando: false, error: '' }
    case 'cancelar': // no se puede cancelar mientras se guarda
      return estado.enviando ? estado : ESTADO_INICIAL
    case 'enviando':
      return estado.pendiente ? { ...estado, enviando: true, error: '' } : estado
    case 'fallo': // se queda la ventana abierta con el error para reintentar
      return { ...estado, enviando: false, error: accion.mensaje }
    case 'listo':
      return ESTADO_INICIAL
    default:
      return estado
  }
}
