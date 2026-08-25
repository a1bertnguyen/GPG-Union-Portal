export interface UnionUnit {
  id: number
  code: string
  name: string
  companyName: string
  location?: string
  chairperson?: string
  termStart?: string
  termEnd?: string
  decisionNumber?: string
  legalStatus: string
  contactPerson?: string
}

export interface BaseRecord {
  id: number
  unionUnit?: UnionUnit
  [key: string]: unknown
}

export interface AlertItem {
  level: 'danger' | 'warning' | 'info'
  title: string
  detail: string
}

export interface DashboardSummary {
  unitCount: number
  activeMemberCount: number
  unionMemberCount: number
  welfareCompletionRate: number
  openCaseCount: number
  overdueCaseCount: number
  monthIncome: number
  monthExpense: number
  allTimeBalance: number
  pendingReportCount: number
  alerts: AlertItem[]
}

export interface MonthlyReport {
  id: number
  preparedBy: string
  planNextMonth?: string
  supportRequest?: string
  status: string
  submittedAt?: string
}

export interface MonthlySummary {
  month: string
  unionUnitId?: number
  unionUnitName: string
  activeEmployees: number
  unionMembers: number
  welfareCases: number
  completedWelfareCases: number
  laborCases: number
  closedLaborCases: number
  activities: number
  participants: number
  income: number
  expense: number
  netChange: number
  incompleteDocuments: number
  narrative?: MonthlyReport
}

export interface PulseSurvey {
  id: number
  surveyCode: string
  title: string
  unionUnit: UnionUnit
  questionText: string
  startDate: string
  endDate: string
  status: string
  targetResponses: number
  responseCount: number
  responseRate: number
}

export interface NeedCount {
  category: string
  count: number
}

export interface EngagementSummary {
  month: string
  activeSurveyCount: number
  totalSurveyCount: number
  totalResponses: number
  surveyResponseRate: number
  averageRating: number
  caseResponseRate: number
  averageActivityScore: number
  topNeeds: NeedCount[]
  alerts: AlertItem[]
}

export interface IntegrationRun {
  id: number
  integrationType: string
  status: 'COMPLETED' | 'PARTIAL' | 'FAILED'
  fileName: string
  totalRows: number
  successfulRows: number
  failedRows: number
  startedBy: string
  completedAt: string
  errorSummary?: string
}

export interface IntegrationImportResult {
  run: IntegrationRun
  createdRows: number
  updatedRows: number
  errors: string[]
}

export interface SpreadsheetImportResult {
  run: IntegrationRun
  resource: string
  createdRows: number
  updatedRows: number
  errors: string[]
}

export interface UserAccount {
  id: number
  username: string
  fullName: string
  role: 'ADMIN' | 'USER'
  active: boolean
  unionUnitId?: number
  unionUnitCode?: string
  unionUnitName?: string
  lastLoginAt?: string
  createdAt: string
}
