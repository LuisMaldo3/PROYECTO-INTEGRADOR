import { useCallback, useEffect, useMemo, useState } from 'react'
import Icono from './Iconos'
import PortadaActividad from './PortadaActividad'
import { urlPortada } from '../services/portadasService'
import { listarAvisos, publicarAviso, validarAviso } from '../services/avisos'

const fechaLegible = (valor) => {
  if (!valor) return '—'
  const fecha = new Date(valor)
  return Number.isNaN(fecha.getTime()) ? '—' : new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' }).format(fecha)
}

export default function PantallaAvisos({ token, onSesionExpirada }) {
  const [avisos, setAvisos] = useState([])
  const [titulo, setTitulo] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [portada, setPortada] = useState(null)
  const [modo, setModo] = useState('ahora')
  const [fechaLocal, setFechaLocal] = useState('')
  const [filtro, setFiltro] = useState('todos')
  const [errores, setErrores] = useState({})
  const [error, setError] = useState('')
  const [exito, setExito] = useState('')
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [conImagen, setConImagen] = useState(false)

  const cargar = useCallback(async () => {
    setCargando(true)
    try { setAvisos(await listarAvisos(token)); setError('') }
    catch (e) { if (e.estado === 401) onSesionExpirada?.(e.detalle); else setError(e.message) }
    finally { setCargando(false) }
  }, [token, onSesionExpirada])
  useEffect(() => { cargar() }, [cargar])

  const programados = useMemo(() => avisos.filter((a) => a.estado === 'programado'), [avisos])
  const publicados = useMemo(() => avisos.filter((a) => a.estado !== 'programado'), [avisos])
  const visibles = filtro === 'programados' ? programados : filtro === 'publicados' ? publicados : avisos

  async function enviar(evento) {
    evento.preventDefault()
    if (guardando) return
    const resultado = validarAviso({ titulo, mensaje, portada, programar: modo === 'programar', fechaLocal })
    setErrores(resultado.errores)
    setExito(''); setError('')
    if (Object.keys(resultado.errores).length) return
    setGuardando(true)
    try {
      const nuevo = await publicarAviso(token, resultado)
      setAvisos((actual) => [nuevo, ...actual.filter((a) => a.id !== nuevo.id)])
      setTitulo(''); setMensaje(''); setPortada(null); setConImagen(false); setFechaLocal(''); setModo('ahora'); setErrores({})
      setExito(nuevo.estado === 'programado' ? `Aviso programado para el ${fechaLegible(nuevo.publicarEn)}.` : 'Aviso publicado. Ya aparece en el Inicio de los estudiantes.')
    } catch (e) {
      if (e.estado === 401) onSesionExpirada?.(e.detalle)
      else { setErrores(e.errores || {}); setError(e.message) }
    } finally { setGuardando(false) }
  }

  return <section className="avisos-pagina">
    <header className="pagina__cabecera">
      <div><h1>Avisos</h1><p>Los avisos publicados aparecen en el Inicio de la web y de la app de estudiantes.</p></div>
      <div className="pagina__acciones"><button className="boton boton--claro" type="button" onClick={cargar} disabled={cargando}><Icono nombre="actualizar" tam={17}/>Actualizar</button></div>
    </header>
    <dl className="cifras" aria-label="Resumen de avisos">
      <div><dt>Publicados</dt><dd>{cargando ? '—' : publicados.length}</dd></div>
      <div><dt>Programados</dt><dd>{cargando ? '—' : programados.length}</dd></div>
      <div><dt>Total</dt><dd>{cargando ? '—' : avisos.length}</dd></div>
    </dl>
    {error && <p role="alert" className="aviso aviso--error"><Icono nombre="alerta" tam={18}/>{error}</p>}
    {exito && <p role="status" className="aviso aviso--ok"><Icono nombre="check" tam={18}/>{exito}</p>}
    <div className="avisos-grid">
      <form className="tarjeta avisos-formulario" onSubmit={enviar} noValidate>
        <h2>Nuevo aviso</h2>
        <label className="campo">Título <input value={titulo} onChange={(e) => { setTitulo(e.target.value); setErrores((a) => ({ ...a, titulo: '' })) }} maxLength={101} placeholder="Ej.: Cambio de horario en la biblioteca" aria-invalid={!!errores.titulo}/>{errores.titulo && <span className="campo__error" role="alert">{errores.titulo}</span>}</label>
        <label className="campo">Mensaje <textarea value={mensaje} onChange={(e) => { setMensaje(e.target.value); setErrores((a) => ({ ...a, mensaje: '' })) }} maxLength={2001} rows={5} placeholder="Escribe el comunicado" aria-invalid={!!errores.mensaje}/><span className="campo__contador">{mensaje.length}/2000</span>{errores.mensaje && <span className="campo__error" role="alert">{errores.mensaje}</span>}</label>
        {conImagen
          ? <PortadaActividad token={token} nombre={titulo} valor={portada} onCambiar={setPortada} />
          : <button type="button" className="boton boton--claro avisos-imagen" onClick={() => setConImagen(true)}><Icono nombre="imagen" tam={18}/>Agregar imagen (opcional)</button>}
        <fieldset className="publicacion-modo"><legend>Publicación</legend>
          <div className="segmentado">
            <label><input type="radio" name="modo-publicacion" checked={modo === 'ahora'} onChange={() => setModo('ahora')}/><span>Publicar ahora</span></label>
            <label><input type="radio" name="modo-publicacion" checked={modo === 'programar'} onChange={() => setModo('programar')}/><span>Programar</span></label>
          </div>
        </fieldset>
        {modo === 'programar' && <label className="campo">Fecha y hora
          <input type="datetime-local" value={fechaLocal} onChange={(e) => { setFechaLocal(e.target.value); setErrores((a) => ({ ...a, publicarEn: '' })) }} aria-invalid={!!errores.publicarEn}/>
          <span className="campo__ayuda">Se usa la hora de tu computadora.</span>
          {errores.publicarEn && <span className="campo__error" role="alert">{errores.publicarEn}</span>}
        </label>}
        {(titulo.trim() || mensaje.trim()) && <div className="avisos-vista-previa" aria-label="Vista previa">
          <span className="avisos-vista-previa__titulo">Así lo verán los estudiantes</span>
          <article>
            {portada?.url && <img src={urlPortada(portada)} alt="" />}
            <div><time>{modo === 'programar' && fechaLocal ? fechaLegible(fechaLocal) : 'Hoy'}</time><h3>{titulo || 'Título del aviso'}</h3><p>{mensaje || 'Mensaje del aviso'}</p></div>
          </article>
        </div>}
        <div className="avisos-pie-formulario"><button className="boton" type="submit" disabled={guardando}><Icono nombre={modo === 'programar' ? 'calendario' : 'megafono'} tam={18}/>{guardando ? 'Guardando…' : modo === 'programar' ? 'Programar aviso' : 'Publicar aviso'}</button></div>
      </form>
      <div className="tarjeta avisos-lista">
        <div className="avisos-lista__cabecera"><h2>Historial</h2>
          <div className="pestanas" role="group" aria-label="Filtrar avisos">
            {[['todos', 'Todos', avisos.length], ['publicados', 'Publicados', publicados.length], ['programados', 'Programados', programados.length]].map(([id, label, numero]) => <button key={id} className={filtro === id ? 'pestana pestana--activa' : 'pestana'} aria-pressed={filtro === id} type="button" onClick={() => setFiltro(id)}>{label} <span>{numero}</span></button>)}
          </div>
        </div>
        {cargando ? <p className="cargando">Cargando avisos…</p> : visibles.length === 0 ? <div className="vacio"><Icono nombre="megafono" tam={28}/><p>No hay avisos en esta sección.</p></div> : <div className="avisos-items">{visibles.map((aviso) => <article key={aviso.id} className="avisos-item">
          {aviso.portada?.url && <img src={urlPortada(aviso.portada)} alt="" loading="lazy" referrerPolicy="no-referrer" />}
          <div className="avisos-item__cuerpo">
            <div className="avisos-item__meta"><span className={aviso.estado === 'programado' ? 'estado estado--programado' : 'estado estado--publicado'}>{aviso.estado === 'programado' ? 'Programado' : 'Publicado'}</span><time>{aviso.estado === 'programado' ? fechaLegible(aviso.publicarEn) : fechaLegible(aviso.publicadoEn || aviso.creadoEn)}</time></div>
            <h3>{aviso.titulo}</h3><p>{aviso.mensaje}</p>
          </div>
        </article>)}</div>}
      </div>
    </div>
  </section>
}
