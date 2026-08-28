import { api, apiAll, apiFacets, apiPage, buildQuery, type ListParams } from './api'
import { getSession } from './auth'
import type { ListFacets, PageResponse } from './types'

const DEFAULT_CACHE_TTL_MS = 30_000

type CacheEntry = {
  path: string
  expiresAt: number
  token: symbol
  value?: unknown
  promise?: Promise<unknown>
}

const entries = new Map<string, CacheEntry>()

const sessionIdentity = () => {
  const user = getSession()?.user
  if (!user) return 'anonymous'
  return `${user.username}|${user.role}|${user.unionUnitId ?? ''}`
}

const withoutSignal = (options?: RequestInit): RequestInit | undefined => {
  if (!options?.signal) return options
  const { signal: _signal, ...rest } = options
  return rest
}

const abortError = () => new DOMException('The request was aborted.', 'AbortError')

/**
 * Abort only this consumer. The shared network request is intentionally left running so a newly
 * mounted tab can reuse it and so React StrictMode does not start the same request twice.
 */
const waitForConsumer = <T>(promise: Promise<T>, signal?: AbortSignal | null): Promise<T> => {
  if (!signal) return promise
  if (signal.aborted) return Promise.reject(abortError())

  return new Promise<T>((resolve, reject) => {
    const onAbort = () => reject(abortError())
    signal.addEventListener('abort', onAbort, { once: true })
    promise.then(
      value => {
        signal.removeEventListener('abort', onAbort)
        resolve(value)
      },
      error => {
        signal.removeEventListener('abort', onAbort)
        reject(error)
      },
    )
  })
}

const cached = <T>(
  path: string,
  key: string,
  loader: () => Promise<T>,
  signal?: AbortSignal | null,
  ttlMs = DEFAULT_CACHE_TTL_MS,
): Promise<T> => {
  const cacheKey = `${sessionIdentity()}|${key}`
  const now = Date.now()
  const existing = entries.get(cacheKey)

  if (existing?.promise) return waitForConsumer(existing.promise as Promise<T>, signal)
  if (existing?.value !== undefined && existing.expiresAt > now) {
    return waitForConsumer(Promise.resolve(existing.value as T), signal)
  }
  if (existing) entries.delete(cacheKey)

  const token = Symbol(cacheKey)
  const promise = loader().then(value => {
    if (entries.get(cacheKey)?.token === token) {
      entries.set(cacheKey, { path, token, value, expiresAt: Date.now() + ttlMs })
    }
    return value
  }).catch(error => {
    if (entries.get(cacheKey)?.token === token) entries.delete(cacheKey)
    throw error
  })

  entries.set(cacheKey, { path, token, promise, expiresAt: now + ttlMs })
  return waitForConsumer(promise, signal)
}

/** Invalidate one endpoint after a write or a user-requested refresh. */
export function invalidateApiCache(pathPrefix?: string) {
  if (!pathPrefix) {
    entries.clear()
    return
  }
  entries.forEach((entry, key) => {
    if (entry.path.startsWith(pathPrefix)) entries.delete(key)
  })
}

export function apiCached<T>(path: string, options?: RequestInit): Promise<T> {
  const signal = options?.signal
  return cached(path, `one|${path}`, () => api<T>(path, withoutSignal(options)), signal)
}

export function apiPageCached<T>(path: string, params: ListParams = {}, options?: RequestInit): Promise<PageResponse<T>> {
  const query = buildQuery(params).toString()
  const signal = options?.signal
  return cached(path, `page|${path}?${query}`, () => apiPage<T>(path, params, withoutSignal(options)), signal)
}

export function apiAllCached<T>(path: string, params: ListParams = {}, options?: RequestInit): Promise<T[]> {
  const query = buildQuery(params).toString()
  const signal = options?.signal
  return cached(path, `all|${path}?${query}`, () => apiAll<T>(path, params, withoutSignal(options)), signal)
}

export function apiFacetsCached(path: string, params: ListParams = {}, options?: RequestInit): Promise<ListFacets> {
  const query = buildQuery(params).toString()
  const signal = options?.signal
  return cached(path, `facets|${path}?${query}`, () => apiFacets(path, params, withoutSignal(options)), signal)
}
