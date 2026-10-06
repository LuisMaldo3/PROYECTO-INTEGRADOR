import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // El puerto está fijo porque el backend (CORS) y Google ("Orígenes autorizados")
  // solo aceptan http://localhost:5173. Si está ocupado, Vite avisa en vez de cambiar.
  server: { port: 5173, strictPort: true },
})
