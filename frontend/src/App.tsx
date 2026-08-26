import { lazy, Suspense, useEffect, useState } from 'react'
import { api } from './api'
import { AUTH_EXPIRED_EVENT, clearSession, getSession, type AuthSession } from './auth'
import LoginPage from './pages/LoginPage'
import { loadPortalApp } from './portalLoader'

const PortalApp = lazy(loadPortalApp)

export default function App() {
  const [session, setSession] = useState<AuthSession | null>(() => getSession())
  const [authNotice, setAuthNotice] = useState('')

  useEffect(() => {
    const handleExpired = (event: Event) => {
      const detail = event instanceof CustomEvent ? event.detail as { message?: string } : undefined
      if (detail?.message) setAuthNotice(detail.message)
      setSession(null)
    }
    window.addEventListener(AUTH_EXPIRED_EVENT, handleExpired)
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handleExpired)
  }, [])

  useEffect(() => {
    if (!session) return
    const remaining = new Date(session.expiresAt).getTime() - Date.now()
    const timer = window.setTimeout(() => {
      clearSession()
      setSession(null)
    }, Math.max(0, remaining))
    return () => window.clearTimeout(timer)
  }, [session])

  useEffect(() => {
    if (!session) return
    const timer = window.setInterval(() => {
      void api('/auth/me').catch(() => undefined)
    }, 10_000)
    return () => window.clearInterval(timer)
  }, [session])

  if (!session) return <LoginPage notice={authNotice} onLogin={nextSession => {
    setAuthNotice('')
    setSession(nextSession)
  }} />

  return (
    <Suspense fallback={<main className="loading-panel">Đang mở hệ thống…</main>}>
      <PortalApp
        session={session}
        onLogout={() => {
          clearSession()
          setAuthNotice('')
          setSession(null)
        }}
      />
    </Suspense>
  )
}
