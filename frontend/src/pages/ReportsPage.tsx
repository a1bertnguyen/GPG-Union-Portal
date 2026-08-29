import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { api, apiAll, currentMonth, formatMoney } from '../api'
import ExcelImportActions from '../components/ExcelImportActions'
import { importSummary } from '../excel'
import type { LaborCase, MonthlyReport, MonthlySummary, UnionUnit, WelfareRecord } from '../types'

type Props = {
  units: UnionUnit[]
  isAdmin: boolean
  currentUnitId?: number
  currentUnitName?: string
  currentUserName: string
}

type ActivityReportItem = {
  id: number
  name: string
  eventDate: string
  participantCount: number
  actualCost: number
  usefulnessScore?: number
}

const MONTHLY_REPORT_REQUIREMENTS = [
  ['A. Thông tin chung', 'Tên Công ty; kỳ báo cáo; Chủ tịch CĐCS; người lập; ngày gửi'],
  ['B. Đoàn viên/NLĐ', 'Tổng NLĐ; tổng đoàn viên; tăng/giảm; tỷ lệ tham gia'],
  ['C. Chăm lo', 'Sinh nhật; hiếu; hỷ; thăm hỏi; sinh con; khó khăn; đúng hạn/chậm'],
  ['D. Kiến nghị NLĐ', 'Mã vụ việc; nhóm vấn đề; PIC; deadline; trạng thái; kết quả'],
  ['E. Hoạt động', 'Tên chương trình; số người; chi phí; tỷ lệ tham gia; phản hồi'],
  ['F. Thu – chi', 'Thu kỳ này; chi; số dư; chứng từ chưa hoàn thiện'],
  ['G. Kế hoạch tháng tới', 'Hoạt động; thời gian; PIC; ngân sách dự kiến'],
  ['H. Đề xuất/kiến nghị', 'Nội dung cần CĐ GPG/Ban CSNLĐ/BLĐ hỗ trợ'],
] as const

const WELFARE_LABELS: Record<string, string> = {
  BIRTHDAY: 'Sinh nhật', FUNERAL: 'Hiếu', WEDDING: 'Hỷ', VISIT: 'Thăm hỏi',
  CHILDBIRTH: 'Sinh con', HARDSHIP: 'Khó khăn',
}

const statusTone = (status?: string) => status === 'APPROVED' ? 'success' : status === 'SUBMITTED' ? 'neutral' : 'warning'

const reportStatusLabel = (status?: string) => {
  if (status === 'APPROVED') return 'ADMIN đã duyệt'
  if (status === 'SUBMITTED') return 'Đã nộp ADMIN'
  if (status === 'DRAFT') return 'Bản nháp chưa nộp'
  return 'Chưa có báo cáo'
}

const formatSubmittedAt = (value?: string) => value
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
  : '—'

const previousMonthOf = (value: string) => {
  const [year, month] = value.split('-').map(Number)
  return new Date(Date.UTC(year, month - 2, 1)).toISOString().slice(0, 7)
}

export default function ReportsPage({ units, isAdmin, currentUnitId, currentUnitName, currentUserName }: Props) {
  const [month, setMonth] = useState(currentMonth())
  const [unitId, setUnitId] = useState(isAdmin ? '' : String(currentUnitId ?? ''))
  const [summary, setSummary] = useState<MonthlySummary | null>(null)
  const [previousSummary, setPreviousSummary] = useState<MonthlySummary | null>(null)
  const [laborCases, setLaborCases] = useState<LaborCase[]>([])
  const [welfareRecords, setWelfareRecords] = useState<WelfareRecord[]>([])
  const [activities, setActivities] = useState<ActivityReportItem[]>([])
  const [reports, setReports] = useState<MonthlyReport[]>([])
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [saving, setSaving] = useState(false)
  const [preparedBy, setPreparedBy] = useState(currentUserName)
  const [planNextMonth, setPlanNextMonth] = useState('')
  const [supportRequest, setSupportRequest] = useState('')

  const loadSummary = useCallback(async () => {
    const query = new URLSearchParams({ month })
    if (unitId) query.set('unitId', unitId)
    try {
      const result = await api<MonthlySummary>(`/reports/monthly?${query}`)
      setSummary(result)
      setPreparedBy(result.narrative?.preparedBy ?? currentUserName)
      setPlanNextMonth(result.narrative?.planNextMonth ?? '')
      setSupportRequest(result.narrative?.supportRequest ?? '')
      setError('')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tải báo cáo')
    }
  }, [currentUserName, month, unitId])

  const loadReports = useCallback(async () => {
    try {
      setReports(await api<MonthlyReport[]>('/reports'))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tải danh sách báo cáo')
    }
  }, [])

  const loadAdminInsights = useCallback(async () => {
    try {
      const [previous, cases] = await Promise.all([
        api<MonthlySummary>(`/reports/monthly?month=${previousMonthOf(month)}`),
        apiAll<LaborCase>('/cases'),
      ])
      setPreviousSummary(previous)
      setLaborCases(cases)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tải dữ liệu điều hành')
    }
  }, [month])

  const loadUserReportDetails = useCallback(async () => {
    if (isAdmin) return
    try {
      const [welfare, activityRows] = await Promise.all([
        apiAll<WelfareRecord>('/welfare'),
        apiAll<ActivityReportItem>('/activities'),
      ])
      setWelfareRecords(welfare)
      setActivities(activityRows)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tải chi tiết báo cáo tháng')
    }
  }, [isAdmin])

  useEffect(() => {
    // Fetch callbacks update state only after their awaited API requests resolve.
    // oxlint-disable-next-line react/set-state-in-effect
    void Promise.all([loadSummary(), loadReports(), loadAdminInsights(), loadUserReportDetails()])
  }, [loadAdminInsights, loadReports, loadSummary, loadUserReportDetails])

  const refresh = async () => {
    await Promise.all([loadSummary(), loadReports(), loadAdminInsights(), loadUserReportDetails()])
  }

  const save = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!unitId) return
    const submitter = (event.nativeEvent as SubmitEvent).submitter as HTMLButtonElement | null
    const status = submitter?.value === 'SUBMITTED' ? 'SUBMITTED' : 'DRAFT'
    setSaving(true)
    try {
      await api('/reports', {
        method: 'POST',
        body: JSON.stringify({ unionUnitId: Number(unitId), month, preparedBy, planNextMonth, supportRequest, status }),
      })
      setMessage(status === 'SUBMITTED' ? 'Đã nộp báo cáo tháng cho ADMIN duyệt.' : 'Đã lưu bản nháp báo cáo tháng.')
      setError('')
      await refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể lưu báo cáo')
    } finally {
      setSaving(false)
    }
  }

  const removeDraft = async () => {
    const report = summary?.narrative
    if (!report || report.status !== 'DRAFT' || !window.confirm(`Xóa bản nháp tháng ${summary.month}?`)) return
    try {
      await api(`/reports/${report.id}`, { method: 'DELETE' })
      setMessage('Đã xóa bản nháp báo cáo tháng.')
      setError('')
      await refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể xóa bản nháp')
    }
  }

  const approve = async (report: MonthlyReport) => {
    setSaving(true)
    try {
      await api(`/reports/${report.id}/approve`, { method: 'POST' })
      setMessage(`Đã duyệt báo cáo tháng ${month} của ${report.unionUnit.code}.`)
      setError('')
      await refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể duyệt báo cáo')
    } finally {
      setSaving(false)
    }
  }

  const monthReports = useMemo(() => reports.filter(report => report.reportMonth.slice(0, 7) === month), [month, reports])
  const reportByUnit = useMemo(() => new Map(monthReports.map(report => [report.unionUnit.id, report])), [monthReports])
  const receivedCount = monthReports.filter(report => report.status === 'SUBMITTED' || report.status === 'APPROVED').length
  const draftCount = monthReports.filter(report => report.status === 'DRAFT').length
  const missingCount = Math.max(0, units.length - receivedCount)
  const narrative = summary?.narrative
  const reportLocked = narrative?.status === 'SUBMITTED' || narrative?.status === 'APPROVED'
  const unfinishedWelfare = summary ? summary.welfareCases - summary.completedWelfareCases : 0
  const openCases = summary ? summary.laborCases - summary.closedLaborCases : 0
  const overdueCases = laborCases.filter(item => item.status !== 'CLOSED' && item.deadline && item.deadline < new Date().toISOString().slice(0, 10))
  const unitCaseSignals = useMemo(() => {
    const signals = new Map<number, { unit: UnionUnit; overdue: number; issueCounts: Map<string, number> }>()
    laborCases.forEach(item => {
      if (!item.unionUnit) return
      const current = signals.get(item.unionUnit.id) ?? { unit: item.unionUnit, overdue: 0, issueCounts: new Map<string, number>() }
      if (item.status !== 'CLOSED' && item.deadline && item.deadline < new Date().toISOString().slice(0, 10)) current.overdue += 1
      current.issueCounts.set(item.issueGroup, (current.issueCounts.get(item.issueGroup) ?? 0) + 1)
      signals.set(item.unionUnit.id, current)
    })
    return [...signals.values()].map(item => ({
      unit: item.unit,
      overdue: item.overdue,
      repeated: [...item.issueCounts.values()].reduce((total, count) => total + Math.max(0, count - 1), 0),
    })).filter(item => item.overdue > 0 || item.repeated > 0)
      .sort((left, right) => (right.overdue * 2 + right.repeated) - (left.overdue * 2 + left.repeated))
  }, [laborCases])
  const supportSuggestions = monthReports.filter(report =>
    report.status !== 'DRAFT' && Boolean(report.supportRequest?.trim()))
  const welfareCompletionRate = summary?.welfareCases
    ? Math.round(summary.completedWelfareCases * 100 / summary.welfareCases)
    : 100
  const caseClosureRate = summary?.laborCases
    ? Math.round(summary.closedLaborCases * 100 / summary.laborCases)
    : 100
  const averageParticipation = summary?.activities
    ? Math.round(summary.participants / summary.activities)
    : 0
  const trendText = (current: number, previous?: number) => {
    if (previous === undefined) return 'Chưa có dữ liệu tháng trước'
    const difference = current - previous
    return `${difference >= 0 ? '+' : ''}${difference} so với tháng trước`
  }
  const selectedUnit = units.find(unit => unit.id === Number(unitId))
  const monthWelfare = welfareRecords.filter(item => item.eventDate.slice(0, 7) === month)
  const welfareCounts = monthWelfare.reduce<Record<string, number>>((counts, item) => {
    counts[item.welfareType] = (counts[item.welfareType] ?? 0) + 1
    return counts
  }, {})
  const welfareBreakdown = Object.entries(welfareCounts)
    .map(([type, count]) => `${WELFARE_LABELS[type] ?? type}: ${count}`)
    .join(' · ')
  const welfareOverdue = monthWelfare.filter(item =>
    item.status !== 'COMPLETED' && Boolean(item.deadline) && item.deadline! < new Date().toISOString().slice(0, 10)).length
  const monthActivities = activities.filter(item => item.eventDate.slice(0, 7) === month)
  const featuredActivity = monthActivities[0]
  const membershipRate = summary?.activeEmployees
    ? Math.round(summary.unionMembers * 100 / summary.activeEmployees)
    : 0
  const monthLabel = `${month.slice(5, 7)}/${month.slice(0, 4)}`
  const activityOutput = featuredActivity
    ? `${monthActivities.length} chương trình · ${featuredActivity.name} – ${featuredActivity.participantCount} người – ${formatMoney(featuredActivity.actualCost)} – ${featuredActivity.usefulnessScore ?? 'chưa đánh giá'}/5`
    : 'Chưa có hoạt động trong kỳ báo cáo'
  const monthlyReportOutputs = summary ? [
    `${selectedUnit?.companyName ?? summary.unionUnitName} – Báo cáo tháng ${monthLabel}; Chủ tịch: ${selectedUnit?.chairperson || 'chưa cập nhật'}; người lập: ${narrative?.preparedBy || currentUserName}; ngày gửi: ${formatSubmittedAt(narrative?.submittedAt)}`,
    `${summary.activeEmployees} NLĐ / ${summary.unionMembers} đoàn viên / ${summary.memberChanges} biến động / ${membershipRate}% tham gia`,
    `${summary.welfareCases} trường hợp – ${summary.completedWelfareCases} hoàn tất / ${welfareOverdue} quá hạn${welfareBreakdown ? ` · ${welfareBreakdown}` : ''}`,
    `${summary.laborCases} vụ việc: ${summary.closedLaborCases} đóng / ${openCases} đang xử lý`,
    activityOutput,
    `Thu ${formatMoney(summary.income)} – Chi ${formatMoney(summary.expense)} – Chênh lệch ${formatMoney(summary.netChange)} – ${summary.incompleteDocuments} chứng từ thiếu`,
    narrative?.planNextMonth || 'Chưa cập nhật kế hoạch tháng tới',
    narrative?.supportRequest || 'Không có đề xuất/kiến nghị hỗ trợ',
  ] : []
  const monthlyReportRows = MONTHLY_REPORT_REQUIREMENTS.map(([group, requirement], index) => ({
    group, requirement, output: monthlyReportOutputs[index] ?? 'Đang tổng hợp dữ liệu…',
  }))

  const reportExportPath = `/spreadsheets/reports/export.xlsx?month=${encodeURIComponent(month)}${unitId ? `&unitId=${encodeURIComponent(unitId)}` : ''}`
  const reportExcelActions = <ExcelImportActions resource="reports" filename={`bao-cao-thang-${month}.xlsx`}
    downloadPath={reportExportPath} importLabel="Nhập Excel" templateLabel="Xuất Excel"
    onError={setError} onImported={async result => {
      const resultMessage = importSummary(result)
      if (result.errors.length) setError(`${resultMessage} Lỗi: ${result.errors.slice(0, 3).join(' · ')}`)
      else { setError(''); setMessage(resultMessage) }
      await refresh()
    }} />

  const reminder = isAdmin
    ? `Đã nhận ${receivedCount}/${units.length} báo cáo. Còn ${missingCount} đơn vị chưa nộp; ${draftCount} đơn vị đang lưu nháp.`
    : narrative?.status === 'APPROVED'
      ? `Báo cáo tháng ${month} đã được ADMIN duyệt.`
      : narrative?.status === 'SUBMITTED'
        ? `Báo cáo tháng ${month} đã nộp và đang chờ ADMIN duyệt.`
        : narrative?.status === 'DRAFT'
          ? `Bản nháp tháng ${month} chưa được nộp. Hãy hoàn tất 8 nhóm nội dung A–H bên dưới.`
          : `Đến kỳ báo cáo tháng ${month}. Hãy rà soát 8 nhóm nội dung A–H và nộp cho ADMIN.`

  if (isAdmin) return (
    <section className="page-section">
      <div className="page-heading">
        <div><p className="eyebrow">Báo cáo dành riêng cho ADMIN</p><h1>Báo cáo điều hành hệ sinh thái</h1><p>Tổng hợp xu hướng, hiệu quả, ngân sách và các rủi ro cần quyết định trên toàn bộ CĐCS.</p></div>
        <div className="report-filters">
          <label><span>Tháng</span><input type="month" value={month} onChange={event => setMonth(event.target.value)} /></label>
          {reportExcelActions}
        </div>
      </div>

      {message && <div className="alert alert--success">{message}</div>}
      {error && <div className="alert alert--danger">{error}</div>}
      <div className="notice report-reminder"><span><strong>Tiến độ nhận báo cáo:</strong> {reminder}</span><span className="status status--neutral">Toàn hệ sinh thái</span></div>

      {summary && <div className="admin-report-grid">
        <article className="panel data-card admin-report-card">
          <div className="panel__heading"><div><p className="eyebrow">01 · Toàn cảnh</p><h2>Xu hướng toàn Hệ sinh thái</h2></div></div>
          <dl className="summary-list"><div><dt>Đoàn viên / NLĐ</dt><dd>{summary.unionMembers} / {summary.activeEmployees}</dd></div><div><dt>Biến động đoàn viên</dt><dd>{summary.memberChanges} <small>{trendText(summary.memberChanges, previousSummary?.memberChanges)}</small></dd></div><div><dt>Vụ việc phát sinh</dt><dd>{summary.laborCases} <small>{trendText(summary.laborCases, previousSummary?.laborCases)}</small></dd></div><div><dt>Lượt tham gia hoạt động</dt><dd>{summary.participants} <small>{trendText(summary.participants, previousSummary?.participants)}</small></dd></div></dl>
        </article>

        <article className="panel data-card admin-report-card">
          <div className="panel__heading"><div><p className="eyebrow">02 · Điểm nóng</p><h2>Đơn vị có vấn đề lặp lại/quá hạn</h2></div></div>
          <div className="admin-report-list">{unitCaseSignals.slice(0, 5).map(item => <div key={item.unit.id}><span><strong>{item.unit.code}</strong><small>{item.unit.name}</small></span><b>{item.overdue} quá hạn · {item.repeated} lặp lại</b></div>)}{!unitCaseSignals.length && <p>Không có đơn vị phát sinh vấn đề lặp lại hoặc quá hạn.</p>}</div>
        </article>

        <article className="panel data-card admin-report-card">
          <div className="panel__heading"><div><p className="eyebrow">03 · Hiệu quả</p><h2>Hiệu quả chính sách/chương trình</h2></div></div>
          <dl className="summary-list"><div><dt>Chăm lo hoàn tất</dt><dd>{welfareCompletionRate}%</dd></div><div><dt>Kiến nghị/vụ việc đã đóng</dt><dd>{caseClosureRate}%</dd></div><div><dt>Chương trình thực hiện</dt><dd>{summary.activities}</dd></div><div><dt>Bình quân tham gia/chương trình</dt><dd>{averageParticipation}</dd></div></dl>
        </article>

        <article className="panel data-card admin-report-card">
          <div className="panel__heading"><div><p className="eyebrow">04 · Nguồn lực</p><h2>Tình hình ngân sách và chăm lo</h2></div></div>
          <dl className="summary-list"><div><dt>Thu trong kỳ</dt><dd className="money--income">{formatMoney(summary.income)}</dd></div><div><dt>Chi gồm tạm ứng</dt><dd className="money--expense">{formatMoney(summary.expense)}</dd></div><div><dt>Chênh lệch số dư</dt><dd>{formatMoney(summary.netChange)}</dd></div><div><dt>Chăm lo hoàn tất / còn lại</dt><dd>{summary.completedWelfareCases} / {unfinishedWelfare}</dd></div></dl>
        </article>

        <article className="panel data-card admin-report-card admin-report-card--risk">
          <div className="panel__heading"><div><p className="eyebrow">05 · Quyết định</p><h2>Rủi ro cần quyết định</h2></div></div>
          <dl className="summary-list"><div><dt>Vụ việc đang quá hạn</dt><dd>{overdueCases.length}</dd></div><div><dt>Vụ việc đang mở</dt><dd>{openCases}</dd></div><div><dt>Chứng từ chưa đủ</dt><dd>{summary.incompleteDocuments}</dd></div><div><dt>CĐCS chưa nộp báo cáo</dt><dd>{missingCount}</dd></div></dl>
        </article>

        <article className="panel data-card admin-report-card">
          <div className="panel__heading"><div><p className="eyebrow">06 · Điều chỉnh</p><h2>Đề xuất chính sách/nguồn lực</h2></div></div>
          <div className="admin-report-list">{supportSuggestions.slice(0, 5).map(report => <div key={report.id}><span><strong>{report.unionUnit.code}</strong><small>{report.preparedBy}</small></span><p>{report.supportRequest}</p></div>)}{!supportSuggestions.length && <p>Chưa có đề xuất hỗ trợ từ các báo cáo đã nộp.</p>}</div>
        </article>
      </div>}

      <article className="panel data-card report-admin-overview">
        <div className="panel__heading"><div><p className="eyebrow">Theo dõi nộp báo cáo</p><h2>Tình trạng từng CĐCS · {month}</h2></div></div>
        <div className="table-wrap"><table><thead><tr><th>CĐCS</th><th>Trạng thái</th><th>Người lập</th><th>Thời điểm nộp</th><th /></tr></thead><tbody>
          {units.map(unit => {
            const report = reportByUnit.get(unit.id)
            return <tr key={unit.id}><td><strong>{unit.code}</strong><br /><small>{unit.name}</small></td><td><span className={`status status--${statusTone(report?.status)}`}>{reportStatusLabel(report?.status)}</span></td><td>{report?.preparedBy ?? '—'}</td><td>{formatSubmittedAt(report?.submittedAt)}</td><td className="actions-cell">{report?.status === 'SUBMITTED' && <button className="icon-button icon-button--view" disabled={saving} onClick={() => void approve(report)}>Duyệt</button>}</td></tr>
          })}
        </tbody></table></div>
      </article>
    </section>
  )

  return (
    <section className="page-section">
      <div className="page-heading">
        <div>
          <p className="eyebrow">M01 · Báo cáo tháng tại CĐCS</p>
          <h1>Báo cáo điều hành CĐCS</h1>
          <p>Cùng cấu trúc điều hành của ADMIN, nhưng toàn bộ số liệu được giới hạn trong CĐCS của tài khoản.</p>
        </div>
        <div className="report-filters">
          <label><span>Tháng</span><input type="month" value={month} onChange={event => setMonth(event.target.value)} /></label>
          {isAdmin
            ? <label><span>Đơn vị</span><select value={unitId} onChange={event => setUnitId(event.target.value)}><option value="">Toàn hệ thống</option>{units.map(unit => <option key={unit.id} value={unit.id}>{unit.code} · {unit.name}</option>)}</select></label>
            : <label><span>CĐCS</span><input readOnly value={currentUnitName ?? summary?.unionUnitName ?? ''} /></label>}
          {reportExcelActions}
        </div>
      </div>

      {message && <div className="alert alert--success">{message}</div>}
      {error && <div className="alert alert--danger">{error}</div>}
      <div className={`${!isAdmin && (narrative?.status === 'SUBMITTED' || narrative?.status === 'APPROVED') ? 'alert alert--success' : 'notice'} report-reminder`}>
        <span><strong>Nhắc báo cáo tháng:</strong> {reminder}</span>
        {narrative && <span className={`status status--${statusTone(narrative.status)}`}>{reportStatusLabel(narrative.status)}</span>}
      </div>

      {summary && <div className="admin-report-grid">
        <article className="panel data-card admin-report-card">
          <div className="panel__heading"><div><p className="eyebrow">01 · Toàn cảnh</p><h2>Xu hướng tại CĐCS</h2></div></div>
          <dl className="summary-list"><div><dt>Đoàn viên / NLĐ</dt><dd>{summary.unionMembers} / {summary.activeEmployees}</dd></div><div><dt>Biến động đoàn viên</dt><dd>{summary.memberChanges} <small>{trendText(summary.memberChanges, previousSummary?.memberChanges)}</small></dd></div><div><dt>Vụ việc phát sinh</dt><dd>{summary.laborCases} <small>{trendText(summary.laborCases, previousSummary?.laborCases)}</small></dd></div><div><dt>Lượt tham gia hoạt động</dt><dd>{summary.participants} <small>{trendText(summary.participants, previousSummary?.participants)}</small></dd></div></dl>
        </article>

        <article className="panel data-card admin-report-card">
          <div className="panel__heading"><div><p className="eyebrow">02 · Điểm nóng</p><h2>Vấn đề lặp lại/quá hạn</h2></div></div>
          <div className="admin-report-list">{unitCaseSignals.map(item => <div key={item.unit.id}><span><strong>{item.unit.code}</strong><small>{item.unit.name}</small></span><b>{item.overdue} quá hạn · {item.repeated} lặp lại</b></div>)}{!unitCaseSignals.length && <p>Không phát sinh vấn đề lặp lại hoặc quá hạn tại CĐCS.</p>}</div>
        </article>

        <article className="panel data-card admin-report-card">
          <div className="panel__heading"><div><p className="eyebrow">03 · Hiệu quả</p><h2>Hiệu quả chính sách/chương trình</h2></div></div>
          <dl className="summary-list"><div><dt>Chăm lo hoàn tất</dt><dd>{welfareCompletionRate}%</dd></div><div><dt>Kiến nghị/vụ việc đã đóng</dt><dd>{caseClosureRate}%</dd></div><div><dt>Chương trình thực hiện</dt><dd>{summary.activities}</dd></div><div><dt>Bình quân tham gia/chương trình</dt><dd>{averageParticipation}</dd></div></dl>
        </article>

        <article className="panel data-card admin-report-card">
          <div className="panel__heading"><div><p className="eyebrow">04 · Nguồn lực</p><h2>Tình hình ngân sách và chăm lo</h2></div></div>
          <dl className="summary-list"><div><dt>Thu trong kỳ</dt><dd className="money--income">{formatMoney(summary.income)}</dd></div><div><dt>Chi gồm tạm ứng</dt><dd className="money--expense">{formatMoney(summary.expense)}</dd></div><div><dt>Chênh lệch số dư</dt><dd>{formatMoney(summary.netChange)}</dd></div><div><dt>Chăm lo hoàn tất / còn lại</dt><dd>{summary.completedWelfareCases} / {unfinishedWelfare}</dd></div></dl>
        </article>

        <article className="panel data-card admin-report-card admin-report-card--risk">
          <div className="panel__heading"><div><p className="eyebrow">05 · Xử lý</p><h2>Rủi ro cần theo dõi</h2></div></div>
          <dl className="summary-list"><div><dt>Vụ việc đang quá hạn</dt><dd>{overdueCases.length}</dd></div><div><dt>Vụ việc đang mở</dt><dd>{openCases}</dd></div><div><dt>Chứng từ chưa đủ</dt><dd>{summary.incompleteDocuments}</dd></div><div><dt>Chăm lo chưa hoàn tất</dt><dd>{unfinishedWelfare}</dd></div></dl>
        </article>

        <article className="panel data-card admin-report-card">
          <div className="panel__heading"><div><p className="eyebrow">06 · Tháng tới</p><h2>Kế hoạch và đề xuất hỗ trợ</h2></div></div>
          <div className="admin-report-list"><div><span><strong>Kế hoạch tháng tới</strong><small>{narrative?.preparedBy || currentUserName}</small></span><p>{narrative?.planNextMonth || 'Chưa cập nhật kế hoạch tháng tới.'}</p></div><div><span><strong>Đề xuất hỗ trợ</strong><small>Gửi ADMIN</small></span><p>{narrative?.supportRequest || 'Chưa có đề xuất hỗ trợ.'}</p></div></div>
        </article>
      </div>}

      <article className="panel data-card monthly-report-template" id="report-monthly">
        <div className="panel__heading"><div><p className="eyebrow">Mẫu báo cáo tháng · {monthLabel}</p><h2>{summary?.unionUnitName ?? currentUnitName}</h2></div></div>
        <div className="table-wrap monthly-report-table"><table><thead><tr><th>Nhóm nội dung</th><th>Chỉ tiêu / dữ liệu bắt buộc</th><th>Kết quả kỳ báo cáo</th></tr></thead><tbody>
          {monthlyReportRows.map(row => <tr key={row.group}><td><strong>{row.group}</strong></td><td>{row.requirement}</td><td>{row.output}</td></tr>)}
        </tbody></table></div>
      </article>

      {!isAdmin && <article className="panel data-card report-form-panel" id="report-narrative">
        <div className="panel__heading"><div><p className="eyebrow">Nội dung CĐCS</p><h2>Kế hoạch tháng tới và đề xuất hỗ trợ</h2></div></div>
        {!unitId ? <div className="empty-state">Tài khoản chưa được gán CĐCS, không thể lập báo cáo.</div> : reportLocked ? <div className="alert alert--success">{narrative?.status === 'APPROVED' ? 'Báo cáo đã được duyệt.' : 'Báo cáo đã nộp ADMIN và được khóa chỉnh sửa trong lúc chờ duyệt.'}</div> : (
          <form className="form-grid" onSubmit={event => void save(event)}>
            <label className="field"><span>Người lập *</span><input required value={preparedBy} onChange={event => setPreparedBy(event.target.value)} /></label>
            <label className="field"><span>Kỳ báo cáo</span><input readOnly value={month} /></label>
            <label className="field field--wide"><span>Kế hoạch tháng tới *</span><textarea required value={planNextMonth} onChange={event => setPlanNextMonth(event.target.value)} /></label>
            <label className="field field--wide"><span>Đề xuất / yêu cầu hỗ trợ</span><textarea value={supportRequest} onChange={event => setSupportRequest(event.target.value)} /></label>
            <div className="form-actions field--wide">
              {narrative?.status === 'DRAFT' && <button type="button" className="button button--danger" onClick={() => void removeDraft()}>Xóa bản nháp</button>}
              <button className="button button--ghost" name="reportAction" value="DRAFT" disabled={saving}>{saving ? 'Đang lưu…' : 'Lưu bản nháp'}</button>
              <button className="button button--primary" name="reportAction" value="SUBMITTED" disabled={saving}>{saving ? 'Đang nộp…' : 'Nộp báo cáo cho ADMIN'}</button>
            </div>
          </form>
        )}
      </article>}
    </section>
  )
}
