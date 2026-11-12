import { pedir } from './usuariosService'

const json = (metodo, cuerpo) => ({ method: metodo, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(cuerpo ?? {}) })

export const competenciasConPuntaje = async (token) => (await pedir('/api/puntajes/competencias/', token)).competencias ?? []
export const obtenerHoja = (token, actividadId) => pedir(`/api/puntajes/${encodeURIComponent(actividadId)}/`, token)
export const guardarHoja = (token, actividadId, cuerpo) => pedir(`/api/puntajes/${encodeURIComponent(actividadId)}/`, token, json('PUT', cuerpo))
export const enviarPuestosHoja = (token, actividadId, puntos, publicar = false) => pedir(`/api/puntajes/${encodeURIComponent(actividadId)}/enviar-puestos/`, token, json('POST', { puntos, publicar }))
export const estadoCierre = (token) => pedir('/api/cierre/', token)
export const cerrarSemana = (token) => pedir('/api/cierre/', token, json('POST'))
export const reabrirSemana = (token) => pedir('/api/cierre/', token, { method: 'DELETE' })
/** Estado de la IA; con probar=true hace una prueba real con Google. */
export const estadoIA = (token, probar = false) => pedir(`/api/ia/estado/${probar ? '?probar=1' : ''}`, token)
