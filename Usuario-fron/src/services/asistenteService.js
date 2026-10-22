import { API_URL } from '../config.js'

/** Envía una pregunta al backend real. El token nunca se guarda en el historial del chat. */
export async function preguntarAsistente({ mensaje, historial = [], token, signal }) {
  if (!token) throw new Error('Debes iniciar sesión para consultar los horarios.')
  const texto = String(mensaje ?? '').trim()
  if (!texto || texto.length > 600) throw new Error('Escribe una pregunta de hasta 600 caracteres.')
  let res
  try {
    res = await fetch(`${API_URL}/api/asistente/preguntar`, {
      method: 'POST', signal,
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ mensaje: texto, historial: historial.slice(-8).map(m => ({ rol: m.rol, contenido: m.contenido })) }),
    })
  } catch (e) {
    if (e.name === 'AbortError') throw e
    throw new Error(`No se pudo conectar con Spring Boot (${API_URL}). Comprueba que esté encendido y que React tenga la URL correcta.`)
  }
  if (!res.ok) {
    if (res.status === 401) throw new Error('Tu sesión expiró. Vuelve a iniciar sesión.')
    if (res.status === 403) throw new Error('No tienes permiso para usar el asistente.')
    if (res.status === 503) throw new Error('La base de datos de actividades no está disponible. Intenta nuevamente.')
    if (res.status === 404) throw new Error('No existe el servicio del asistente en Spring Boot. Actualiza el backend y reinícialo.')
    throw new Error(`El servidor no pudo responder (HTTP ${res.status}).`)
  }
  const datos = await res.json()
  if (!datos || typeof datos.respuesta !== 'string') throw new Error('El backend devolvió una respuesta inválida.')
  return datos
}

/** Datos estructurados que proceden del backend; sin enlaces geográficos no verificados. */
export function normalizarUbicaciones(datos) {
  if (!Array.isArray(datos)) return []
  const texto = valor => typeof valor === 'string' ? valor.trim().slice(0, 300) : ''
  return datos.slice(0, 7).filter(item => item && typeof item === 'object').map(item => ({
    actividadId: texto(item.actividadId), nombre: texto(item.nombre),
    fecha: texto(item.fecha), horaInicio: texto(item.horaInicio),
    lugar: texto(item.lugar) || 'Ubicación por confirmar',
    sede: texto(item.sede), pabellon: texto(item.pabellon), aula: texto(item.aula),
    direccion: texto(item.direccion), referencia: texto(item.referencia),
    confirmada: item.confirmada === true,
  }))
}


/** Resultados publicados por Tecsup; rechaza datos incompletos antes de mostrarlos. */
export function normalizarActividadesMomento(datos) {
  if (!Array.isArray(datos)) return []
  const texto = valor => typeof valor === 'string' ? valor.trim().slice(0, 220) : ''
  return datos.slice(0, 7).filter(a => a && typeof a === 'object').map(a => ({
    id: texto(a.id), nombre: texto(a.nombre), fecha: texto(a.fecha),
    horaInicio: texto(a.horaInicio), horaFin: texto(a.horaFin),
    organizador: texto(a.organizador),
    cuposDisponibles: Number.isInteger(a.cuposDisponibles) && a.cuposDisponibles >= 0 ? a.cuposDisponibles : null,
  })).filter(a => a.nombre && a.fecha && a.horaInicio && a.horaFin)
}


/** Requisitos publicados; si no están registrados se muestra estado pendiente, no "sin requisitos". */
export function normalizarRequisitos(datos) {
  if (!Array.isArray(datos)) return []
  const texto = valor => typeof valor === 'string' ? valor.trim().slice(0, 300) : ''
  return datos.slice(0, 7).filter(d => d && typeof d === 'object').map(d => ({
    actividadId: texto(d.actividadId), nombre: texto(d.nombre),
    items: Array.isArray(d.items) ? d.items.filter(i => typeof i === 'string' && i.trim()).slice(0, 12).map(texto) : [],
    confirmados: d.confirmados === true,
  })).filter(d => d.nombre)
}
