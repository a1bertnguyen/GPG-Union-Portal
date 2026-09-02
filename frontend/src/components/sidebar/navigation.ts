export type PageKey =
  | 'home'
  | 'dashboard'
  | 'dashboardWelfare'
  | 'dashboardCases'
  | 'dashboardActivities'
  | 'dashboardFinance'
  | 'units'
  | 'members'
  | 'memberChanges'
  | 'memberDocuments'
  | 'welfare'
  | 'welfarePolicies'
  | 'welfareDocuments'
  | 'cases'
  | 'caseIssueGroups'
  | 'caseReports'
  | 'caseAnalytics'
  | 'activities'
  | 'activityReports'
  | 'activityGallery'
  | 'finance'
  | 'documents'
  | 'reports'
  | 'kpi'
  | 'users'

export type SidebarNavItem = {
  key: PageKey
  label: string
  mark: string
  adminOnly?: boolean
  userOnly?: boolean
  children?: SidebarNavItem[]
}

export type SidebarNavGroup = {
  label: string
  items: SidebarNavItem[]
}

export const navGroups: SidebarNavGroup[] = [
  { label: 'Tổng quan', items: [
    { key: 'home', label: 'Trang chủ hôm nay', mark: 'HN', userOnly: true },
    { key: 'dashboard', label: 'Điều hành', mark: 'ĐH', adminOnly: true },
    { key: 'dashboardWelfare', label: 'Chăm lo', mark: 'CL' },
    { key: 'dashboardCases', label: 'Kiến nghị', mark: 'KN' },
    { key: 'dashboardActivities', label: 'Chương trình', mark: 'CT' },
    { key: 'dashboardFinance', label: 'Tài chính', mark: 'TC' },
    { key: 'kpi', label: 'Báo cáo KPI', mark: 'KPI' },
  ] },
  { label: 'Nghiệp vụ', items: [
    { key: 'members', label: 'Đoàn viên', mark: 'ĐV', children: [
      { key: 'memberChanges', label: 'Cập nhật thông tin', mark: 'CN' },
      { key: 'memberDocuments', label: 'Tài liệu', mark: 'TL' },
    ] },
    { key: 'welfare', label: 'Chăm lo', mark: 'CL', children: [
      { key: 'welfarePolicies', label: 'Chính sách', mark: 'CS' },
      { key: 'welfareDocuments', label: 'Chứng từ', mark: 'CT' },
    ] },
    { key: 'cases', label: 'Kiến nghị', mark: 'KN', children: [
      { key: 'caseIssueGroups', label: 'Nhóm vấn đề', mark: 'NV', adminOnly: true },
      { key: 'caseReports', label: 'Báo cáo', mark: 'BC' },
      { key: 'caseAnalytics', label: 'Phân tích', mark: 'PT' },
    ] },
    { key: 'activities', label: 'Chương trình', mark: 'CT', children: [
      { key: 'activityReports', label: 'Báo cáo chương trình', mark: 'BC' },
      { key: 'activityGallery', label: 'Ảnh & tài liệu báo cáo', mark: 'TL' },
    ] },
    { key: 'documents', label: 'Kho tài liệu', mark: 'KT' },
    { key: 'finance', label: 'Tài chính nội bộ', mark: 'TC' },
    { key: 'reports', label: 'Báo cáo', mark: 'BC' },
  ] },
  { label: 'Quản trị', items: [
    { key: 'units', label: 'CĐCS / BCH', mark: 'ĐV', adminOnly: true },
    { key: 'users', label: 'Tài khoản', mark: 'TK', adminOnly: true },
  ] },
]

export const getPageLabel = (page: PageKey) =>
  navGroups.flatMap(group => group.items.flatMap(item => [item, ...(item.children ?? [])]))
    .find(item => item.key === page)?.label
