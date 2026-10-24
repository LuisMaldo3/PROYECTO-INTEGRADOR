const sinBarraFinal = (url) => url.replace(/\/$/, '')

/** Usuario-back (Spring Boot): login y logout. */
export const AUTH_URL = sinBarraFinal(import.meta.env?.VITE_AUTH_URL ?? 'http://localhost:8081')

/** Admin-back (Django): lista y detalle de usuarios. */
export const ADMIN_URL = sinBarraFinal(import.meta.env?.VITE_ADMIN_URL ?? 'http://localhost:8000')
