import type { AuthSession } from '../auth'
import CrudPage from '../components/CrudPage'
import type { PageKey } from '../components/sidebar/navigation'
import ActivityGalleryPage from '../pages/ActivityGalleryPage'
import DashboardPage from '../pages/DashboardPage'
import EngagementPage from '../pages/EngagementPage'
import HomeDashboardPage from '../pages/HomeDashboardPage'
import { MemberChangesPage, MemberDetailPanel, MemberDocumentsPage } from '../pages/MemberWorkspacePages'
import { CasesInsightPage, WelfareInsightPage } from '../pages/OperationalInsightPages'
import ReportsPage from '../pages/ReportsPage'
import UsersPage from '../pages/UsersPage'
import type { UnionUnit } from '../types'
import { activityColumns, caseColumns, financeColumns, memberColumns, unitColumns, welfareColumns } from './crudColumns'
import { activityFields, caseFields, financeFields, memberFields, unitFields, welfareFields } from './crudFields'
import {
  activityPresetFilters, activitySummary, casePresetFilters, caseSummary, financeSummary,
  memberPresetFilters, memberSummary, unitSummary, welfarePresetFilters, welfareSummary,
} from './crudSummaries'

type Props = {
  active: PageKey
  session: AuthSession
  units: UnionUnit[]
  createActivityRequested: boolean
  onNavigate: (key: PageKey) => void
  onCreateActivity: () => void
  onInitialCreateOpened: () => void
}

export default function PortalPage({
  active,
  session,
  units,
  createActivityRequested,
  onNavigate,
  onCreateActivity,
  onInitialCreateOpened,
}: Props) {
  const isAdmin = session.user.role === 'ADMIN'

  if (active === 'home') return <HomeDashboardPage unitName={session.user.unionUnitName} onNavigate={onNavigate} />
  if (active === 'dashboard') return <DashboardPage kind="executive" />
  if (active === 'dashboardWelfare') return <DashboardPage kind="welfare" />
  if (active === 'dashboardCases') return <DashboardPage kind="cases" />
  if (active === 'dashboardActivities') return <DashboardPage kind="activities" />
  if (active === 'dashboardFinance') return <DashboardPage kind="finance" />
  if (active === 'dashboardVoice') return <DashboardPage kind="voice" />
  if (active === 'voice') return <EngagementPage units={units} />
  if (active === 'reports') return <ReportsPage units={units} />
  if (active === 'users') return <UsersPage units={units} />
  if (active === 'memberChanges') return <MemberChangesPage units={units} />
  if (active === 'memberDocuments') return <MemberDocumentsPage units={units} />
  if (active === 'caseReports') return <CasesInsightPage units={units} mode="reports" />
  if (active === 'caseAnalytics') return <CasesInsightPage units={units} mode="analytics" />
  if (active === 'welfarePolicies') return <WelfareInsightPage units={units} mode="policies" />
  if (active === 'welfareDocuments') return <WelfareInsightPage units={units} mode="documents" />
  if (active === 'activityGallery') return <ActivityGalleryPage units={units} onCreateActivity={onCreateActivity} />

  if (active === 'units') return <CrudPage endpoint="/units" title="Hồ sơ CĐCS & Ban chấp hành" description="Theo dõi pháp lý, nhiệm kỳ, quyết định và đầu mối của từng đơn vị." singular="CĐCS" fields={unitFields} columns={unitColumns} units={units} excelResource="units" excelFilename="mau-cdcs.xlsx" canImportExcel={isAdmin} readOnly={!isAdmin} readOnlyMessage="Chỉ ADMIN được thay đổi cấu trúc và thông tin CĐCS." summaryBuilder={unitSummary} />
  if (active === 'members') return <CrudPage endpoint="/members" title="Hồ sơ đoàn viên & người lao động" description="Thêm đoàn viên, cập nhật biến động, xuất Excel và kiểm tra dữ liệu thiếu trên một danh sách." singular="Đoàn viên" fields={memberFields} columns={memberColumns} units={units} enableMemberExcel excelResource="members" excelFilename="mau-doan-vien.xlsx" summaryBuilder={memberSummary} presetFilters={memberPresetFilters} detailActionLabel="Mở hồ sơ" detailRenderer={(member, refresh) => <MemberDetailPanel member={member} refreshMembers={refresh} />} />
  if (active === 'welfare') return <CrudPage endpoint="/welfare" title="Chăm lo, phúc lợi & chính sách" description="Theo dõi đúng đối tượng, đúng định mức, đúng hạn và đầy đủ chứng từ." singular="Hồ sơ chăm lo" fields={welfareFields} columns={welfareColumns} units={units} excelResource="welfare" excelFilename="mau-cham-lo.xlsx" summaryBuilder={welfareSummary} presetFilters={welfarePresetFilters} />
  if (active === 'cases') return <CrudPage endpoint="/cases" title="Quản lý kiến nghị & vụ việc" description="Tiếp nhận, phân loại, giao PIC, theo dõi deadline và phản hồi người lao động." singular="Vụ việc" fields={caseFields} columns={caseColumns} units={units} excelResource="cases" excelFilename="mau-vu-viec.xlsx" summaryBuilder={caseSummary} presetFilters={casePresetFilters} />
  if (active === 'activities') return <CrudPage endpoint="/activities" title="Hoạt động & báo cáo sau chương trình" description="Quản lý trọn vòng đời trước, trong và sau từng chương trình." singular="Hoạt động" fields={activityFields} columns={activityColumns} units={units} excelResource="activities" excelFilename="mau-hoat-dong.xlsx" summaryBuilder={activitySummary} presetFilters={activityPresetFilters} openCreateInitially={createActivityRequested} onInitialCreateOpened={onInitialCreateOpened} />

  return <CrudPage endpoint="/finance" title="Thu – chi nội bộ" description="Nhập liệu phiếu thu/chi và tính số dư ngay trong hệ thống." singular="Phiếu thu/chi" fields={financeFields} columns={financeColumns} units={units}
    excelResource="finance" excelFilename="mau-tai-chinh-noi-bo.xlsx"
    summaryBuilder={financeSummary}
    notice={<div className="notice"><strong>Phạm vi tài chính nội bộ</strong><span>Chỉ nhập và tổng hợp số liệu nội bộ; không kết nối ngân hàng, ví điện tử hoặc cổng thanh toán.</span></div>} />
}
