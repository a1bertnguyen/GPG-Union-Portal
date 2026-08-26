export type PageKey =
  | 'home'
  | 'dashboard'
  | 'dashboardWelfare'
  | 'dashboardCases'
  | 'dashboardActivities'
  | 'dashboardFinance'
  | 'dashboardVoice'
  | 'units'
  | 'members'
  | 'memberChanges'
  | 'memberDocuments'
  | 'welfare'
  | 'welfarePolicies'
  | 'welfareDocuments'
  | 'cases'
  | 'caseReports'
  | 'caseAnalytics'
  | 'activities'
  | 'activityGallery'
  | 'finance'
  | 'voice'
  | 'reports'
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
  { label: 'Dashboard', items: [
    { key: 'home', label: 'Trang chủ hôm nay', mark: 'HN', userOnly: true },
    { key: 'dashboard', label: 'Điều hành', mark: 'ĐH', adminOnly: true },
    { key: 'dashboardWelfare', label: 'Chăm lo', mark: 'CL' },
    { key: 'dashboardCases', label: 'Vụ việc', mark: 'VV' },
    { key: 'dashboardActivities', label: 'Hoạt động', mark: 'HĐ' },
    { key: 'dashboardFinance', label: 'Tài chính', mark: 'TC' },
    { key: 'dashboardVoice', label: 'Tiếng nói NLĐ', mark: 'TN' },
  ] },
  { label: 'Nghiệp vụ', items: [
    { key: 'members', label: 'Đoàn viên', mark: 'ĐV', children: [
      { key: 'memberChanges', label: 'Biến động', mark: 'BĐ' },
      { key: 'memberDocuments', label: 'Tài liệu', mark: 'TL' },
    ] },
    { key: 'welfare', label: 'Chăm lo', mark: 'CL', children: [
      { key: 'welfarePolicies', label: 'Chính sách', mark: 'CS' },
      { key: 'welfareDocuments', label: 'Chứng từ', mark: 'CT' },
    ] },
    { key: 'cases', label: 'Vụ việc', mark: 'VV', children: [
      { key: 'caseReports', label: 'Báo cáo', mark: 'BC' },
      { key: 'caseAnalytics', label: 'Phân tích', mark: 'PT' },
    ] },
    { key: 'activities', label: 'Hoạt động', mark: 'HĐ', children: [
      { key: 'activityGallery', label: 'Ảnh & tài liệu', mark: 'TL' },
    ] },
    { key: 'voice', label: 'Tiếng nói NLĐ', mark: 'TN' },
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
