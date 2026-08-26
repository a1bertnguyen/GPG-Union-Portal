import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { getCanonicalUrl } from './canonicalUrl.ts'

const canonicalUrl = getCanonicalUrl(import.meta.env.VITE_CANONICAL_ORIGIN, window.location.href)

if (canonicalUrl) {
  window.location.replace(canonicalUrl)
} else {
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <App />
    </StrictMode>,
  )
}
