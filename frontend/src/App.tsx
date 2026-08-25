import { useEffect, useMemo, useState } from 'react'
import { api, enumLabel, formatDate, formatMoney } from './api'
import { AUTH_EXPIRED_EVENT, clearSession, getSession, type AuthSession } from './auth'
import CrudPage, { StatusBadge, type ColumnConfig, type FieldConfig, type SummaryCard } from './components/CrudPage'
import DashboardPage from './pages/DashboardPage'
import EngagementPage from './pages/EngagementPage'
import LoginPage from './pages/LoginPage'
import ReportsPage from './pages/ReportsPage'
import IntegrationsPage from './pages/IntegrationsPage'
import UsersPage from './pages/UsersPage'
import type { BaseRecord, UnionUnit } from './types'

type PageKey = 'dashboard' | 'dashboardWelfare' | 'dashboardCases' | 'dashboardActivities' | 'dashboardFinance' | 'dashboardVoice' | 'units' | 'members' | 'welfare' | 'cases' | 'activities' | 'finance' | 'voice' | 'reports' | 'integrations' | 'users'
type NavItem = { key: PageKey; label: string; mark: string; adminOnly?: boolean }

const moduleSubnav: Partial<Record<PageKey, { label: string; target: string }[]>> = {
  dashboard: [{ label: 'Chỉ số hệ thống', target: 'dashboard-executive' }, { label: 'Ưu tiên điều hành', target: 'executive-priorities' }],
  dashboardWelfare: [{ label: 'KPI chăm lo', target: 'dashboard-welfare' }, { label: 'Cơ cấu hồ sơ', target: 'welfare-breakdown' }],
  dashboardCases: [{ label: 'KPI vụ việc', target: 'dashboard-cases' }, { label: 'Danh sách ưu tiên', target: 'case-priorities' }],
  dashboardActivities: [{ label: 'KPI hoạt động', target: 'dashboard-activities' }, { label: 'Chương trình triển khai', target: 'activity-priorities' }],
  dashboardFinance: [{ label: 'KPI thu – chi', target: 'dashboard-finance' }, { label: 'Theo nhóm', target: 'finance-breakdown' }, { label: 'Theo đơn vị', target: 'finance-units' }],
  dashboardVoice: [{ label: 'KPI kết nối', target: 'dashboard-voice' }, { label: 'Top nhu cầu', target: 'voice-needs' }],
  members: [{ label: 'Danh sách đoàn viên', target: 'records' }, { label: 'Import dữ liệu', target: 'page-actions' }, { label: 'Tác vụ hồ sơ', target: 'module-insights' }],
  welfare: [{ label: 'Chăm lo', target: 'records' }, { label: 'Đến hạn', target: 'module-summary' }, { label: 'Chứng từ', target: 'module-insights' }],
  cases: [{ label: 'Danh sách vụ việc', target: 'records' }, { label: 'Quá hạn', target: 'module-summary' }, { label: 'Workflow', target: 'module-insights' }],
  activities: [{ label: 'Kế hoạch', target: 'records' }, { label: 'Báo cáo sau CT', target: 'module-insights' }],
  finance: [{ label: 'Thu – chi', target: 'records' }, { label: 'Chứng từ', target: 'module-summary' }],
  reports: [{ label: 'Báo cáo tháng', target: 'report-monthly' }, { label: 'Kế hoạch & đề xuất', target: 'report-narrative' }],
  voice: [{ label: 'Tổng quan', target: 'voice-overview' }, { label: 'Khảo sát', target: 'voice-surveys' }],
  units: [{ label: 'CĐCS / BCH', target: 'records' }, { label: 'Nhiệm kỳ', target: 'module-insights' }],
  users: [{ label: 'Tài khoản', target: 'users-list' }, { label: 'Phân quyền', target: 'role-summary' }],
  integrations: [{ label: 'Import dữ liệu', target: 'integration-import' }, { label: 'Lịch sử', target: 'integration-history' }],
}

const navGroups: { label: string; items: NavItem[] }[] = [
  { label: 'Dashboard', items: [
    { key: 'dashboard', label: 'Điều hành', mark: 'ĐH' },
    { key: 'dashboardWelfare', label: 'Chăm lo', mark: 'CL' },
    { key: 'dashboardCases', label: 'Vụ việc', mark: 'VV' },
    { key: 'dashboardActivities', label: 'Hoạt động', mark: 'HĐ' },
    { key: 'dashboardFinance', label: 'Tài chính', mark: 'TC' },
    { key: 'dashboardVoice', label: 'Tiếng nói NLĐ', mark: 'TN' },
  ] },
  { label: 'Nghiệp vụ', items: [
    { key: 'members', label: 'Đoàn viên', mark: 'ĐV' },
    { key: 'welfare', label: 'Chăm lo', mark: 'CL' },
    { key: 'cases', label: 'Vụ việc', mark: 'VV' },
    { key: 'activities', label: 'Hoạt động', mark: 'HĐ' },
    { key: 'voice', label: 'Tiếng nói NLĐ', mark: 'TN' },
    { key: 'finance', label: 'Tài chính nội bộ', mark: 'TC' },
    { key: 'reports', label: 'Báo cáo', mark: 'BC' },
  ] },
  { label: 'Quản trị', items: [
    { key: 'units', label: 'CĐCS / BCH', mark: 'ĐV', adminOnly: true },
    { key: 'users', label: 'Tài khoản', mark: 'TK', adminOnly: true },
    { key: 'integrations', label: 'Tích hợp dữ liệu', mark: 'TH', adminOnly: true },
  ] },
]

const option = (...values: string[]) => values.map(value => ({ value, label: enumLabel(value) }))
const text = (item: BaseRecord, key: string) => String(item[key] ?? '—')

const unitFields: FieldConfig[] = [
  { name: 'code', label: 'Mã CĐCS', required: true }, { name: 'name', label: 'Tên CĐCS', required: true },
  { name: 'companyName', label: 'Công ty', required: true }, { name: 'location', label: 'Địa điểm' },
  { name: 'chairperson', label: 'Chủ tịch CĐCS' }, { name: 'contactPerson', label: 'Đầu mối' },
  { name: 'termStart', label: 'Bắt đầu nhiệm kỳ', type: 'date' }, { name: 'termEnd', label: 'Kết thúc nhiệm kỳ', type: 'date' },
  { name: 'decisionNumber', label: 'Số quyết định' },
  { name: 'legalStatus', label: 'Tình trạng pháp lý', type: 'select', required: true, options: option('ACTIVE', 'INACTIVE'), defaultValue: 'ACTIVE' },
]

const memberFields: FieldConfig[] = [
  { name: 'employeeCode', label: 'Mã nhân viên', required: true }, { name: 'fullName', label: 'Họ và tên', required: true },
  { name: 'unionUnitId', label: 'CĐCS', type: 'unit', required: true }, { name: 'jobTitle', label: 'Chức danh' },
  { name: 'workplace', label: 'Địa điểm làm việc' }, { name: 'joinDate', label: 'Ngày gia nhập', type: 'date' },
  { name: 'membershipStatus', label: 'Tình trạng công đoàn', type: 'select', required: true, options: option('MEMBER', 'NOT_JOINED', 'LEFT'), defaultValue: 'MEMBER' },
  { name: 'employmentStatus', label: 'Trạng thái nhân sự', type: 'select', required: true, options: option('ACTIVE', 'INACTIVE'), defaultValue: 'ACTIVE' },
  { name: 'email', label: 'Email', type: 'email' }, { name: 'phone', label: 'Điện thoại' },
]

const welfareFields: FieldConfig[] = [
  { name: 'recordCode', label: 'Mã hồ sơ', required: true },
  { name: 'welfareType', label: 'Loại chăm lo', type: 'select', required: true, options: option('BIRTHDAY', 'FUNERAL', 'WEDDING', 'VISIT', 'CHILDBIRTH', 'HARDSHIP'), defaultValue: 'BIRTHDAY' },
  { name: 'unionUnitId', label: 'CĐCS', type: 'unit', required: true }, { name: 'beneficiaryName', label: 'Người thụ hưởng', required: true },
  { name: 'eventDate', label: 'Ngày thực hiện', type: 'date', required: true },
  { name: 'status', label: 'Trạng thái', type: 'select', required: true, options: option('NEW', 'PENDING_APPROVAL', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'), defaultValue: 'NEW' },
  { name: 'amount', label: 'Số tiền', type: 'number', step: '1000', required: true, defaultValue: '0' },
  { name: 'documentStatus', label: 'Chứng từ', type: 'select', required: true, options: option('COMPLETE', 'INCOMPLETE', 'NOT_REQUIRED'), defaultValue: 'INCOMPLETE' },
  { name: 'notes', label: 'Ghi chú', type: 'textarea', wide: true },
]

const caseFields: FieldConfig[] = [
  { name: 'caseCode', label: 'Mã vụ việc', required: true }, { name: 'receivedDate', label: 'Ngày nhận', type: 'date', required: true },
  { name: 'unionUnitId', label: 'Đơn vị', type: 'unit', required: true }, { name: 'issueGroup', label: 'Nhóm vấn đề', required: true },
  { name: 'severity', label: 'Mức độ', type: 'select', required: true, options: option('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'), defaultValue: 'MEDIUM' },
  { name: 'ownerName', label: 'PIC', required: true }, { name: 'deadline', label: 'Deadline', type: 'date', required: true },
  { name: 'status', label: 'Trạng thái', type: 'select', required: true, options: option('NEW', 'VERIFYING', 'IN_PROGRESS', 'WAITING_RESPONSE', 'CLOSED'), defaultValue: 'NEW' },
  { name: 'affectedPeople', label: 'Số NLĐ ảnh hưởng', type: 'number', required: true, defaultValue: '1' },
  { name: 'description', label: 'Mô tả', type: 'textarea', required: true, wide: true },
  { name: 'resultText', label: 'Kết quả / phản hồi', type: 'textarea', wide: true },
  { name: 'overdueReason', label: 'Lý do quá hạn / ETA mới', type: 'textarea', wide: true },
]

const activityFields: FieldConfig[] = [
  { name: 'activityCode', label: 'Mã hoạt động', required: true }, { name: 'name', label: 'Tên chương trình', required: true },
  { name: 'unionUnitId', label: 'Đơn vị', type: 'unit', required: true }, { name: 'eventDate', label: 'Ngày tổ chức', type: 'date', required: true },
  { name: 'status', label: 'Trạng thái', type: 'select', required: true, options: option('PLANNED', 'APPROVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'), defaultValue: 'PLANNED' },
  { name: 'plannedBudget', label: 'Ngân sách dự kiến', type: 'number', step: '1000', required: true, defaultValue: '0' },
  { name: 'actualCost', label: 'Chi phí thực tế', type: 'number', step: '1000', required: true, defaultValue: '0' },
  { name: 'participantCount', label: 'Số người tham dự', type: 'number', required: true, defaultValue: '0' },
  { name: 'usefulnessScore', label: 'Điểm hữu ích (0–5)', type: 'number', step: '0.1' },
  { name: 'reportCompleted', label: 'Đã có báo cáo sau CT', type: 'checkbox', defaultValue: false },
  { name: 'objective', label: 'Mục tiêu', type: 'textarea', wide: true },
  { name: 'followUpOwner', label: 'PIC follow-up' }, { name: 'followUpDeadline', label: 'Deadline follow-up', type: 'date' },
]

const financeFields: FieldConfig[] = [
  { name: 'entryCode', label: 'Mã phiếu', required: true }, { name: 'unionUnitId', label: 'Đơn vị', type: 'unit', required: true },
  { name: 'transactionDate', label: 'Ngày giao dịch', type: 'date', required: true },
  { name: 'entryType', label: 'Loại', type: 'select', required: true, options: option('INCOME', 'EXPENSE'), defaultValue: 'EXPENSE' },
  { name: 'category', label: 'Nhóm thu/chi', required: true }, { name: 'amount', label: 'Số tiền', type: 'number', step: '1000', required: true },
  { name: 'documentNumber', label: 'Số chứng từ' },
  { name: 'documentStatus', label: 'Tình trạng chứng từ', type: 'select', required: true, options: option('COMPLETE', 'INCOMPLETE', 'NOT_REQUIRED'), defaultValue: 'INCOMPLETE' },
  { name: 'description', label: 'Nội dung', type: 'textarea', required: true, wide: true },
]

const unitColumns: ColumnConfig[] = [
  { label: 'Mã', render: item => <strong>{text(item, 'code')}</strong> }, { label: 'CĐCS', render: item => text(item, 'name') },
  { label: 'Công ty', render: item => text(item, 'companyName') }, { label: 'Địa điểm', render: item => text(item, 'location') },
  { label: 'Chủ tịch', render: item => text(item, 'chairperson') }, { label: 'Trạng thái', render: item => <StatusBadge value={item.legalStatus} /> },
]

const memberColumns: ColumnConfig[] = [
  { label: 'Mã NV', render: item => <strong>{text(item, 'employeeCode')}</strong> }, { label: 'Họ tên', render: item => text(item, 'fullName') },
  { label: 'CĐCS', render: item => item.unionUnit?.code ?? '—' }, { label: 'Chức danh', render: item => text(item, 'jobTitle') },
  { label: 'Gia nhập', render: item => formatDate(item.joinDate) }, { label: 'Tình trạng', render: item => <StatusBadge value={item.membershipStatus} /> },
]

const welfareColumns: ColumnConfig[] = [
  { label: 'Mã', render: item => <strong>{text(item, 'recordCode')}</strong> }, { label: 'Loại', render: item => enumLabel(item.welfareType) },
  { label: 'Người thụ hưởng', render: item => text(item, 'beneficiaryName') }, { label: 'Đơn vị', render: item => item.unionUnit?.code ?? '—' },
  { label: 'Ngày', render: item => formatDate(item.eventDate) }, { label: 'Số tiền', render: item => formatMoney(item.amount as number) },
  { label: 'Trạng thái', render: item => <StatusBadge value={item.status} /> }, { label: 'Chứng từ', render: item => <StatusBadge value={item.documentStatus} /> },
]

const caseColumns: ColumnConfig[] = [
  { label: 'Mã', render: item => <strong>{text(item, 'caseCode')}</strong> }, { label: 'Đơn vị', render: item => item.unionUnit?.code ?? '—' },
  { label: 'Nhóm vấn đề', render: item => text(item, 'issueGroup') }, { label: 'Mức độ', render: item => <StatusBadge value={item.severity} /> },
  { label: 'PIC', render: item => text(item, 'ownerName') }, { label: 'Deadline', render: item => formatDate(item.deadline) },
  { label: 'Trạng thái', render: item => <StatusBadge value={item.status} /> },
]

const activityColumns: ColumnConfig[] = [
  { label: 'Mã', render: item => <strong>{text(item, 'activityCode')}</strong> }, { label: 'Chương trình', render: item => text(item, 'name') },
  { label: 'Đơn vị', render: item => item.unionUnit?.code ?? '—' }, { label: 'Ngày', render: item => formatDate(item.eventDate) },
  { label: 'Trạng thái', render: item => <StatusBadge value={item.status} /> }, { label: 'Tham dự', render: item => text(item, 'participantCount') },
  { label: 'Chi phí', render: item => formatMoney(item.actualCost as number) }, { label: 'Báo cáo', render: item => item.reportCompleted ? <StatusBadge value="COMPLETE" /> : <StatusBadge value="INCOMPLETE" /> },
]

const financeColumns: ColumnConfig[] = [
  { label: 'Mã phiếu', render: item => <strong>{text(item, 'entryCode')}</strong> }, { label: 'Ngày', render: item => formatDate(item.transactionDate) },
  { label: 'Đơn vị', render: item => item.unionUnit?.code ?? '—' }, { label: 'Loại', render: item => <StatusBadge value={item.entryType} /> },
  { label: 'Nội dung', render: item => text(item, 'description') }, { label: 'Số tiền', render: item => <strong>{formatMoney(item.amount as number)}</strong> },
  { label: 'Chứng từ', render: item => <StatusBadge value={item.documentStatus} /> },
]

const summary = (cards: SummaryCard[]) => cards
const count = (items: BaseRecord[], field: string, value: string) => items.filter(item => String(item[field] ?? '') === value).length
const amount = (items: BaseRecord[], type?: string) => items
  .filter(item => !type || String(item.entryType ?? '') === type)
  .reduce((total, item) => total + Number(item.amount ?? 0), 0)
const today = () => new Date().toISOString().slice(0, 10)

const unitSummary = (items: BaseRecord[]) => summary([
  { label: 'CĐCS đang theo dõi', value: items.length, tone: 'blue' },
  { label: 'Đang hoạt động', value: count(items, 'legalStatus', 'ACTIVE'), tone: 'teal' },
  { label: 'Ngừng hoạt động', value: count(items, 'legalStatus', 'INACTIVE'), tone: 'orange' },
  { label: 'Có thông tin Chủ tịch', value: items.filter(item => Boolean(item.chairperson)).length, tone: 'green' },
])
const memberSummary = (items: BaseRecord[]) => summary([
  { label: 'Tổng hồ sơ NLĐ', value: items.length, tone: 'blue' },
  { label: 'Đoàn viên', value: count(items, 'membershipStatus', 'MEMBER'), tone: 'teal' },
  { label: 'Chưa gia nhập', value: count(items, 'membershipStatus', 'NOT_JOINED'), tone: 'orange' },
  { label: 'Đang làm việc', value: count(items, 'employmentStatus', 'ACTIVE'), tone: 'green' },
])
const welfareSummary = (items: BaseRecord[]) => summary([
  { label: 'Sinh nhật', value: count(items, 'welfareType', 'BIRTHDAY'), tone: 'blue' },
  { label: 'Thăm hỏi', value: count(items, 'welfareType', 'VISIT'), tone: 'teal' },
  { label: 'Hiếu / hỷ', value: count(items, 'welfareType', 'FUNERAL') + count(items, 'welfareType', 'WEDDING'), tone: 'orange' },
  { label: 'Chưa hoàn tất', value: items.filter(item => !['COMPLETED', 'CANCELLED'].includes(String(item.status ?? ''))).length, tone: 'orange' },
])
const caseSummary = (items: BaseRecord[]) => summary([
  { label: 'Vụ việc đang mở', value: items.filter(item => item.status !== 'CLOSED').length, tone: 'blue' },
  { label: 'Sắp / quá hạn', value: items.filter(item => item.status !== 'CLOSED' && String(item.deadline ?? '') <= today()).length, tone: 'orange' },
  { label: 'Mức độ cao', value: items.filter(item => ['HIGH', 'CRITICAL'].includes(String(item.severity ?? ''))).length, tone: 'orange' },
  { label: 'Đã đóng', value: count(items, 'status', 'CLOSED'), tone: 'green' },
])
const activitySummary = (items: BaseRecord[]) => summary([
  { label: 'Kế hoạch', value: count(items, 'status', 'PLANNED'), tone: 'blue' },
  { label: 'Đang triển khai', value: count(items, 'status', 'IN_PROGRESS'), tone: 'teal' },
  { label: 'Đã hoàn tất', value: count(items, 'status', 'COMPLETED'), tone: 'green' },
  { label: 'Thiếu báo cáo sau CT', value: items.filter(item => item.status === 'COMPLETED' && !item.reportCompleted).length, tone: 'orange' },
])
const financeSummary = (items: BaseRecord[]) => summary([
  { label: 'Tổng thu', value: formatMoney(amount(items, 'INCOME')), tone: 'green' },
  { label: 'Tổng chi', value: formatMoney(amount(items, 'EXPENSE')), tone: 'orange' },
  { label: 'Số dư nội bộ', value: formatMoney(amount(items, 'INCOME') - amount(items, 'EXPENSE')), tone: 'blue' },
  { label: 'Chứng từ chưa đủ', value: count(items, 'documentStatus', 'INCOMPLETE'), tone: 'orange' },
])

const WorkflowFooter = ({ cards }: { cards: { title: string; detail: string; tone?: string }[] }) => <div className="workflow-grid" id="module-insights">
  {cards.map(card => <article className={`workflow-card workflow-card--${card.tone ?? 'blue'}`} key={card.title}><span>Quy trình nghiệp vụ</span><strong>{card.title}</strong><p>{card.detail}</p></article>)}
</div>

export default function App() {
  const [session, setSession] = useState<AuthSession | null>(() => getSession())
  const [active, setActive] = useState<PageKey>('dashboard')
  const [units, setUnits] = useState<UnionUnit[]>([])
  const [menuOpen, setMenuOpen] = useState(false)
  const isAdmin = session?.user.role === 'ADMIN'

  useEffect(() => {
    if (!session) return
    api<UnionUnit[]>('/units').then(setUnits).catch(() => setUnits([]))
  }, [active, session])

  useEffect(() => {
    const handleExpired = () => setSession(null)
    window.addEventListener(AUTH_EXPIRED_EVENT, handleExpired)
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handleExpired)
  }, [])

  useEffect(() => {
    if (!session) return
    const remaining = new Date(session.expiresAt).getTime() - Date.now()
    const timer = window.setTimeout(() => {
      clearSession()
      setSession(null)
      setUnits([])
    }, Math.max(0, remaining))
    return () => window.clearTimeout(timer)
  }, [session])

  const page = useMemo(() => {
    if (active === 'dashboard') return <DashboardPage key="executive" kind="executive" />
    if (active === 'dashboardWelfare') return <DashboardPage key="welfare" kind="welfare" />
    if (active === 'dashboardCases') return <DashboardPage key="cases" kind="cases" />
    if (active === 'dashboardActivities') return <DashboardPage key="activities" kind="activities" />
    if (active === 'dashboardFinance') return <DashboardPage key="finance" kind="finance" />
    if (active === 'dashboardVoice') return <DashboardPage key="voice" kind="voice" />
    if (active === 'voice') return <EngagementPage units={units} />
    if (active === 'reports') return <ReportsPage units={units} />
    if (active === 'integrations') return <IntegrationsPage units={units} />
    if (active === 'users') return <UsersPage units={units} />
    if (active === 'units') return <CrudPage endpoint="/units" title="Hồ sơ CĐCS & Ban chấp hành" description="Theo dõi pháp lý, nhiệm kỳ, quyết định và đầu mối của từng đơn vị." singular="CĐCS" fields={unitFields} columns={unitColumns} units={units} excelResource="units" excelFilename="mau-cdcs.xlsx" canImportExcel={isAdmin} readOnly={!isAdmin} readOnlyMessage="Chỉ ADMIN được thay đổi cấu trúc và thông tin CĐCS." statusField="legalStatus" summaryBuilder={unitSummary} footer={<WorkflowFooter cards={[{ title: 'HỒ SƠ PHÁP LÝ', detail: 'Mã đơn vị, nhiệm kỳ, quyết định và trạng thái pháp lý được quản lý trên một hồ sơ.' }, { title: 'ĐẦU MỐI', detail: 'Chủ tịch CĐCS và người liên hệ là đầu mối chịu trách nhiệm cập nhật dữ liệu.', tone: 'teal' }]} />} />
    if (active === 'members') return <CrudPage endpoint="/members" title="Hồ sơ đoàn viên & người lao động" description="Một nguồn dữ liệu thống nhất cho hồ sơ, trạng thái tham gia và tình trạng làm việc." singular="Đoàn viên" fields={memberFields} columns={memberColumns} units={units} enableMemberCsv excelResource="members" excelFilename="mau-doan-vien.xlsx" statusField="membershipStatus" summaryBuilder={memberSummary} footer={<WorkflowFooter cards={[{ title: 'TÁC VỤ HỒ SƠ', detail: 'Thêm mới, cập nhật trạng thái, xuất danh sách và kiểm tra dữ liệu thiếu.' }, { title: 'PHẠM VI DỮ LIỆU', detail: 'USER chỉ thấy CĐCS được gán; ADMIN theo dõi toàn hệ thống.', tone: 'teal' }]} />} />
    if (active === 'welfare') return <CrudPage endpoint="/welfare" title="Chăm lo, phúc lợi & chính sách" description="Theo dõi đúng đối tượng, đúng định mức, đúng hạn và đầy đủ chứng từ." singular="Hồ sơ chăm lo" fields={welfareFields} columns={welfareColumns} units={units} excelResource="welfare" excelFilename="mau-cham-lo.xlsx" summaryBuilder={welfareSummary} footer={<WorkflowFooter cards={[{ title: 'TỰ ĐỘNG NHẮC', detail: 'Ưu tiên hồ sơ chưa hoàn tất, hồ sơ gần đến hạn và trường hợp còn thiếu chứng từ.', tone: 'orange' }, { title: 'KIỂM SOÁT', detail: 'Đối tượng, ngày thực hiện, số tiền, trạng thái và chứng từ nằm trên cùng hồ sơ.', tone: 'teal' }]} />} />
    if (active === 'cases') return <CrudPage endpoint="/cases" title="Quản lý kiến nghị & vụ việc" description="Tiếp nhận, phân loại, giao PIC, theo dõi deadline và phản hồi người lao động." singular="Vụ việc" fields={caseFields} columns={caseColumns} units={units} excelResource="cases" excelFilename="mau-vu-viec.xlsx" summaryBuilder={caseSummary} footer={<WorkflowFooter cards={[{ title: 'THÔNG TIN BẮT BUỘC', detail: 'Mã vụ việc, nhóm vấn đề, mức độ, người ảnh hưởng, PIC và deadline.' }, { title: 'WORKFLOW', detail: 'Mới → xác minh → xử lý → chờ phản hồi → đóng.', tone: 'teal' }, { title: 'CẢNH BÁO', detail: 'Nêu bật vụ việc quá hạn, mức độ cao và hồ sơ chưa có kết quả.', tone: 'orange' }]} />} />
    if (active === 'activities') return <CrudPage endpoint="/activities" title="Hoạt động & báo cáo sau chương trình" description="Quản lý kế hoạch, ngân sách, người tham dự, đánh giá và hành động tiếp theo." singular="Hoạt động" fields={activityFields} columns={activityColumns} units={units} excelResource="activities" excelFilename="mau-hoat-dong.xlsx" summaryBuilder={activitySummary} footer={<WorkflowFooter cards={[{ title: 'TRƯỚC CHƯƠNG TRÌNH', detail: 'Mục tiêu, kế hoạch, ngân sách, đơn vị và phê duyệt.' }, { title: 'TRONG CHƯƠNG TRÌNH', detail: 'Theo dõi số người tham dự và các vấn đề phát sinh.', tone: 'teal' }, { title: 'SAU CHƯƠNG TRÌNH', detail: 'Chi phí thực tế, điểm hữu ích, báo cáo và PIC follow-up.', tone: 'orange' }]} />} />
    return <CrudPage endpoint="/finance" title="Thu – chi nội bộ" description="Nhập liệu phiếu thu/chi và tính số dư ngay trong hệ thống." singular="Phiếu thu/chi" fields={financeFields} columns={financeColumns} units={units}
      excelResource="finance" excelFilename="mau-tai-chinh-noi-bo.xlsx"
      statusField="entryType" summaryBuilder={financeSummary} footer={<WorkflowFooter cards={[{ title: 'KIỂM SOÁT CHỨNG TỪ', detail: 'Mỗi khoản thu/chi gắn với mã phiếu, ngày, nhóm, số tiền và trạng thái chứng từ.' }, { title: 'RANH GIỚI AN TOÀN', detail: 'Chỉ nhập liệu và tổng hợp nội bộ; hệ thống không thực hiện giao dịch tiền.', tone: 'orange' }]} />}
      notice={<div className="notice"><strong>Phạm vi tài chính nội bộ</strong><span>Chỉ nhập và tổng hợp số liệu nội bộ; không kết nối ngân hàng, ví điện tử hoặc cổng thanh toán.</span></div>} />
  }, [active, units, isAdmin])

  const selectPage = (key: PageKey) => { setActive(key); setMenuOpen(false) }
  const logout = () => {
    clearSession()
    setSession(null)
    setUnits([])
    setActive('dashboard')
  }

  if (!session) return <LoginPage onLogin={setSession} />

  const visibleNavGroups = navGroups
    .map(group => ({ ...group, items: group.items.filter(item => !item.adminOnly || isAdmin) }))
    .filter(group => group.items.length)
  const activeItem = visibleNavGroups.flatMap(group => group.items).find(item => item.key === active)
  const activeSubnav = moduleSubnav[active] ?? []

  return (
    <div className="app-shell">
      <header className="portal-topbar">
        <button className="portal-menu-button" aria-label="Mở menu" onClick={() => setMenuOpen(value => !value)}>☰</button>
        <div className="portal-brand"><div className="portal-brand__mark">G</div><div><strong>GPG UNION PORTAL</strong><span>Genuine Partner Trade Union</span></div></div>
        <div className="portal-context"><i /> <strong>{activeItem?.label ?? 'Tổng quan'}</strong></div>
        <div className="portal-user"><div><strong>{session.user.fullName}</strong><span>{isAdmin ? 'ADMIN · Toàn hệ thống' : `USER · ${session.user.unionUnitCode}`}</span></div><button onClick={logout}>Đăng xuất</button></div>
      </header>
      <aside className={menuOpen ? 'sidebar sidebar--open' : 'sidebar'}>
        <nav>{visibleNavGroups.map(group => <div className="nav-group" key={group.label}><p>{group.label}</p>{group.items.map(item => <div key={item.key}>
          <button className={active === item.key ? 'nav-item nav-item--active' : 'nav-item'} onClick={() => selectPage(item.key)}><span>{item.mark}</span>{item.label}</button>
          {active === item.key && activeSubnav.length > 0 && <div className="module-subnav">{activeSubnav.map((subitem, index) => <a className={index === 0 ? 'module-subnav__active' : ''} href={`#${subitem.target}`} key={subitem.label}>{subitem.label}</a>)}</div>}
        </div>)}</div>)}</nav>
        <div className="sidebar__footer"><span className="system-dot" />Hệ thống nội bộ<span>{isAdmin ? 'Toàn hệ thống' : session.user.unionUnitName}</span></div>
      </aside>
      <main className="main-content">
        {page}
      </main>
      {menuOpen && <button className="menu-scrim" aria-label="Đóng menu" onClick={() => setMenuOpen(false)} />}
    </div>
  )
}
