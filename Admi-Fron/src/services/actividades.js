/** Lógica pura de actividades en la web (sin React ni red, para probarla con `npm test`). */

/** Lo que se envía a Admin-back: solo estos campos, sin espacios sobrantes. La hora de fin vacía se envía como "". */
export function armarCuerpo({ nombre, descripcion, categoriaId, ubicacionId, organizadorId, fecha, horaInicio, horaFin, maximoParticipantes, tipoActividad = "actividad", modalidad = "individual", categoriaGenero = "", maxIntegrantesEquipo = "", reglasCompetencia = "", puntosPorPuesto = "", portada = null, areaTecsup = "", encargadosPorArea = {} } = {}) {
  const limpio = (valor) => (typeof valor === 'string' ? valor.trim() : '')
  return {
    nombre: limpio(nombre),
    portada,
    areaTecsup, encargadosPorArea,
    descripcion: limpio(descripcion),
    categoriaId: limpio(categoriaId),
    ubicacionId: limpio(ubicacionId),
    organizadorId: limpio(organizadorId),
    fecha: limpio(fecha),
    horaInicio: limpio(horaInicio),
    horaFin: limpio(horaFin),
    maximoParticipantes: Number(maximoParticipantes),
    tipoActividad, modalidad: tipoActividad === "competencia" ? modalidad : null,
    categoriaGenero: tipoActividad === "competencia" ? (categoriaGenero || "") : "",
    reglasCompetencia: tipoActividad === "competencia" ? limpio(reglasCompetencia) : "",
    maxIntegrantesEquipo: tipoActividad === "competencia" && modalidad === "equipos" ? Number(maxIntegrantesEquipo) : null,
    puntosPorPuesto: tipoActividad === "competencia" ? String(puntosPorPuesto).split(",").map(v => v.trim()).filter(Boolean).map(Number) : [],
  }
}
