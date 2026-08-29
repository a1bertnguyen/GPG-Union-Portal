import { formatMoney } from '../api'
import type { SummaryCard } from '../components/CrudPage'

const metric = (metrics: Record<string, number>, key: string) => metrics[key] ?? 0
const cards = (items: SummaryCard[]) => items

export const memberPresetFilters = [{ value: 'missing', label: 'Dữ liệu còn thiếu' }]
export const welfarePresetFilters = [
  { value: 'due', label: 'Đến hạn' },
  { value: 'new', label: 'Yêu cầu chờ duyệt' },
]
export const casePresetFilters = [
  { value: 'due24', label: 'Đến hạn 24h' },
  { value: 'overdue', label: 'Quá hạn' },
  { value: 'repeated', label: 'Vụ việc lặp lại' },
  { value: 'many', label: 'Ảnh hưởng nhiều NLĐ' },
]
export const activityPresetFilters = [
  { value: 'running', label: 'Đang triển khai' },
  { value: 'completed', label: 'Đã hoàn tất' },
]

export const unitSummary = (metrics: Record<string, number>) => cards([
  { label: 'CĐCS đang theo dõi', value: metric(metrics, 'total'), tone: 'blue' },
  { label: 'Đang hoạt động', value: metric(metrics, 'active'), tone: 'teal' },
  { label: 'Ngừng hoạt động', value: metric(metrics, 'inactive'), tone: 'orange' },
  { label: 'Có thông tin Chủ tịch', value: metric(metrics, 'withChairperson'), tone: 'green' },
])
export const memberSummary = (metrics: Record<string, number>) => cards([
  { label: 'Tổng hồ sơ NLĐ', value: metric(metrics, 'total'), tone: 'blue' },
  { label: 'Đoàn viên', value: metric(metrics, 'unionMembers'), tone: 'teal' },
  { label: 'Chưa gia nhập', value: metric(metrics, 'notJoined'), tone: 'orange' },
  { label: 'Đang làm việc', value: metric(metrics, 'activeEmployment'), tone: 'green' },
])
export const welfareSummary = (metrics: Record<string, number>) => cards([
  { label: 'Sinh nhật', value: metric(metrics, 'birthday'), tone: 'blue' },
  { label: 'Thăm hỏi', value: metric(metrics, 'visit'), tone: 'teal' },
  { label: 'Hiếu / hỷ', value: metric(metrics, 'funeralOrWedding'), tone: 'orange' },
  { label: 'Chưa hoàn tất', value: metric(metrics, 'unfinished'), tone: 'orange' },
])
export const welfarePolicySummary = (metrics: Record<string, number>) => cards([
  { label: 'Tổng chính sách', value: metric(metrics, 'total'), tone: 'blue' },
  { label: 'Đang áp dụng', value: metric(metrics, 'active'), tone: 'green' },
  { label: 'Công đoàn hỗ trợ', value: metric(metrics, 'union'), tone: 'teal' },
  { label: 'Công ty hỗ trợ', value: metric(metrics, 'company'), tone: 'orange' },
])
export const caseSummary = (metrics: Record<string, number>) => cards([
  { label: 'Vụ việc đang mở', value: metric(metrics, 'open'), tone: 'blue' },
  { label: 'Sắp / quá hạn', value: metric(metrics, 'dueOrOverdue'), tone: 'orange' },
  { label: 'Chờ ADMIN duyệt', value: metric(metrics, 'pendingApproval'), tone: 'orange' },
  { label: 'Đã đóng', value: metric(metrics, 'closed'), tone: 'green' },
])
export const activitySummary = (metrics: Record<string, number>) => cards([
  { label: 'Kế hoạch', value: metric(metrics, 'planned'), tone: 'blue' },
  { label: 'Đang triển khai', value: metric(metrics, 'inProgress'), tone: 'teal' },
  { label: 'Đã hoàn tất', value: metric(metrics, 'completed'), tone: 'green' },
  { label: 'Thiếu báo cáo sau CT', value: metric(metrics, 'missingReport'), tone: 'orange' },
])
export const financeSummary = (metrics: Record<string, number>) => cards([
  { label: 'Tổng thu', value: formatMoney(metric(metrics, 'income')), tone: 'green' },
  { label: 'Tổng chi', value: formatMoney(metric(metrics, 'expense')), tone: 'orange' },
  { label: 'Tạm ứng', value: formatMoney(metric(metrics, 'advance')), tone: 'blue' },
  { label: 'Chênh lệch số dư', value: formatMoney(metric(metrics, 'balance')), tone: 'teal' },
  { label: 'Chứng từ chưa đủ', value: metric(metrics, 'incompleteDocuments'), tone: 'orange' },
])
