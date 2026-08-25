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

const SESSION_KEY = 'gpg-union-admin-session'
export const AUTH_EXPIRED_EVENT = 'gpg-auth-expired'

export function saveSession(session: AuthSession) {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session))
}

export function getSession(): AuthSession | null {
  const raw = sessionStorage.getItem(SESSION_KEY)
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
  sessionStorage.removeItem(SESSION_KEY)
}

export function notifyAuthExpired() {
  clearSession()
  window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT))
}
