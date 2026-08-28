import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    // Browser code can always be inspected. Do not publish original TypeScript through source maps.
    sourcemap: false,
  },
  server: {
    port: 3637,
    strictPort: true,
    proxy: {
      '/api': 'http://localhost:3638',
      '/actuator': 'http://localhost:3638',
    },
  },
})
