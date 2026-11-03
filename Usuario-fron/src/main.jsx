import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './estilos/base.css'
import './estilos/shell.css'
import './estilos/acceso.css'
import './estilos/inicio.css'
import './estilos/actividades.css'
import './estilos/agenda.css'
import './estilos/asistente.css'
import './estilos/perfil.css'
import './estilos/resultados.css'
import './estilos/organizador.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
