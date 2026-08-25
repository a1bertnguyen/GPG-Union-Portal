import { useEffect, useState } from 'react'
import { api, currentMonth, enumLabel, formatDate, formatMoney } from '../api'
import type { BaseRecord, DashboardSummary, EngagementSummary, UnionUnit } from '../types'

export type DashboardKind = 'executive' | 'welfare' | 'cases' | 'activities' | 'finance' | 'voice'

type Props = { kind: DashboardKind }
type DashboardLoadResult = { summary?: DashboardSummary; engagement?: EngagementSummary; records?: BaseRecord[]; units?: UnionUnit[] }

const emptySummary: DashboardSummary = {
  unitCount: 0, activeMemberCount: 0, unionMemberCount: 0, welfareCompletionRate: 0,
  openCaseCount: 0, overdueCaseCount: 0, monthIncome: 0, monthExpense: 0,
  allTimeBalance: 0, pendingReportCount: 0, alerts: [],
}

const emptyEngagement: EngagementSummary = {
  month: '', activeSurveyCount: 0, totalSurveyCount: 0, totalResponses: 0,
  surveyResponseRate: 0, averageRating: 0, caseResponseRate: 0, averageActivityScore: 0,
  topNeeds: [], alerts: [],
}

const dashboardMeta: Record<DashboardKind, { eyebrow: string; title: string; description: string }> = {
  executive: { eyebrow: 'Dashboard điều hành', title: 'Tổng quan Ban lãnh đạo', description: 'Các chỉ số toàn hệ thống, công việc ưu tiên và vấn đề cần quyết định.' },
  welfare: { eyebrow: 'Dashboard chăm lo', title: 'Chăm lo & chính sách', description: 'Tiến độ thực hiện, cơ cấu đối tượng và hồ sơ cần hoàn thiện.' },
  cases: { eyebrow: 'Dashboard vụ việc', title: 'Kiến nghị & quan hệ lao động', description: 'Theo dõi SLA, mức độ nghiêm trọng, PIC và kết quả xử lý.' },
  activities: { eyebrow: 'Dashboard hoạt động', title: 'Chương trình công đoàn', description: 'Kế hoạch, mức tham gia, ngân sách và báo cáo sau chương trình.' },
  finance: { eyebrow: 'Dashboard tài chính', title: 'Thu – chi nội bộ', description: 'Tổng hợp dữ liệu do người dùng nhập; không kết nối ngân hàng hoặc dịch vụ thanh toán.' },
  voice: { eyebrow: 'Dashboard Employee Voice', title: 'Tiếng nói người lao động', description: 'Mức độ kết nối, tỷ lệ phản hồi và nhu cầu nổi bật trong kỳ.' },
}

export default function DashboardPage({ kind }: Props) {
  const [month, setMonth] = useState(currentMonth())
  const [summary, setSummary] = useState(emptySummary)
  const [engagement, setEngagement] = useState(emptyEngagement)
  const [records, setRecords] = useState<BaseRecord[]>([])
  const [units, setUnits] = useState<UnionUnit[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const request: Promise<DashboardLoadResult> = kind === 'executive'
      ? Promise.all([api<DashboardSummary>(`/dashboard?month=${month}`)]).then(([data]) => ({ summary: data }))
      : kind === 'voice'
        ? Promise.all([api<EngagementSummary>(`/engagement?month=${month}`)]).then(([data]) => ({ engagement: data }))
        : kind === 'welfare'
          ? Promise.all([api<BaseRecord[]>('/welfare')]).then(([data]) => ({ records: data }))
          : kind === 'cases'
            ? Promise.all([api<BaseRecord[]>('/cases')]).then(([data]) => ({ records: data }))
            : kind === 'activities'
              ? Promise.all([api<BaseRecord[]>('/activities')]).then(([data]) => ({ records: data }))
              : Promise.all([api<BaseRecord[]>('/finance'), api<UnionUnit[]>('/units')]).then(([data, unitData]) => ({ records: data, units: unitData }))

    request.then(result => {
      if (result.summary) setSummary(result.summary)
      if (result.engagement) setEngagement(result.engagement)
      if (result.records) setRecords(result.records)
      if (result.units) setUnits(result.units)
      setError('')
    }).catch(err => setError(err instanceof Error ? err.message : 'Không thể tổng hợp dashboard'))
      .finally(() => setLoading(false))
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
          {kind === 'voice' && <VoiceDashboard data={engagement} />}
        </>
      )}
    </section>
  )
}

function ExecutiveDashboard({ data, month }: { data: DashboardSummary; month: string }) {
  const reportCompletion = data.unitCount ? Math.round((data.unitCount - data.pendingReportCount) * 100 / data.unitCount) : 100
  return <div id="dashboard-executive">
    <MetricGrid cards={[
      ['CĐCS đang theo dõi', data.unitCount, 'Phạm vi tài khoản hiện tại', 'blue'],
      ['Đoàn viên', data.unionMemberCount, `${data.activeMemberCount} NLĐ đang hoạt động`, 'teal'],
      ['Chăm lo hoàn tất', `${data.welfareCompletionRate}%`, 'Mục tiêu từ 95%', 'green'],
      ['Vụ việc đang mở', data.openCaseCount, `${data.overdueCaseCount} vụ quá hạn`, 'orange'],
    ]} />
    <div className="task-strip" id="executive-priorities">
      <div><strong>{data.overdueCaseCount}</strong><span>Vụ việc quá hạn</span><small>Cần PIC và ETA mới</small></div>
      <div><strong>{data.pendingReportCount}</strong><span>Báo cáo chưa nộp</span><small>Kỳ {month}</small></div>
      <div><strong>{reportCompletion}%</strong><span>Đơn vị đã báo cáo</span><small>Mục tiêu 100%</small></div>
    </div>
    <div className="dashboard-grid">
      <article className="panel">
        <PanelHeading eyebrow="Tiến độ hệ thống" title="Mức độ hoàn thành" />
        <KpiLine label="Chăm lo hoàn tất" value={data.welfareCompletionRate} tone="green" />
        <KpiLine label="Đơn vị đã nộp báo cáo" value={reportCompletion} tone="blue" />
        <KpiLine label="Vụ việc trong hạn" value={data.openCaseCount ? 100 - Math.round(data.overdueCaseCount * 100 / data.openCaseCount) : 100} tone="teal" />
      </article>
      <article className="panel">
        <PanelHeading eyebrow="Điểm cần quyết định" title="Ưu tiên điều hành" count={data.alerts.length} />
        <AlertList alerts={data.alerts} empty="Không có vấn đề vượt ngưỡng trong kỳ." />
      </article>
    </div>
  </div>
}

function WelfareDashboard({ records, month }: { records: BaseRecord[]; month: string }) {
  const monthly = records.filter(item => String(item.eventDate ?? '').startsWith(month))
  const source = monthly
  const completed = source.filter(item => item.status === 'COMPLETED').length
  const incompleteDocuments = source.filter(item => item.documentStatus === 'INCOMPLETE').length
  const active = source.filter(item => !['COMPLETED', 'CANCELLED'].includes(String(item.status ?? '')))
  const completion = source.length ? Math.round(completed * 100 / source.length) : 100
  return <div id="dashboard-welfare">
    <MetricGrid cards={[
      ['Hồ sơ trong kỳ', source.length, `Kỳ ${month}`, 'blue'],
      ['Đã hoàn tất', `${completion}%`, `${completed} hồ sơ`, 'green'],
      ['Đang xử lý', active.length, 'Cần theo dõi đến khi đóng', 'teal'],
      ['Thiếu chứng từ', incompleteDocuments, 'Cần bổ sung hồ sơ', 'orange'],
    ]} />
    <div className="dashboard-grid">
      <article className="panel" id="welfare-breakdown"><PanelHeading eyebrow="Cơ cấu chăm lo" title="Hồ sơ theo nhóm" /><BreakdownList rows={grouped(source, 'welfareType')} /></article>
      <article className="panel"><PanelHeading eyebrow="Danh sách ưu tiên" title="Hồ sơ chưa hoàn tất" count={active.length} /><RecordList records={active.slice(0, 8)} primary="beneficiaryName" secondary="recordCode" meta="eventDate" empty="Không còn hồ sơ đang xử lý." /></article>
    </div>
  </div>
}

function CasesDashboard({ records, month }: { records: BaseRecord[]; month: string }) {
  const monthly = records.filter(item => String(item.receivedDate ?? '').startsWith(month))
  const source = monthly
  const open = source.filter(item => item.status !== 'CLOSED')
  const overdue = open.filter(item => String(item.deadline ?? '') < new Date().toISOString().slice(0, 10))
  const high = open.filter(item => ['HIGH', 'CRITICAL'].includes(String(item.severity ?? '')))
  const responded = source.filter(item => Boolean(item.resultText)).length
  const responseRate = source.length ? Math.round(responded * 100 / source.length) : 100
  const priorities = [...open].sort((left, right) => String(left.deadline ?? '').localeCompare(String(right.deadline ?? ''))).slice(0, 10)
  return <div id="dashboard-cases">
    <MetricGrid cards={[
      ['Vụ việc trong kỳ', source.length, `Tiếp nhận trong ${month}`, 'blue'],
      ['Đang mở', open.length, 'Chưa ở trạng thái đóng', 'teal'],
      ['Quá hạn', overdue.length, 'Cần cập nhật nguyên nhân và ETA', 'orange'],
      ['Có kết quả phản hồi', `${responseRate}%`, `${responded} hồ sơ có kết quả`, 'green'],
    ]} />
    <div className="dashboard-grid">
      <article className="panel"><PanelHeading eyebrow="Phân loại rủi ro" title="Mức độ vụ việc" /><BreakdownList rows={grouped(source, 'severity')} /></article>
      <article className="panel"><PanelHeading eyebrow="SLA & trách nhiệm" title="Tình trạng xử lý" /><KpiLine label="Có kết quả phản hồi" value={responseRate} tone="green" /><KpiLine label="Vụ việc trong hạn" value={open.length ? 100 - Math.round(overdue.length * 100 / open.length) : 100} tone="blue" /><KpiLine label="Không thuộc mức cao/nghiêm trọng" value={open.length ? 100 - Math.round(high.length * 100 / open.length) : 100} tone="teal" /></article>
    </div>
    <DashboardTable id="case-priorities" title="Vụ việc cần ưu tiên" columns={['Mã', 'Nhóm vấn đề', 'Mức độ', 'PIC', 'Deadline', 'Trạng thái']} rows={priorities.map(item => [String(item.caseCode ?? '—'), String(item.issueGroup ?? '—'), enumLabel(item.severity), String(item.ownerName ?? '—'), formatDate(item.deadline), enumLabel(item.status)])} />
  </div>
}

function ActivitiesDashboard({ records, month }: { records: BaseRecord[]; month: string }) {
  const monthly = records.filter(item => String(item.eventDate ?? '').startsWith(month))
  const source = monthly
  const completed = source.filter(item => item.status === 'COMPLETED')
  const missingReports = completed.filter(item => !item.reportCompleted)
  const plannedBudget = sum(source, 'plannedBudget')
  const actualCost = sum(source, 'actualCost')
  const participants = sum(source, 'participantCount')
  const scores = source.map(item => Number(item.usefulnessScore ?? 0)).filter(Boolean)
  const averageScore = scores.length ? (scores.reduce((total, value) => total + value, 0) / scores.length).toFixed(1) : '—'
  const upcoming = source.filter(item => ['PLANNED', 'APPROVED', 'IN_PROGRESS'].includes(String(item.status ?? ''))).sort((left, right) => String(left.eventDate ?? '').localeCompare(String(right.eventDate ?? ''))).slice(0, 10)
  return <div id="dashboard-activities">
    <MetricGrid cards={[
      ['Chương trình trong kỳ', source.length, `Kỳ ${month}`, 'blue'],
      ['Người tham dự', participants, 'Tổng lượt tham gia', 'teal'],
      ['Điểm hữu ích', averageScore === '—' ? averageScore : `${averageScore}/5`, 'Từ phản hồi sau chương trình', 'green'],
      ['Thiếu báo cáo sau CT', missingReports.length, 'Cần hoàn thiện đầu ra', 'orange'],
    ]} />
    <div className="dashboard-grid">
      <article className="panel"><PanelHeading eyebrow="Ngân sách" title="Kế hoạch và thực tế" /><div className="money-row"><div><span>Dự kiến</span><strong>{formatMoney(plannedBudget)}</strong></div><div><span>Thực tế</span><strong>{formatMoney(actualCost)}</strong></div></div><KpiLine label="Tỷ lệ sử dụng ngân sách" value={plannedBudget ? Math.round(actualCost * 100 / plannedBudget) : 0} tone="blue" /></article>
      <article className="panel"><PanelHeading eyebrow="Trạng thái" title="Tiến độ chương trình" /><BreakdownList rows={grouped(source, 'status')} /></article>
    </div>
    <DashboardTable id="activity-priorities" title="Chương trình đang triển khai" columns={['Mã', 'Chương trình', 'Đơn vị', 'Ngày', 'Ngân sách', 'Trạng thái']} rows={upcoming.map(item => [String(item.activityCode ?? '—'), String(item.name ?? '—'), item.unionUnit?.code ?? '—', formatDate(item.eventDate), formatMoney(Number(item.plannedBudget ?? 0)), enumLabel(item.status)])} />
  </div>
}

function FinanceDashboard({ records, units, month }: { records: BaseRecord[]; units: UnionUnit[]; month: string }) {
  const monthly = records.filter(item => String(item.transactionDate ?? '').startsWith(month))
  const income = sum(monthly.filter(item => item.entryType === 'INCOME'), 'amount')
  const expense = sum(monthly.filter(item => item.entryType === 'EXPENSE'), 'amount')
  const incomplete = monthly.filter(item => item.documentStatus === 'INCOMPLETE').length
  const unitRows = units.map(unit => {
    const entries = monthly.filter(item => item.unionUnit?.id === unit.id)
    return [unit.code, formatMoney(sum(entries.filter(item => item.entryType === 'INCOME'), 'amount')), formatMoney(sum(entries.filter(item => item.entryType === 'EXPENSE'), 'amount')), String(entries.filter(item => item.documentStatus === 'INCOMPLETE').length)]
  }).filter(row => row[1] !== formatMoney(0) || row[2] !== formatMoney(0))
  return <div id="dashboard-finance">
    <MetricGrid cards={[
      ['Tổng thu', formatMoney(income), `Kỳ ${month}`, 'green'],
      ['Tổng chi', formatMoney(expense), `Kỳ ${month}`, 'orange'],
      ['Chênh lệch trong kỳ', formatMoney(income - expense), 'Tính từ dữ liệu nhập nội bộ', 'blue'],
      ['Chứng từ chưa đủ', incomplete, `${monthly.length} phiếu trong kỳ`, 'orange'],
    ]} />
    <div className="dashboard-grid">
      <article className="panel" id="finance-breakdown"><PanelHeading eyebrow="Cơ cấu thu – chi" title="Theo nhóm nghiệp vụ" /><BreakdownList rows={groupedAmount(monthly, 'category').map(([label, value]) => [label, formatMoney(value)])} /></article>
      <article className="panel"><PanelHeading eyebrow="Kiểm soát hồ sơ" title="Mức độ hoàn thiện" /><KpiLine label="Chứng từ hợp lệ/không yêu cầu" value={monthly.length ? Math.round((monthly.length - incomplete) * 100 / monthly.length) : 100} tone="green" /><div className="boundary-note"><strong>Ranh giới hệ thống</strong><span>Không chuyển tiền, không truy vấn số dư ngân hàng và không lưu thông tin tài khoản thanh toán.</span></div></article>
    </div>
    <DashboardTable id="finance-units" title="Thu – chi theo đơn vị" columns={['Đơn vị', 'Thu', 'Chi', 'Chứng từ thiếu']} rows={unitRows} />
  </div>
}

function VoiceDashboard({ data }: { data: EngagementSummary }) {
  const maxNeed = Math.max(...data.topNeeds.map(item => item.count), 1)
  return <div id="dashboard-voice">
    <MetricGrid cards={[
      ['Tỷ lệ phản hồi', `${data.surveyResponseRate}%`, `${data.totalResponses} phản hồi trong kỳ`, 'blue'],
      ['Điểm kết nối', data.averageRating ? `${data.averageRating}/5` : '—', 'Mục tiêu từ 3,5/5', 'teal'],
      ['Kiến nghị có phản hồi', `${data.caseResponseRate}%`, 'Mục tiêu từ 90%', 'green'],
      ['Khảo sát đang mở', data.activeSurveyCount, `${data.totalSurveyCount} chiến dịch`, 'orange'],
    ]} />
    <div className="dashboard-grid">
      <article className="panel" id="voice-needs"><PanelHeading eyebrow="Top nhu cầu" title="NLĐ đang quan tâm" /><div className="need-list">{data.topNeeds.map(item => <div key={item.category}><div><strong>{item.category}</strong><span>{item.count} ý kiến</span></div><div className="progress-line"><div style={{ width: `${item.count * 100 / maxNeed}%` }} /></div></div>)}{!data.topNeeds.length && <div className="empty-state">Chưa có phản hồi trong kỳ.</div>}</div></article>
      <article className="panel"><PanelHeading eyebrow="Chỉ số cần theo dõi" title="Kết nối người lao động" count={data.alerts.length} /><AlertList alerts={data.alerts} empty="Các chỉ số đang trong ngưỡng theo dõi." /></article>
    </div>
  </div>
}

type MetricCardData = [string, string | number, string, string]
function MetricGrid({ cards }: { cards: MetricCardData[] }) {
  return <div className="metric-grid metric-grid--compact">{cards.map(([label, value, note, tone]) => <article className={`metric-card metric-card--${tone}`} key={label}><span>{label}</span><strong>{value}</strong><small>{note}</small></article>)}</div>
}

function PanelHeading({ eyebrow, title, count }: { eyebrow: string; title: string; count?: number }) {
  return <div className="panel__heading"><div><p className="eyebrow">{eyebrow}</p><h2>{title}</h2></div>{count !== undefined && <span className="alert-count">{count}</span>}</div>
}

function KpiLine({ label, value, tone }: { label: string; value: number; tone: string }) {
  const safeValue = Math.max(0, Math.min(value, 100))
  return <div className="kpi-line"><div><span>{label}</span><strong>{Math.round(value)}%</strong></div><div className="progress-line"><div className={`progress-fill progress-fill--${tone}`} style={{ width: `${safeValue}%` }} /></div></div>
}

function AlertList({ alerts, empty }: { alerts: DashboardSummary['alerts']; empty: string }) {
  return <div className="alert-list">{alerts.length ? alerts.map((alert, index) => <div key={`${alert.title}-${index}`} className={`alert-item alert-item--${alert.level}`}><i /><div><strong>{alert.title}</strong><span>{alert.detail}</span></div></div>) : <div className="empty-state">{empty}</div>}</div>
}

function BreakdownList({ rows }: { rows: Array<[string, string | number]> }) {
  return <div className="care-list">{rows.map(([label, value]) => <div key={label}><strong>{enumLabel(label)}</strong><span>{value}</span></div>)}{!rows.length && <div className="empty-state">Chưa có dữ liệu trong kỳ.</div>}</div>
}

function RecordList({ records, primary, secondary, meta, empty }: { records: BaseRecord[]; primary: string; secondary: string; meta: string; empty: string }) {
  return <div className="todo-list">{records.map(item => <div key={item.id}><i /><strong>{String(item[primary] ?? '—')}</strong><span>{String(item[secondary] ?? '—')} · {formatDate(item[meta])}</span></div>)}{!records.length && <div className="empty-state">{empty}</div>}</div>
}

function DashboardTable({ id, title, columns, rows }: { id: string; title: string; columns: string[]; rows: string[][] }) {
  return <div className="data-card dashboard-table" id={id}><div className="data-card__header"><div className="record-count"><strong>{title}</strong><span>{rows.length} bản ghi đang hiển thị</span></div></div><div className="table-wrap"><table><thead><tr>{columns.map(column => <th key={column}>{column}</th>)}</tr></thead><tbody>{rows.map((row, rowIndex) => <tr key={`${row[0]}-${rowIndex}`}>{row.map((cell, index) => <td key={`${columns[index]}-${cell}`}>{index === 0 ? <strong>{cell}</strong> : cell}</td>)}</tr>)}{!rows.length && <tr><td className="empty-cell" colSpan={columns.length}>Không có dữ liệu phù hợp kỳ đã chọn.</td></tr>}</tbody></table></div></div>
}

function grouped(records: BaseRecord[], field: string): Array<[string, number]> {
  const values = new Map<string, number>()
  records.forEach(item => {
    const key = String(item[field] ?? 'Chưa phân loại')
    values.set(key, (values.get(key) ?? 0) + 1)
  })
  return [...values.entries()].sort((left, right) => right[1] - left[1])
}

function groupedAmount(records: BaseRecord[], field: string): Array<[string, number]> {
  const values = new Map<string, number>()
  records.forEach(item => {
    const key = String(item[field] ?? 'Chưa phân loại')
    values.set(key, (values.get(key) ?? 0) + Number(item.amount ?? 0))
  })
  return [...values.entries()].sort((left, right) => right[1] - left[1])
}

function sum(records: BaseRecord[], field: string) {
  return records.reduce((total, item) => total + Number(item[field] ?? 0), 0)
}
