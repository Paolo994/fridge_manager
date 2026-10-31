import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // In sviluppo le chiamate /api vanno al backend Spring
    proxy: { '/api': 'http://localhost:8080' },
  },
})
