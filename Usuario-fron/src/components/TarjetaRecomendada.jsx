import { cabeEnHorario, descripcionCompatibilidad } from '../services/compatibilidadHorario'
export function TarjetaRecomendada({ actividad, posicion, onDescartar, deshabilitado = false }) {
  const a = actividad
  return <article className="rec-tarjeta">
    <div className="rec-tarjeta__meta"><span className="rec-rank">#{posicion} · Para ti</span><span className="rec-tarjeta__cupos">{a.cuposDisponibles == null ? 'Cupos por confirmar' : `${a.cuposDisponibles} cupos`}</span></div>
    <h3>{a.nombre}</h3><p className="rec-desc">{a.descripcion || 'Actividad publicada en VYNK.'}</p>
    <div className="rec-datos"><span>📅 {a.fecha || 'Fecha por confirmar'}</span><span>🕒 {a.horaInicio || '--:--'} – {a.horaFin || '--:--'}</span></div>
    {cabeEnHorario(a) && <div className="rec-compatible" aria-label="Actividad compatible con tu horario confirmado">
      <strong>✓ Cabe en tu horario</strong>
      <small>{descripcionCompatibilidad(a)}</small>
      {a.motivoHorario && <span>{a.motivoHorario}</span>}
    </div>}
    <div className="rec-motivo"><strong>✦ ¿Por qué te la recomendamos?</strong><p>{a.motivo || 'Actividad publicada por Tecsup.'}</p></div>
    <a className="vynk-up-card__action" href={`#/actividad?id=${encodeURIComponent(a.id)}`}>Ver detalles e inscribirme <span aria-hidden="true">→</span></a>
    <div className="rec-tarjeta__pie">
      <span className="rec-tarjeta__nota">Ajusta tus sugerencias</span>
      <button type="button" className="rec-descartar" onClick={onDescartar} disabled={deshabilitado} aria-label={`No me interesa ${a.nombre}`}>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true"><path d="M18 6 6 18M6 6l12 12"/></svg>
        No me interesa
      </button>
    </div>
  </article>
}
