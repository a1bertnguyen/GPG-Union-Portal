import { getAccessToken, notifyAuthExpired } from './auth'

const API_BASE = import.meta.env.VITE_API_URL ?? '/api'

export class ApiError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'ApiError'
  }
}

export const apiUrl = (path: string) => `${API_BASE}${path}`

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

  if (response.status === 401 && path !== '/auth/login') notifyAuthExpired()

  if (!response.ok) {
    let message = `Yêu cầu thất bại (${response.status})`
    try {
      const body = await response.json() as { message?: string }
      if (body.message) message = body.message
    } catch {
      // Server did not return JSON.
    }
    throw new ApiError(message)
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export async function downloadFile(path: string, filename: string) {
  const headers = new Headers()
  const accessToken = getAccessToken()
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
  const response = await fetch(apiUrl(path), { headers })
  if (response.status === 401) notifyAuthExpired()
  if (!response.ok) throw new ApiError(`Không thể tải tệp (${response.status})`)
  const url = URL.createObjectURL(await response.blob())
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

export const formatMoney = (value: number | string | undefined) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 })
    .format(Number(value ?? 0))

export const formatDate = (value: unknown) => {
  if (!value || typeof value !== 'string') return '—'
  return new Intl.DateTimeFormat('vi-VN').format(new Date(`${value}T00:00:00`))
}

const labels: Record<string, string> = {
  ACTIVE: 'Đang hoạt động', INACTIVE: 'Ngừng hoạt động', MEMBER: 'Đoàn viên',
  NOT_JOINED: 'Chưa gia nhập', LEFT: 'Đã rời', BIRTHDAY: 'Sinh nhật', FUNERAL: 'Hiếu',
  WEDDING: 'Hỷ', VISIT: 'Thăm hỏi', CHILDBIRTH: 'Sinh con', HARDSHIP: 'Khó khăn',
  NEW: 'Mới', PENDING_APPROVAL: 'Chờ duyệt', IN_PROGRESS: 'Đang xử lý', COMPLETED: 'Hoàn tất',
  CANCELLED: 'Đã hủy', COMPLETE: 'Đủ', INCOMPLETE: 'Chưa đủ', NOT_REQUIRED: 'Không yêu cầu',
  LOW: 'Thấp', MEDIUM: 'Trung bình', HIGH: 'Cao', CRITICAL: 'Khẩn cấp',
  VERIFYING: 'Đang xác minh', WAITING_RESPONSE: 'Chờ phản hồi', CLOSED: 'Đã đóng',
  PLANNED: 'Kế hoạch', APPROVED: 'Đã duyệt', INCOME: 'Thu', EXPENSE: 'Chi',
  DRAFT: 'Bản nháp', SUBMITTED: 'Đã nộp',
  HR_IMPORT: 'Nhập dữ liệu HR', FINANCE_IMPORT: 'Nhập dữ liệu tài chính',
  UNITS_IMPORT: 'Nhập CĐCS từ Excel', MEMBERS_IMPORT: 'Nhập đoàn viên từ Excel',
  WELFARE_IMPORT: 'Nhập chăm lo từ Excel', CASES_IMPORT: 'Nhập vụ việc từ Excel',
  ACTIVITIES_IMPORT: 'Nhập hoạt động từ Excel', FINANCE_EXCEL_IMPORT: 'Nhập tài chính từ Excel',
  SURVEYS_IMPORT: 'Nhập khảo sát từ Excel', SURVEY_RESPONSES_IMPORT: 'Nhập phản hồi khảo sát từ Excel',
  REPORTS_IMPORT: 'Nhập báo cáo từ Excel', USERS_IMPORT: 'Nhập tài khoản từ Excel',
  PARTIAL: 'Hoàn tất một phần', FAILED: 'Thất bại',
}

export const enumLabel = (value: unknown) => labels[String(value)] ?? String(value ?? '—')
export const currentMonth = () => new Date().toISOString().slice(0, 7)
