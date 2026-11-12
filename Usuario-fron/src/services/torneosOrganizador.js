/**
 * Organizador de T&C ARENA y Karadúo · lógica de pantalla sin React ni red (se prueba con node --test).
 * El organizador solo programa horarios, cambia estados y registra resultados de SUS torneos; validar, anular y
 * configurar quedan para administración. Las reglas reales las aplica Admin-back: aquí solo se decide qué botones mostrar.
 */

/** Respuesta de Admin-back (departamentos como objeto) → la misma forma que usa la vista de estudiantes (solo identificadores). */
export function normalizarTorneo(t) {
  return {
    id: t.id, actividadId: t.actividadId ?? null, tipo: t.tipo, deporte: t.deporte, modoResultado: t.modoResultado, setsParaGanar: t.setsParaGanar ?? null, estado: t.estado,
    participantes: (t.participantes ?? []).map((d) => d.id),
    equipos: Object.fromEntries((t.participantes ?? []).map((d) => [d.id, d])), campeon: t.campeon?.id ?? null, subcampeon: t.subcampeon?.id ?? null,
    campeonValidado: !!t.campeonValidado, pendientesDeValidar: t.pendientesDeValidar ?? 0,
    partidos: (t.partidos ?? []).map((p) => ({
      id: p.id, ronda: p.ronda, posicion: p.posicion, nombreRonda: p.nombreRonda, a: p.a?.id ?? null, b: p.b?.id ?? null, estado: p.estado,
      descanso: !!p.descanso, fecha: p.fecha ?? '', hora: p.hora ?? '', lugar: p.lugar ?? '', ganador: p.ganador ?? null, validado: !!p.validado,
      provisional: !!p.resultado && !p.validado,
      resultado: p.resultado ? { modo: p.resultado.modo, a: p.resultado.a, b: p.resultado.b, setsA: p.resultado.setsA, setsB: p.resultado.setsB, ganadorPenales: p.resultado.ganadorPenales, observaciones: p.resultado.observaciones ?? '' } : null,
    })),
  }
}

const SIN_LOS_DOS = (p) => p.descanso || !p.a || !p.b
export const puedeProgramar = (p) => !SIN_LOS_DOS(p) && ['pendiente', 'programado', 'suspendido'].includes(p.estado)
export const puedeRegistrar = (p) => !SIN_LOS_DOS(p) && ['pendiente', 'programado', 'en_curso', 'finalizado'].includes(p.estado)
export const esCorreccion = (p) => p.estado === 'finalizado'

const TRANSICIONES = { pendiente: ['suspendido', 'cancelado'], programado: ['en_curso', 'suspendido', 'cancelado'], en_curso: ['suspendido', 'cancelado'], suspendido: ['programado', 'pendiente', 'cancelado'], finalizado: [], cancelado: [] }
export function transicionesDisponibles(p) {
  if (p.descanso) return []
  if (p.estado === 'suspendido') return TRANSICIONES.suspendido.filter((e) => e !== 'programado' || (p.fecha && p.hora))
  return TRANSICIONES[p.estado] ?? []
}
export const ACCION_ESTADO = Object.freeze({ en_curso: 'Iniciar', suspendido: 'Suspender', cancelado: 'Cancelar', programado: 'Reprogramar', pendiente: 'Dejar pendiente' })

export function cuerpoResultado(modo, f) {
  const obs = (f.observaciones ?? '').trim()
  if (modo === 'sets') return { setsA: f.setsA, setsB: f.setsB, observaciones: obs }
  if (modo === 'ganador') return { ganador: f.ganador, observaciones: obs }
  const cuerpo = { a: f.a, b: f.b, observaciones: obs }
  if (f.a !== '' && f.b !== '' && Number(f.a) === Number(f.b) && f.ganadorPenales) cuerpo.ganadorPenales = f.ganadorPenales
  return cuerpo
}

export function formularioDeResultado(p) {
  const r = p.resultado
  return { a: r?.a ?? '', b: r?.b ?? '', setsA: r?.setsA ?? '', setsB: r?.setsB ?? '', ganador: r ? (p.ganador === p.a ? 'a' : 'b') : '', ganadorPenales: r?.ganadorPenales ?? '', observaciones: r?.observaciones ?? '' }
}

/** Primer mensaje de error útil: si el servidor marcó un solo campo, ese texto; si no, uno general. */
export function mensajeDeError(e) {
  const lista = Object.values(e?.errores ?? {}).filter(Boolean)
  if (lista.length === 1) return lista[0]
  return lista.length ? 'Revisa los datos marcados.' : e?.message || 'No se pudo completar la acción.'
}
