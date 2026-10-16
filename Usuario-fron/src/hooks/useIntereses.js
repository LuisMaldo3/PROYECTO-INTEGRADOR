import { useCallback, useEffect, useRef, useState } from 'react'
import { guardarMisIntereses, listarIntereses, obtenerMisIntereses, omitirIntereses } from '../services/interesesService'
import { alternar, mismosIntereses, ordenarComoCatalogo, puedeGuardar } from '../services/intereses'

/**
 * Carga los intereses disponibles y los que eligió la persona, y maneja la selección.
 * seleccion = lo que está marcado ahora · guardados = lo que ya está en el servidor
 * alternar(id) marca/desmarca · guardar() envía · descartar() vuelve a lo guardado · recargar() pide todo de nuevo
 * aviso = { tipo: 'ok' | 'error', texto } del último guardado.
 */
export function useIntereses({ token, onSesionExpirada, onGuardado, onOmitir }) {
  const [estado, setEstado] = useState({ cargando: true, error: '', catalogo: [], guardados: [] })
  const [seleccion, setSeleccion] = useState([])
  const [guardando, setGuardando] = useState(false)
  const [omitiendo, setOmitiendo] = useState(false)
  const [aviso, setAviso] = useState(null)
  const [version, setVersion] = useState(0)

  // Se guarda en una referencia para que un `onSesionExpirada` nuevo en cada pintado no vuelva a pedir los datos.
  const sesionExpirada = useRef(onSesionExpirada)
  const callbacks = useRef({ onGuardado, onOmitir })
  useEffect(() => {
    sesionExpirada.current = onSesionExpirada
    callbacks.current = { onGuardado, onOmitir }
  })

  useEffect(() => {
    const ctrl = new AbortController()
    Promise.all([listarIntereses(token, ctrl.signal), obtenerMisIntereses(token, ctrl.signal)])
      .then(([catalogo, mios]) => {
        if (ctrl.signal.aborted) return
        const guardados = ordenarComoCatalogo(mios, catalogo)
        setEstado({ cargando: false, error: '', catalogo, guardados })
        setSeleccion(guardados)
      })
      .catch((e) => {
        if (ctrl.signal.aborted) return
        if (e.estado === 401) return sesionExpirada.current?.()
        setEstado({ cargando: false, error: e.message, catalogo: [], guardados: [] })
      })
    return () => ctrl.abort()
  }, [token, version])

  const recargar = useCallback(() => {
    setEstado((e) => ({ ...e, cargando: true, error: '' }))
    setAviso(null)
    setVersion((v) => v + 1)
  }, [])

  const cambiar = useCallback((id) => {
    setAviso(null)
    setSeleccion((actual) => alternar(actual, id))
  }, [])

  const descartar = useCallback(() => {
    setAviso(null)
    setSeleccion(estado.guardados)
  }, [estado.guardados])

  const guardar = useCallback(async () => {
    setGuardando(true)
    setAviso(null)
    try {
      const quedaron = ordenarComoCatalogo(await guardarMisIntereses(token, seleccion), estado.catalogo)
      setEstado((e) => ({ ...e, guardados: quedaron }))
      setSeleccion(quedaron)
      setAviso({ tipo: 'ok', texto: 'Tus intereses se guardaron.' })
      callbacks.current.onGuardado?.()
    } catch (e) {
      if (e.estado === 401) return sesionExpirada.current?.()
      setAviso({ tipo: 'error', texto: e.message })
    } finally {
      setGuardando(false)
    }
  }, [token, seleccion, estado.catalogo])

  const omitir = useCallback(async () => {
    if (omitiendo || guardando) return
    setOmitiendo(true)
    setAviso(null)
    try {
      await omitirIntereses(token)
      callbacks.current.onOmitir?.()
    } catch (e) {
      if (e.estado === 401) return sesionExpirada.current?.()
      // Si el servidor está temporalmente caído o aún no expone /omitir,
      // la decisión se recuerda localmente y se sincronizará al volver a entrar.
      if (e.estado === 0 || e.estado === 404 || e.estado >= 500) {
        callbacks.current.onOmitir?.()
      } else {
        setAviso({ tipo: 'error', texto: e.message })
      }
    } finally {
      setOmitiendo(false)
    }
  }, [token, omitiendo, guardando])

  return {
    ...estado,
    seleccion,
    guardando,
    omitiendo,
    omitir,
    aviso,
    hayCambios: !mismosIntereses(seleccion, estado.guardados),
    puedeGuardar: !omitiendo && puedeGuardar({ seleccion, guardados: estado.guardados, guardando }),
    alternar: cambiar,
    guardar,
    descartar,
    recargar,
  }
}
