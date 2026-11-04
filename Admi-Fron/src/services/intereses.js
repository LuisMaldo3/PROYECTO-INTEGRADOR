/** US-38: normalización segura de estadísticas; no genera datos de ejemplo. */
export function normalizarIntereses(respuesta = {}) {
  const totales = respuesta.totales || {}
  const numero = (v) => Math.max(0, Number.isFinite(Number(v)) ? Number(v) : 0)
  const intereses = Array.isArray(respuesta.intereses) ? respuesta.intereses : []
  return {
    generadoEn: respuesta.generadoEn || null,
    criterio: String(respuesta.criterio || ''),
    aclaracion: String(respuesta.aclaracion || ''),
    totales: {
      estudiantesActivos: numero(totales.estudiantesActivos),
      conIntereses: numero(totales.conIntereses),
      sinIntereses: numero(totales.sinIntereses),
      selecciones: numero(totales.selecciones),
      categorias: numero(totales.categorias),
    },
    intereses: intereses.map((interes, i) => ({
      id: String(interes.id || i),
      nombre: String(interes.nombre || interes.id || 'Interés'),
      posicion: numero(interes.posicion) || i + 1,
      estudiantes: numero(interes.estudiantes),
      porcentaje: Math.min(100, numero(interes.porcentaje)),
    })),
    ia: {
      estado: respuesta.ia?.estado || 'no_solicitada',
      texto: typeof respuesta.ia?.texto === 'string' ? respuesta.ia.texto : null,
      mensaje: String(respuesta.ia?.mensaje || ''),
    },
  }
}

export function filtrarIntereses(intereses, busqueda = '', soloConSelecciones = false) {
  const texto = String(busqueda).trim().toLocaleLowerCase('es')
  return intereses.filter((item) =>
    (!soloConSelecciones || item.estudiantes > 0) &&
    (!texto || item.nombre.toLocaleLowerCase('es').includes(texto))
  )
}
