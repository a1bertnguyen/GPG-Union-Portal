import { lazy, Suspense, useEffect, useState } from 'react'
import { AUTH_EXPIRED_EVENT, clearSession, getSession, type AuthSession } from './auth'
import LoginPage from './pages/LoginPage'
import { loadPortalApp } from './portalLoader'

const PortalApp = lazy(loadPortalApp)

export default function App() {
  const [session, setSession] = useState<AuthSession | null>(() => getSession())

  useEffect(() => {
    const handleExpired = () => setSession(null)
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

  if (!session) return <LoginPage onLogin={setSession} />

  return (
    <Suspense fallback={<main className="loading-panel">Đang mở hệ thống…</main>}>
      <PortalApp
        session={session}
        onLogout={() => {
          clearSession()
          setSession(null)
        }}
      />
    </Suspense>
  )
}
