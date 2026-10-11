import { useEffect } from 'react'
import { etiquetaRol } from '../services/usuarios'

/** Tarea 5 · Ventana que pide confirmar el cambio de rol antes de guardarlo. */
export default function ConfirmarRol({ usuario, rolNuevo, enviando = false, error = '', onConfirmar, onCancelar }) {
  // Escape cierra la ventana, salvo mientras se está guardando.
  useEffect(() => {
    const alTeclear = (e) => e.key === 'Escape' && !enviando && onCancelar()
    document.addEventListener('keydown', alTeclear)
    return () => document.removeEventListener('keydown', alTeclear)
  }, [enviando, onCancelar])

  const nombre = usuario?.nombre ?? usuario?.correo ?? 'este usuario'

  return (
    <div className="modal" onClick={() => !enviando && onCancelar()}>
      <div
        className="modal__ventana"
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-titulo"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="modal-titulo">Confirmar cambio de rol</h2>
        <p>
          ¿Cambiar el rol de <strong>{nombre}</strong> de{' '}
          <span className={`etiqueta etiqueta--${etiquetaRol(usuario?.rol).toLowerCase()}`}>{etiquetaRol(usuario?.rol)}</span> a{' '}
          <span className={`etiqueta etiqueta--${etiquetaRol(rolNuevo).toLowerCase()}`}>{etiquetaRol(rolNuevo)}</span>?
        </p>
        {error && (
          <p className="aviso aviso--error" role="alert">
            {error}
          </p>
        )}
        <div className="modal__acciones">
          <button type="button" className="boton boton--claro" onClick={onCancelar} disabled={enviando}>
            Cancelar
          </button>
          <button type="button" className="boton" onClick={onConfirmar} disabled={enviando} autoFocus>
            {enviando ? 'Guardando…' : 'Confirmar'}
          </button>
        </div>
      </div>
    </div>
  )
}
