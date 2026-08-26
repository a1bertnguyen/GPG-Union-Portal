import { lazy, Suspense, useEffect, useState } from 'react'
import { api } from './api'
import { AUTH_EXPIRED_EVENT, AUTH_SESSION_STORAGE_KEY, clearSession, getSession, type AuthSession } from './auth'
import LoginPage from './pages/LoginPage'
import { loadPortalApp } from './portalLoader'

const PortalApp = lazy(loadPortalApp)
const SESSION_CHECK_INTERVAL_MS = 30_000

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
    const syncSharedSession = (event: StorageEvent) => {
      if (event.key !== AUTH_SESSION_STORAGE_KEY) return
      setAuthNotice('')
      setSession(getSession())
    }
    window.addEventListener('storage', syncSharedSession)
    return () => window.removeEventListener('storage', syncSharedSession)
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
    let checkInFlight = false
    const checkSession = async () => {
      if (document.visibilityState !== 'visible' || checkInFlight) return
      checkInFlight = true
      try {
        await api('/auth/me')
      } catch {
        // api() dispatches the auth-expired event for an invalid or replaced session.
      } finally {
        checkInFlight = false
      }
    }
    const handleVisibilityChange = () => { void checkSession() }
    const timer = window.setInterval(() => { void checkSession() }, SESSION_CHECK_INTERVAL_MS)
    document.addEventListener('visibilitychange', handleVisibilityChange)
    return () => {
      window.clearInterval(timer)
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
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
