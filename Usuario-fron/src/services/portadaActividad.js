import { API_URL } from '../config.js'

/** URL de portada segura: Commons o archivo generado y publicado por Spring Boot. */
export function urlImagenActividad(actividad) {
  const url = actividad?.portadaUrl || actividad?.portada?.url
  if (!url || typeof url !== 'string') return ''
  if (url.startsWith('/api/portadas/archivo/')) return `${API_URL}${url}`
  if (/^https:\/\/upload\.wikimedia\.org\//i.test(url)) return url
  if (actividad?.portada?.tipo === 'url' && /^https:\/\//i.test(url)) return url
  return ''
}

/** Ilustración local temática para actividades antiguas sin foto oficial. */
export function imagenTematicaActividad(actividad) {
  const titulo = `${actividad?.nombre || ''} ${actividad?.areaTecsup || ''}`.toLowerCase()
  if (/f[uú]tbol|soccer|bal[oó]n/.test(titulo)) return '/departamentos/futbol.svg'
  if (/qu[ií]mica|miner[ií]a|laboratorio|procesos/.test(titulo)) return '/departamentos/quimica.svg'
  if (/electr[oó]nica|electricidad|circuit/.test(titulo)) return '/departamentos/electronica.svg'
  if (/mec[aá]nica|taller|automotriz/.test(titulo)) return '/departamentos/mecanica.svg'
  if (/digital|programaci[oó]n|c[oó]digo|software|tecnolog/.test(titulo)) return '/departamentos/digital.svg'
  if (/gesti[oó]n|producci[oó]n|negocios|log[ií]stica/.test(titulo)) return '/departamentos/gestion.svg'
  return '/departamentos/actividades.svg'
}

export function creditoPortada(actividad) {
  const p = actividad?.portada
  if (!p || p.tipo !== 'commons' || !p.creditoUrl?.startsWith('https://commons.wikimedia.org/')) return null
  return { texto: `Foto: ${p.autor || 'Wikimedia Commons'} · ${p.licencia || 'licencia abierta'}`, url: p.creditoUrl }
}
