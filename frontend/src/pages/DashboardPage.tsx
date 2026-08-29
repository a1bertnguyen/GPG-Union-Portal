import { useEffect, useState } from 'react'
import { currentMonth } from '../api'
import { apiAllCached, apiCached } from '../apiCache'
import type { BaseRecord, DashboardSummary, UnionUnit } from '../types'
import { CasesDashboard, ExecutiveDashboard, WelfareDashboard } from './dashboard/OperationalDashboardViews'
import { ActivitiesDashboard, FinanceDashboard } from './dashboard/ProgramDashboardViews'

export type DashboardKind = 'executive' | 'welfare' | 'cases' | 'activities' | 'finance'

type Props = { kind: DashboardKind }
type DashboardLoadResult = { summary?: DashboardSummary; records?: BaseRecord[]; units?: UnionUnit[] }

const emptySummary: DashboardSummary = {
  unitCount: 0, activeMemberCount: 0, unionMemberCount: 0, welfareCompletionRate: 0,
  openCaseCount: 0, overdueCaseCount: 0, monthIncome: 0, monthExpense: 0,
  pendingReportCount: 0, alerts: [],
}

const dashboardMeta: Record<DashboardKind, { eyebrow: string; title: string; description: string }> = {
  executive: { eyebrow: 'Dashboard điều hành', title: 'Tổng quan Ban lãnh đạo', description: 'Các chỉ số toàn hệ thống, công việc ưu tiên và vấn đề cần quyết định.' },
  welfare: { eyebrow: 'Dashboard chăm lo', title: 'Chăm lo & chính sách', description: 'Tiến độ thực hiện, cơ cấu đối tượng và hồ sơ cần hoàn thiện.' },
  cases: { eyebrow: 'Dashboard vụ việc', title: 'Kiến nghị & quan hệ lao động', description: 'Theo dõi SLA, mức độ nghiêm trọng, PIC và kết quả xử lý.' },
  activities: { eyebrow: 'Dashboard hoạt động', title: 'Chương trình công đoàn', description: 'Kế hoạch, mức tham gia, ngân sách và báo cáo sau chương trình.' },
  finance: { eyebrow: 'Dashboard tài chính', title: 'Thu • Chi • Tạm ứng', description: 'Tổng hợp phiếu và tình trạng chứng từ; không kết nối ngân hàng hoặc dịch vụ thanh toán.' },
}

export default function DashboardPage({ kind }: Props) {
  const [month, setMonth] = useState(currentMonth())
  const [summary, setSummary] = useState(emptySummary)
  const [records, setRecords] = useState<BaseRecord[]>([])
  const [units, setUnits] = useState<UnionUnit[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    const options = { signal: controller.signal }
    // These dashboards aggregate a whole period client-side. Cached requests keep one in-flight
    // request per account/query, so remounting a tab or React StrictMode does not duplicate it.
    const request: Promise<DashboardLoadResult> = kind === 'executive'
      ? apiCached<DashboardSummary>(`/dashboard?month=${month}`, options).then(data => ({ summary: data }))
      : kind === 'welfare'
          ? apiAllCached<BaseRecord>('/welfare', { month }, options).then(data => ({ records: data }))
          : kind === 'cases'
            ? apiAllCached<BaseRecord>('/cases', {}, options).then(data => ({ records: data }))
            : kind === 'activities'
              ? apiAllCached<BaseRecord>('/activities', { month }, options).then(data => ({ records: data }))
              : Promise.all([
                  apiAllCached<BaseRecord>('/finance', { month }, options),
                  apiAllCached<UnionUnit>('/units', {}, options),
                ])
                  .then(([data, unitData]) => ({ records: data, units: unitData }))

    request.then(result => {
      if (controller.signal.aborted) return
      if (result.summary) setSummary(result.summary)
      if (result.records) setRecords(result.records)
      if (result.units) setUnits(result.units)
      setError('')
    }).catch(err => {
      if (!controller.signal.aborted) {
        setError(err instanceof Error ? err.message : 'Không thể tổng hợp dashboard')
      }
    }).finally(() => {
      if (!controller.signal.aborted) setLoading(false)
    })

    return () => controller.abort()
  }, [kind, month])

  const meta = dashboardMeta[kind]

  return (
    <section className="page-section dashboard-page">
      <div className="page-heading">
        <div><p className="eyebrow">{meta.eyebrow}</p><h1>{meta.title}</h1><p>{meta.description}</p></div>
        <label className="month-picker"><span>Kỳ dữ liệu</span><input type="month" value={month} onChange={event => { setLoading(true); setMonth(event.target.value) }} /></label>
      </div>
      {error && <div className="alert alert--danger">Không thể tải dữ liệu. Vui lòng thử lại hoặc liên hệ quản trị hệ thống.</div>}
      {loading ? <div className="loading-panel">Đang tổng hợp dữ liệu…</div> : (
        <>
          {kind === 'executive' && <ExecutiveDashboard data={summary} month={month} />}
          {kind === 'welfare' && <WelfareDashboard records={records} month={month} />}
          {kind === 'cases' && <CasesDashboard records={records} month={month} />}
          {kind === 'activities' && <ActivitiesDashboard records={records} month={month} />}
          {kind === 'finance' && <FinanceDashboard records={records} units={units} month={month} />}
        </>
      )}
    </section>
  )
}
