import assert from 'node:assert/strict'
import test from 'node:test'

const SESSION_KEY = 'gpg-union-admin-session'
let importId = 0

class MemoryStorage {
  constructor(entries = {}) {
    this.values = new Map(Object.entries(entries))
  }

  getItem(key) {
    return this.values.get(key) ?? null
  }

  setItem(key, value) {
    this.values.set(key, String(value))
  }

  removeItem(key) {
    this.values.delete(key)
  }
}

const installStorage = (localEntries = {}, sessionEntries = {}) => {
  const local = new MemoryStorage(localEntries)
  const session = new MemoryStorage(sessionEntries)
  Object.defineProperties(globalThis, {
    localStorage: { configurable: true, value: local },
    sessionStorage: { configurable: true, value: session },
  })
  return { local, session }
}

const loadAuth = () => import(`../src/auth.ts?test=${++importId}`)
const authSession = (token = 'shared-token') => ({
  accessToken: token,
  tokenType: 'Bearer',
  expiresAt: new Date(Date.now() + 60_000).toISOString(),
  user: { username: 'admin', fullName: 'Admin', role: 'ADMIN' },
})

test('migrates the legacy per-tab session into shared browser storage', async () => {
  const expected = authSession()
  const raw = JSON.stringify(expected)
  const storage = installStorage({}, { [SESSION_KEY]: raw })

  const auth = await loadAuth()

  assert.equal(storage.local.getItem(SESSION_KEY), raw)
  assert.equal(storage.session.getItem(SESSION_KEY), null)
  assert.deepEqual(auth.getSession(), expected)
})

test('keeps the shared session when another tab still has a stale legacy token', async () => {
  const shared = authSession('current-token')
  const storage = installStorage(
    { [SESSION_KEY]: JSON.stringify(shared) },
    { [SESSION_KEY]: JSON.stringify(authSession('stale-token')) },
  )

  const auth = await loadAuth()

  assert.equal(auth.getAccessToken(), 'current-token')
  assert.equal(storage.session.getItem(SESSION_KEY), null)
})

test('save and clear update the browser-wide session', async () => {
  const storage = installStorage()
  const auth = await loadAuth()
  const expected = authSession()

  auth.saveSession(expected)
  assert.deepEqual(JSON.parse(storage.local.getItem(SESSION_KEY)), expected)
  assert.equal(storage.session.getItem(SESSION_KEY), null)

  auth.clearSession()
  assert.equal(storage.local.getItem(SESSION_KEY), null)
  assert.equal(storage.session.getItem(SESSION_KEY), null)
})

test('expired shared sessions are removed instead of being reused by another tab', async () => {
  const expired = { ...authSession(), expiresAt: new Date(Date.now() - 1_000).toISOString() }
  const storage = installStorage({ [SESSION_KEY]: JSON.stringify(expired) })
  const auth = await loadAuth()

  assert.equal(auth.getSession(), null)
  assert.equal(storage.local.getItem(SESSION_KEY), null)
  assert.equal(storage.session.getItem(SESSION_KEY), null)
})
