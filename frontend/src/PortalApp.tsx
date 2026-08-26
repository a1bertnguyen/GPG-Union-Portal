import { useEffect, useMemo, useState } from 'react'
import { apiAll, enumLabel, formatDate, formatMoney, loadEnumLabels } from './api'
import type { AuthSession } from './auth'
import CrudPage, { StatusBadge, type ColumnConfig, type FieldConfig, type SummaryCard } from './components/CrudPage'
import Sidebar from './components/sidebar/Sidebar'
import { getPageLabel, type PageKey } from './components/sidebar/navigation'
import DashboardPage from './pages/DashboardPage'
import EngagementPage from './pages/EngagementPage'
import ReportsPage from './pages/ReportsPage'
import UsersPage from './pages/UsersPage'
import HomeDashboardPage from './pages/HomeDashboardPage'
import { MemberChangesPage, MemberDetailPanel, MemberDocumentsPage } from './pages/MemberWorkspacePages'
import { CasesInsightPage, WelfareInsightPage } from './pages/OperationalInsightPages'
import ActivityGalleryPage from './pages/ActivityGalleryPage'
import type { BaseRecord, UnionUnit } from './types'

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
  { name: 'policyName', label: 'Chính sách / định mức áp dụng' },
  { name: 'unionUnitId', label: 'CĐCS', type: 'unit', required: true }, { name: 'beneficiaryName', label: 'Người thụ hưởng', required: true },
  { name: 'eventDate', label: 'Ngày sự kiện', type: 'date', required: true }, { name: 'deadline', label: 'Hạn hoàn tất', type: 'date' },
  { name: 'status', label: 'Trạng thái', type: 'select', required: true, options: option('NEW', 'PENDING_APPROVAL', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'), defaultValue: 'NEW' },
  { name: 'amount', label: 'Số tiền', type: 'number', step: '1000', required: true, defaultValue: '0' },
  { name: 'standardAmount', label: 'Định mức', type: 'number', step: '1000' },
  { name: 'documentStatus', label: 'Hồ sơ / chứng từ', type: 'select', required: true, options: option('COMPLETE', 'INCOMPLETE', 'NOT_REQUIRED'), defaultValue: 'INCOMPLETE' },
  { name: 'receiptStatus', label: 'Biên nhận / quyết toán', type: 'select', required: true, options: option('COMPLETE', 'INCOMPLETE', 'NOT_REQUIRED'), defaultValue: 'INCOMPLETE' },
  { name: 'hasImage', label: 'Đã có hình ảnh', type: 'checkbox', defaultValue: false },
  { name: 'notes', label: 'Ghi chú', type: 'textarea', wide: true },
]

const caseFields: FieldConfig[] = [
  { name: 'caseCode', label: 'Mã vụ việc', required: true }, { name: 'receivedDate', label: 'Ngày nhận', type: 'date', required: true },
  { name: 'unionUnitId', label: 'Đơn vị', type: 'unit', required: true }, { name: 'requesterName', label: 'Người gửi', required: true },
  { name: 'source', label: 'Kênh tiếp nhận' }, { name: 'issueGroup', label: 'Nhóm vấn đề', required: true },
  { name: 'severity', label: 'Mức độ', type: 'select', required: true, options: option('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'), defaultValue: 'MEDIUM' },
  { name: 'ownerName', label: 'PIC', required: true }, { name: 'deadline', label: 'Deadline', type: 'date', required: true },
  { name: 'status', label: 'Trạng thái', type: 'select', required: true, options: option('NEW', 'VERIFYING', 'CLASSIFYING', 'ASSIGNED', 'IN_PROGRESS', 'WAITING_RESPONSE', 'CLOSED'), defaultValue: 'NEW' },
  { name: 'affectedPeople', label: 'Số NLĐ ảnh hưởng', type: 'number', required: true, defaultValue: '1' },
  { name: 'description', label: 'Mô tả', type: 'textarea', required: true, wide: true },
  { name: 'attachmentNote', label: 'Tài liệu đính kèm / liên kết', type: 'textarea', wide: true },
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
  { name: 'participantList', label: 'Danh sách tham dự', type: 'textarea', wide: true },
  { name: 'checkInCount', label: 'Số người check-in', type: 'number', required: true, defaultValue: '0' },
  { name: 'usefulnessScore', label: 'Điểm hữu ích (0–5)', type: 'number', step: '0.1' },
  { name: 'quickFeedback', label: 'Phản hồi nhanh', type: 'textarea', wide: true },
  { name: 'issues', label: 'Vấn đề phát sinh', type: 'textarea', wide: true },
  { name: 'reportCompleted', label: 'Đã có báo cáo sau CT', type: 'checkbox', defaultValue: false },
  { name: 'documentStatus', label: 'Chứng từ sau chương trình', type: 'select', required: true, options: option('COMPLETE', 'INCOMPLETE', 'NOT_REQUIRED'), defaultValue: 'INCOMPLETE' },
  { name: 'objective', label: 'Mục tiêu', type: 'textarea', wide: true },
  { name: 'followUpOwner', label: 'PIC follow-up' }, { name: 'followUpDeadline', label: 'Deadline follow-up', type: 'date' },
  { name: 'lessonsLearned', label: 'Bài học sau chương trình', type: 'textarea', wide: true },
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
  { label: 'Đến hạn', render: item => formatDate(item.deadline ?? item.eventDate) }, { label: 'Số tiền', render: item => formatMoney(item.amount as number) },
  { label: 'Trạng thái', render: item => <StatusBadge value={item.status} /> }, { label: 'Chứng từ', render: item => <StatusBadge value={item.documentStatus} /> },
]

const caseColumns: ColumnConfig[] = [
  { label: 'Mã', render: item => <strong>{text(item, 'caseCode')}</strong> }, { label: 'Đơn vị', render: item => item.unionUnit?.code ?? '—' },
  { label: 'Người gửi', render: item => text(item, 'requesterName') }, { label: 'Nhóm vấn đề', render: item => text(item, 'issueGroup') }, { label: 'Mức độ', render: item => <StatusBadge value={item.severity} /> },
  { label: 'PIC', render: item => text(item, 'ownerName') }, { label: 'Deadline', render: item => formatDate(item.deadline) },
  { label: 'Trạng thái', render: item => <StatusBadge value={item.status} /> },
]

const activityColumns: ColumnConfig[] = [
  { label: 'Mã', render: item => <strong>{text(item, 'activityCode')}</strong> }, { label: 'Chương trình', render: item => text(item, 'name') },
  { label: 'Đơn vị', render: item => item.unionUnit?.code ?? '—' }, { label: 'Ngày', render: item => formatDate(item.eventDate) },
  { label: 'Trạng thái', render: item => <StatusBadge value={item.status} /> }, { label: 'Check-in', render: item => `${text(item, 'checkInCount')}/${text(item, 'participantCount')}` },
  { label: 'Chi phí', render: item => formatMoney(item.actualCost as number) }, { label: 'Báo cáo', render: item => item.reportCompleted ? <StatusBadge value="COMPLETE" /> : <StatusBadge value="INCOMPLETE" /> },
]

const financeColumns: ColumnConfig[] = [
  { label: 'Mã phiếu', render: item => <strong>{text(item, 'entryCode')}</strong> }, { label: 'Ngày', render: item => formatDate(item.transactionDate) },
  { label: 'Đơn vị', render: item => item.unionUnit?.code ?? '—' }, { label: 'Loại', render: item => <StatusBadge value={item.entryType} /> },
  { label: 'Nội dung', render: item => text(item, 'description') }, { label: 'Số tiền', render: item => <strong>{formatMoney(item.amount as number)}</strong> },
  { label: 'Chứng từ', render: item => <StatusBadge value={item.documentStatus} /> },
]

const summary = (cards: SummaryCard[]) => cards
/** Reads one metric from the `/facets` payload, defaulting to 0 while the request is in flight. */
const metric = (metrics: Record<string, number>, key: string) => metrics[key] ?? 0

// Tracking filters. The predicates that used to run in the browser now live in the `*Specs` classes
// on the server; these entries only supply the key and the label.
const memberPresetFilters = [
  { value: 'missing', label: 'Dữ liệu còn thiếu' },
]
const welfarePresetFilters = [
  { value: 'due', label: 'Đến hạn' },
  { value: 'new', label: 'Yêu cầu mới' },
]
const casePresetFilters = [
  { value: 'due24', label: 'Đến hạn 24h' },
  { value: 'overdue', label: 'Quá hạn' },
  { value: 'repeated', label: 'Vụ việc lặp lại' },
  { value: 'many', label: 'Ảnh hưởng nhiều NLĐ' },
]
const activityPresetFilters = [
  { value: 'running', label: 'Đang triển khai' },
  { value: 'completed', label: 'Đã hoàn tất' },
]

const unitSummary = (metrics: Record<string, number>) => summary([
  { label: 'CĐCS đang theo dõi', value: metric(metrics, 'total'), tone: 'blue' },
  { label: 'Đang hoạt động', value: metric(metrics, 'active'), tone: 'teal' },
  { label: 'Ngừng hoạt động', value: metric(metrics, 'inactive'), tone: 'orange' },
  { label: 'Có thông tin Chủ tịch', value: metric(metrics, 'withChairperson'), tone: 'green' },
])
const memberSummary = (metrics: Record<string, number>) => summary([
  { label: 'Tổng hồ sơ NLĐ', value: metric(metrics, 'total'), tone: 'blue' },
  { label: 'Đoàn viên', value: metric(metrics, 'unionMembers'), tone: 'teal' },
  { label: 'Chưa gia nhập', value: metric(metrics, 'notJoined'), tone: 'orange' },
  { label: 'Đang làm việc', value: metric(metrics, 'activeEmployment'), tone: 'green' },
])
const welfareSummary = (metrics: Record<string, number>) => summary([
  { label: 'Sinh nhật', value: metric(metrics, 'birthday'), tone: 'blue' },
  { label: 'Thăm hỏi', value: metric(metrics, 'visit'), tone: 'teal' },
  { label: 'Hiếu / hỷ', value: metric(metrics, 'funeralOrWedding'), tone: 'orange' },
  { label: 'Chưa hoàn tất', value: metric(metrics, 'unfinished'), tone: 'orange' },
])
const caseSummary = (metrics: Record<string, number>) => summary([
  { label: 'Vụ việc đang mở', value: metric(metrics, 'open'), tone: 'blue' },
  { label: 'Sắp / quá hạn', value: metric(metrics, 'dueOrOverdue'), tone: 'orange' },
  { label: 'Mức độ cao', value: metric(metrics, 'highSeverity'), tone: 'orange' },
  { label: 'Đã đóng', value: metric(metrics, 'closed'), tone: 'green' },
])
const activitySummary = (metrics: Record<string, number>) => summary([
  { label: 'Kế hoạch', value: metric(metrics, 'planned'), tone: 'blue' },
  { label: 'Đang triển khai', value: metric(metrics, 'inProgress'), tone: 'teal' },
  { label: 'Đã hoàn tất', value: metric(metrics, 'completed'), tone: 'green' },
  { label: 'Thiếu báo cáo sau CT', value: metric(metrics, 'missingReport'), tone: 'orange' },
])
const financeSummary = (metrics: Record<string, number>) => summary([
  { label: 'Tổng thu', value: formatMoney(metric(metrics, 'income')), tone: 'green' },
  { label: 'Tổng chi', value: formatMoney(metric(metrics, 'expense')), tone: 'orange' },
  { label: 'Số dư nội bộ', value: formatMoney(metric(metrics, 'balance')), tone: 'blue' },
  { label: 'Chứng từ chưa đủ', value: metric(metrics, 'incompleteDocuments'), tone: 'orange' },
])

type Props = {
  session: AuthSession
  onLogout: () => void
}

export default function PortalApp({ session, onLogout }: Props) {
  const [active, setActive] = useState<PageKey>(() => session.user.role === 'ADMIN' ? 'dashboard' : 'home')
  const [units, setUnits] = useState<UnionUnit[]>([])
  const [menuOpen, setMenuOpen] = useState(false)
  const [createActivityRequested, setCreateActivityRequested] = useState(false)
  const isAdmin = session?.user.role === 'ADMIN'

  useEffect(() => {
    if (!session) return
    // Every CĐCS dropdown in the app reads this list, so ask for the whole set rather than a page.
    apiAll<UnionUnit>('/units').then(setUnits).catch(() => setUnits([]))
  }, [active, session])

  useEffect(() => {
    if (!session) return
    void loadEnumLabels()
  }, [session])

  const page = useMemo(() => {
    if (active === 'home') return <HomeDashboardPage unitName={session?.user.unionUnitName} onNavigate={key => { setActive(key); setMenuOpen(false) }} />
    if (active === 'dashboard') return <DashboardPage key="executive" kind="executive" />
    if (active === 'dashboardWelfare') return <DashboardPage key="welfare" kind="welfare" />
    if (active === 'dashboardCases') return <DashboardPage key="cases" kind="cases" />
    if (active === 'dashboardActivities') return <DashboardPage key="activities" kind="activities" />
    if (active === 'dashboardFinance') return <DashboardPage key="finance" kind="finance" />
    if (active === 'dashboardVoice') return <DashboardPage key="voice" kind="voice" />
    if (active === 'voice') return <EngagementPage units={units} />
    if (active === 'reports') return <ReportsPage units={units} />
    if (active === 'users') return <UsersPage units={units} />
    if (active === 'memberChanges') return <MemberChangesPage units={units} />
    if (active === 'memberDocuments') return <MemberDocumentsPage units={units} />
    if (active === 'caseReports') return <CasesInsightPage units={units} mode="reports" />
    if (active === 'caseAnalytics') return <CasesInsightPage units={units} mode="analytics" />
    if (active === 'welfarePolicies') return <WelfareInsightPage units={units} mode="policies" />
    if (active === 'welfareDocuments') return <WelfareInsightPage units={units} mode="documents" />
    if (active === 'activityGallery') return <ActivityGalleryPage units={units} onCreateActivity={() => {
      setCreateActivityRequested(true)
      setActive('activities')
      setMenuOpen(false)
    }} />
    if (active === 'units') return <CrudPage endpoint="/units" title="Hồ sơ CĐCS & Ban chấp hành" description="Theo dõi pháp lý, nhiệm kỳ, quyết định và đầu mối của từng đơn vị." singular="CĐCS" fields={unitFields} columns={unitColumns} units={units} excelResource="units" excelFilename="mau-cdcs.xlsx" canImportExcel={isAdmin} readOnly={!isAdmin} readOnlyMessage="Chỉ ADMIN được thay đổi cấu trúc và thông tin CĐCS." summaryBuilder={unitSummary} />
    if (active === 'members') return <CrudPage endpoint="/members" title="Hồ sơ đoàn viên & người lao động" description="Thêm đoàn viên, cập nhật biến động, xuất Excel và kiểm tra dữ liệu thiếu trên một danh sách." singular="Đoàn viên" fields={memberFields} columns={memberColumns} units={units} enableMemberExcel excelResource="members" excelFilename="mau-doan-vien.xlsx" summaryBuilder={memberSummary} presetFilters={memberPresetFilters} detailActionLabel="Mở hồ sơ" detailRenderer={(member, refresh) => <MemberDetailPanel member={member} refreshMembers={refresh} />} />
    if (active === 'welfare') return <CrudPage endpoint="/welfare" title="Chăm lo, phúc lợi & chính sách" description="Theo dõi đúng đối tượng, đúng định mức, đúng hạn và đầy đủ chứng từ." singular="Hồ sơ chăm lo" fields={welfareFields} columns={welfareColumns} units={units} excelResource="welfare" excelFilename="mau-cham-lo.xlsx" summaryBuilder={welfareSummary} presetFilters={welfarePresetFilters} />
    if (active === 'cases') return <CrudPage endpoint="/cases" title="Quản lý kiến nghị & vụ việc" description="Tiếp nhận, phân loại, giao PIC, theo dõi deadline và phản hồi người lao động." singular="Vụ việc" fields={caseFields} columns={caseColumns} units={units} excelResource="cases" excelFilename="mau-vu-viec.xlsx" summaryBuilder={caseSummary} presetFilters={casePresetFilters} />
    if (active === 'activities') return <CrudPage endpoint="/activities" title="Hoạt động & báo cáo sau chương trình" description="Quản lý trọn vòng đời trước, trong và sau từng chương trình." singular="Hoạt động" fields={activityFields} columns={activityColumns} units={units} excelResource="activities" excelFilename="mau-hoat-dong.xlsx" summaryBuilder={activitySummary} presetFilters={activityPresetFilters} openCreateInitially={createActivityRequested} onInitialCreateOpened={() => setCreateActivityRequested(false)} />
    return <CrudPage endpoint="/finance" title="Thu – chi nội bộ" description="Nhập liệu phiếu thu/chi và tính số dư ngay trong hệ thống." singular="Phiếu thu/chi" fields={financeFields} columns={financeColumns} units={units}
      excelResource="finance" excelFilename="mau-tai-chinh-noi-bo.xlsx"
      summaryBuilder={financeSummary}
      notice={<div className="notice"><strong>Phạm vi tài chính nội bộ</strong><span>Chỉ nhập và tổng hợp số liệu nội bộ; không kết nối ngân hàng, ví điện tử hoặc cổng thanh toán.</span></div>} />
  }, [active, units, isAdmin, session?.user.unionUnitName, createActivityRequested])

  const selectPage = (key: PageKey) => { setActive(key); setMenuOpen(false) }
  return (
    <div className="app-shell">
      <header className="portal-topbar">
        <button className="portal-menu-button" aria-label="Mở menu" onClick={() => setMenuOpen(value => !value)}>☰</button>
        <div className="portal-brand"><div className="portal-brand__mark">G</div><div><strong>GPG UNION PORTAL</strong><span>Genuine Partner Trade Union</span></div></div>
        <div className="portal-context"><i /> <strong>{getPageLabel(active) ?? 'Tổng quan'}</strong></div>
        <div className="portal-user"><div><strong>{session.user.fullName}</strong><span>{isAdmin ? 'ADMIN · Toàn hệ thống' : `USER · ${session.user.unionUnitCode}`}</span></div><button onClick={onLogout}>Đăng xuất</button></div>
      </header>
      <Sidebar
        active={active}
        isAdmin={isAdmin}
        isOpen={menuOpen}
        onSelect={selectPage}
        unitName={session.user.unionUnitName}
      />
      <main className="main-content">
        {page}
      </main>
      {menuOpen && <button className="menu-scrim" aria-label="Đóng menu" onClick={() => setMenuOpen(false)} />}
    </div>
  )
}
