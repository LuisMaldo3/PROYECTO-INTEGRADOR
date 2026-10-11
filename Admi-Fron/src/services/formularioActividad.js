import { validarActividad } from './validacionActividad.js'

/**
 * Estado puro del formulario: qué escribió la persona, qué campos ya tocó y qué errores devolvió el servidor.
 * Un error solo se muestra cuando el campo ya se tocó (o el servidor lo rechazó), para no regañar antes de tiempo.
 */
export const CAMPOS = ['nombre', 'descripcion', 'categoriaId', 'fecha', 'horaInicio', 'horaFin']

export const ESTADO_INICIAL = {
  valores: { nombre: '', descripcion: '', categoriaId: '', fecha: '', horaInicio: '', horaFin: '' },
  tocados: {},
  servidor: {},
}

const todosTocados = () => Object.fromEntries(CAMPOS.map((c) => [c, true]))

export function reducir(estado, accion) {
  switch (accion.tipo) {
    case 'cambiar': {
      const servidor = { ...estado.servidor }
      delete servidor[accion.campo] // al corregir un campo, su error del servidor ya no aplica
      return { ...estado, valores: { ...estado.valores, [accion.campo]: accion.valor }, servidor }
    }
    case 'tocar':
      return { ...estado, tocados: { ...estado.tocados, [accion.campo]: true } }
    case 'tocarTodo':
      return { ...estado, tocados: todosTocados() }
    case 'servidor':
      return { ...estado, servidor: accion.errores ?? {}, tocados: todosTocados() }
    case 'reiniciar':
      return ESTADO_INICIAL
    default:
      return estado
  }
}

/** { campo: mensaje } de lo que se debe mostrar ahora: el error del servidor, o el local si el campo se tocó. */
export function erroresVisibles(estado) {
  const locales = validarActividad(estado.valores)
  const visibles = {}
  for (const campo of CAMPOS) {
    const mensaje = estado.servidor[campo] ?? (estado.tocados[campo] ? locales[campo] : undefined)
    if (mensaje) visibles[campo] = mensaje
  }
  return visibles
}

export const puedeEnviar = (estado) => Object.keys(validarActividad(estado.valores)).length === 0
