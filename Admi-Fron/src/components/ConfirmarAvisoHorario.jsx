/** Confirmación explícita antes de actualizar una actividad cuyo horario cambió. */
export default function ConfirmarAvisoHorario({ actividad, enviando = false, onElegir, onCancelar }) {
  return (
    <div className="confirmacion-fondo" role="presentation" onMouseDown={(e) => { if (e.target === e.currentTarget && !enviando) onCancelar() }}>
      <section className="confirmacion-panel" role="alertdialog" aria-modal="true" aria-labelledby="aviso-horario-titulo" aria-describedby="aviso-horario-texto">
        <div className="confirmacion-icono" aria-hidden="true">i</div>
        <h2 id="aviso-horario-titulo">¿Avisar a los inscritos?</h2>
        <p id="aviso-horario-texto">Has modificado el horario o la ubicación de <strong>{actividad?.nombre}</strong>. ¿Quieres informar a las personas inscritas?</p>
        <p className="suave">Los cambios se guardarán en ambos casos. Si confirmas el aviso, cada inscrito activo recibirá una notificación por el cambio correspondiente.</p>
        <div className="confirmacion-acciones">
          <button type="button" className="boton boton--claro" disabled={enviando} onClick={onCancelar}>Volver a editar</button>
          <button type="button" className="boton boton--claro" disabled={enviando} onClick={() => onElegir(false)}>{enviando ? 'Guardando…' : 'Guardar sin avisar'}</button>
          <button type="button" className="boton" disabled={enviando} onClick={() => onElegir(true)}>{enviando ? 'Guardando…' : 'Guardar y avisar'}</button>
        </div>
      </section>
    </div>
  )
}
