/** Confirmación explícita antes de actualizar una actividad cuyo horario cambió. */
export default function ConfirmarAvisoHorario({ actividad, enviando = false, onElegir, onCancelar }) {
  return (
    <div className="confirmacion-fondo" role="presentation" onMouseDown={(e) => { if (e.target === e.currentTarget && !enviando) onCancelar() }}>
      <section className="confirmacion-panel" role="alertdialog" aria-modal="true" aria-labelledby="aviso-horario-titulo" aria-describedby="aviso-horario-texto">
        <div className="confirmacion-icono" aria-hidden="true">i</div>
        <h2 id="aviso-horario-titulo">¿Avisar a los inscritos?</h2>
        <p id="aviso-horario-texto">Cambiaste la fecha o las horas de <strong>{actividad?.nombre}</strong>. ¿Quieres generar un aviso para sus participantes inscritos?</p>
        <p className="suave">Se guardará el horario igualmente. Puedes elegir no enviar el aviso.</p>
        <div className="confirmacion-acciones">
          <button type="button" className="boton boton--claro" disabled={enviando} onClick={onCancelar}>Volver a editar</button>
          <button type="button" className="boton boton--claro" disabled={enviando} onClick={() => onElegir(false)}>{enviando ? 'Guardando…' : 'Guardar sin avisar'}</button>
          <button type="button" className="boton" disabled={enviando} onClick={() => onElegir(true)}>{enviando ? 'Guardando…' : 'Guardar y avisar'}</button>
        </div>
      </section>
    </div>
  )
}
