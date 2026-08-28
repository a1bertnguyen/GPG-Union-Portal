import { getAccessToken, notifyAuthExpired } from './auth'
import type { ListFacets, PageResponse } from './types'

const API_BASE = import.meta.env.VITE_API_URL ?? '/api'

export class ApiError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'ApiError'
  }
}

export const apiUrl = (path: string) => `${API_BASE}${path}`

type ApiErrorPayload = { code?: string; message?: string }

const readApiError = async (response: Response): Promise<ApiErrorPayload> => {
  try {
    return await response.json() as ApiErrorPayload
  } catch {
    return {}
  }
}

const handleUnauthorized = (response: Response, path: string, payload: ApiErrorPayload) => {
  if (response.status !== 401 || path === '/auth/login') return
  notifyAuthExpired(payload.code === 'SESSION_REPLACED' ? payload.message : undefined)
}

export async function api<T>(path: string, options?: RequestInit): Promise<T> {
  const headers = new Headers(options?.headers)
  const accessToken = getAccessToken()
  if (accessToken && !headers.has('Authorization')) headers.set('Authorization', `Bearer ${accessToken}`)
  if (!(options?.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(apiUrl(path), {
    ...options,
    headers,
  })

  if (!response.ok) {
    const payload = await readApiError(response)
    handleUnauthorized(response, path, payload)
    const message = payload.message ?? `Yêu cầu thất bại (${response.status})`
    throw new ApiError(message)
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

/** Query params a paginated list endpoint understands. Empty and nullish values are dropped. */
export type ListParams = Record<string, string | number | boolean | null | undefined>

export function buildQuery(params: ListParams) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value === null || value === undefined || value === '' || value === false) return
    query.set(key, String(value))
  })
  return query
}

const withQuery = (path: string, params: ListParams) => {
  const query = buildQuery(params)
  return query.size ? `${path}?${query}` : path
}

/** One page from a list endpoint. */
export const apiPage = <T>(path: string, params: ListParams = {}, options?: RequestInit) =>
  api<PageResponse<T>>(withQuery(path, params), options)

/**
 * Every row from a list endpoint, for the dropdowns and dashboards that genuinely need the whole
 * set. Sends `all=true` so the server skips paging, and unwraps the envelope.
 */
export const apiAll = async <T>(path: string, params: ListParams = {}, options?: RequestInit) =>
  (await apiPage<T>(path, { ...params, all: true }, options)).content

/** Whole-dataset numbers for the same filters — metric cards and the status dropdown. */
export const apiFacets = (path: string, params: ListParams = {}, options?: RequestInit) =>
  api<ListFacets>(withQuery(`${path}/facets`, params), options)

export async function downloadFile(path: string, filename: string) {
  const headers = new Headers()
  const accessToken = getAccessToken()
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
  const response = await fetch(apiUrl(path), { headers })
  if (!response.ok) {
    const payload = await readApiError(response)
    handleUnauthorized(response, path, payload)
    throw new ApiError(payload.message ?? `Không thể tải tệp (${response.status})`)
  }
  const url = URL.createObjectURL(await response.blob())
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

export async function loadFileUrl(path: string) {
  const headers = new Headers()
  const accessToken = getAccessToken()
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
  const response = await fetch(apiUrl(path), { headers })
  if (!response.ok) {
    const payload = await readApiError(response)
    handleUnauthorized(response, path, payload)
    throw new ApiError(payload.message ?? `Không thể tải nội dung tệp (${response.status})`)
  }
  return URL.createObjectURL(await response.blob())
}

export const formatMoney = (value: number | string | undefined) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 })
    .format(Number(value ?? 0))

export const formatDate = (value: unknown) => {
  if (!value || typeof value !== 'string') return '—'
  return new Intl.DateTimeFormat('vi-VN').format(new Date(`${value}T00:00:00`))
}

/**
 * Built-in Vietnamese labels for domain enum constants.
 *
 * The server holds the same map in `EnumLabels` and uses it to make search match on labels
 * ("Chờ duyệt" finds `PENDING_APPROVAL`). This copy is the offline fallback so the UI still reads
 * correctly before {@link loadEnumLabels} resolves, or if that call fails.
 */
const fallbackLabels: Record<string, string> = {
  ACTIVE: 'Đang hoạt động', INACTIVE: 'Ngừng hoạt động', MEMBER: 'Đoàn viên',
  NOT_JOINED: 'Chưa gia nhập', LEFT: 'Đã rời', MALE: 'Nam', FEMALE: 'Nữ',
  BIRTHDAY: 'Sinh nhật', FUNERAL: 'Hiếu',
  WEDDING: 'Hỷ', VISIT: 'Thăm hỏi', CHILDBIRTH: 'Sinh con', HARDSHIP: 'Khó khăn',
  NEW: 'Mới', PENDING_APPROVAL: 'Chờ duyệt', IN_PROGRESS: 'Đang xử lý', COMPLETED: 'Hoàn tất',
  CANCELLED: 'Đã hủy', COMPLETE: 'Đủ', INCOMPLETE: 'Chưa đủ', NOT_REQUIRED: 'Không yêu cầu',
  LOW: 'Thấp', MEDIUM: 'Trung bình', HIGH: 'Cao', CRITICAL: 'Khẩn cấp',
  VERIFYING: 'Đang xác minh', WAITING_RESPONSE: 'Chờ phản hồi', CLOSED: 'Đã đóng',
  CLASSIFYING: 'Đang phân loại', ASSIGNED: 'Đã giao PIC',
  PLANNED: 'Kế hoạch', APPROVED: 'Đã duyệt', INCOME: 'Thu', EXPENSE: 'Chi',
  DRAFT: 'Bản nháp', SUBMITTED: 'Đã nộp',
  HR_IMPORT: 'Nhập dữ liệu HR', FINANCE_IMPORT: 'Nhập dữ liệu tài chính',
  UNITS_IMPORT: 'Nhập CĐCS từ Excel', MEMBERS_IMPORT: 'Nhập đoàn viên từ Excel',
  WELFARE_IMPORT: 'Nhập chăm lo từ Excel', CASES_IMPORT: 'Nhập vụ việc từ Excel',
  ACTIVITIES_IMPORT: 'Nhập hoạt động từ Excel', FINANCE_EXCEL_IMPORT: 'Nhập tài chính từ Excel',
  SURVEYS_IMPORT: 'Nhập khảo sát từ Excel', SURVEY_RESPONSES_IMPORT: 'Nhập phản hồi khảo sát từ Excel',
  REPORTS_IMPORT: 'Nhập báo cáo từ Excel', USERS_IMPORT: 'Nhập tài khoản từ Excel',
  PARTIAL: 'Hoàn tất một phần', FAILED: 'Thất bại',
  JOIN_APPLICATION: 'Đơn gia nhập', DECISION: 'Quyết định', BCH_DOCUMENT: 'Tài liệu BCH',
  PHOTO: 'Ảnh', DOCUMENT: 'Tài liệu',
}

let labels: Record<string, string> = fallbackLabels

/**
 * Pulls the labels the server searches on, so a constant added on the backend shows its Vietnamese
 * name here without a frontend release. Failures are ignored — the built-in map already covers
 * everything shipped today.
 */
export async function loadEnumLabels() {
  try {
    labels = { ...fallbackLabels, ...await api<Record<string, string>>('/meta/enum-labels') }
  } catch {
    // Offline or unauthorised: keep the built-in labels.
  }
}

export const enumLabel = (value: unknown) => labels[String(value)] ?? String(value ?? '—')
export const currentMonth = () => new Date().toISOString().slice(0, 7)
