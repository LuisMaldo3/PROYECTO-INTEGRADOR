import { useEffect, useState } from 'react'
import Icono from './Iconos'
import SelectorUbicacion from './SelectorUbicacion'
import { useFormularioActividad } from '../hooks/useFormularioActividad'
import { crearActividad } from '../services/actividadesService'

/** Formulario «Nueva actividad» (ventana): datos, categoría, ubicación, fecha y horario. Las revisiones salen del hook. */
export default function FormularioActividad({ token, categorias, ubicaciones = [], onCreada, onCancelar, onSesionExpirada }) {
  const f = useFormularioActividad()
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState('')

  // Escape cierra la ventana, salvo mientras se está guardando.
  useEffect(() => {
    const alTeclear = (e) => e.key === 'Escape' && !enviando && onCancelar()
    document.addEventListener('keydown', alTeclear)
    return () => document.removeEventListener('keydown', alTeclear)
  }, [enviando, onCancelar])

  async function enviar(evento) {
    evento.preventDefault()
    setError('')
    if (!f.revisar()) return
    setEnviando(true)
    try {
      onCreada(await crearActividad(token, f.valores))
    } catch (e) {
      if (e.estado === 401) return onSesionExpirada(e.detalle)
      if (e.errores && Object.keys(e.errores).length > 0) f.errorDelServidor(e.errores)
      else setError(e.message)
    } finally {
      setEnviando(false)
    }
  }

  const { valores, errores, limites } = f
  const campo = (nombre) => ({
    'aria-invalid': errores[nombre] ? 'true' : undefined,
    onBlur: () => f.tocar(nombre),
    disabled: enviando,
  })

  return (
    <div className="modal">
      <form className="modal__ventana modal__ventana--ancha" role="dialog" aria-modal="true" aria-labelledby="form-titulo" onSubmit={enviar} noValidate>
        <h2 id="form-titulo">Nueva actividad</h2>

        <label className="campo">
          Nombre
          <input
            type="text"
            value={valores.nombre}
            onChange={(e) => f.cambiar('nombre', e.target.value)}
            placeholder="Ej.: Torneo de fútbol"
            autoFocus
            {...campo('nombre')}
          />
          {errores.nombre && <span className="campo__error" role="alert">{errores.nombre}</span>}
        </label>

        <label className="campo">
          Descripción
          <textarea
            rows={4}
            value={valores.descripcion}
            onChange={(e) => f.cambiar('descripcion', e.target.value)}
            placeholder="¿De qué trata la actividad?"
            {...campo('descripcion')}
          />
          <span className="campo__contador">{valores.descripcion.trim().length}/{limites.descripcionMax}</span>
          {errores.descripcion && <span className="campo__error" role="alert">{errores.descripcion}</span>}
        </label>

        <div className="campo-fila campo-fila--dos">
          <label className="campo">
            Categoría
            <select value={valores.categoriaId} onChange={(e) => f.cambiar('categoriaId', e.target.value)} {...campo('categoriaId')}>
              <option value="">Elige una categoría</option>
              {categorias.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.nombre}
                </option>
              ))}
            </select>
            {errores.categoriaId && <span className="campo__error" role="alert">{errores.categoriaId}</span>}
          </label>
          <SelectorUbicacion
            valor={valores.ubicacionId ?? ''}
            ubicaciones={ubicaciones}
            error={errores.ubicacionId}
            deshabilitado={enviando}
            onCambiar={(id) => f.cambiar('ubicacionId', id)}
            onSalir={() => f.tocar('ubicacionId')}
          />
        </div>

        <div className="campo-fila">
          <label className="campo">
            Fecha
            <input type="date" value={valores.fecha ?? ''} onChange={(e) => f.cambiar('fecha', e.target.value)} {...campo('fecha')} />
            {errores.fecha && <span className="campo__error" role="alert">{errores.fecha}</span>}
          </label>
          <label className="campo">
            Hora de inicio
            <input type="time" value={valores.horaInicio ?? ''} onChange={(e) => f.cambiar('horaInicio', e.target.value)} {...campo('horaInicio')} />
            {errores.horaInicio && <span className="campo__error" role="alert">{errores.horaInicio}</span>}
          </label>
          <label className="campo">
            <span className="campo__titulo">Hora de fin <small>opcional</small></span>
            <input type="time" value={valores.horaFin ?? ''} onChange={(e) => f.cambiar('horaFin', e.target.value)} {...campo('horaFin')} />
            {errores.horaFin && <span className="campo__error" role="alert">{errores.horaFin}</span>}
          </label>
        </div>

        {error && (
          <p className="aviso aviso--error" role="alert">
            <Icono nombre="alerta" tam={18} /> {error}
          </p>
        )}

        <div className="modal__acciones">
          <button type="button" className="boton boton--claro" onClick={onCancelar} disabled={enviando}>
            Cancelar
          </button>
          <button type="submit" className="boton" disabled={enviando}>
            <Icono nombre="check" tam={18} /> {enviando ? 'Guardando…' : 'Crear actividad'}
          </button>
        </div>
      </form>
    </div>
  )
}
