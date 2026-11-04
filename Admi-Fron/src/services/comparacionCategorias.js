/** US-40: cálculos de interfaz sobre la respuesta oficial de Django. */
export const CINCO_AREAS = Object.freeze([
  'Gestión y Producción', 'Tecnología Digital', 'Minería y Procesos Químicos',
  'Electricidad y Electrónica', 'Mecánica',
])
const numero = valor => Number.isFinite(Number(valor)) ? Math.max(0, Number(valor)) : 0

export function normalizarComparacion(raw) {
  const categorias = Array.isArray(raw?.categorias) ? raw.categorias : []
  const areas = Array.isArray(raw?.clasificacion?.areas) ? raw.clasificacion.areas : []
  const resultados = Array.isArray(raw?.competencias) ? raw.competencias : []
  return {
    generadoEn: raw?.generadoEn || null,
    criterio: String(raw?.criterio || ''),
    aclaracion: String(raw?.aclaracion || ''),
    totales: {
      categorias: numero(raw?.totales?.categorias),
      actividadesVigentes: numero(raw?.totales?.actividadesVigentes),
      participaciones: numero(raw?.totales?.participaciones),
      equiposCompletos: numero(raw?.totales?.equiposCompletos),
      puntosOficiales: numero(raw?.totales?.puntosOficiales),
      competenciasConResultados: numero(raw?.totales?.competenciasConResultados),
    },
    categorias: categorias.map(c => ({
      id: String(c.id ?? ''), nombre: String(c.nombre || 'Sin categoría'),
      actividades: numero(c.actividades), personasInscritas: numero(c.personasInscritas),
      equiposRegistrados: numero(c.equiposRegistrados), equiposCompletos: numero(c.equiposCompletos),
      equiposIncompletos: numero(c.equiposIncompletos),
      cuposPersonasOcupados: numero(c.cuposPersonasOcupados), cuposPersonasTotales: numero(c.cuposPersonasTotales),
      cuposEquiposOcupados: numero(c.cuposEquiposOcupados), cuposEquiposTotales: numero(c.cuposEquiposTotales),
      porcentajeParticipaciones: numero(c.porcentajeParticipaciones),
    })),
    clasificacion: { areas: areas.map(a => ({
      area: String(a.area || ''), puntos: numero(a.puntos), primerosPuestos: numero(a.primerosPuestos),
      posicion: numero(a.posicion), desglose: Array.isArray(a.desglose) ? a.desglose : [],
    })) },
    competencias: resultados.map(r => ({
      actividadId: String(r.actividadId ?? ''), nombre: String(r.nombre || 'Competencia'),
      categoriaId: String(r.categoriaId || ''), categoriaNombre: String(r.categoriaNombre || 'Sin categoría'),
      puntosDistribuidos: numero(r.puntosDistribuidos),
      posiciones: (Array.isArray(r.posiciones) ? r.posiciones : []).map(p => ({
        puesto: numero(p.puesto), areaTecsup: String(p.areaTecsup || ''),
        puntos: numero(p.puntos), nombre: String(p.nombre || 'Participante'),
      })),
    })),
  }
}

/** Una selección vacía significa todas; no mezcla personas y equipos. */
export function categoriasComparadas(datos, elegidas = []) {
  const todas = datos?.categorias || []
  return elegidas.length ? todas.filter(c => elegidas.includes(c.id)) : todas
}

/** Reconstruye el ranking filtrado desde POSICIONES OFICIALES sin persistir puntos.
 * Con todas las categorías debe coincidir con el ranking oficial de US-39. */
export function clasificacionComparada(datos, elegidas = [], filtroArea = '') {
  const competencias = (datos?.competencias || []).filter(c =>
    !elegidas.length || elegidas.includes(c.categoriaId))
  const fila = new Map(CINCO_AREAS.map(area => [area, {
    area, puntos: 0, primerosPuestos: 0, desglose: [],
  }]))
  for (const competencia of competencias) {
    for (const p of competencia.posiciones) {
      const dato = fila.get(p.areaTecsup)
      if (!dato) continue
      dato.puntos += p.puntos
      if (p.puesto === 1) dato.primerosPuestos++
      dato.desglose.push({
        actividadId: competencia.actividadId, competencia: competencia.nombre,
        categoria: competencia.categoriaNombre, puesto: p.puesto, puntos: p.puntos,
      })
    }
  }
  const areas = [...fila.values()].sort((a, b) => b.puntos - a.puntos ||
    b.primerosPuestos - a.primerosPuestos || a.area.localeCompare(b.area, 'es'))
  areas.forEach((a, i) => { a.posicion = i + 1 })
  return {
    totalCompetencias: competencias.length,
    puntosTotales: areas.reduce((s, a) => s + a.puntos, 0),
    areas: filtroArea ? areas.filter(a => a.area === filtroArea) : areas,
  }
}
