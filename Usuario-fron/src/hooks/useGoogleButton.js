import { useEffect, useRef, useState } from 'react'

const GSI_SRC = 'https://accounts.google.com/gsi/client'

function cargarScriptGoogle() {
  return new Promise((resolve, reject) => {
    if (window.google?.accounts?.id) return resolve()

    let script = document.querySelector(`script[src="${GSI_SRC}"]`)
    if (!script) {
      script = document.createElement('script')
      script.src = GSI_SRC
      script.async = true
      script.defer = true
      document.head.appendChild(script)
    }
    script.addEventListener('load', () => resolve())
    script.addEventListener('error', () => reject(new Error('No se pudo cargar Google')))
  })
}

/**
 * Dibuja el botón oficial "Continuar con Google" dentro de `contenedorRef`.
 * Cuando el usuario elige su cuenta, Google entrega un ID token y se llama a
 * `onCredencial(idToken)`.
 *
 * estado: 'cargando' | 'listo' | 'error' | 'sin-config'
 */
export function useGoogleButton({ clientId, contenedorRef, onCredencial }) {
  const [estado, setEstado] = useState(clientId ? 'cargando' : 'sin-config')
  const callbackRef = useRef(onCredencial)

  useEffect(() => {
    callbackRef.current = onCredencial
  })

  useEffect(() => {
    if (!clientId) return undefined
    let cancelado = false
    let observador

    cargarScriptGoogle()
      .then(() => {
        const contenedor = contenedorRef.current
        if (cancelado || !contenedor) return

        window.google.accounts.id.initialize({
          client_id: clientId,
          callback: (respuesta) => callbackRef.current?.(respuesta.credential),
          auto_select: false,
          cancel_on_tap_outside: true,
        })

        let anchoActual = 0
        const dibujar = () => {
          // Google solo acepta anchos entre 200 y 400 px: lo ajustamos al contenedor.
          const ancho = Math.max(200, Math.min(400, Math.floor(contenedor.clientWidth)))
          if (ancho === anchoActual) return
          anchoActual = ancho
          window.google.accounts.id.renderButton(contenedor, {
            type: 'standard',
            theme: 'outline',
            size: 'large',
            text: 'continue_with',
            shape: 'pill',
            logo_alignment: 'left',
            locale: 'es',
            width: ancho,
          })
        }

        dibujar()
        observador = new ResizeObserver(dibujar)
        observador.observe(contenedor)
        setEstado('listo')
      })
      .catch(() => {
        if (!cancelado) setEstado('error')
      })

    return () => {
      cancelado = true
      observador?.disconnect()
    }
  }, [clientId, contenedorRef])

  return estado
}
