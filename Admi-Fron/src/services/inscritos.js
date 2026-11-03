/**
 * US-35 · Lógica pura del panel «Inscritos por actividad» (sin React ni red, se prueba con node --test).
 */

const numero = (v) => (Number.isFinite(Number(v)) && Number(v) >= 0 ? Math.trunc(Number(v)) : 0)

/** Normaliza una actividad del servidor: los conteos faltantes valen 0 y nunca son negativos. */
export function normalizarActividad(a = {}) {
  const porEquipos = a.unidadCupo === 'equipos'
  const capacidad = Number(a.capacidad) > 0 ? Math.trunc(Number(a.capacidad)) : null
  const equiposRegistrados = numero(a.equiposRegistrados)
  const equiposCompletos = Math.min(numero(a.equiposCompletos), equiposRegistrados)
  const personasInscritas = numero(a.personasInscritas)
  const ocupados = a.ocupados == null ? (porEquipos ? equiposRegistrados : personasInscritas) : numero(a.ocupados)
  return {
    id: String(a.id ?? ''),
    nombre: String(a.nombre || 'Actividad sin nombre'),
    fecha: a.fecha || null,
    estado: a.estado || 'programada',
    tipoActividad: a.tipoActividad || 'actividad',
    areaTecsup: a.areaTecsup || '',
    porEquipos,
    capacidad,
    personasInscritas,
    equiposRegistrados,
    equiposCompletos,
    equiposIncompletos: equiposRegistrados - equiposCompletos,
    ocupados,
    porcentaje: capacidad ? Math.round((ocupados * 100) / capacidad) : null,
    cancelaciones: numero(a.cancelaciones),
  }
}

/** Respuesta completa del servidor → { totales, actividades } listos para la pantalla. */
export function normalizarRespuesta(datos) {
  const actividades = Array.isArray(datos?.actividades) ? datos.actividades.map(normalizarActividad) : []
  const t = datos?.totales || {}
  return {
    generadoEn: datos?.generadoEn || null,
    actividades,
    totales: {
      actividades: numero(t.actividades),
      personasInscritas: numero(t.personasInscritas),
      equiposRegistrados: numero(t.equiposRegistrados),
      equiposCompletos: numero(t.equiposCompletos),
      sinInscritos: numero(t.sinInscritos),
    },
  }
}

export const FILTROS = [
  { id: 'todas', titulo: 'Todas' },
  { id: 'competencias', titulo: 'Competencias' },
  { id: 'equipos', titulo: 'Por equipos' },
  { id: 'sin-inscritos', titulo: 'Sin inscritos' },
]

/** Aplica el filtro y la búsqueda. Las actividades canceladas se ocultan salvo que se pidan. */
export function filtrarActividades(actividades, { filtro = 'todas', busqueda = '', incluirCanceladas = false } = {}) {
  const texto = busqueda.trim().toLocaleLowerCase('es-PE')
  return actividades.filter((a) => {
    if (!incluirCanceladas && a.estado === 'cancelada') return false
    if (texto && !a.nombre.toLocaleLowerCase('es-PE').includes(texto)) return false
    if (filtro === 'competencias') return a.tipoActividad === 'competencia'
    if (filtro === 'equipos') return a.porEquipos
    if (filtro === 'sin-inscritos') return a.personasInscritas === 0
    return true
  })
}

export const ORDENES = [
  { id: 'mas', titulo: 'Más inscritos' },
  { id: 'ocupacion', titulo: 'Mayor ocupación' },
  { id: 'fecha', titulo: 'Fecha' },
]

/** Ordena sin modificar la lista original. */
export function ordenarActividades(actividades, orden = 'mas') {
  const lista = [...actividades]
  const nombre = (a, b) => a.nombre.localeCompare(b.nombre, 'es')
  if (orden === 'ocupacion') return lista.sort((a, b) => (b.porcentaje ?? -1) - (a.porcentaje ?? -1) || nombre(a, b))
  if (orden === 'fecha') return lista.sort((a, b) => String(a.fecha || '9999').localeCompare(String(b.fecha || '9999')) || nombre(a, b))
  return lista.sort((a, b) => b.personasInscritas - a.personasInscritas || b.equiposRegistrados - a.equiposRegistrados || nombre(a, b))
}

/**
 * Máximo «redondo» del eje del gráfico para que las barras sean comparables entre sí.
 * Ej.: 7 → 10, 23 → 25, 140 → 150. Nunca devuelve menos de 5.
 */
export function maximoEje(valores) {
  const max = Math.max(0, ...valores.map(numero))
  if (max <= 5) return 5
  const potencia = 10 ** Math.floor(Math.log10(max))
  const paso = [1, 2, 2.5, 5, 10].find((m) => m * potencia >= max) * potencia
  return paso
}

/** Marcas del eje (0, ¼, ½, ¾ y el máximo) redondeadas a enteros. */
export const marcasEje = (maximo) => [0, 0.25, 0.5, 0.75, 1].map((f) => Math.round(maximo * f))

/** Texto del detalle de una barra: «12 personas · 3 de 4 equipos completos». */
export function describirInscritos(a) {
  const personas = `${a.personasInscritas} ${a.personasInscritas === 1 ? 'persona' : 'personas'}`
  if (!a.porEquipos) return personas
  if (a.equiposRegistrados === 0) return `${personas} · sin equipos`
  return `${personas} · ${a.equiposCompletos} de ${a.equiposRegistrados} ${a.equiposRegistrados === 1 ? 'equipo completo' : 'equipos completos'}`
}
