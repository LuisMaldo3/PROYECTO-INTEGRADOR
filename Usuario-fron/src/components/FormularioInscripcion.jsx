import { useEffect, useState } from 'react'
import { hoyISO } from '../services/presentacion'
import { cancelarInscripcion, inscribirme, unirmeEquipo, verMiInscripcion } from '../services/inscripcionesService'

/** Formulario de detalle: una inscripción solo se confirma después de la respuesta del backend. */
export function FormularioInscripcion({ actividad }) {
  const [estado, setEstado] = useState({ cargando: true, inscrito: false, capitan: false, detalle: null, error: '', aviso: '' })
  const [modo, setModo] = useState('crear')
  const [nombreEquipo, setNombreEquipo] = useState('')
  const [codigoEquipo, setCodigoEquipo] = useState('')
  const [codigoGenerado, setCodigoGenerado] = useState('')
  const [procesando, setProcesando] = useState(false)
  const esEquipo = actividad.tipoActividad === 'competencia' && actividad.modalidad === 'equipos'
  const agotado = Number(actividad.cuposDisponibles) === 0 && actividad.cuposDisponibles != null
  const inscripcionCerrada = actividad.estado === 'cancelada' || (actividad.fecha && actividad.fecha < hoyISO())

  useEffect(() => {
    const ctrl = new AbortController()
    setCodigoGenerado('')
    setEstado({ cargando: true, inscrito: false, capitan: false, detalle: null, error: '', aviso: '' })
    verMiInscripcion(actividad.id, ctrl.signal)
      .then(r => { if (!ctrl.signal.aborted) setEstado({ cargando: false, inscrito: Boolean(r.inscrito), capitan: Boolean(r.capitan), detalle: r.detalle || null, error: '', aviso: '' }) })
      .catch(e => { if (!ctrl.signal.aborted) setEstado(s => ({ ...s, cargando: false, error: e.message })) })
    return () => ctrl.abort()
  }, [actividad.id])

  async function ejecutar(accion, tipo) {
    setProcesando(true); setEstado(s => ({ ...s, error: '', aviso: '' }))
    try {
      const resultado = await accion()
      const actual = await verMiInscripcion(actividad.id)
      setEstado(s => ({ ...s, inscrito: Boolean(actual.inscrito), capitan: Boolean(actual.capitan), detalle: actual.detalle || null,
        error: '', aviso: tipo === 'cancelar' ? 'Tu inscripción se canceló correctamente.' : 'Inscripción confirmada. Ya puedes verla en Mi agenda.' }))
      if (resultado.codigoEquipo) setCodigoGenerado(resultado.codigoEquipo)
    } catch (e) { setEstado(s => ({ ...s, error: e.message })) }
    finally { setProcesando(false) }
  }

  return <section className="vynk-detalle__registro" id="inscripcion" aria-labelledby="vynk-inscripcion-title">
    <div className="vynk-detalle__registro-head"><span className="vynk-up-eyebrow">PARTICIPA EN VYNK</span><h2 id="vynk-inscripcion-title">{estado.inscrito ? 'Ya tienes tu lugar' : 'Inscríbete en esta actividad'}</h2>
      <p>Inscripción personal con tu cuenta institucional. {esEquipo ? 'Primero debe existir un equipo registrado; cada integrante confirma su inscripción por separado.' : 'No necesitas ingresar nuevamente tus datos.'}</p></div>
    {estado.cargando ? <p role="status" className="vynk-up-message">Comprobando tu inscripción…</p>
      : estado.inscrito ? <div className="vynk-detalle__registrado"><strong>Inscripción confirmada</strong><p>{estado.detalle?.nombreEquipo ? `Equipo: ${estado.detalle.nombreEquipo}` : 'Tu participación está registrada.'}</p>
          {esEquipo && <p className="vynk-up-message">✓ Tu inscripción personal está confirmada en el equipo registrado. Cada integrante debe ingresar con su cuenta y confirmar usando el código privado del equipo.</p>}
          {esEquipo && <p className="vynk-up-message"><strong>{estado.detalle?.integrantesConfirmados ?? 1}</strong> integrante(s) con inscripción personal · {estado.detalle?.equipoHabilitado ? 'Equipo habilitado para competir' : 'Debe haber al menos 2 inscripciones confirmadas para competir'}</p>}
          {esEquipo && codigoGenerado && <div className="vynk-up-codigo"><span>Código privado para integrantes (guárdalo ahora)</span><code>{codigoGenerado}</code><button type="button" onClick={() => navigator.clipboard?.writeText(codigoGenerado)}>Copiar código</button></div>}
          {actividad.tipoActividad === 'competencia' && !estado.detalle?.areaVerificada && <p className="vynk-up-message">Tu área de Tecsup aún no está verificada. Esto no afecta tu inscripción.</p>}
          <div className="vynk-detalle__registro-actions"><a className="vynk-up-btn vynk-up-btn--primary" href="#/agenda">Ir a mi agenda</a>
            <button type="button" className="vynk-up-btn vynk-up-btn--outline" disabled={procesando} onClick={() => { if (window.confirm(esEquipo && estado.capitan ? 'Eres capitán: cancelar tu inscripción cancelará la inscripción de TODO el equipo. ¿Deseas continuar?' : '¿Seguro que deseas cancelar tu inscripción?')) ejecutar(() => cancelarInscripcion(actividad.id), 'cancelar') }}>Cancelar inscripción</button></div>
        </div>
      : <div className="vynk-detalle__registro-form">
        {esEquipo && <><div className="vynk-up-tabs" role="group" aria-label="Forma de inscripción de equipo"><button type="button" aria-pressed={modo === 'crear'} onClick={() => setModo('crear')}>Crear equipo</button><button type="button" aria-pressed={modo === 'unir'} onClick={() => setModo('unir')}>Unirme a un equipo</button></div>
          <label className="vynk-up-field">{modo === 'crear' ? 'Nombre de tu equipo' : 'Código privado del equipo'}
            <input type="text" maxLength={modo === 'crear' ? 70 : 100} value={modo === 'crear' ? nombreEquipo : codigoEquipo} onChange={e => modo === 'crear' ? setNombreEquipo(e.target.value) : setCodigoEquipo(e.target.value)} placeholder={modo === 'crear' ? 'Ej. Innovadores Digitales' : 'Código que te dio el capitán'} /></label></>}
        <button type="button" className="vynk-up-btn vynk-up-btn--primary vynk-up-btn--full"
          disabled={procesando || inscripcionCerrada || (agotado && (!esEquipo || modo === 'crear')) || (esEquipo && (modo === 'crear' ? nombreEquipo.trim().length < 3 : codigoEquipo.trim().length < 10))}
          onClick={() => ejecutar(() => esEquipo && modo === 'unir' ? unirmeEquipo(actividad.id, codigoEquipo.trim()) : inscribirme(actividad.id, nombreEquipo.trim()), 'inscribir')}>
          {procesando ? 'Procesando tu inscripción…' : inscripcionCerrada ? 'Inscripciones cerradas' : agotado && (!esEquipo || modo === 'crear') ? 'Cupos agotados' : esEquipo ? (modo === 'crear' ? 'Registrar equipo y confirmar mi inscripción' : 'Confirmar mi inscripción individual al equipo') : 'Confirmar inscripción'} <span aria-hidden="true">→</span>
        </button>
        <p className="vynk-up-footnote">Los cupos se confirman en el servidor. Si la actividad está llena, te avisaremos antes de registrar la inscripción.</p>
      </div>}
    {estado.error && <p role="alert" className="vynk-up-message vynk-up-message--error">{estado.error}</p>}
    {estado.aviso && <p role="status" className="vynk-up-message vynk-up-message--ok">{estado.aviso}</p>}
  </section>
}
