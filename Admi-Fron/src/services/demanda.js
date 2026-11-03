/** US-37 · Normaliza el ranking sin simular métricas inexistentes. */
export function normalizarDemanda(respuesta = {}) {
  const actividades = Array.isArray(respuesta.actividades) ? respuesta.actividades : []
  return {
    generadoEn: respuesta.generadoEn || null,
    criterio: respuesta.criterio || '',
    aclaracion: respuesta.aclaracion || '',
    totales: {
      actividades: Number(respuesta.totales?.actividades) || 0,
      conCupo: Number(respuesta.totales?.conCupo) || 0,
      conAltaDemanda: Number(respuesta.totales?.conAltaDemanda) || 0,
      sinInscripciones: Number(respuesta.totales?.sinInscripciones) || 0,
    },
    actividades: actividades.map((a, indice) => ({
      id: String(a.id || indice),
      posicion: Number(a.posicion) || indice + 1,
      nombre: String(a.nombre || 'Actividad'),
      tipoActividad: a.tipoActividad || 'actividad',
      modalidad: a.modalidad || null,
      areaTecsup: a.areaTecsup || '',
      estado: a.estado || 'programada',
      unidadCupo: a.unidadCupo === 'equipos' ? 'equipos' : 'estudiantes',
      ocupados: Math.max(0, Number(a.ocupados) || 0),
      personasInscritas: Math.max(0, Number(a.personasInscritas) || 0),
      equiposCompletos: Math.max(0, Number(a.equiposCompletos) || 0),
      equiposIncompletos: Math.max(0, Number(a.equiposIncompletos) || 0),
      capacidad: Number(a.capacidad) > 0 ? Number(a.capacidad) : null,
      porcentajeOcupacion: a.porcentajeOcupacion === null || a.porcentajeOcupacion === undefined
        ? null : Math.max(0, Number(a.porcentajeOcupacion) || 0),
      nivelDemanda: a.nivelDemanda || 'Sin definir',
    })),
    ia: {
      estado: respuesta.ia?.estado || 'no_solicitada',
      texto: typeof respuesta.ia?.texto === 'string' ? respuesta.ia.texto : null,
      mensaje: respuesta.ia?.mensaje || '',
    },
  }
}

export function filtrarDemanda(actividades, { tipo = 'todas', consulta = '' } = {}) {
  const q = consulta.toLocaleLowerCase('es').trim()
  return actividades.filter((a) => {
    if (tipo === 'competencias' && a.tipoActividad !== 'competencia') return false
    if (tipo === 'actividades' && a.tipoActividad === 'competencia') return false
    return !q || `${a.nombre} ${a.areaTecsup}`.toLocaleLowerCase('es').includes(q)
  })
}
