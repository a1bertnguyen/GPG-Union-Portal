import type { AuthSession } from '../auth'
import CrudPage from '../components/CrudPage'
import type { PageKey } from '../components/sidebar/navigation'
import ActivityGalleryPage from '../pages/ActivityGalleryPage'
import ActivityReportsPage from '../pages/ActivityReportsPage'
import DashboardPage from '../pages/DashboardPage'
import DocumentLibraryPage from '../pages/DocumentLibraryPage'
import FinanceEntryPage from '../pages/FinancePages'
import HomeDashboardPage from '../pages/HomeDashboardPage'
import KpiPage from '../pages/KpiPage'
import { MemberChangesPage, MemberDetailPanel, MemberDocumentsPage } from '../pages/MemberWorkspacePages'
import { CasesInsightPage, WelfareInsightPage } from '../pages/OperationalInsightPages'
import LaborCasePage from '../pages/CasePages'
import { WelfarePolicyPage, WelfareRecordPage } from '../pages/WelfarePages'
import ReportsPage from '../pages/ReportsPage'
import UsersPage from '../pages/UsersPage'
import type { UnionUnit } from '../types'
import { activityColumns, memberColumns, unitColumns } from './crudColumns'
import { activityFields, memberFields, unitFields } from './crudFields'
import {
  activityPresetFilters, activitySummary,
  memberPresetFilters, memberSummary, unitSummary,
} from './crudSummaries'

type Props = {
  active: PageKey
  session: AuthSession
  units: UnionUnit[]
  onNavigate: (key: PageKey) => void
}

export default function PortalPage({
  active,
  session,
  units,
  onNavigate,
}: Props) {
  const isAdmin = session.user.role === 'ADMIN'

  if (active === 'home') return <HomeDashboardPage unitName={session.user.unionUnitName} isAdmin={isAdmin} onNavigate={onNavigate} />
  if (active === 'dashboard') return <DashboardPage kind="executive" />
  if (active === 'dashboardWelfare') return <DashboardPage kind="welfare" />
  if (active === 'dashboardCases') return <DashboardPage kind="cases" />
  if (active === 'dashboardActivities') return <DashboardPage kind="activities" />
  if (active === 'dashboardFinance') return <DashboardPage kind="finance" />
  if (active === 'documents') return <DocumentLibraryPage units={units} isAdmin={isAdmin} currentUnitName={session.user.unionUnitName} />
  if (active === 'kpi') return <KpiPage units={units} isAdmin={isAdmin} currentUnitId={session.user.unionUnitId} currentUnitCode={session.user.unionUnitCode} currentUnitName={session.user.unionUnitName} />
  if (active === 'reports') return <ReportsPage units={units} isAdmin={isAdmin} currentUnitId={session.user.unionUnitId} currentUnitName={session.user.unionUnitName} currentUserName={session.user.fullName} />
  if (active === 'users') return <UsersPage units={units} />
  if (active === 'memberChanges') return <MemberChangesPage units={units} />
  if (active === 'memberDocuments') return <MemberDocumentsPage units={units} />
  if (active === 'caseReports') return <CasesInsightPage units={units} mode="reports" isAdmin={isAdmin} unitCode={session.user.unionUnitCode} />
  if (active === 'caseAnalytics') return <CasesInsightPage units={units} mode="analytics" isAdmin={isAdmin} unitCode={session.user.unionUnitCode} />
  if (active === 'welfarePolicies') return <WelfarePolicyPage units={units} isAdmin={isAdmin} />
  if (active === 'welfareDocuments') return <WelfareInsightPage units={units} mode="documents" />
  if (active === 'activityGallery') return <ActivityGalleryPage units={units} />
  if (active === 'activityReports') return <ActivityReportsPage isAdmin={isAdmin} />

  if (active === 'units') return <CrudPage endpoint="/units" title="Hồ sơ CĐCS & Ban chấp hành" description="Theo dõi pháp lý, nhiệm kỳ, quyết định và đầu mối của từng đơn vị." singular="CĐCS" fields={unitFields} columns={unitColumns} units={units} excelResource="units" excelFilename="mau-cdcs.xlsx" canImportExcel={isAdmin} readOnly={!isAdmin} readOnlyMessage="Chỉ ADMIN được thay đổi cấu trúc và thông tin CĐCS." summaryBuilder={unitSummary} />
  if (active === 'members') return <CrudPage endpoint="/members" title="Hồ sơ đoàn viên & người lao động" description="Quản lý hồ sơ theo danh mục công ty, nơi làm việc chuẩn; cập nhật biến động và xuất Excel theo cấu trúc thống nhất." singular="Đoàn viên" fields={memberFields} columns={memberColumns} units={units} wideForm enableMemberExcel excelResource="members" excelFilename="mau-doan-vien.xlsx" summaryBuilder={memberSummary} presetFilters={memberPresetFilters} detailActionLabel="Mở hồ sơ" detailRenderer={(member, refresh) => <MemberDetailPanel member={member} refreshMembers={refresh} />} />
  if (active === 'welfare') return <WelfareRecordPage units={units} isAdmin={isAdmin} />
  if (active === 'cases') return <LaborCasePage units={units} isAdmin={isAdmin} />
  if (active === 'activities') return <CrudPage endpoint="/activities" title="Kế hoạch hoạt động" description="Tạo và quản lý thông tin kế hoạch. Kết quả thực tế, KPI và chứng cứ được hoàn thiện tại Báo cáo chương trình." singular="Hoạt động" fields={activityFields} columns={activityColumns} units={units} excelResource="activities" excelFilename="mau-hoat-dong.xlsx" summaryBuilder={activitySummary} presetFilters={activityPresetFilters} />

  return <FinanceEntryPage units={units} />
}
