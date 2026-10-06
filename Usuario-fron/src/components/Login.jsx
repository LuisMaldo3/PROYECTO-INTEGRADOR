import { useCallback, useRef, useState } from 'react'
import { DOMINIO_PERMITIDO, GOOGLE_CLIENT_ID } from '../config'
import { useGoogleButton } from '../hooks/useGoogleButton'
import { guardarSesion, loginConGoogle, loginConPassword } from '../services/authService'
import { Fondo } from './Fondo'

const iconoProps = { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, strokeLinecap: 'round', strokeLinejoin: 'round', 'aria-hidden': true }

export function Login({ onSesion }) {
  const [correo, setCorreo] = useState('')
  const [password, setPassword] = useState('')
  const [verPassword, setVerPassword] = useState(false)
  const [error, setError] = useState('')
  const [via, setVia] = useState(null) // null | 'google' | 'password'
  const googleRef = useRef(null)
  const verificando = via !== null

  const terminar = useCallback(
    (sesion) => {
      guardarSesion(sesion)
      onSesion(sesion)
    },
    [onSesion],
  )

  const manejarGoogle = useCallback(
    async (idToken) => {
      setError('')
      setVia('google')
      try {
        terminar(await loginConGoogle(idToken))
      } catch (e) {
        setError(e.message)
        setVia(null)
      }
    },
    [terminar],
  )

  const manejarPassword = async (e) => {
    e.preventDefault()
    setError('')
    const correoLimpio = correo.trim().toLowerCase()
    if (!correoLimpio.endsWith(DOMINIO_PERMITIDO)) {
      setError(`Usa tu correo institucional ${DOMINIO_PERMITIDO}`)
      return
    }
    setVia('password')
    try {
      terminar(await loginConPassword(correoLimpio, password))
    } catch (err) {
      setError(err.message)
      setVia(null)
    }
  }

  const estado = useGoogleButton({
    clientId: GOOGLE_CLIENT_ID,
    contenedorRef: googleRef,
    onCredencial: manejarGoogle,
  })

  return (
    <Fondo>
      <h2 className="card-title">Inicia sesión</h2>
      <p className="card-subtitle">Usa tu correo de Tecsup para entrar.</p>

      {error && (
        <div className="card-error-msg" role="alert">
          {error}
        </div>
      )}

      <form onSubmit={manejarPassword}>
        <div className="form-group">
          <label htmlFor="correo">Correo institucional</label>
          <div className="input-box">
            <svg className="input-icon" {...iconoProps}>
              <rect x="2" y="4" width="20" height="16" rx="2" />
              <path d="m22 7-10 6L2 7" />
            </svg>
            <input
              id="correo"
              type="email"
              inputMode="email"
              autoComplete="username"
              placeholder={`tu.nombre${DOMINIO_PERMITIDO}`}
              value={correo}
              onChange={(e) => setCorreo(e.target.value)}
              disabled={verificando}
              required
            />
          </div>
        </div>

        <div className="form-group">
          <label htmlFor="password">Contraseña</label>
          <div className="input-box">
            <svg className="input-icon" {...iconoProps}>
              <rect x="3" y="11" width="18" height="11" rx="2" />
              <path d="M7 11V7a5 5 0 0 1 10 0v4" />
            </svg>
            <input
              id="password"
              type={verPassword ? 'text' : 'password'}
              autoComplete="current-password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={verificando}
              required
            />
            <button
              type="button"
              className="toggle-pass"
              onClick={() => setVerPassword((v) => !v)}
              aria-label={verPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
              aria-pressed={verPassword}
            >
              {verPassword ? (
                <svg {...iconoProps}>
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                  <path d="m1 1 22 22" />
                </svg>
              ) : (
                <svg {...iconoProps}>
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
              )}
            </button>
          </div>
        </div>

        <button type="submit" className="btn-blue-submit" disabled={verificando}>
          {via === 'password' ? (
            <>
              <span className="spinner spinner-claro" aria-hidden="true" /> Verificando…
            </>
          ) : (
            <>Iniciar sesión</>
          )}
        </button>
      </form>

      <div className="line-divider">
        <span>o</span>
      </div>

      {estado === 'sin-config' && (
        <div className="card-error-msg" role="alert">
          El inicio de sesión con Google aún no está configurado.
          {import.meta.env.DEV && <> Define <code>VITE_GOOGLE_CLIENT_ID</code> en <code>.env.local</code> y reinicia Vite.</>}
        </div>
      )}

      {estado === 'error' && (
        <div className="card-error-msg" role="alert">
          No se pudo cargar Google. Revisa tu conexión o desactiva el bloqueador de anuncios.{' '}
          <button type="button" className="link-btn" onClick={() => window.location.reload()}>
            Reintentar
          </button>
        </div>
      )}

      <div className={`google-zone${via === 'google' ? ' is-busy' : ''}`} aria-busy={via === 'google'}>
        <div ref={googleRef} className="google-slot" />
        {estado === 'cargando' && <div className="google-skeleton" aria-hidden="true" />}
        {via === 'google' && (
          <div className="google-overlay" role="status">
            <span className="spinner" aria-hidden="true" />
            Verificando tu cuenta…
          </div>
        )}
      </div>

      <p className="card-hint">
        <svg width="14" height="14" {...iconoProps}>
          <rect x="3" y="11" width="18" height="11" rx="2" />
          <path d="M7 11V7a5 5 0 0 1 10 0v4" />
        </svg>
        Solo cuentas <strong>{DOMINIO_PERMITIDO}</strong>
      </p>
      <p className="card-nota">¿Aún no tienes contraseña? Entra con Google.</p>
    </Fondo>
  )
}
