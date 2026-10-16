import { useEffect, useState } from 'react'
import { leerSesion } from '../services/authService'
import { obtenerAlternativasCruce } from '../services/alternativasService'

/** Solicita cada cruce por separado; aborta las solicitudes al cambiar la agenda. */
export function useAlternativasCruces(cruces = []) {
  const [resultados, setResultados] = useState({})
  const [recargas, setRecargas] = useState(0)
  const clave = cruces.map((c) => [c.id, c.actividadAId, c.actividadBId].join(':')).join('|')

  useEffect(() => {
    const token = leerSesion()?.token
    const ctrl = new AbortController()
    const ids = cruces.map((c) => c.id).filter(Boolean)
    // Lógica asincrónica para no actualizar el estado directamente durante el efecto.
    queueMicrotask(() => { if (!ctrl.signal.aborted) setResultados({}) })
    if (ids.length && token) {
      for (const id of ids) {
        queueMicrotask(() => { if (!ctrl.signal.aborted) setResultados((prev) => ({ ...prev, [id]: { cargando: true } })) })
        obtenerAlternativasCruce(token, id, ctrl.signal)
          .then((respuesta) => { if (!ctrl.signal.aborted) setResultados((prev) => ({ ...prev, [id]: { ...respuesta, cargando: false } })) })
          .catch((e) => { if (!ctrl.signal.aborted) setResultados((prev) => ({ ...prev, [id]: { cargando: false, error: e.message } })) })
      }
    } else if (ids.length) {
      queueMicrotask(() => { if (!ctrl.signal.aborted) setResultados(Object.fromEntries(ids.map((id) => [id, { error: 'Inicia sesión para ver alternativas.' }]))) })
    }
    return () => ctrl.abort()
    // Los IDs y reintentos determinan exactamente la carga; las solicitudes usan una sola señal de cancelación.
  }, [clave, recargas])

  const reintentar = () => setRecargas((n) => n + 1)
  return { resultados, reintentar }
}
