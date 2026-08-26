import { useEffect, useState, type FormEvent } from 'react'
import { api, currentMonth, enumLabel, formatMoney } from '../api'
import ExcelImportActions from '../components/ExcelImportActions'
import { importSummary } from '../excel'
import type { MonthlySummary, UnionUnit } from '../types'

type Props = { units: UnionUnit[]; canManage?: boolean }

export default function ReportsPage({ units, canManage = true }: Props) {
  const [month, setMonth] = useState(currentMonth())
  const [unitId, setUnitId] = useState('')
  const [summary, setSummary] = useState<MonthlySummary | null>(null)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [saving, setSaving] = useState(false)
  const [preparedBy, setPreparedBy] = useState('')
  const [planNextMonth, setPlanNextMonth] = useState('')
  const [supportRequest, setSupportRequest] = useState('')
  const [status, setStatus] = useState('DRAFT')

  const load = () => {
    const query = new URLSearchParams({ month })
    if (unitId) query.set('unitId', unitId)
    api<MonthlySummary>(`/reports/monthly?${query}`)
      .then(result => {
        setSummary(result)
        setPreparedBy(result.narrative?.preparedBy ?? '')
        setPlanNextMonth(result.narrative?.planNextMonth ?? '')
        setSupportRequest(result.narrative?.supportRequest ?? '')
        setStatus(result.narrative?.status ?? 'DRAFT')
        setError('')
      })
      .catch(err => setError(err instanceof Error ? err.message : 'Không thể tải báo cáo'))
  }

  useEffect(load, [month, unitId])

  const save = async (event: FormEvent) => {
    event.preventDefault()
    if (!unitId) return
    setSaving(true)
    try {
      await api('/reports', {
        method: 'POST',
        body: JSON.stringify({ unionUnitId: Number(unitId), month, preparedBy, planNextMonth, supportRequest, status }),
      })
      load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể lưu báo cáo')
    } finally {
      setSaving(false)
    }
  }

  const removeReport = async () => {
    if (!summary?.narrative || !window.confirm(`Xóa nội dung báo cáo tháng ${summary.month} của ${summary.unionUnitName}?`)) return
    try {
      await api(`/reports/${summary.narrative.id}`, { method: 'DELETE' })
      setMessage('Đã xóa nội dung báo cáo tháng.')
      setError('')
      load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể xóa báo cáo')
    }
  }

  const metrics = summary ? [
    ['Đoàn viên / NLĐ', `${summary.unionMembers} / ${summary.activeEmployees}`],
    ['Chăm lo hoàn tất', `${summary.completedWelfareCases} / ${summary.welfareCases}`],
    ['Vụ việc đã đóng', `${summary.closedLaborCases} / ${summary.laborCases}`],
    ['Hoạt động / tham dự', `${summary.activities} / ${summary.participants}`],
  ] : []

  const exportCsv = () => {
    if (!summary) return
    const rows: Array<[string, string | number]> = [
      ['Kỳ báo cáo', summary.month], ['Đơn vị', summary.unionUnitName],
      ['Người lao động đang làm việc', summary.activeEmployees], ['Đoàn viên', summary.unionMembers],
      ['Hồ sơ chăm lo', summary.welfareCases], ['Chăm lo hoàn tất', summary.completedWelfareCases],
      ['Vụ việc', summary.laborCases], ['Vụ việc đã đóng', summary.closedLaborCases],
      ['Hoạt động', summary.activities], ['Người tham dự', summary.participants],
      ['Thu nội bộ', summary.income], ['Chi nội bộ', summary.expense], ['Biến động số dư', summary.netChange],
      ['Chứng từ thiếu', summary.incompleteDocuments], ['Trạng thái', summary.narrative?.status ?? 'DRAFT'],
      ['Người lập', summary.narrative?.preparedBy ?? ''], ['Kế hoạch tháng tới', summary.narrative?.planNextMonth ?? ''],
      ['Đề xuất hỗ trợ', summary.narrative?.supportRequest ?? ''],
    ]
    const cell = (value: string | number) => {
      const text = String(value)
      const safeText = /^[=+\-@]/.test(text) ? `'${text}` : text
      return `"${safeText.replaceAll('"', '""')}"`
    }
    const content = `\uFEFFChỉ tiêu,Giá trị\r\n${rows.map(row => row.map(cell).join(',')).join('\r\n')}`
    const url = URL.createObjectURL(new Blob([content], { type: 'text/csv;charset=utf-8' }))
    const link = document.createElement('a')
    link.href = url
    link.download = `bao-cao-${summary.month}-${unitId || 'toan-he-thong'}.csv`
    link.click()
    URL.revokeObjectURL(url)
  }

  return (
    <section className="page-section">
      <div className="page-heading">
        <div><p className="eyebrow">M01 · Báo cáo tháng</p><h1>Báo cáo công đoàn</h1><p>Tổng hợp tự động từ dữ liệu đã nhập, bổ sung kế hoạch và đề xuất của đơn vị.</p></div>
        <div className="report-filters">
          <label><span>Tháng</span><input type="month" value={month} onChange={event => setMonth(event.target.value)} /></label>
          <label><span>Đơn vị</span><select value={unitId} onChange={event => setUnitId(event.target.value)}><option value="">Toàn hệ thống</option>{units.map(unit => <option key={unit.id} value={unit.id}>{unit.code}</option>)}</select></label>
          {canManage && <ExcelImportActions resource="reports" filename="mau-bao-cao-thang.xlsx" importLabel="Nhập báo cáo" templateLabel="Mẫu Excel"
            onError={setError} onImported={async result => {
              const summary = importSummary(result)
              if (result.errors.length) setError(`${summary} Lỗi: ${result.errors.slice(0, 3).join(' · ')}`)
              else { setError(''); setMessage(summary) }
              load()
            }} />}
          <button className="button button--ghost" disabled={!summary} onClick={exportCsv}>Xuất CSV</button>
        </div>
      </div>
      {message && <div className="alert alert--success">{message}</div>}
      {error && <div className="alert alert--danger">{error}</div>}

      {summary && (
        <div id="report-monthly">
          <div className="report-title"><span>Báo cáo kỳ {summary.month}</span><h2>{summary.unionUnitName}</h2></div>
          <div className="report-metrics">{metrics.map(([label, value]) => <div key={label}><span>{label}</span><strong>{value}</strong></div>)}</div>
          <div className="dashboard-grid">
            <article className="panel">
              <div className="panel__heading"><div><p className="eyebrow">Thu – chi</p><h2>Tài chính trong kỳ</h2></div></div>
              <dl className="summary-list"><div><dt>Thu</dt><dd className="money--income">{formatMoney(summary.income)}</dd></div><div><dt>Chi</dt><dd className="money--expense">{formatMoney(summary.expense)}</dd></div><div><dt>Biến động số dư</dt><dd>{formatMoney(summary.netChange)}</dd></div><div><dt>Chứng từ thiếu</dt><dd>{summary.incompleteDocuments}</dd></div></dl>
            </article>

            <article className="panel">
              <div className="panel__heading"><div><p className="eyebrow">Kết luận</p><h2>Trạng thái báo cáo</h2></div></div>
              <div className="report-status"><span className={`status status--${summary.narrative?.status === 'SUBMITTED' ? 'success' : 'warning'}`}>{enumLabel(summary.narrative?.status ?? 'DRAFT')}</span><p>{summary.narrative ? `Người lập: ${summary.narrative.preparedBy}` : 'Chọn một đơn vị để lập và nộp báo cáo.'}</p></div>
            </article>
          </div>
        </div>
      )}

      {canManage && <article className="panel report-form-panel" id="report-narrative">
        <div className="panel__heading"><div><p className="eyebrow">Nội dung đơn vị</p><h2>Kế hoạch và đề xuất</h2></div></div>
        {!unitId ? <div className="empty-state">Chọn một CĐCS để nhập nội dung báo cáo.</div> : (
          <form className="form-grid" onSubmit={event => void save(event)}>
            <label className="field"><span>Người lập *</span><input required value={preparedBy} onChange={event => setPreparedBy(event.target.value)} /></label>
            <label className="field"><span>Trạng thái *</span><select value={status} onChange={event => setStatus(event.target.value)}><option value="DRAFT">Bản nháp</option><option value="SUBMITTED">Đã nộp</option><option value="APPROVED">Đã duyệt</option></select></label>
            <label className="field field--wide"><span>Kế hoạch tháng tới</span><textarea value={planNextMonth} onChange={event => setPlanNextMonth(event.target.value)} /></label>
            <label className="field field--wide"><span>Đề xuất / yêu cầu hỗ trợ</span><textarea value={supportRequest} onChange={event => setSupportRequest(event.target.value)} /></label>
            <div className="form-actions field--wide">{summary?.narrative && <button type="button" className="button button--danger" onClick={() => void removeReport()}>Xóa báo cáo</button>}<button className="button button--primary" disabled={saving}>{saving ? 'Đang lưu…' : 'Lưu báo cáo'}</button></div>
          </form>
        )}
      </article>}
    </section>
  )
}
