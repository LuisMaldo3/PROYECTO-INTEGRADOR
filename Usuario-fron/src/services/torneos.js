/**
 * T&C ARENA y Karadúo para estudiantes · lógica de pantalla de solo lectura (sin React ni red).
 * El backend entrega los departamentos como identificador (td, quimica…); aquí se escribe su nombre.
 */
import { departamentoPorId, nombreDepartamento } from './departamentos.js'

export const ESTADOS = Object.freeze({ pendiente: 'Pendiente', programado: 'Programado', en_curso: 'En curso', finalizado: 'Finalizado', suspendido: 'Suspendido', cancelado: 'Cancelado' })
export const etiquetaEstado = (p) => (p.descanso ? 'Descansa' : ESTADOS[p.estado] ?? p.estado)

/** Rondas en orden, cada una con sus enfrentamientos por posición. */
export function agruparRondas(partidos = []) {
  const rondas = new Map()
  for (const p of partidos) {
    if (!rondas.has(p.ronda)) rondas.set(p.ronda, { ronda: p.ronda, nombre: p.nombreRonda, partidos: [] })
    rondas.get(p.ronda).partidos.push(p)
  }
  return [...rondas.values()].sort((x, y) => x.ronda - y.ronda).map((r) => ({ ...r, partidos: r.partidos.sort((x, y) => x.posicion - y.posicion) }))
}

/** Número junto a cada departamento (marcador o sets); null si el torneo solo registra ganador. */
export function puntajeDeLado(p, lado) {
  const r = p.resultado
  if (!r) return null
  if (r.modo === 'sets') return lado === 'a' ? r.setsA : r.setsB
  if (r.modo === 'marcador') return lado === 'a' ? r.a : r.b
  return null
}

/** Frase corta para quien lee el resultado: «Ganó TD en el desempate», «Ganó Química»… */
export function frasePorGanador(p, nombreDe = nombreDepartamento) {
  if (!p.ganador || p.descanso) return ''
  const nombre = nombreDe(p.ganador)
  return p.resultado?.ganadorPenales ? `Ganó ${nombre} en el desempate` : `Ganó ${nombre}`
}

/** Enfrentamientos con horario, por fecha y hora. `soloProximos` deja los que aún no se juegan. */
export function calendario(torneo, { hoyISO = '', soloProximos = false } = {}) {
  return torneo.partidos
    .filter((p) => !p.descanso && p.a && p.b && p.fecha && p.hora && p.estado !== 'cancelado')
    .filter((p) => !soloProximos || (p.estado !== 'finalizado' && p.fecha >= hoyISO))
    .sort((x, y) => `${x.fecha} ${x.hora}`.localeCompare(`${y.fecha} ${y.hora}`))
}

/** Agrupa el calendario por día: [[fecha, [partidos]], …]. */
export function porDia(partidos) {
  const dias = new Map()
  for (const p of partidos) dias.set(p.fecha, [...(dias.get(p.fecha) ?? []), p])
  return [...dias.entries()]
}

export function avance(torneo) {
  const jugables = torneo.partidos.filter((p) => !p.descanso)
  const jugados = jugables.filter((p) => p.estado === 'finalizado').length
  return { jugados, total: jugables.length }
}

/** Torneos de un tipo, con el deporte en orden alfabético. */
export const deTipo = (torneos, tipo) => torneos.filter((t) => t.tipo === tipo).sort((a, b) => a.deporte.localeCompare(b.deporte, 'es'))

/**
 * Respuesta pública → torneo por equipos: `a`, `b`, `ganador`, `campeon` y `participantes` pasan a ser identificadores
 * de equipo (o alumno) y `equipos` guarda su nombre, área y departamento. Un torneo antiguo, que solo tenía
 * departamentos, se deja como estaba.
 */
export function normalizarTorneo(t) {
  const lista = Array.isArray(t?.equipos) ? t.equipos : []
  if (!lista.length) return { ...t, equipos: {} }
  const de = (p, campo) => p[`${campo}Id`] ?? null
  return {
    ...t,
    equipos: Object.fromEntries(lista.map((e) => [e.id, e])),
    participantes: lista.map((e) => e.id),
    campeon: t.campeonId ?? null,
    subcampeon: t.subcampeonId ?? null,
    partidos: (t.partidos ?? []).map((p) => ({ ...p, a: de(p, 'a'), b: de(p, 'b'), ganador: de(p, 'ganador') })),
  }
}

/** Nombre, área y departamento de un participante. Si no está entre los equipos, se trata como un departamento. */
export function equipo(torneo, id) {
  if (!id) return null
  const e = torneo?.equipos?.[id]
  if (e) return { id, nombre: e.nombre, area: e.area || '', departamento: e.departamento || null }
  const d = departamentoPorId(id)
  return d ? { id, nombre: d.nombre, area: d.area, departamento: id } : { id, nombre: id, area: '', departamento: null }
}
