import { useEffect, useRef, useState } from 'react'
import { buscarPortadas, generarPortadaGemini, subirPortada, urlPortada } from '../services/portadasService'

/** Elige una portada una vez; nunca busca o genera automáticamente al cargar una tarjeta. */
export default function PortadaActividad({ token, nombre, valor, onCambiar, deshabilitado = false }) {
  const [fotos, setFotos] = useState([])
  const [buscando, setBuscando] = useState(false)
  const [generando, setGenerando] = useState(false)
  const [subiendo, setSubiendo] = useState(false)
  const [error, setError] = useState('')
  const [modo, setModo] = useState('buscar')
  const [urlManual, setUrlManual] = useState('')
  const ultimaBusqueda = useRef('')
  const ocupado = deshabilitado || buscando || generando || subiendo

  async function buscar() {
    if (ocupado || nombre.trim().length < 3) return
    setBuscando(true); setError(''); setModo('buscar')
    try {
      const resultados = await buscarPortadas(token, nombre)
      setFotos(resultados)
      if (!resultados.length) setError('No encontramos fotos. Prueba con un título más concreto o genera una imagen.')
    } catch (e) { setError(e.message) }
    finally { setBuscando(false) }
  }

  async function generar() {
    if (ocupado || nombre.trim().length < 3) return
    setGenerando(true); setError(''); setModo('gemini')
    try { onCambiar(await generarPortadaGemini(token, nombre)) }
    catch (e) { setError(e.message) }
    finally { setGenerando(false) }
  }

  // Una sola búsqueda automática por nombre estabilizado. No genera imágenes pagadas sin consentimiento.
  // Solo propone una fotografía con URL real y conserva autor/licencia.
  useEffect(() => {
    const consulta = (nombre || '').trim()
    if (consulta.length < 4 || valor?.url || ocupado || consulta === ultimaBusqueda.current) return undefined
    const controlador = new AbortController()
    const temporizador = setTimeout(async () => {
      ultimaBusqueda.current = consulta
      try {
        const resultados = await buscarPortadas(token, consulta)
        if (!controlador.signal.aborted && resultados.length) {
          setFotos(resultados)
          onCambiar(resultados[0])
        }
      } catch { /* Si la búsqueda falla, el administrador conserva las opciones manuales. */ }
    }, 1250)
    return () => { controlador.abort(); clearTimeout(temporizador) }
  }, [nombre, token, valor?.url, ocupado, onCambiar])
  async function subirImagen(evento) {
    const archivo = evento.target.files?.[0]
    evento.target.value = ''
    if (!archivo) return
    setError('')
    setSubiendo(true)
    try { onCambiar(await subirPortada(token, archivo)) }
    catch (e) { setError(e.message) }
    finally { setSubiendo(false) }
  }

  function agregarUrl() {
    try {
      const url = new URL(urlManual.trim())
      if (url.protocol !== 'https:' || !url.hostname || url.username || url.password) throw Error('URL inválida')
      onCambiar({ tipo: 'url', url: url.href })
      setUrlManual('')
      setError('')
    } catch { setError('Usa una URL pública HTTPS de una imagen para la que tengas permiso de uso.') }
  }

  return <section className="portada-actividad" aria-labelledby="portada-actividad-titulo">
    <div className="portada-actividad__heading">
      <div><h3 id="portada-actividad-titulo">Imagen</h3>
      <p>Sugerimos una foto según el título. También puedes generarla, subirla o pegar un enlace.</p></div>
    </div>
    <div className="portada-actividad__acciones">
      <button type="button" className={modo === 'buscar' ? 'portada-actividad__boton portada-actividad__boton--activo' : 'portada-actividad__boton'} onClick={buscar} disabled={ocupado || nombre.trim().length < 3}>{buscando ? 'Buscando…' : 'Buscar fotos'}</button>
      <button type="button" className={modo === 'gemini' ? 'portada-actividad__boton portada-actividad__boton--activo' : 'portada-actividad__boton'} onClick={generar} disabled={ocupado || nombre.trim().length < 3}>{generando ? 'Generando…' : 'Generar con IA'}</button>
    </div>
    <label className="vynk-portada-subir">Subir imagen (JPG o PNG, máx. 5 MB)
      <input type="file" accept="image/jpeg,image/png" disabled={ocupado} onChange={subirImagen} />
      {subiendo && <span role="status">Guardando imagen en VYNK…</span>}
    </label>
    <div className="vynk-portada-manual"><label>Enlace de imagen (https)
      <input type="url" placeholder="https://.../fotografia.jpg" value={urlManual} disabled={ocupado} onChange={e => setUrlManual(e.target.value)} /></label>
      <button type="button" className="portada-actividad__boton" disabled={ocupado || !urlManual.trim()} onClick={agregarUrl}>Usar URL</button>
    </div>
    {nombre.trim().length < 3 && <p className="portada-actividad__ayuda">Escribe primero el título para buscar o generar.</p>}
    {error && <p role="alert" className="portada-actividad__error">{error}</p>}
    {valor?.url && <div className="portada-actividad__elegida">
      <img src={urlPortada(valor)} alt={`Portada seleccionada para ${nombre}`} loading="lazy" />
      <div className="portada-actividad__elegida-info"><div><strong>Imagen elegida</strong><span>{valor.tipo === 'gemini' ? 'Generada con IA' : valor.tipo === 'subida' ? 'Subida desde tu equipo' : valor.tipo === 'url' ? 'Desde un enlace' : `Foto de ${valor.autor || 'Wikimedia Commons'}`}</span></div>
        <button type="button" onClick={() => onCambiar(null)} disabled={ocupado}>Quitar</button></div>
    </div>}
    {fotos.length > 0 && <div className="portada-actividad__resultados">
      <p>Elige una foto:</p>
      <div className="portada-actividad__galeria">{fotos.map((foto,i) => <button type="button" key={`${foto.url}-${i}`} className={valor?.url === foto.url ? 'portada-actividad__foto portada-actividad__foto--elegida' : 'portada-actividad__foto'} onClick={() => onCambiar(foto)} disabled={ocupado} aria-label={`Elegir fotografía ${i+1} de ${foto.autor}`}>
        <img src={foto.url} alt={`Opción de portada ${i+1}`} loading="lazy" /><span>{foto.autor} · {foto.licencia}</span></button>)}</div>
    </div>}
  </section>
}
