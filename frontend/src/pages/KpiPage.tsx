import { useCallback, useEffect, useMemo, useState } from 'react'
import { api } from '../api'
import type { UnionUnit, UnitKpi } from '../types'

const currentMonth = () => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

export default function KpiPage({ units }: { units: UnionUnit[] }) {
  const [month, setMonth] = useState(currentMonth)
  const [unitId, setUnitId] = useState('')
  const [rows, setRows] = useState<UnitKpi[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    const query = new URLSearchParams({ month })
    if (unitId) query.set('unitId', unitId)
    try {
      setRows(await api<UnitKpi[]>(`/kpi?${query}`))
    } catch (err) {
      setRows([])
      setError(err instanceof Error ? err.message : 'Không thể tính KPI CĐCS')
    } finally {
      setLoading(false)
    }
  }, [month, unitId])

  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { void load() }, [load])

  const summary = useMemo(() => {
    const average = rows.length ? Math.round(rows.reduce((sum, row) => sum + row.score, 0) / rows.length) : 0
    return {
      average,
      excellent: rows.filter(row => row.score >= 90).length,
      passed: rows.filter(row => row.score >= 65).length,
      attention: rows.filter(row => row.score < 65).length,
    }
  }, [rows])

  return <section className="page-section">
    <div className="page-heading">
      <div>
        <p className="eyebrow">Điều hành / Đánh giá định kỳ</p>
        <h1>Xét KPI từng công đoàn</h1>
        <p>Chấm tự động 10 tiêu chí theo dữ liệu chăm lo, vụ việc, khảo sát, hoạt động và đoàn viên của từng CĐCS.</p>
      </div>
      <div className="page-actions kpi-filters">
        <label><span>Tháng đánh giá</span><input type="month" value={month} onChange={event => setMonth(event.target.value)} /></label>
        <label><span>CĐCS</span><select value={unitId} onChange={event => setUnitId(event.target.value)}>
          <option value="">Tất cả CĐCS</option>
          {units.map(unit => <option key={unit.id} value={unit.id}>{unit.code} · {unit.name}</option>)}
        </select></label>
        <button className="button button--ghost" onClick={() => void load()}>Tính lại</button>
      </div>
    </div>

    {error && <div className="alert alert--danger">{error}</div>}

    <div className="metric-grid metric-grid--compact">
      <article className="metric-card metric-card--blue"><span>Điểm trung bình</span><strong>{summary.average}/100</strong><small>{rows.length} CĐCS trong kỳ</small></article>
      <article className="metric-card metric-card--green"><span>Xuất sắc</span><strong>{summary.excellent}</strong><small>Từ 90 điểm</small></article>
      <article className="metric-card metric-card--teal"><span>Đạt trở lên</span><strong>{summary.passed}</strong><small>Từ 65 điểm</small></article>
      <article className="metric-card metric-card--orange"><span>Cần cải thiện</span><strong>{summary.attention}</strong><small>Dưới 65 điểm</small></article>
    </div>

    <div className="notice">
      <strong>Nguyên tắc tính điểm</strong>
      <span>Mỗi tiêu chí đạt được 10 điểm. BCH họp định kỳ được nhận diện từ tên/mục tiêu hoạt động; hồ sơ đoàn viên đầy đủ cần có mã, họ tên, nơi làm việc và ngày vào làm.</span>
    </div>

    {loading
      ? <div className="empty-state">Đang tổng hợp KPI…</div>
      : !rows.length
        ? <div className="empty-state">Không có dữ liệu KPI phù hợp.</div>
        : <div className="kpi-unit-list">{rows.map(row => <article className="data-card kpi-unit-card" key={row.unionUnitId}>
            <header className="kpi-unit-card__header">
              <div>
                <span>{row.unionUnitCode} · Kỳ {row.month}</span>
                <h2>{row.unionUnitName}</h2>
                <small>{row.passedCriteria}/10 tiêu chí đạt</small>
              </div>
              <div className={`kpi-score kpi-score--${row.score >= 90 ? 'excellent' : row.score >= 65 ? 'passed' : 'attention'}`}>
                <strong>{row.score}</strong><span>/100</span><b>{row.rating}</b>
              </div>
            </header>
            <div className="kpi-criteria-grid">{row.criteria.map(criterion => <div className={`kpi-criterion${criterion.met ? ' kpi-criterion--met' : ' kpi-criterion--missed'}`} key={criterion.code}>
                <div className="kpi-criterion__status">{criterion.met ? '✓' : '!'}</div>
                <div>
                  <span>Mục tiêu {criterion.target}</span>
                  <strong>{criterion.label}</strong>
                  <small>{criterion.note}</small>
                </div>
                <b>{criterion.actualLabel}</b>
              </div>)}</div>
          </article>)}</div>}
  </section>
}
