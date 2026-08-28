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
  /** Only set while a request is in flight; used to cancel the real network call once abandoned. */
  controller?: AbortController
  /** Number of callers still waiting on `promise`. */
  waiters?: number
}

const entries = new Map<string, CacheEntry>()

const sessionIdentity = () => {
  const user = getSession()?.user
  if (!user) return 'anonymous'
  return `${user.username}|${user.role}|${user.unionUnitId ?? ''}`
}

const withSignal = (options: RequestInit | undefined, signal: AbortSignal | undefined): RequestInit | undefined => {
  if (!signal) return options
  return { ...options, signal }
}

const abortError = () => new DOMException('The request was aborted.', 'AbortError')

/**
 * A caller walking away shouldn't kill a request other callers still need (a newly mounted tab
 * reusing it, or React StrictMode's double-invoke). So the real fetch is only cancelled once the
 * waiter count drops to zero, and that check is deferred a microtask so a synchronous
 * unmount+remount (StrictMode) can resubscribe before cancellation actually happens.
 */
const scheduleCancelIfAbandoned = (cacheKey: string, entry: CacheEntry) => {
  queueMicrotask(() => {
    if ((entry.waiters ?? 0) > 0) return
    if (entries.get(cacheKey) !== entry) return
    entry.controller?.abort()
    entries.delete(cacheKey)
  })
}

const attachWaiter = (cacheKey: string, entry: CacheEntry, signal?: AbortSignal | null) => {
  entry.waiters = (entry.waiters ?? 0) + 1
  if (!signal) return
  const onAbort = () => {
    entry.waiters = (entry.waiters ?? 1) - 1
    if ((entry.waiters ?? 0) <= 0) scheduleCancelIfAbandoned(cacheKey, entry)
  }
  if (signal.aborted) {
    onAbort()
    return
  }
  signal.addEventListener('abort', onAbort, { once: true })
}

/**
 * Reject only this consumer's wait. Whether the underlying network request itself is cancelled is
 * handled separately by the waiter refcount in `attachWaiter`/`scheduleCancelIfAbandoned`.
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
  loader: (signal: AbortSignal) => Promise<T>,
  signal?: AbortSignal | null,
  ttlMs = DEFAULT_CACHE_TTL_MS,
): Promise<T> => {
  const cacheKey = `${sessionIdentity()}|${key}`
  const now = Date.now()
  const existing = entries.get(cacheKey)

  if (existing?.promise) {
    attachWaiter(cacheKey, existing, signal)
    return waitForConsumer(existing.promise as Promise<T>, signal)
  }
  if (existing?.value !== undefined && existing.expiresAt > now) {
    return waitForConsumer(Promise.resolve(existing.value as T), signal)
  }
  if (existing) entries.delete(cacheKey)

  const token = Symbol(cacheKey)
  const controller = new AbortController()
  const promise = loader(controller.signal).then(value => {
    if (entries.get(cacheKey)?.token === token) {
      entries.set(cacheKey, { path, token, value, expiresAt: Date.now() + ttlMs })
    }
    return value
  }).catch(error => {
    if (entries.get(cacheKey)?.token === token) entries.delete(cacheKey)
    throw error
  })

  const entry: CacheEntry = { path, token, promise, expiresAt: now + ttlMs, controller, waiters: 0 }
  entries.set(cacheKey, entry)
  attachWaiter(cacheKey, entry, signal)
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
  return cached(path, `one|${path}`, netSignal => api<T>(path, withSignal(options, netSignal)), signal)
}

export function apiPageCached<T>(path: string, params: ListParams = {}, options?: RequestInit): Promise<PageResponse<T>> {
  const query = buildQuery(params).toString()
  const signal = options?.signal
  return cached(path, `page|${path}?${query}`, netSignal => apiPage<T>(path, params, withSignal(options, netSignal)), signal)
}

export function apiAllCached<T>(path: string, params: ListParams = {}, options?: RequestInit): Promise<T[]> {
  const query = buildQuery(params).toString()
  const signal = options?.signal
  return cached(path, `all|${path}?${query}`, netSignal => apiAll<T>(path, params, withSignal(options, netSignal)), signal)
}

export function apiFacetsCached(path: string, params: ListParams = {}, options?: RequestInit): Promise<ListFacets> {
  const query = buildQuery(params).toString()
  const signal = options?.signal
  return cached(path, `facets|${path}?${query}`, netSignal => apiFacets(path, params, withSignal(options, netSignal)), signal)
}
