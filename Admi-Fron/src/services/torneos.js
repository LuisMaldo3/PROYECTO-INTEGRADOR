/**
 * T&C ARENA y Karadúo · lógica de pantalla sin React ni red (se prueba con node --test).
 * Las reglas reales (quién avanza, choques de horario, validaciones) viven en Admin-back; aquí solo
 * se decide qué botones mostrar y cómo escribir los datos.
 */

export const ESTADOS = Object.freeze({
  pendiente: 'Pendiente', programado: 'Programado', en_curso: 'En curso',
  finalizado: 'Finalizado', suspendido: 'Suspendido', cancelado: 'Cancelado',
})

export const etiquetaEstado = (p) => (p.descanso ? 'Descansa' : ESTADOS[p.estado] ?? p.estado)

/** Rondas en orden, cada una con sus partidos por posición. */
export function agruparRondas(partidos = []) {
  const rondas = new Map()
  for (const p of partidos) {
    if (!rondas.has(p.ronda)) rondas.set(p.ronda, { ronda: p.ronda, nombre: p.nombreRonda, partidos: [] })
    rondas.get(p.ronda).partidos.push(p)
  }
  return [...rondas.values()].sort((x, y) => x.ronda - y.ronda).map((r) => ({ ...r, partidos: r.partidos.sort((x, y) => x.posicion - y.posicion) }))
}

/** Cantidad de descansos en la primera ronda para `n` participantes (sin inventar ninguno). */
export function descansos(n) {
  if (!Number.isInteger(n) || n < 2) return 0
  let tam = 1
  while (tam < n) tam *= 2
  return tam - n
}

/** Texto corto del resultado de un enfrentamiento: «2 – 1», «Sets 2 – 1» o «Ganó TD». */
export function textoResultado(partido, modo) {
  const r = partido.resultado
  if (!r) return ''
  if (modo === 'sets' || r.modo === 'sets') return `Sets ${r.setsA} – ${r.setsB}`
  if (modo === 'ganador' || r.modo === 'ganador') return `Ganó ${(partido.ganador === partido.a?.id ? partido.a : partido.b)?.nombre ?? ''}`.trim()
  const penales = r.a === r.b ? ' (pen.)' : ''
  return `${r.a} – ${r.b}${penales}`
}

/** Número que se muestra junto a cada departamento (marcador o sets); null si no aplica. */
export function puntajeDeLado(partido, lado) {
  const r = partido.resultado
  if (!r) return null
  if (r.modo === 'sets') return lado === 'a' ? r.setsA : r.setsB
  if (r.modo === 'marcador') return lado === 'a' ? r.a : r.b
  return null
}

const SIN_LOS_DOS = (p) => p.descanso || !p.a || !p.b
export const puedeProgramar = (p) => !SIN_LOS_DOS(p) && ['pendiente', 'programado', 'suspendido'].includes(p.estado)
export const puedeRegistrar = (p) => !SIN_LOS_DOS(p) && ['pendiente', 'programado', 'en_curso', 'finalizado'].includes(p.estado)
export const esCorreccion = (p) => p.estado === 'finalizado'
export const puedeValidar = (p) => p.estado === 'finalizado' && !p.descanso && !p.validado && !!p.resultado

const TRANSICIONES = { pendiente: ['suspendido', 'cancelado'], programado: ['en_curso', 'suspendido', 'cancelado'], en_curso: ['suspendido', 'cancelado'], suspendido: ['programado', 'pendiente', 'cancelado'], finalizado: [], cancelado: [] }
/** Cambios de estado manuales posibles (finalizar se hace registrando el resultado). */
export function transicionesDisponibles(p) {
  if (p.descanso) return []
  if (p.estado === 'suspendido') return TRANSICIONES.suspendido.filter((e) => e !== 'programado' || (p.fecha && p.hora))
  return TRANSICIONES[p.estado] ?? []
}

export const ACCION_ESTADO = Object.freeze({ en_curso: 'Iniciar', suspendido: 'Suspender', cancelado: 'Cancelar', programado: 'Reprogramar', pendiente: 'Dejar pendiente' })

/** Convierte el formulario en el cuerpo que espera el backend, según cómo se mide el torneo. */
export function cuerpoResultado(modo, f) {
  const obs = (f.observaciones ?? '').trim()
  if (modo === 'sets') return { setsA: f.setsA, setsB: f.setsB, observaciones: obs }
  if (modo === 'ganador') return { ganador: f.ganador, observaciones: obs }
  const cuerpo = { a: f.a, b: f.b, observaciones: obs }
  if (f.a !== '' && f.b !== '' && Number(f.a) === Number(f.b) && f.ganadorPenales) cuerpo.ganadorPenales = f.ganadorPenales
  return cuerpo
}

/** Valores iniciales del formulario a partir de un enfrentamiento (sirve para corregir). */
export function formularioDeResultado(p) {
  const r = p.resultado
  return { a: r?.a ?? '', b: r?.b ?? '', setsA: r?.setsA ?? '', setsB: r?.setsB ?? '', ganador: r ? (p.ganador === p.a?.id ? 'a' : 'b') : '', ganadorPenales: r?.ganadorPenales ?? '', observaciones: r?.observaciones ?? '' }
}

/** Avance de un torneo: partidos jugados sobre los que realmente se juegan (sin descansos). */
export function resumenTorneo(t) {
  const jugables = t.partidos.filter((p) => !p.descanso)
  const jugados = jugables.filter((p) => p.estado === 'finalizado').length
  return { jugados, total: jugables.length, porcentaje: jugables.length ? Math.round((jugados * 100) / jugables.length) : 0 }
}

/** Próximos enfrentamientos programados de todos los torneos, del más cercano al más lejano. */
export function proximosPartidos(torneos, hoyISO, limite = 6) {
  const lista = []
  for (const t of torneos) {
    if (t.estado === 'anulado') continue
    for (const p of t.partidos) {
      if (p.descanso || !p.fecha || !p.hora || !p.a || !p.b) continue
      if (['finalizado', 'cancelado'].includes(p.estado) || p.fecha < hoyISO) continue
      lista.push({ ...p, torneoId: t.id, deporte: t.deporte, tipo: t.tipo })
    }
  }
  return lista.sort((x, y) => `${x.fecha} ${x.hora}`.localeCompare(`${y.fecha} ${y.hora}`)).slice(0, limite)
}

export const pendientesDeValidar = (torneos) => torneos.reduce((s, t) => s + (t.estado === 'anulado' ? 0 : t.pendientesDeValidar ?? 0), 0)

export const tituloAccion = (tipo) => (tipo === 'karaduo' ? { menu: 'Karadúo', nuevo: 'Nuevo torneo de Karadúo' } : { menu: 'T&C ARENA', nuevo: 'Nuevo torneo deportivo' })

/** Puntos que recibe un puesto según lo que el administrador configuró en la competencia (0 si no definió ese puesto). */
export const puntosDelPuesto = (tabla, puesto) => (Array.isArray(tabla) && puesto >= 1 && puesto <= tabla.length ? tabla[puesto - 1] : 0)

/** En qué paso está el cierre del torneo. */
export function estadoPuestos(t) {
  if (!t.posiciones?.length) return { tono: 'info', texto: 'Cuando valides la final, aquí escribirás los puntos de cada puesto y publicarás los resultados.' }
  if (t.resultadosPublicados) return { tono: 'ok', texto: 'Resultados oficiales publicados: los puntos ya suman a la tabla acumulada.' }
  if (t.puestosEnviados) return { tono: 'aviso', texto: 'Puntos guardados sin publicar. Publícalos aquí (o en Actividades → Resultados) para que sumen.' }
  return { tono: 'aviso', texto: 'Escribe los puntos de cada puesto y publica los resultados oficiales.' }
}

/** Puntos escritos a mano → cuerpo del servidor. Devuelve null si falta alguno o no es un entero válido. */
export function puntosEscritos(posiciones, valores) {
  const salida = {}
  for (const { puesto } of posiciones) {
    const v = valores[puesto]
    if (v === '' || v == null || !/^\d+$/.test(String(v)) || Number(v) > 10000) return null
    salida[puesto] = Number(v)
  }
  return salida
}
