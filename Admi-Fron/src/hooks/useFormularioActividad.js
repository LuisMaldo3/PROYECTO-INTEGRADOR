import { useState } from 'react'

/**
 * PLACEHOLDER · La tarea 3 (revisiones del formulario) es de Jordi. Se reemplaza por su archivo al integrar.
 * Misma interfaz que la real; aquí solo se exige que los tres campos no estén vacíos.
 */
const LIMITES = { nombreMin: 3, nombreMax: 80, descripcionMin: 10, descripcionMax: 500 }

export function useFormularioActividad() {
  const [valores, setValores] = useState({ nombre: '', descripcion: '', categoriaId: '' })
  const [errores, setErrores] = useState({})

  return {
    valores,
    errores,
    limites: LIMITES,
    cambiar: (campo, valor) => setValores((v) => ({ ...v, [campo]: valor })),
    tocar: () => {},
    revisar: () => {
      const faltan = Object.fromEntries(Object.entries(valores).filter(([, v]) => !v.trim()).map(([c]) => [c, 'Obligatorio.']))
      setErrores(faltan)
      return Object.keys(faltan).length === 0
    },
    errorDelServidor: setErrores,
    reiniciar: () => setValores({ nombre: '', descripcion: '', categoriaId: '' }),
  }
}
