export type KpiPeriodType = 'MONTH' | 'QUARTER' | 'HALF_YEAR' | 'YEAR'

export type KpiResultStatus = 'CALCULATED' | 'NA' | 'MISSING_DATA' | 'FAILED_VALIDATION'
export type KpiRunStatus = 'DRAFT' | 'PROVISIONAL' | 'FINAL' | 'REOPENED'
export type KpiWarningSeverity = 'INFO' | 'WARNING' | 'CRITICAL'
export type KpiEvidenceRole = 'NUMERATOR' | 'DENOMINATOR' | 'EXCLUDED'
export type KpiEvidenceValidation = 'VALID' | 'INVALID' | 'PENDING'

export interface KpiEvidenceView {
  evidenceId: string
  resultId: string
  sourceModule: string
  sourceRecordId: string
  role: KpiEvidenceRole
  evidenceUrl: string | null
  fileName: string | null
  validationStatus: KpiEvidenceValidation
  redacted: boolean
}

export interface KpiEvidenceFieldView {
  label: string
  value: string
}

export interface KpiEvidenceAttachmentView {
  id: number
  fileName: string
  downloadPath: string
}

export interface KpiEvidenceRecordView {
  sourceModule: string
  sourceRecordId: string
  title: string
  fields: KpiEvidenceFieldView[]
  attachments: KpiEvidenceAttachmentView[]
}

export interface KpiDetailView {
  resultId: string
  kpiCode: string
  groupCode: string
  name: string
  weight: number
  numerator: number | null
  denominator: number | null
  targetValue: number | null
  normalizedScore: number | null
  eligibleWeight: number
  earnedPoints: number
  resultStatus: KpiResultStatus
  explanation: string
  evidence: KpiEvidenceView[]
}

export interface KpiGroupView {
  groupCode: string
  name: string
  configuredWeight: number
  eligibleWeight: number
  earnedPoints: number
  score: number | null
  status: string
}

export interface KpiWarningView {
  code: string
  severity: KpiWarningSeverity
  message: string
  recommendedAction: string | null
  dueAt: string | null
  sourceModule: string | null
  sourceRecordId: string | null
  redacted: boolean
}

export interface KpiAdjustmentAuditView {
  adjustmentId: number
  adjustmentType: 'BONUS' | 'PENALTY'
  penaltyCode: string | null
  points: number
  reason: string | null
  evidenceModule: string | null
  evidenceRecordId: string | null
  effectivenessVerified: boolean
  nonDuplicateVerified: boolean
  requestedBy: string | null
  approvedBy: string | null
  approvedAt: string
  redacted: boolean
}

export interface KpiUnitResultView {
  unionUnitId: number
  unionUnitCode: string
  unionUnitName: string
  activeMemberCount: number | null
  dataQualityRate: number
  baseScore: number
  bonusPoints: number
  penaltyPoints: number
  finalScore: number
  rawClassification: string
  finalClassification: string
  runStatus: KpiRunStatus
  rank: number | null
  reportOnTimeRate: number | null
  groups: KpiGroupView[]
  warnings: KpiWarningView[]
  adjustments: KpiAdjustmentAuditView[]
  details: KpiDetailView[]
}

export interface KpiDashboardSummary {
  averageScore: number
  finalUnitCount: number
  provisionalUnitCount: number
  excellentCount: number
  attentionCount: number
}

export interface KpiDashboardView {
  versionId: string
  periodType: KpiPeriodType
  periodStart: string
  periodEnd: string
  cutoffAt: string
  generatedAt: string
  summary: KpiDashboardSummary
  results: KpiUnitResultView[]
}

export interface KpiVersionWindowView {
  versionId: string
  name: string
  effectiveFrom: string
  effectiveTo: string | null
  status: string
}

export interface KpiMetadataView {
  versions: KpiVersionWindowView[]
}

export interface KpiDashboardParams {
  periodType: KpiPeriodType
  year: number
  period: number
  unitId?: number
}

export interface KpiPeriodOption {
  value: number
  label: string
}

export const KPI_PERIOD_TYPES: ReadonlyArray<{ value: KpiPeriodType; label: string }> = [
  { value: 'MONTH', label: 'Tháng' },
  { value: 'QUARTER', label: 'Quý' },
  { value: 'HALF_YEAR', label: '6 tháng' },
  { value: 'YEAR', label: 'Năm' },
]

export function kpiPeriodOptions(
  periodType: KpiPeriodType,
  year?: number,
  date = new Date(),
  versions?: ReadonlyArray<KpiVersionWindowView>,
): KpiPeriodOption[] {
  let options: KpiPeriodOption[]
  if (periodType === 'MONTH') {
    options = Array.from({ length: 12 }, (_, index) => ({ value: index + 1, label: `Tháng ${index + 1}` }))
  } else if (periodType === 'QUARTER') {
    options = Array.from({ length: 4 }, (_, index) => ({ value: index + 1, label: `Quý ${index + 1}` }))
  } else if (periodType === 'HALF_YEAR') {
    options = [
      { value: 1, label: '6 tháng đầu năm' },
      { value: 2, label: '6 tháng cuối năm' },
    ]
  } else {
    options = [{ value: 1, label: 'Cả năm' }]
  }

  if (year === undefined) return options
  const startedOptions = options.filter(option => periodDateRange(periodType, year, option.value).start <= date)
  if (versions === undefined) return startedOptions
  return startedOptions.filter(option => {
    const periodRange = periodDateRange(periodType, year, option.value)
    return versions.filter(version => versionCoversPeriod(version, periodRange)).length === 1
  })
}

function periodDateRange(periodType: KpiPeriodType, year: number, ordinal: number) {
  const startMonthIndex = periodType === 'MONTH'
    ? ordinal - 1
    : periodType === 'QUARTER'
      ? (ordinal - 1) * 3
      : periodType === 'HALF_YEAR'
        ? (ordinal - 1) * 6
        : 0
  const monthCount = periodType === 'MONTH' ? 1 : periodType === 'QUARTER' ? 3 : periodType === 'HALF_YEAR' ? 6 : 12
  return {
    start: new Date(year, startMonthIndex, 1),
    end: new Date(year, startMonthIndex + monthCount, 0),
  }
}

function versionCoversPeriod(
  version: KpiVersionWindowView,
  period: { start: Date; end: Date },
): boolean {
  const effectiveFrom = new Date(`${version.effectiveFrom}T00:00:00`)
  const effectiveTo = version.effectiveTo ? new Date(`${version.effectiveTo}T00:00:00`) : null
  if (Number.isNaN(effectiveFrom.getTime()) || effectiveTo && Number.isNaN(effectiveTo.getTime())) return false
  return effectiveFrom <= period.start && (effectiveTo === null || effectiveTo >= period.end)
}

export function defaultKpiPeriod(periodType: KpiPeriodType, date = new Date()): number {
  const month = date.getMonth() + 1
  if (periodType === 'MONTH') return month
  if (periodType === 'QUARTER') return Math.ceil(month / 3)
  if (periodType === 'HALF_YEAR') return month <= 6 ? 1 : 2
  return 1
}

export function kpiYearOptions(
  date = new Date(),
  versions?: ReadonlyArray<KpiVersionWindowView>,
): number[] {
  const currentYear = date.getFullYear()
  if (versions === undefined) return [currentYear]
  const availableYears = new Set<number>()

  // A year is selectable only when its calendar range intersects an approved version window.
  // Period-level clipping inside that year remains the responsibility of kpiPeriodOptions.
  versions.forEach(version => {
    const firstYear = Number(version.effectiveFrom.slice(0, 4))
    const configuredLastYear = version.effectiveTo
      ? Number(version.effectiveTo.slice(0, 4))
      : currentYear
    const lastYear = Math.min(currentYear, configuredLastYear)
    if (!Number.isInteger(firstYear) || !Number.isInteger(lastYear) || firstYear > lastYear) return
    for (let year = firstYear; year <= lastYear; year += 1) availableYears.add(year)
  })

  return [...availableYears].sort((left, right) => right - left)
}

export function kpiPeriodLabel(periodType: KpiPeriodType, year: number, period: number): string {
  const periodLabel = kpiPeriodOptions(periodType).find(option => option.value === period)?.label ?? `Kỳ ${period}`
  return periodType === 'YEAR' ? `Năm ${year}` : `${periodLabel}/${year}`
}

export function formatKpiNumber(value: number | null | undefined): string {
  return value === null || value === undefined || !Number.isFinite(value) ? '—' : value.toFixed(2)
}

export function formatKpiRate(value: number | null | undefined): string {
  return value === null || value === undefined || !Number.isFinite(value) ? '—' : `${(value * 100).toFixed(2)}%`
}

export function classificationTone(classification: string): string {
  if (classification === 'Xuất sắc') return 'excellent'
  if (classification === 'Tốt') return 'good'
  if (classification === 'Khá') return 'passed'
  if (classification === 'Trung bình') return 'average'
  return 'attention'
}

const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Bản nháp',
  PROVISIONAL: 'Tạm tính',
  FINAL: 'Chính thức',
  REOPENED: 'Đã mở lại',
  CALCULATED: 'Đã tính',
  NA: 'Không phát sinh',
  MISSING_DATA: 'Thiếu dữ liệu',
  FAILED_VALIDATION: 'Không đạt kiểm tra',
  VALID: 'Hợp lệ',
  INVALID: 'Không hợp lệ',
  PENDING: 'Chờ kiểm tra',
  NUMERATOR: 'Tử số',
  DENOMINATOR: 'Mẫu số',
  EXCLUDED: 'Bị loại',
  INFO: 'Thông tin',
  WARNING: 'Cảnh báo',
  CRITICAL: 'Nghiêm trọng',
}

export function kpiStatusLabel(status: string): string {
  return STATUS_LABELS[status] ?? status.replaceAll('_', ' ').toLocaleLowerCase('vi-VN')
}
