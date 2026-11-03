import { useCallback, useEffect, useState } from 'react'
import { obtenerInscritosPorActividad } from '../services/inscritosService'

const VACIO = { generadoEn: null, totales: null, actividades: [] }

/**
 * US-35 · Tarea 2 · Carga de los inscritos por actividad.
 * Pide los datos al montar y con recargar(). Si el usuario sale de la pantalla, la petición se cancela.
 * Devuelve { datos, cargando, error, recargar }.
 */
export function useInscritosPorActividad({ token, onSesionExpirada }) {
  const [estado, setEstado] = useState({ datos: VACIO, cargando: true, error: '' })
  const [version, setVersion] = useState(0)

  useEffect(() => {
    const control = new AbortController()
    obtenerInscritosPorActividad(token, control.signal)
      .then((datos) => { if (!control.signal.aborted) setEstado({ datos, cargando: false, error: '' }) })
      .catch((e) => {
        if (control.signal.aborted) return
        if (e?.estado === 401) { onSesionExpirada?.(e.detalle); return }
        setEstado((anterior) => ({ ...anterior, cargando: false, error: e?.message || 'No se pudieron cargar los inscritos.' }))
      })
    return () => control.abort()
  }, [token, version, onSesionExpirada])

  const recargar = useCallback(() => {
    setEstado((e) => ({ ...e, cargando: true, error: '' }))
    setVersion((v) => v + 1)
  }, [])
  return { ...estado, recargar }
}
