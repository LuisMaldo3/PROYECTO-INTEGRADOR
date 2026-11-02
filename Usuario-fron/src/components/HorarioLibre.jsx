import { cabeEnHorario } from '../services/compatibilidadHorario'
import { useEffect, useState } from 'react'
import { leerSesion } from '../services/authService'
import { guardarDisponibilidad, interpretarHorario, obtenerDisponibilidad, obtenerSugerenciasHuecos, revisarBloques } from '../services/disponibilidadService'
import Icono from './Iconos'
import './HorarioLibre.css'

const dias = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo']
const bloqueNuevo = () => ({ dia: 1, inicio: '08:00', fin: '09:00', nombre: 'Clase / ocupación' })

export default function HorarioLibre() {
  const [bloques, setBloques] = useState([])
  const [mostrarHorario, setMostrarHorario] = useState(false)
  const [confirmado, setConfirmado] = useState(false)
  const [pendiente, setPendiente] = useState(false)
  const [cargando, setCargando] = useState(true)
  const [procesando, setProcesando] = useState(false)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const [avisos, setAvisos] = useState([])
  const [sugerencias, setSugerencias] = useState(null)
  const [cargandoSugerencias, setCargandoSugerencias] = useState(false)
  const [version, setVersion] = useState(0)
  const token = leerSesion()?.token

  useEffect(() => {
    const controller = new AbortController()
    if (!token) { queueMicrotask(() => { setError('Inicia sesión para gestionar tu disponibilidad.'); setMostrarHorario(true); setCargando(false) }); return () => controller.abort() }
    obtenerDisponibilidad(token, controller.signal)
      .then((r) => { if (!controller.signal.aborted) { setBloques(r.bloques || []); setMostrarHorario(!(r.bloques || []).length); setConfirmado(Boolean(r.confirmado)); setPendiente(false) } })
      .catch((e) => { if (!controller.signal.aborted) { setError(e.message); setMostrarHorario(true) } })
      .finally(() => { if (!controller.signal.aborted) setCargando(false) })
    return () => controller.abort()
  }, [token])

  useEffect(() => {
    if (!confirmado || pendiente || !token) return undefined
    const controller = new AbortController()
    queueMicrotask(() => setCargandoSugerencias(true))
    obtenerSugerenciasHuecos(token, controller.signal)
      .then((r) => { if (!controller.signal.aborted) setSugerencias(r) })
      .catch((e) => { if (!controller.signal.aborted) setError(e.message) })
      .finally(() => { if (!controller.signal.aborted) setCargandoSugerencias(false) })
    return () => controller.abort()
  }, [confirmado, pendiente, token, version])

  const modificar = (index, campo, valor) => {
    setBloques((actual) => actual.map((b, i) => i === index ? { ...b, [campo]: campo === 'dia' ? Number(valor) : valor } : b))
    setPendiente(true)
  }
  const leerImagen = async (event) => {
    const imagen = event.target.files?.[0]
    if (!imagen) return
    setError(''); setAvisos([]); setProcesando(true)
    try {
      const datos = await interpretarHorario(token, imagen)
      setBloques(Array.isArray(datos.bloques) ? datos.bloques : [])
      setMostrarHorario(true)
      setAvisos(Array.isArray(datos.advertencias) ? datos.advertencias : [])
      setPendiente(true)
      setSugerencias(null)
    } catch (e) { setError(e.message) }
    finally { setProcesando(false); event.target.value = '' }
  }
  const guardar = async () => {
    const validacion = revisarBloques(bloques)
    if (validacion) { setError(validacion); return }
    setError(''); setGuardando(true)
    try {
      const r = await guardarDisponibilidad(token, bloques)
      setBloques(r.bloques || [])
      setConfirmado(true)
      setPendiente(false)
      setMostrarHorario(false)
      setVersion((n) => n + 1)
    } catch (e) { setError(e.message) }
    finally { setGuardando(false) }
  }
  return (
    <section className="horario-libre">
      <div className="horario-libre__hero">
        <div><span className="horario-libre__eyebrow">DISPONIBILIDAD PERSONAL</span><h2>Tus clases, sin ocupar toda la pantalla</h2>
          <p>Guarda tus horas ocupadas una vez y encuentra actividades que no se crucen con tus clases. Puedes ocultar o editar el horario cuando quieras.</p></div>
        <div className="horario-libre__icon"><Icono nombre="calendario" tamano={35} /></div>
      </div>

      <div className="horario-libre__tarjeta">
        <div className="horario-libre__cabecera"><div><h3>Horario de clases y ocupaciones</h3><p>Solo se guardan los bloques que confirmes. La foto no se conserva.</p></div>
          <span className={`horario-libre__estado ${confirmado && !pendiente ? 'horario-libre__estado--ok' : ''}`}>{confirmado && !pendiente ? 'Confirmado' : pendiente ? 'Cambios sin guardar' : 'Sin configurar'}</span></div>
        {!cargando && <div className="horario-libre__resumen">
          <div className="horario-libre__resumen-datos"><strong>{bloques.length}</strong><span>{bloques.length === 1 ? 'bloque ocupado' : 'bloques ocupados'} · {new Set(bloques.map(b => b.dia)).size} días de la semana</span></div>
          <button type="button" className="horario-libre__toggle" aria-controls="horario-editor" aria-expanded={mostrarHorario} onClick={() => setMostrarHorario(v => !v)}>
            <Icono nombre="calendario" tamano={17} /> {mostrarHorario ? 'Ocultar horario' : 'Mostrar y editar horario'} <span aria-hidden="true">{mostrarHorario ? '↑' : '↓'}</span>
          </button>
        </div>}
        {error && <p className="horario-libre__error" role="alert">{error}</p>}
        {!mostrarHorario && pendiente && <p className="horario-libre__advertencia">Tienes cambios pendientes. Abre el horario y guarda para actualizar las sugerencias.</p>}
        <div id="horario-editor" hidden={!mostrarHorario}>
        <label className={`horario-libre__subir ${procesando ? 'horario-libre__subir--busy' : ''}`}>
          <Icono nombre="calendario" tamano={25} /><strong>{procesando ? 'Leyendo la imagen con Gemini…' : 'Seleccionar imagen del horario'}</strong>
          <small>JPG, PNG o WEBP · Máximo 5 MB</small>
          <input type="file" accept="image/jpeg,image/png,image/webp" onChange={leerImagen} disabled={procesando || guardando} aria-label="Seleccionar una foto de tu horario" />
        </label>
        {cargando && <p role="status">Cargando tu horario…</p>}
        {avisos.map((a, i) => <p key={i} className="horario-libre__advertencia">{a}</p>)}
        {!cargando && <>
          {bloques.length > 0 && <div className="horario-libre__tabla"><div className="horario-libre__fila-titulos"><span>Día</span><span>Inicio</span><span>Fin</span><span>Curso / ocupación</span><span></span></div>
            {bloques.map((b, i) => <div className="horario-libre__fila" key={i}>
              <select aria-label={`Día del bloque ${i + 1}`} value={b.dia} onChange={(e) => modificar(i, 'dia', e.target.value)}>{dias.map((d, j) => <option key={j} value={j + 1}>{d}</option>)}</select>
              <input aria-label={`Inicio del bloque ${i + 1}`} type="time" value={b.inicio} onChange={(e) => modificar(i, 'inicio', e.target.value)} />
              <input aria-label={`Fin del bloque ${i + 1}`} type="time" value={b.fin} onChange={(e) => modificar(i, 'fin', e.target.value)} />
              <input aria-label={`Curso del bloque ${i + 1}`} maxLength={80} value={b.nombre || ''} onChange={(e) => modificar(i, 'nombre', e.target.value)} />
              <button type="button" aria-label={`Eliminar bloque ${i + 1}`} onClick={() => { setBloques((v) => v.filter((_, j) => j !== i)); setPendiente(true) }}>×</button>
            </div>)}</div>}
          {!bloques.length && <p className="horario-libre__vacio">Aún no hay bloques. Sube la imagen o añade las horas ocupadas que falten.</p>}
          <div className="horario-libre__acciones">
            <button type="button" className="boton-secundario" onClick={() => { setBloques((v) => [...v, bloqueNuevo()]); setPendiente(true) }} disabled={procesando || guardando}>+ Agregar bloque</button>
            <button type="button" className="horario-libre__guardar" onClick={guardar} disabled={procesando || guardando || cargando}>{guardando ? 'Guardando…' : 'Confirmar y guardar horario'}</button>
          </div>
          <p className="horario-libre__nota">Revisa días y horas antes de guardar; el reconocimiento de imágenes puede equivocarse. Las sugerencias consideran la franja de 08:00 a 21:00.</p>
        </>}
        </div>
      </div>
      <div className="horario-libre__resultados">
        <div className="horario-libre__cabecera"><div><h3>Actividades compatibles con tu tiempo</h3><p>Resultados de actividades publicadas que no se cruzan con tu disponibilidad confirmada.</p></div>
        {confirmado && !pendiente && <button className="boton-secundario" type="button" onClick={() => setVersion((n) => n + 1)}>Actualizar</button>}</div>
        {!confirmado && <p>Sube y confirma tu horario para ver recomendaciones.</p>}
        {pendiente && <p>Guarda tus correcciones para volver a calcular las sugerencias.</p>}
        {cargandoSugerencias && <p role="status">Buscando planes disponibles…</p>}
        {sugerencias && !pendiente && <>
          <p className="horario-libre__origen">{sugerencias.origen === 'ia' ? '✦ Priorizadas con Gemini' : 'Sugerencias por reglas, sin IA'} · {sugerencias.aviso}</p>
          {!sugerencias.actividades?.length && <p>No hay nuevas actividades compatibles por ahora.</p>}
          <div className="horario-libre__grid">{(sugerencias.actividades || []).map((a) => <article className="horario-libre__sugerencia" key={a.id}>
            <div className="horario-libre__sugerencia-icon"><Icono nombre="destellos" tamano={22} /></div>
            <div><strong>{a.nombre || a.nombreActividad}</strong><small>{a.fecha} · {a.horaInicio} – {a.horaFin}</small>
              {cabeEnHorario(a) && <strong className="horario-libre__compatible">✓ Cabe en tu horario</strong>}
              <span>Hueco libre: {a.huecoInicio} – {a.huecoFin}</span><p>{a.motivo || 'Se ajusta a tus preferencias y disponibilidad.'}</p><a className="vynk-up-card__action" href={`#/actividad?id=${encodeURIComponent(a.id)}`}>Ver detalles e inscribirme →</a></div>
          </article>)}</div>
          <small>La plataforma no te inscribe automáticamente en ninguna actividad.</small>
        </>}
      </div>
    </section>
  )
}
