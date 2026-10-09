import { useEffect, useState } from 'react'

/** Devuelve la parte "#/..." de la dirección y se actualiza cuando cambia (sin recargar). */
export function useHash() {
  const [hash, setHash] = useState(() => window.location.hash)

  useEffect(() => {
    const actualizar = () => setHash(window.location.hash)
    window.addEventListener('hashchange', actualizar)
    return () => window.removeEventListener('hashchange', actualizar)
  }, [])

  return hash
}
