import { API_URL } from '../config.js'

const BASE = '/api/organizador/actividades'

/** Solo el token de la sesión actual identifica al organizador: jamás pasar IDs del dueño desde React. */
async function peticion(ruta, token, { method = 'GET', body, signal } = {}) {
  if (!token) throw new Error('Inicia sesión como organizador para continuar.')
  let respuesta
  try {
    respuesta = await fetch(`${API_URL}${ruta}`, {
      method,
      headers: {
        Authorization: `Bearer ${token}`,
        ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
      },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
      signal,
    })
  } catch (error) {
    if (signal?.aborted || error?.name === 'AbortError') throw error
    throw new Error('Sin conexión con Spring Boot. Comprueba que funcione en el puerto 8081.')
  }
  const datos = await respuesta.json().catch(() => null)
  if (!respuesta.ok) {
    if (respuesta.status === 401) throw new Error('Tu sesión expiró. Inicia sesión nuevamente.')
    if (respuesta.status === 403) throw new Error('Solo una cuenta con rol organizador activo puede acceder.')
    if (respuesta.status === 404) throw new Error('Esta actividad no existe o no está asignada a tu cuenta.')
    if (respuesta.status === 503) throw new Error('MongoDB no está disponible. Revisa la conexión del backend.')
    const detalle = datos?.mensaje || datos?.detail || datos?.message
    throw new Error(detalle || `No se pudo completar la operación (HTTP ${respuesta.status}).`)
  }
  return datos
}

export async function obtenerMisActividades(token, signal) {
  const datos = await peticion(BASE, token, { signal })
  return Array.isArray(datos?.actividades) ? datos.actividades : []
}

export async function obtenerDetalleActividad(token, actividadId, signal) {
  if (!actividadId) throw new Error('Selecciona primero una actividad.')
  return peticion(`${BASE}/${encodeURIComponent(actividadId)}`, token, { signal })
}

export async function obtenerInscritos(token, actividadId, signal, buscar = '') {
  if (!actividadId) throw new Error('Selecciona primero una actividad.')
  const termino = String(buscar).trim()
  if (termino.length > 100) throw new Error('La búsqueda no debe superar 100 caracteres.')
  const consulta = termino ? `?buscar=${encodeURIComponent(termino)}` : ''
  const datos = await peticion(`${BASE}/${encodeURIComponent(actividadId)}/inscritos${consulta}`, token, { signal })
  const inscritos = Array.isArray(datos?.inscritos) ? datos.inscritos : []
  return {
    actividad: datos?.actividad || null,
    inscritos,
    ...(datos?.totalInscritos === undefined ? {} : { totalInscritos: datos.totalInscritos }),
    equiposRegistrados: datos?.equiposRegistrados ?? 0,
    equiposConfirmados: datos?.equiposConfirmados ?? 0,
  }
}

/** US-57: consulta autorizada y separada de la búsqueda de inscritos vigentes. */
export async function obtenerHistorialReemplazos(token, actividadId, signal) {
  if (!actividadId) throw new Error('Selecciona primero una actividad.')
  const datos = await peticion(`${BASE}/${encodeURIComponent(actividadId)}/inscritos/historial`, token, { signal })
  return {
    historial: Array.isArray(datos?.historial) ? datos.historial : [],
    total: Number.isInteger(datos?.total) && datos.total >= 0 ? datos.total : 0,
  }
}

export async function reemplazarInscrito(token, actividadId, inscripcionId, correoNuevo, motivo) {
  if (!actividadId || !inscripcionId) throw new Error('Selecciona la actividad y el estudiante que deseas reemplazar.')
  const correo = String(correoNuevo || '').trim().toLowerCase()
  if (!/^[a-z0-9._%+-]+@tecsup\.edu\.pe$/.test(correo)) throw new Error('Ingresa un correo institucional @tecsup.edu.pe.')
  const explicacion = String(motivo || '').trim()
  if (explicacion.length < 5 || explicacion.length > 400) throw new Error('Indica el motivo (entre 5 y 400 caracteres).')
  return peticion(`${BASE}/${encodeURIComponent(actividadId)}/inscritos/reemplazar`, token, {
    method: 'POST', body: { inscripcionId, correoNuevo: correo, motivo: explicacion },
  })
}

export async function reportarProblema(token, actividadId, categoria, descripcion) {
  if (!actividadId) throw new Error('Selecciona primero una actividad.')
  const texto = descripcion.trim()
  if (texto.length < 15 || texto.length > 2000) throw new Error('Describe el problema entre 15 y 2000 caracteres.')
  return peticion(`${BASE}/${encodeURIComponent(actividadId)}/reportes`, token, {
    method: 'POST', body: { categoria, descripcion: texto },
  })
}

/** Navegación de subpantallas del organizador usando el hash existente del proyecto. */

export async function obtenerResultadosOrganizador(token, actividadId, signal) {
  if (!actividadId) throw new Error('Selecciona primero una competencia.')
  return peticion(`${BASE}/${encodeURIComponent(actividadId)}/resultados`, token, { signal })
}

export async function enviarResultadosOrganizador(token, actividadId, posiciones) {
  if (!actividadId) throw new Error('Selecciona primero una competencia.')
  return peticion(`${BASE}/${encodeURIComponent(actividadId)}/resultados`, token, {
    method: 'PUT', body: { posiciones },
  })
}

/** US-48: solo se envían descripción y reglamento; nunca cupos, fecha ni propietario. */
export async function actualizarDatosPermitidos(token, actividadId, cambios) {
  if (!actividadId) throw new Error('Selecciona una actividad para editar.')
  const campos = Object.keys(cambios || {})
  if (!campos.length || campos.some(k => !['descripcion', 'reglasCompetencia'].includes(k))) {
    throw new Error('Solo puedes modificar la descripción y el reglamento.')
  }
  if (campos.includes('descripcion') && (typeof cambios.descripcion !== 'string'
      || cambios.descripcion.trim().length < 15 || cambios.descripcion.trim().length > 2500)) {
    throw new Error('La descripción debe tener entre 15 y 2500 caracteres.')
  }
  if (campos.includes('reglasCompetencia') && (typeof cambios.reglasCompetencia !== 'string'
      || cambios.reglasCompetencia.trim().length > 3000)) {
    throw new Error('El reglamento debe tener como máximo 3000 caracteres.')
  }
  const body = Object.fromEntries(campos.map(k => [k, cambios[k].trim()]))
  return peticion(`${BASE}/${encodeURIComponent(actividadId)}/edicion`, token, { method: 'PATCH', body })
}

export async function revisarResultadosArea(token, actividadId, estado, observacion) {
  if (!actividadId) throw new Error('Selecciona una competencia.')
  if (!['conforme', 'observado'].includes(estado)) throw new Error('Elige Conforme u Observado.')
  const motivo = String(observacion || '').trim()
  if (motivo.length > 800 || (estado === 'observado' && motivo.length < 10)) {
    throw new Error('Describe la observación en 10 a 800 caracteres.')
  }
  return peticion(`${BASE}/${encodeURIComponent(actividadId)}/resultados/revision-area`, token, {
    method: 'POST', body: { estado, observacion: motivo },
  })
}

export function rutaOrganizador(pantalla, actividadId) {
  return `#/${pantalla}${actividadId ? `?actividad=${encodeURIComponent(actividadId)}` : ''}`
}

export function actividadSeleccionada(hash = '') {
  const consulta = String(hash).split('?')[1] || ''
  return new URLSearchParams(consulta).get('actividad') || ''
}

/** US-58: cifras reales, sin modificar cupos ni inscripciones. */
export async function obtenerOcupacionActividad(token, actividadId, signal) {
  if (!actividadId) throw new Error('Selecciona una actividad para consultar sus cupos.')
  return peticion(`${BASE}/${encodeURIComponent(actividadId)}/ocupacion`, token, { signal })
}

/** US-59: inscripción personal y condición de cada equipo registrado. */
export async function verificarEquiposActividad(token, actividadId, signal) {
  if (!actividadId) throw new Error('Selecciona una competencia por equipos.')
  const datos = await peticion(`${BASE}/${encodeURIComponent(actividadId)}/equipos/verificacion`, token, { signal })
  return {
    totalEquipos: Number(datos?.totalEquipos || 0),
    equiposCompletos: Number(datos?.equiposCompletos || 0),
    equiposPendientes: Number(datos?.equiposPendientes || 0),
    limiteIntegrantesEquipo: Number(datos?.limiteIntegrantesEquipo || 0),
    equipos: Array.isArray(datos?.equipos) ? datos.equipos : [],
  }
}
