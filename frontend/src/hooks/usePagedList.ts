import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { apiFacets, apiPage, type ListParams } from '../api'
import type { ListFacets, PageResponse } from '../types'

/** Page sizes offered in the pagination footer. */
export const PAGE_SIZES = [20, 50, 100] as const
export const DEFAULT_PAGE_SIZE = PAGE_SIZES[0]

const SEARCH_DEBOUNCE_MS = 300
const emptyFacets: ListFacets = { total: 0, statusValues: [], metrics: {} }

type Options = {
  /** List endpoint, e.g. `/members`. Facets are read from `{endpoint}/facets`. */
  endpoint: string
  /**
   * Server-side filters. Everything except `q` is applied immediately; `q` is debounced so typing
   * does not fire a request per keystroke.
   */
  filters?: ListParams
  /** Set false to skip fetching facets on screens with no metric cards or status dropdown. */
  withFacets?: boolean
  /** Params that scope the list but are not user filters, e.g. a parent `memberId`. */
  scope?: ListParams
}

export type PagedList<T> = {
  rows: T[]
  /** Rows matching the current filters, across all pages. */
  total: number
  totalPages: number
  page: number
  size: number
  setPage: (page: number) => void
  setSize: (size: number) => void
  /** Whole-dataset numbers for the metric cards and status dropdown. */
  facets: ListFacets
  loading: boolean
  error: string
  reload: () => Promise<void>
}

/**
 * Drives one server-paginated list: paging state, debounced search, and the facets that keep the
 * metric cards accurate across every page.
 *
 * Two behaviours matter for correctness:
 * - changing any filter resets to the first page, otherwise narrowing the list while on page 7
 *   lands the user on an empty table;
 * - the in-flight request is aborted when the query changes, so a slow response for an old filter
 *   cannot overwrite a newer one.
 */
export function usePagedList<T>({ endpoint, filters = {}, withFacets = true, scope = {} }: Options): PagedList<T> {
  const [size, setSize] = useState<number>(DEFAULT_PAGE_SIZE)
  const [result, setResult] = useState<PageResponse<T>>({ content: [], page: 0, size, totalElements: 0, totalPages: 0 })
  const [facets, setFacets] = useState<ListFacets>(emptyFacets)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // Serialised so the effects compare by value — callers pass fresh object literals every render.
  const filterKey = JSON.stringify(filters)
  const scopeKey = JSON.stringify(scope)
  const search = String(filters.q ?? '')
  const [debouncedSearch, setDebouncedSearch] = useState(search)

  useEffect(() => {
    if (search === debouncedSearch) return
    const timer = window.setTimeout(() => setDebouncedSearch(search), SEARCH_DEBOUNCE_MS)
    return () => window.clearTimeout(timer)
  }, [search, debouncedSearch])

  const query = useMemo(() => ({
    ...JSON.parse(scopeKey) as ListParams,
    ...JSON.parse(filterKey) as ListParams,
    q: debouncedSearch || undefined,
  }), [filterKey, scopeKey, debouncedSearch])
  const queryKey = JSON.stringify(query)

  // Keep the selected page tied to the filters that produced it. A new query immediately derives
  // page zero without a render-time state update or an extra request for the old page.
  const [selection, setSelection] = useState({ queryKey, page: 0 })
  const requestedPage = selection.queryKey === queryKey ? selection.page : 0

  const controller = useRef<AbortController | null>(null)

  const load = useCallback(async () => {
    controller.current?.abort()
    const current = new AbortController()
    controller.current = current
    let redirectingToValidPage = false
    setLoading(true)
    setError('')
    try {
      const [pageResult, facetResult] = await Promise.all([
        apiPage<T>(endpoint, { ...query, page: requestedPage, size }, { signal: current.signal }),
        withFacets ? apiFacets(endpoint, query, { signal: current.signal }) : Promise.resolve(emptyFacets),
      ])
      if (current.signal.aborted) return

      // Deleting the last row on the last page can leave the selected page outside the new range.
      // Move to the new last page and let the effect fetch that slice instead of showing an empty
      // table with a page number greater than totalPages.
      const lastPage = Math.max(0, pageResult.totalPages - 1)
      if (requestedPage > lastPage) {
        redirectingToValidPage = true
        setFacets(facetResult)
        setSelection({ queryKey, page: lastPage })
        return
      }

      setResult(pageResult)
      setFacets(facetResult)
      setSelection(current => current.queryKey === queryKey && current.page === requestedPage
        ? current
        : { queryKey, page: requestedPage })
    } catch (err) {
      if (current.signal.aborted || (err instanceof DOMException && err.name === 'AbortError')) return
      setError(err instanceof Error ? err.message : 'Không thể tải dữ liệu')
    } finally {
      if (!current.signal.aborted && !redirectingToValidPage) setLoading(false)
    }
  }, [endpoint, query, queryKey, requestedPage, size, withFacets])

  // Fetching the selected slice is the intended synchronization performed by this effect.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { void load() }, [load])
  useEffect(() => () => controller.current?.abort(), [])

  const setPage = useCallback((next: number) => {
    const lastPage = Math.max(0, result.totalPages - 1)
    const bounded = Math.min(Math.max(0, Math.trunc(next)), lastPage)
    setSelection({ queryKey, page: bounded })
  }, [queryKey, result.totalPages])

  const changeSize = useCallback((next: number) => {
    setSize(Math.max(1, Math.trunc(next)))
    setSelection({ queryKey, page: 0 })
  }, [queryKey])

  return {
    rows: result.content,
    total: result.totalElements,
    totalPages: result.totalPages,
    page: requestedPage,
    size,
    setPage,
    setSize: changeSize,
    facets,
    loading,
    error,
    reload: load,
  }
}
