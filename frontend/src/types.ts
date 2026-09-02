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

/** Envelope every list endpoint returns. Mirrors `ApiModels.PageResponse` on the server. */
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/**
 * Whole-dataset numbers for a filtered list, from `GET {resource}/facets`.
 *
 * `total` is the grand total for the caller's CĐCS scope ignoring filters — use
 * `PageResponse.totalElements` for the filtered count. `metrics` is computed over the filtered set
 * and holds raw numbers only; labels, tones and money formatting stay here in the frontend.
 */
export interface ListFacets {
  total: number
  statusValues: string[]
  metrics: Record<string, number>
}

/** Per-issue-group rollup behind the case analytics bars, from `GET /cases/issue-groups`. */
export interface CaseGroupCount {
  issueGroup: string
  count: number
  affectedPeople: number
  overdue: number
}

export interface BaseRecord {
  id: number
  unionUnit?: UnionUnit
  [key: string]: unknown
}

export interface WelfarePolicy extends BaseRecord {
  code: string
  source: 'UNION' | 'COMPANY'
  sequenceNumber: number
  welfareType: string
  name: string
  supportAmount: number
  eligibilityNotes?: string
  processingWeeks: number
  active: boolean
}

export interface WelfareRecord extends BaseRecord {
  recordCode: string
  policyId?: number
  policyName?: string
  welfareType: string
  beneficiaryName: string
  eventDate: string
  deadline?: string
  status: string
  amount: number
  standardAmount?: number
  documentStatus: string
  receiptStatus: string
  hasImage: boolean
  notes?: string
}

export interface LaborCase extends BaseRecord {
  caseCode: string
  receivedDate: string
  requesterName: string
  employeeCode?: string
  jobTitle?: string
  workplace?: string
  startWorkDate?: string
  leaveDate?: string
  phone?: string
  source?: string
  issueGroup: string
  severity: string
  ownerName?: string
  deadline?: string
  status: string
  description: string
  affectedPeople: number
  attachmentNote?: string
  resultText?: string
  responseDate?: string
  overdueReason?: string
  approvedBy?: string
  approvedAt?: string
}

export interface LaborCaseDocument {
  id: number
  laborCaseId: number
  caseCode: string
  fileName: string
  contentType: string
  fileSize: number
  uploadedBy: string
  createdAt: string
}

export interface WelfareDocument {
  id: number
  welfareRecordId: number
  recordCode: string
  documentType: 'SUPPORTING_DOCUMENT' | 'RECEIPT' | 'IMAGE'
  fileName: string
  contentType: string
  fileSize: number
  uploadedBy: string
  createdAt: string
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
  pendingReportCount: number
  alerts: AlertItem[]
}

export interface MonthlyReport {
  id: number
  unionUnit: UnionUnit
  reportMonth: string
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
  memberChanges: number
  welfareCases: number
  completedWelfareCases: number
  laborCases: number
  closedLaborCases: number
  activities: number
  participants: number
  income: number
  expense: number
  advance: number
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

export interface MemberChange {
  id: number
  memberId: number
  employeeCode: string
  memberName: string
  unionUnit: UnionUnit
  changeType: string
  effectiveDate: string
  description: string
  recordedBy: string
  createdAt: string
}

export interface MemberDocument {
  id: number
  memberId: number
  employeeCode: string
  memberName: string
  unionUnit: UnionUnit
  documentType: 'JOIN_APPLICATION' | 'DECISION' | 'BCH_DOCUMENT'
  fileName: string
  contentType: string
  fileSize: number
  uploadedBy: string
  createdAt: string
}

/**
 * One member's required-document status, from `GET /member-documents/compliance`.
 * Built server-side because the grid used to cross-join every member against every document.
 */
export interface MemberCompliance {
  memberId: number
  employeeCode: string
  memberName: string
  unionUnit: UnionUnit
  documents: MemberDocument[]
  missing: MemberDocument['documentType'][]
}

export interface ActivityMedia {
  id: number
  activityId: number
  activityCode: string
  activityName: string
  unionUnit: UnionUnit
  mediaType: 'PHOTO' | 'DOCUMENT'
  title?: string
  fileName: string
  contentType: string
  fileSize: number
  uploadedBy: string
  createdAt: string
}

export interface FinanceDocument {
  id: number
  financeEntryId: number
  entryCode: string
  fileName: string
  contentType: string
  fileSize: number
  uploadedBy: string
  createdAt: string
}

export interface DocumentLibraryItem {
  id: number
  unionUnit: UnionUnit
  category: string
  title: string
  description?: string
  fileName: string
  contentType: string
  fileSize: number
  uploadedBy: string
  createdAt: string
}

export interface KpiCriterion {
  code: string
  label: string
  target: string
  actual: number
  actualLabel: string
  met: boolean
  note: string
}

export interface UnitKpi {
  unionUnitId: number
  unionUnitCode: string
  unionUnitName: string
  month: string
  score: number
  rating: string
  passedCriteria: number
  criteria: KpiCriterion[]
}
