import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Puerto fijo 5174 (el 5173 es de Usuario-fron). Los backends (CORS) solo aceptan ese origen.
  server: { port: 5174, strictPort: true },
})
