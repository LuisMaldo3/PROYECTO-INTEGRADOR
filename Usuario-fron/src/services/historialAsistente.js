/** Historial aislado por cuenta y acotado, sin tokens ni datos de autenticación. */
export const CLAVE_CHAT = 'vynk_asistente_v2:'
export function claveHistorial(correo) { return CLAVE_CHAT + String(correo ?? '').trim().toLowerCase() }
export function leerHistorial(correo) {
  if (!correo || typeof localStorage === 'undefined') return []
  try {
    const lista = JSON.parse(localStorage.getItem(claveHistorial(correo)) || '[]')
    return Array.isArray(lista) ? lista.filter(m => ['usuario', 'asistente'].includes(m?.rol) && typeof m.contenido === 'string').slice(-30) : []
  } catch { return [] }
}
export function guardarHistorial(correo, mensajes) {
  if (!correo || typeof localStorage === 'undefined') return
  try {
    localStorage.setItem(claveHistorial(correo), JSON.stringify(mensajes.slice(-30).map(m => ({ rol: m.rol, contenido: m.contenido, origen: m.origen || '', aviso: m.aviso || '', ubicaciones: Array.isArray(m.ubicaciones) ? m.ubicaciones.slice(0, 7) : [], tipoConsulta: m.tipoConsulta || 'general', actividades: Array.isArray(m.actividades) ? m.actividades.slice(0, 7) : [] }))))
  } catch { /* la app funciona aun si el navegador bloquea almacenamiento */ }
}
export function borrarHistorial(correo) {
  if (correo && typeof localStorage !== 'undefined') localStorage.removeItem(claveHistorial(correo))
}
