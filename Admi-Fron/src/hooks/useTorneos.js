import { useCallback, useEffect, useState } from 'react'
import { listarTorneos } from '../services/torneosService'

/** Torneos de un tipo ('arena' o 'karaduo'). `reemplazar` actualiza uno con lo que devolvió el backend. */
export function useTorneos({ token, tipo, onSesionExpirada }) {
  const [estado, setEstado] = useState({ torneos: [], cargando: true, error: '' })
  const [version, setVersion] = useState(0)

  useEffect(() => {
    const control = new AbortController()
    listarTorneos(token, tipo, control.signal)
      .then((torneos) => { if (!control.signal.aborted) setEstado({ torneos, cargando: false, error: '' }) })
      .catch((e) => {
        if (control.signal.aborted) return
        if (e?.estado === 401) { onSesionExpirada?.(e.detalle); return }
        setEstado((a) => ({ ...a, cargando: false, error: e?.message || 'No se pudieron cargar los torneos.' }))
      })
    return () => control.abort()
  }, [token, tipo, version, onSesionExpirada])

  const recargar = useCallback(() => { setEstado((a) => ({ ...a, cargando: true, error: '' })); setVersion((v) => v + 1) }, [])
  const reemplazar = useCallback((torneo) => setEstado((a) => ({ ...a, torneos: a.torneos.some((t) => t.id === torneo.id) ? a.torneos.map((t) => (t.id === torneo.id ? { ...t, ...torneo } : t)) : [torneo, ...a.torneos] })), [])
  const quitar = useCallback((id) => setEstado((a) => ({ ...a, torneos: a.torneos.filter((t) => t.id !== id) })), [])
  return { ...estado, recargar, reemplazar, quitar }
}
