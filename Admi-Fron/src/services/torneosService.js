import { pedir } from './usuariosService'

const enviar = (cuerpo) => ({ method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(cuerpo ?? {}) })
const ruta = (torneoId, partidoId, accion) => `/api/torneos/${encodeURIComponent(torneoId)}/partidos/${encodeURIComponent(partidoId)}/${accion}/`

/** Torneos de un tipo ('arena' o 'karaduo'). */
export const listarTorneos = async (token, tipo, signal) => (await pedir(`/api/torneos/?tipo=${encodeURIComponent(tipo)}`, token, { signal })).torneos ?? []
export const obtenerTorneo = (token, id) => pedir(`/api/torneos/${encodeURIComponent(id)}/`, token)
export const crearTorneo = (token, datos) => pedir('/api/torneos/', token, enviar(datos))
export const eliminarTorneo = (token, id) => pedir(`/api/torneos/${encodeURIComponent(id)}/`, token, { method: 'DELETE' })
/** Prepara los puestos finales en «Resultados» de la competencia (el administrador los publica allí). */
export const enviarPuestos = (token, id, puntos, publicar = false) => pedir(`/api/torneos/${encodeURIComponent(id)}/enviar-puestos/`, token, enviar({ puntos, publicar }))
/** Equipos o alumnos confirmados de una competencia y sus puntos por puesto. */
export const participantesDisponibles = (token, actividadId) => pedir(`/api/torneos/participantes/?actividadId=${encodeURIComponent(actividadId)}`, token)
export const programarPartido = (token, torneoId, partidoId, datos) => pedir(ruta(torneoId, partidoId, 'programar'), token, enviar(datos))
export const cambiarEstadoPartido = (token, torneoId, partidoId, estado) => pedir(ruta(torneoId, partidoId, 'estado'), token, enviar({ estado }))
export const registrarResultado = (token, torneoId, partidoId, datos) => pedir(ruta(torneoId, partidoId, 'resultado'), token, enviar(datos))
export const validarResultado = (token, torneoId, partidoId) => pedir(ruta(torneoId, partidoId, 'validar'), token, enviar({}))


/** La IA propone el orden de siembra (solo propone; tú decides). */
export const proponerSorteo = (token, actividadId, participantes, modoParticipantes = 'departamentos') => pedir('/api/torneos/propuesta-ia/', token, enviar({ actividadId, participantes, modoParticipantes }))
