import { useEffect, useState } from 'react'
import Icono from './Iconos'
import ConfirmarAvisoHorario from './ConfirmarAvisoHorario'
import { datosEdicionActividad } from '../services/datosEdicionActividad'
import { hayCambioNotificable } from '../services/cambiosNotificables'
import SelectorUbicacion from './SelectorUbicacion'
import { useFormularioActividad } from '../hooks/useFormularioActividad'
import { crearActividad, modificarActividad } from '../services/actividadesService'

const organizadorIdVacio = (organizadores) => organizadores.length === 0

/** Formulario «Nueva actividad» (ventana): datos, categoría, ubicación, fecha y horario. Las revisiones salen del hook. */
export default function FormularioActividad({ token, categorias, ubicaciones = [], organizadores = [], onCreada, onCancelar, onSesionExpirada, actividad = null }) {
  const f = useFormularioActividad(actividad ? datosEdicionActividad(actividad) : null)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState('')
  const [confirmandoHorario, setConfirmandoHorario] = useState(false)

  // Escape cierra la ventana, salvo mientras se está guardando.
  useEffect(() => {
    const alTeclear = (e) => e.key === 'Escape' && !enviando && onCancelar()
    document.addEventListener('keydown', alTeclear)
    return () => document.removeEventListener('keydown', alTeclear)
  }, [enviando, onCancelar])

  async function guardar(avisarInscritos = false) {
    setEnviando(true)
    try {
      const guardada = actividad
        ? await modificarActividad(token, actividad.id, f.valores, { avisarInscritos })
        : await crearActividad(token, f.valores)
      setConfirmandoHorario(false)
      onCreada(guardada)

    } catch (e) {
      if (e.estado === 401) return onSesionExpirada(e.detalle)
      if (e.errores && Object.keys(e.errores).length > 0) f.errorDelServidor(e.errores)
      else setError(e.message)
    } finally {
      setEnviando(false)
    }
  }

  async function enviar(evento) {
    evento.preventDefault()
    setError('')
    if (!f.revisar()) return
    if (hayCambioNotificable(actividad, f.valores)) {
      setConfirmandoHorario(true)
      return
    }
    await guardar(false)
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
        <div className="modal__cabecera"><div className="modal__distintivo"><Icono nombre="calendario" tam={21} /></div>
          <div><p className="eyebrow">GESTIÓN DE ACTIVIDADES</p><h2 id="form-titulo">{actividad ? 'Editar actividad' : 'Nueva actividad'}</h2>
            <p className="suave">Completa los detalles, lugar y horario del evento.</p></div>
          <button type="button" className="detalle__cerrar" aria-label="Cerrar formulario" onClick={onCancelar} disabled={enviando}><Icono nombre="cerrar" /></button>
        </div>

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

        <label className="campo">
          Organizador responsable
          <select value={valores.organizadorId ?? ''} onChange={(e) => f.cambiar('organizadorId', e.target.value)} {...campo('organizadorId')}>
            <option value="">Elige un organizador</option>
            {organizadores.map((o) => (
              <option key={o.id} value={o.id}>{o.nombre}{o.correo ? ` (${o.correo})` : ''}</option>
            ))}
          </select>
          {organizadorIdVacio(organizadores) && <span className="campo__ayuda">No hay organizadores activos registrados.</span>}
          {errores.organizadorId && <span className="campo__error" role="alert">{errores.organizadorId}</span>}
        </label>

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

        <label className="campo">
          Máximo de participantes
          <input type="number" min="1" step="1" value={valores.maximoParticipantes}
            onChange={(e) => f.cambiar('maximoParticipantes', e.target.value)}
            placeholder="Ej.: 30" {...campo('maximoParticipantes')} />
          {errores.maximoParticipantes && <span className="campo__error" role="alert">{errores.maximoParticipantes}</span>}
        </label>

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
            <Icono nombre="check" tam={18} /> {enviando ? 'Guardando…' : (actividad ? 'Guardar cambios' : 'Crear actividad')}
          </button>
        </div>
      </form>
      {confirmandoHorario && <ConfirmarAvisoHorario actividad={actividad} enviando={enviando}
        onCancelar={() => setConfirmandoHorario(false)} onElegir={(avisar) => guardar(avisar)} />}
    </div>
  )
}
