import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3637,
    strictPort: true,
    proxy: {
      '/api': 'http://localhost:3638',
      '/actuator': 'http://localhost:3638',
    },
  },
})
