import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './estilos/base.css'
import './estilos/panel.css'
import './estilos/login.css'
import './estilos/pantallas.css'
import './estilos/formularios.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
