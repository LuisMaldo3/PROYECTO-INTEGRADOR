/** PLACEHOLDER · Tarea 5 es de Mishel. Se reemplaza por su archivo al integrar. */
export default function ConfirmarRol({ rolNuevo, enviando = false, error = '', onConfirmar, onCancelar }) {
  return (
    <div role="dialog" aria-modal="true">
      <p>¿Cambiar el rol a «{rolNuevo}»?</p>
      {error && <p className="aviso aviso--error" role="alert">{error}</p>}
      <button type="button" className="boton boton--claro" onClick={onCancelar} disabled={enviando}>Cancelar</button>{' '}
      <button type="button" className="boton" onClick={onConfirmar} disabled={enviando}>{enviando ? 'Guardando…' : 'Confirmar'}</button>
    </div>
  )
}
