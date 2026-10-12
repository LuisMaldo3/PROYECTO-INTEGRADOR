import { useCallback, useEffect, useState } from 'react'
import { listarAvisos, publicarAviso, validarAviso } from '../services/avisos'

const fechaLegible = (valor) => {
  if (!valor) return ''
  const fecha = new Date(valor)
  return Number.isNaN(fecha.getTime()) ? '' : new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' }).format(fecha)
}

export default function PantallaAvisos({ token, onSesionExpirada }) {
  const [avisos, setAvisos] = useState([])
  const [titulo, setTitulo] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [errores, setErrores] = useState({})
  const [error, setError] = useState('')
  const [exito, setExito] = useState('')
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const cargar = useCallback(async () => {
    try { setAvisos(await listarAvisos(token)); setError('') }
    catch (e) { if (e.estado === 401) onSesionExpirada?.(e.detalle); else setError(e.message) }
    finally { setCargando(false) }
  }, [token, onSesionExpirada])
  useEffect(() => { cargar() }, [cargar])
  async function enviar(evento) {
    evento.preventDefault()
    if (guardando) return
    const resultado = validarAviso({ titulo, mensaje })
    setErrores(resultado.errores)
    setExito(''); setError('')
    if (Object.keys(resultado.errores).length) return
    setGuardando(true)
    try {
      const nuevo = await publicarAviso(token, resultado)
      setAvisos((actual) => [nuevo, ...actual.filter((a) => a.id !== nuevo.id)])
      setTitulo(''); setMensaje(''); setErrores({})
      setExito('El aviso se publicó correctamente.')
    } catch (e) {
      if (e.estado === 401) onSesionExpirada?.(e.detalle)
      else { setErrores(e.errores || {}); setError(e.message) }
    } finally { setGuardando(false) }
  }
  return <section className="avisos-pagina">
    <header className="avisos-encabezado">
      <div><p className="avisos-eyebrow">COMUNICACIONES</p><h1>Avisos</h1><p className="suave">Publica información importante y consulta los comunicados anteriores.</p></div>
      <span className="avisos-total">{avisos.length} publicados</span>
    </header>
    <div className="avisos-grid">
      <form className="avisos-tarjeta avisos-formulario" onSubmit={enviar} noValidate>
        <div className="avisos-tarjeta-cabecera"><h2>Nuevo aviso</h2><p>El mensaje se publicará para su consulta.</p></div>
        {error && <p role="alert" className="aviso aviso--error">{error}</p>}
        {exito && <p role="status" className="aviso aviso--ok">{exito}</p>}
        <label className="campo">Título <input value={titulo} onChange={(e) => {setTitulo(e.target.value);setErrores((a)=>({...a,titulo:''}))}} maxLength={101} placeholder="Ej. Cambio de horario" aria-invalid={!!errores.titulo} />{errores.titulo && <span className="campo__error">{errores.titulo}</span>}</label>
        <label className="campo">Mensaje <textarea value={mensaje} onChange={(e) => {setMensaje(e.target.value);setErrores((a)=>({...a,mensaje:''}))}} maxLength={2001} rows={7} placeholder="Escribe aquí el comunicado..." aria-invalid={!!errores.mensaje}/>{errores.mensaje && <span className="campo__error">{errores.mensaje}</span>}</label>
        <div className="avisos-pie-formulario"><span>{mensaje.length}/2000 caracteres</span><button className="boton" type="submit" disabled={guardando}>{guardando ? 'Publicando…' : 'Publicar aviso'}</button></div>
      </form>
      <div className="avisos-tarjeta avisos-lista"><div className="avisos-tarjeta-cabecera"><h2>Avisos publicados</h2><p>Historial de comunicaciones del panel.</p></div>
        {cargando ? <p className="avisos-vacio">Cargando avisos…</p> : avisos.length === 0 ? <div className="avisos-vacio"><strong>Aún no hay avisos publicados</strong><p>Los nuevos comunicados aparecerán aquí.</p></div> : <div className="avisos-items">{avisos.map((aviso) => <article key={aviso.id} className="avisos-item"><div className="avisos-item-cabecera"><h3>{aviso.titulo}</h3><time>{fechaLegible(aviso.creadoEn)}</time></div><p>{aviso.mensaje}</p></article>)}</div>}
      </div>
    </div>
  </section>
}
