import { useCallback, useEffect, useMemo, useState } from 'react'
import Icono from './Iconos'
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
  const [modo, setModo] = useState('ahora')
  const [fechaLocal, setFechaLocal] = useState('')
  const [filtro, setFiltro] = useState('todos')
  const [errores, setErrores] = useState({})
  const [error, setError] = useState('')
  const [exito, setExito] = useState('')
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)

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
    const resultado = validarAviso({ titulo, mensaje, programar: modo === 'programar', fechaLocal })
    setErrores(resultado.errores)
    setExito(''); setError('')
    if (Object.keys(resultado.errores).length) return
    setGuardando(true)
    try {
      const nuevo = await publicarAviso(token, resultado)
      setAvisos((actual) => [nuevo, ...actual.filter((a) => a.id !== nuevo.id)])
      setTitulo(''); setMensaje(''); setFechaLocal(''); setModo('ahora'); setErrores({})
      setExito(nuevo.estado === 'programado' ? `Aviso programado para el ${fechaLegible(nuevo.publicarEn)}.` : 'Aviso publicado correctamente.')
    } catch (e) {
      if (e.estado === 401) onSesionExpirada?.(e.detalle)
      else { setErrores(e.errores || {}); setError(e.message) }
    } finally { setGuardando(false) }
  }

  return <section className="avisos-pagina">
    <header className="avisos-encabezado">
      <div><p className="eyebrow">CENTRO DE COMUNICACIONES</p><h1>Avisos y publicaciones</h1><p className="suave">Mantén informada a la comunidad con comunicados claros y oportunos.</p></div>
      <button className="boton boton--claro" type="button" onClick={cargar} disabled={cargando}><Icono nombre="actualizar" tam={17}/>{cargando ? 'Cargando…' : 'Actualizar'}</button>
    </header>
    <div className="metricas-actividad avisos-metricas" aria-label="Resumen de avisos">
      <div className="metrica"><span>Todos los avisos</span><strong>{avisos.length}</strong><small>Comunicaciones creadas</small></div>
      <div className="metrica"><span>Publicados</span><strong>{publicados.length}</strong><small>Ya visibles para la comunidad</small></div>
      <div className="metrica"><span>Programados</span><strong>{programados.length}</strong><small>Pendientes de publicación</small></div>
    </div>
    {error && <p role="alert" className="aviso aviso--error"><Icono nombre="alerta" tam={18}/>{error}</p>}
    {exito && <p role="status" className="aviso aviso--ok"><Icono nombre="check" tam={18}/>{exito}</p>}
    <div className="avisos-grid">
      <form className="avisos-tarjeta avisos-formulario" onSubmit={enviar} noValidate>
        <div className="avisos-tarjeta-cabecera"><span className="seccion-etiqueta"><Icono nombre="mas" tam={17}/> REDACTAR COMUNICADO</span><h2>Nuevo aviso</h2><p>Prepara un mensaje y decide cuándo publicarlo.</p></div>
        <label className="campo">Título <input value={titulo} onChange={(e) => { setTitulo(e.target.value); setErrores((a) => ({ ...a, titulo: '' })) }} maxLength={101} placeholder="Ej.: Información importante para estudiantes" aria-invalid={!!errores.titulo}/>{errores.titulo && <span className="campo__error" role="alert">{errores.titulo}</span>}</label>
        <label className="campo">Mensaje <textarea value={mensaje} onChange={(e) => { setMensaje(e.target.value); setErrores((a) => ({ ...a, mensaje: '' })) }} maxLength={2001} rows={7} placeholder="Escribe el contenido del comunicado..." aria-invalid={!!errores.mensaje}/><span className="campo__contador">{mensaje.length}/2000 caracteres</span>{errores.mensaje && <span className="campo__error" role="alert">{errores.mensaje}</span>}</label>
        <fieldset className="publicacion-modo"><legend>¿Cuándo publicar?</legend>
          <label className={`opcion-publicacion${modo === 'ahora' ? ' opcion-publicacion--activa' : ''}`}><input type="radio" name="modo-publicacion" checked={modo === 'ahora'} onChange={() => setModo('ahora')}/><Icono nombre="check" tam={18}/><span><strong>Publicar ahora</strong><small>Visible inmediatamente al crear el aviso</small></span></label>
          <label className={`opcion-publicacion${modo === 'programar' ? ' opcion-publicacion--activa' : ''}`}><input type="radio" name="modo-publicacion" checked={modo === 'programar'} onChange={() => setModo('programar')}/><Icono nombre="calendario" tam={18}/><span><strong>Programar publicación</strong><small>Elige una fecha y hora futura</small></span></label>
        </fieldset>
        {modo === 'programar' && <label className="campo">Fecha y hora de publicación
          <input type="datetime-local" value={fechaLocal} onChange={(e) => { setFechaLocal(e.target.value); setErrores((a) => ({ ...a, publicarEn: '' })) }} aria-invalid={!!errores.publicarEn}/>
          <span className="campo__ayuda">Usaremos la zona horaria local de tu dispositivo.</span>
          {errores.publicarEn && <span className="campo__error" role="alert">{errores.publicarEn}</span>}
        </label>}
        <div className="avisos-pie-formulario"><span>Revisa la información antes de continuar.</span><button className="boton" type="submit" disabled={guardando}><Icono nombre={modo === 'programar' ? 'calendario' : 'check'} tam={18}/>{guardando ? 'Guardando…' : modo === 'programar' ? 'Programar aviso' : 'Publicar aviso'}</button></div>
      </form>
      <div className="avisos-tarjeta avisos-lista"><div className="avisos-tarjeta-cabecera"><span className="seccion-etiqueta"><Icono nombre="correo" tam={17}/> HISTORIAL Y SEGUIMIENTO</span><h2>Comunicados</h2><p>Consulta los publicados y las próximas publicaciones.</p></div>
        <div className="pestanas pestanas--avisos" role="group" aria-label="Filtrar avisos">
          {[['todos', 'Todos', avisos.length], ['publicados', 'Publicados', publicados.length], ['programados', 'Programados', programados.length]].map(([id, label, numero]) => <button key={id} className={filtro === id ? 'pestana pestana--activa' : 'pestana'} type="button" onClick={() => setFiltro(id)}>{label} <span>{numero}</span></button>)}
        </div>
        {cargando ? <p className="avisos-vacio">Cargando comunicados…</p> : visibles.length === 0 ? <div className="avisos-vacio"><Icono nombre="correo" tam={30}/><strong>Sin avisos en esta sección</strong><p>Los comunicados aparecerán aquí cuando se creen.</p></div> : <div className="avisos-items">{visibles.map((aviso) => <article key={aviso.id} className="avisos-item"><div className="avisos-item-cabecera"><div><span className={aviso.estado === 'programado' ? 'estado-programado' : 'estado-publicado'}>{aviso.estado === 'programado' ? 'Programado' : 'Publicado'}</span><h3>{aviso.titulo}</h3></div><time>{aviso.estado === 'programado' ? fechaLegible(aviso.publicarEn) : fechaLegible(aviso.publicadoEn || aviso.creadoEn)}</time></div><p>{aviso.mensaje}</p></article>)}</div>}
      </div>
    </div>
  </section>
}
