/** US-39 · Normalización de un resumen oficial (nunca se generan cifras ficticias). */
const numero = (v) => Number.isFinite(Number(v)) && Number(v) >= 0 ? Number(v) : 0

export function normalizarResumenEvento(raw = {}) {
  const t = raw.totales || {}
  const areas = Array.isArray(raw.areas) ? raw.areas : []
  const resultados = Array.isArray(raw.resultados) ? raw.resultados : []
  return {
    generadoEn: raw.generadoEn || null,
    criterio: String(raw.criterio || ''),
    aclaracion: String(raw.aclaracion || ''),
    totales: {
      actividadesRegistradas: numero(t.actividadesRegistradas),
      actividadesVigentes: numero(t.actividadesVigentes),
      actividadesCanceladas: numero(t.actividadesCanceladas),
      competenciasVigentes: numero(t.competenciasVigentes),
      competenciasConResultados: numero(t.competenciasConResultados),
      competenciasSinResultados: numero(t.competenciasSinResultados),
      inscripcionesActivas: numero(t.inscripcionesActivas),
      equiposCompletos: numero(t.equiposCompletos),
      posicionesOficiales: numero(t.posicionesOficiales),
      puntosOficiales: numero(t.puntosOficiales),
    },
    areas: areas.map((a, i) => ({
      area: String(a.area || ''),
      posicion: numero(a.posicion) || i + 1,
      puntos: numero(a.puntos),
      primerosPuestos: numero(a.primerosPuestos),
      desglose: Array.isArray(a.desglose) ? a.desglose : [],
    })),
    resultados: resultados.map(r => ({
      actividadId: String(r.actividadId || ''),
      nombre: String(r.nombre || 'Competencia'),
      publicadoEn: r.publicadoEn || null,
      puntosDistribuidos: numero(r.puntosDistribuidos),
      ganadores: (Array.isArray(r.ganadores) ? r.ganadores : []).map(g => ({
        nombre: String(g.nombre || 'Participante'),
        areaTecsup: String(g.areaTecsup || ''),
        puntos: numero(g.puntos),
        puesto: numero(g.puesto),
      })),
      posiciones: (Array.isArray(r.posiciones) ? r.posiciones : []).map(p => ({
        nombre: String(p.nombre || 'Participante'),
        areaTecsup: String(p.areaTecsup || ''),
        puntos: numero(p.puntos),
        puesto: numero(p.puesto),
      })),
    })),
    ia: {
      estado: String(raw.ia?.estado || 'no_solicitada'),
      texto: typeof raw.ia?.texto === 'string' ? raw.ia.texto : null,
      mensaje: String(raw.ia?.mensaje || ''),
    },
  }
}

export function filtrarResultados(resultados, consulta = '') {
  const q = String(consulta).trim().toLocaleLowerCase('es')
  return resultados.filter(r => !q || r.nombre.toLocaleLowerCase('es').includes(q) ||
    r.posiciones.some(p => p.nombre.toLocaleLowerCase('es').includes(q) || p.areaTecsup.toLocaleLowerCase('es').includes(q)))
}
