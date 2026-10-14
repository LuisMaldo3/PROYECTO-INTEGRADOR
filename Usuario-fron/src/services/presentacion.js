/** Cómo se ven las actividades en pantalla (días, horas, cupos, categorías). Sin React ni red, para probarlo con `npm test`. */

const DIAS = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado']
const DIAS_CORTOS = ['dom', 'lun', 'mar', 'mié', 'jue', 'vie', 'sáb']
const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre']
const MESES_CORTOS = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']

/** "2026-10-19" → { anio, mes, dia, diaSemana } (0 = domingo), sin desfase de zona horaria; null si no es una fecha válida. */
export function partesDeFecha(iso) {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(iso ?? ''))
  if (!m) return null
  const [anio, mes, dia] = [Number(m[1]), Number(m[2]), Number(m[3])]
  const f = new Date(Date.UTC(anio, mes - 1, dia))
  if (f.getUTCFullYear() !== anio || f.getUTCMonth() !== mes - 1 || f.getUTCDate() !== dia) return null
  return { anio, mes, dia, diaSemana: f.getUTCDay() }
}

const mayuscula = (t) => t.charAt(0).toUpperCase() + t.slice(1)

/** "2026-10-19" → "Lunes 19 de octubre"; sin fecha válida, "Fecha por confirmar". */
export function nombreDia(iso) {
  const p = partesDeFecha(iso)
  return p ? `${mayuscula(DIAS[p.diaSemana])} ${p.dia} de ${MESES[p.mes - 1]}` : 'Fecha por confirmar'
}

/** "2026-10-19" → { numero: "19", diaSemana: "lun", mes: "oct" }; null si no hay fecha válida. */
export function bloqueDia(iso) {
  const p = partesDeFecha(iso)
  return p ? { numero: String(p.dia), diaSemana: DIAS_CORTOS[p.diaSemana], mes: MESES_CORTOS[p.mes - 1] } : null
}

/** ("11:00", "12:30") → "11:00 – 12:30"; solo el inicio → "11:00"; sin horas → "Hora por confirmar". */
export function rangoHoras(inicio, fin) {
  if (inicio && fin) return `${inicio} – ${fin}`
  return inicio || 'Hora por confirmar'
}

/** La fecha de hoy ("AAAA-MM-DD") en la hora local del navegador. */
export function hoyISO(ahora = new Date()) {
  const dos = (n) => String(n).padStart(2, '0')
  return `${ahora.getFullYear()}-${dos(ahora.getMonth() + 1)}-${dos(ahora.getDate())}`
}

/**
 * Agrupa por día, de la fecha más próxima a la más lejana y, dentro de cada día, por hora de inicio.
 * Las actividades sin fecha válida van al final en un grupo con fecha null.
 * @returns {{ fecha: string|null, items: object[] }[]}
 */
export function agruparPorDia(lista) {
  const porFecha = new Map()
  for (const a of lista) {
    const fecha = partesDeFecha(a.fecha) ? a.fecha : null
    if (!porFecha.has(fecha)) porFecha.set(fecha, [])
    porFecha.get(fecha).push(a)
  }
  const grupos = [...porFecha.entries()].map(([fecha, items]) => ({
    fecha,
    items: [...items].sort((x, y) => String(x.horaInicio ?? '').localeCompare(String(y.horaInicio ?? ''))),
  }))
  return grupos.sort((a, b) => (a.fecha === null) - (b.fecha === null) || String(a.fecha).localeCompare(String(b.fecha)))
}

/** Cupos → { texto, tono }: tono "ok" (hay), "poco" (quedan 5 o menos), "agotado" (0) o "neutro" (no se sabe). */
export function estadoCupos(cupos) {
  if (cupos === null || cupos === undefined) return { texto: 'Cupos por confirmar', tono: 'neutro' }
  if (cupos <= 0) return { texto: 'Sin cupos', tono: 'agotado' }
  if (cupos <= 5) return { texto: cupos === 1 ? 'Queda 1 cupo' : `Quedan ${cupos} cupos`, tono: 'poco' }
  return { texto: `${cupos} cupos`, tono: 'ok' }
}

// Los colores salen del programa del T&C Week: celeste/azul para lo cultural y verde azulado para los concursos.
const CATEGORIAS = {
  cultural: { clase: 'cultural', icono: 'mascaras', nombre: 'Actividad cultural' },
  concurso: { clase: 'concurso', icono: 'trofeo', nombre: 'Concurso T&C Week' },
}
const OTRA = { clase: 'otra', icono: 'etiqueta', nombre: 'Actividad' }

const sinTildes = (t) => String(t ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim()

/** Estilo de una categoría a partir de su id ("cultural") o de su nombre ("Actividad cultural"). Lo desconocido sale neutro. */
export function estiloCategoria({ id, nombre } = {}) {
  const porId = Object.hasOwn(CATEGORIAS, id) ? CATEGORIAS[id] : null
  if (porId) return porId
  const texto = sinTildes(nombre)
  if (texto.includes('concurso')) return CATEGORIAS.concurso
  if (texto.includes('cultural')) return CATEGORIAS.cultural
  return { ...OTRA, nombre: nombre || OTRA.nombre }
}

/** "1 actividad" / "3 actividades". */
export function cantidad(n, singular, plural) {
  return `${n} ${n === 1 ? singular : plural}`
}
