/** US-36 · Cálculos puros del panel de seguimiento, sin escritura de cupos. */
export function normalizarReemplazos(datos = {}) {
  const actividades = Array.isArray(datos.actividades) ? datos.actividades.map((a) => ({
    id: String(a.id ?? ''),
    nombre: String(a.nombre || 'Actividad'),
    estado: String(a.estado || 'programada'),
    unidadCupo: a.unidadCupo === 'equipos' ? 'equipos' : 'estudiantes',
    capacidad: Number.isInteger(a.capacidad) && a.capacidad > 0 ? a.capacidad : null,
    ocupados: Math.max(0, Number(a.ocupados) || 0),
    personasInscritas: Math.max(0, Number(a.personasInscritas) || 0),
    equiposRegistrados: Math.max(0, Number(a.equiposRegistrados) || 0),
    equiposIncompletos: Math.max(0, Number(a.equiposIncompletos) || 0),
    reemplazosConfirmados: Math.max(0, Number(a.reemplazosConfirmados) || 0),
    historial: Array.isArray(a.historial) ? a.historial : [],
  })) : []
  return {
    actividades,
    generadoEn: datos.generadoEn ?? null,
    historialDisponible: Boolean(datos.historialDisponible),
    totales: {
      actividades: Math.max(0, Number(datos.totales?.actividades) || 0),
      reemplazosConfirmados: Math.max(0, Number(datos.totales?.reemplazosConfirmados) || 0),
      actividadesConExceso: Math.max(0, Number(datos.totales?.actividadesConExceso) || 0),
      equiposIncompletos: Math.max(0, Number(datos.totales?.equiposIncompletos) || 0),
    },
  }
}
export function diagnosticoCupo(a) {
  if (a.capacidad === null) return { estado: 'sin-limite', etiqueta: 'Sin límite definido', disponibles: null }
  if (a.ocupados > a.capacidad) return { estado: 'exceso', etiqueta: `Excede en ${a.ocupados - a.capacidad}`, disponibles: 0 }
  if (a.ocupados === a.capacidad) return { estado: 'completo', etiqueta: 'Cupo completo', disponibles: 0 }
  return { estado: 'disponible', etiqueta: `${a.capacidad - a.ocupados} disponibles`, disponibles: a.capacidad - a.ocupados }
}
