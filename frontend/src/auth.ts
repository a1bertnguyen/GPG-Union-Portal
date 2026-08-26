export type AdminProfile = {
  id?: number
  username: string
  fullName: string
  role: string
  unionUnitId?: number
  unionUnitCode?: string
  unionUnitName?: string
}

export type AuthSession = {
  accessToken: string
  tokenType: string
  expiresAt: string
  user: AdminProfile
}

export const AUTH_SESSION_STORAGE_KEY = 'gpg-union-admin-session'
const SESSION_KEY = AUTH_SESSION_STORAGE_KEY
export const AUTH_EXPIRED_EVENT = 'gpg-auth-expired'

// sessionStorage is isolated per browser tab. Move an existing session once so tabs in the same
// browser share one server-issued token instead of logging in separately and replacing each other.
const migrateLegacyTabSession = () => {
  const legacySession = sessionStorage.getItem(SESSION_KEY)
  if (!localStorage.getItem(SESSION_KEY) && legacySession) {
    localStorage.setItem(SESSION_KEY, legacySession)
  }
  sessionStorage.removeItem(SESSION_KEY)
}

migrateLegacyTabSession()

export function saveSession(session: AuthSession) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  sessionStorage.removeItem(SESSION_KEY)
}

export function getSession(): AuthSession | null {
  const raw = localStorage.getItem(SESSION_KEY)
  if (!raw) return null
  try {
    const session = JSON.parse(raw) as AuthSession
    if (!session.accessToken || new Date(session.expiresAt).getTime() <= Date.now()) {
      clearSession()
      return null
    }
    return session
  } catch {
    clearSession()
    return null
  }
}

export function getAccessToken() {
  return getSession()?.accessToken ?? null
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY)
  sessionStorage.removeItem(SESSION_KEY)
}

export function notifyAuthExpired(message?: string) {
  clearSession()
  window.dispatchEvent(new CustomEvent(AUTH_EXPIRED_EVENT, { detail: { message } }))
}
