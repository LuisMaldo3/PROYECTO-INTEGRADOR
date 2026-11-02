import { pedir } from './usuariosService'
export const participantesCompetencia = (token, id) => pedir(`/api/actividades/${encodeURIComponent(id)}/participantes/`, token)
export const obtenerResultadosCompetencia = (token, id) => pedir(`/api/actividades/${encodeURIComponent(id)}/resultados/`, token)
export const guardarPropuesta = (token, id, posiciones) => pedir(`/api/actividades/${encodeURIComponent(id)}/resultados/`, token, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ posiciones }) })
export const publicarResultados = (token, id) => pedir(`/api/actividades/${encodeURIComponent(id)}/resultados/publicar/`, token, { method: 'POST' })
export const anunciarGanadores = (token, id) => pedir(`/api/actividades/${encodeURIComponent(id)}/resultados/anunciar/`, token, { method: 'POST' })

export const consultarClasificacionAdmin = token => pedir('/api/clasificacion/', token)
