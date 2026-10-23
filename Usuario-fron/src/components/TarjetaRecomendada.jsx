import { cabeEnHorario, descripcionCompatibilidad } from '../services/compatibilidadHorario'
export function TarjetaRecomendada({ actividad, posicion }) {
  const a = actividad
  return <article className="rec-tarjeta">
    <div className="rec-tarjeta__meta"><span className="rec-rank">#{posicion} · Para ti</span><span>{a.cuposDisponibles == null ? 'Cupos por confirmar' : `${a.cuposDisponibles} cupos`}</span></div>
    <h3>{a.nombre}</h3><p className="rec-desc">{a.descripcion || 'Actividad publicada en VYNK.'}</p>
    <div className="rec-datos"><span>📅 {a.fecha || 'Fecha por confirmar'}</span><span>🕒 {a.horaInicio || '--:--'} – {a.horaFin || '--:--'}</span></div>
    {cabeEnHorario(a) && <div className="rec-compatible" aria-label="Actividad compatible con tu horario confirmado">
      <strong>✓ Cabe en tu horario</strong>
      <small>{descripcionCompatibilidad(a)}</small>
      {a.motivoHorario && <span>{a.motivoHorario}</span>}
    </div>}
    <div className="rec-motivo"><strong>✦ ¿Por qué te la recomendamos?</strong><p>{a.motivo || 'Actividad publicada por Tecsup.'}</p></div>
  </article>
}
