import { useIntereses } from '../hooks/useIntereses'
import { iconoDeInteres, textoAyuda, textoContador } from '../services/intereses'
import EstadoPantalla from './EstadoPantalla'
import Icono from './Iconos'
import './Intereses.css'

/** Pantalla «Mis intereses»: la persona marca los temas que le gustan y los guarda. */
export function Intereses({ token, onSesionExpirada, primerIngreso = false, onGuardado, onOmitir }) {
  const i = useIntereses({ token, onSesionExpirada, onGuardado, onOmitir })

  if (i.cargando) {
    return (
      <div className="intereses" aria-busy="true">
        <div className="intereses__grid">
          {Array.from({ length: 8 }, (_, n) => (
            <span key={n} className="interes interes--esqueleto" />
          ))}
        </div>
      </div>
    )
  }

  if (i.error) {
    return (
      <div className="intereses">
      <EstadoPantalla
        icono="alerta"
        tono="error"
        titulo="No pudimos cargar los intereses"
        texto={i.error}
        accion={
          <button type="button" className="boton-secundario" onClick={i.recargar}>
            <Icono nombre="actualizar" tamano={16} /> Reintentar
          </button>
        }
      />
      {primerIngreso && (
        <div className="intereses__acciones">
          <button type="button" className="boton-secundario" disabled={i.omitiendo} onClick={i.omitir}>
            {i.omitiendo ? 'Omitiendo…' : 'Omitir por ahora'}
          </button>
        </div>
      )}
      {i.aviso?.tipo === 'error' && <p role="alert" className="intereses__aviso intereses__aviso--error">{i.aviso.texto}</p>}
      </div>
    )
  }

  return (
    <div className="intereses">
      {primerIngreso && (
        <div className="intereses__intro">
          <strong>Personaliza tu experiencia</strong>
          <p>Elige las actividades que te interesan. También puedes continuar y hacerlo después desde tu perfil.</p>
        </div>
      )}
      <p className="intereses__guia">{textoAyuda(i.seleccion.length)}</p>

      <div className="intereses__grid" role="group" aria-label="Intereses disponibles">
        {i.catalogo.map((interes) => {
          const marcado = i.seleccion.includes(interes.id)
          return (
            <button
              key={interes.id}
              type="button"
              className={`interes${marcado ? ' interes--marcado' : ''}`}
              aria-pressed={marcado}
              disabled={i.guardando || i.omitiendo}
              onClick={() => i.alternar(interes.id)}
            >
              <span className="interes__icono">
                <Icono nombre={iconoDeInteres(interes.id)} tamano={22} />
              </span>
              <span className="interes__texto">
                <strong>{interes.nombre}</strong>
                <small>{interes.descripcion}</small>
              </span>
              <span className="interes__marca" aria-hidden="true">
                {marcado && <Icono nombre="check" tamano={14} />}
              </span>
            </button>
          )
        })}
      </div>

      <div className="intereses__barra">
        <div className="intereses__estado" aria-live="polite">
          <strong>{textoContador(i.seleccion.length)}</strong>
          {i.aviso && (
            <span className={`intereses__aviso intereses__aviso--${i.aviso.tipo}`}>
              <Icono nombre={i.aviso.tipo === 'ok' ? 'check' : 'alerta'} tamano={16} />
              {i.aviso.texto}
            </span>
          )}
        </div>
        <div className="intereses__acciones">
          {primerIngreso && (
            <button type="button" className="boton-secundario" onClick={i.omitir} disabled={i.omitiendo || i.guardando}>
              {i.omitiendo ? 'Omitiendo…' : 'Omitir por ahora'}
            </button>
          )}
          <button type="button" className="boton-secundario" onClick={i.descartar} disabled={!i.hayCambios || i.guardando || i.omitiendo}>
            Descartar cambios
          </button>
          <button type="button" className="boton-primario" onClick={i.guardar} disabled={!i.puedeGuardar}>
            {i.guardando ? 'Guardando…' : 'Guardar intereses'}
          </button>
        </div>
      </div>
    </div>
  )
}
