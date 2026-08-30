import { useState } from 'react'
import type { CSSProperties } from 'react'
import type { UnionUnit } from '../types'

type PeriodMode = 'quarter' | 'year'

type KpiCategory = {
  code: string
  label: string
  description: string
  score: number
  target: number
  change: number
}

type MockUnitKpi = {
  unitId: number
  unitCode: string
  unitName: string
  quarterlyScores: number[]
  memberCount: number
  reportProgress: number
  updatedAt: string
  categories: KpiCategory[]
}

type Props = {
  units: UnionUnit[]
  isAdmin: boolean
  currentUnitId?: number
  currentUnitCode?: string
  currentUnitName?: string
}

const FALLBACK_UNITS: UnionUnit[] = [
  { id: 1, code: 'VCS', name: 'CĐCS VCS', companyName: 'VCS', legalStatus: 'ACTIVE' },
  { id: 2, code: 'GPL', name: 'CĐCS GPL', companyName: 'GPL', legalStatus: 'ACTIVE' },
  { id: 3, code: 'AZC', name: 'CĐCS AZC', companyName: 'AZC', legalStatus: 'ACTIVE' },
  { id: 4, code: 'GPD', name: 'CĐCS GPD', companyName: 'GPD', legalStatus: 'ACTIVE' },
]

const MOCK_PROFILES = [
  { scores: [84, 88, 92, 94], members: 326, progress: 100, updatedAt: '28/08/2026 · 16:42' },
  { scores: [78, 82, 86, 89], members: 248, progress: 92, updatedAt: '28/08/2026 · 15:10' },
  { scores: [72, 76, 79, 83], members: 194, progress: 76, updatedAt: '27/08/2026 · 17:25' },
  { scores: [87, 90, 93, 95], members: 287, progress: 100, updatedAt: '29/08/2026 · 09:18' },
]

const CATEGORY_META = [
  { code: 'WELFARE', label: 'Chăm lo & phúc lợi', description: 'Hồ sơ đúng hạn, đủ chứng từ', target: 90, offset: 5 },
  { code: 'CASES', label: 'Vụ việc & kiến nghị', description: 'Phản hồi đúng SLA, có PIC xử lý', target: 85, offset: -3 },
  { code: 'ACTIVITIES', label: 'Hoạt động công đoàn', description: 'Tỷ lệ tham gia và mức hữu ích', target: 80, offset: 2 },
  { code: 'MEMBERS', label: 'Dữ liệu đoàn viên', description: 'Hồ sơ đầy đủ và cập nhật', target: 95, offset: 4 },
  { code: 'SURVEYS', label: 'Khảo sát & lắng nghe', description: 'Tỷ lệ phản hồi người lao động', target: 75, offset: -7 },
  { code: 'REPORTS', label: 'Báo cáo & tuân thủ', description: 'Nộp báo cáo đúng kỳ, đủ minh chứng', target: 90, offset: 1 },
]

const clampScore = (value: number) => Math.max(52, Math.min(100, value))
const average = (values: number[]) => values.length
  ? values.reduce((sum, value) => sum + value, 0) / values.length
  : 0

const ratingFor = (score: number) => {
  if (score >= 90) return 'Xuất sắc'
  if (score >= 80) return 'Tốt'
  if (score >= 65) return 'Đạt'
  return 'Cần cải thiện'
}

const ratingTone = (score: number) => {
  if (score >= 90) return 'excellent'
  if (score >= 80) return 'good'
  if (score >= 65) return 'passed'
  return 'attention'
}

const buildMockRows = (units: UnionUnit[], year: number, quarter: number): MockUnitKpi[] => {
  const yearOffset = (year - 2026) * 2
  return units.map((unit, index) => {
    const profile = MOCK_PROFILES[index % MOCK_PROFILES.length]
    const quarterlyScores = profile.scores.map(score => clampScore(score + yearOffset))
    const activeScore = quarterlyScores[quarter - 1]
    return {
      unitId: unit.id,
      unitCode: unit.code,
      unitName: unit.name,
      quarterlyScores,
      memberCount: profile.members + index * 11,
      reportProgress: clampScore(profile.progress + yearOffset),
      updatedAt: profile.updatedAt,
      categories: CATEGORY_META.map((category, categoryIndex) => ({
        code: category.code,
        label: category.label,
        description: category.description,
        target: category.target,
        score: clampScore(activeScore + category.offset + ((index + categoryIndex) % 3) - 1),
        change: ((index + categoryIndex) % 5) - 1,
      })),
    }
  })
}

const periodScore = (row: MockUnitKpi, mode: PeriodMode, quarter: number) => mode === 'quarter'
  ? row.quarterlyScores[quarter - 1]
  : Math.round(average(row.quarterlyScores))

const formatChange = (value: number) => `${value > 0 ? '+' : ''}${value.toFixed(1)}`

export default function KpiPage({ units, isAdmin, currentUnitId, currentUnitCode, currentUnitName }: Props) {
  const [periodMode, setPeriodMode] = useState<PeriodMode>('quarter')
  const [year, setYear] = useState(2026)
  const [quarter, setQuarter] = useState(3)
  const [unitId, setUnitId] = useState(isAdmin ? '' : String(currentUnitId ?? ''))
  const [selectedUnitId, setSelectedUnitId] = useState<number | null>(currentUnitId ?? null)

  const availableUnits = units.length ? units : FALLBACK_UNITS
  const ownUnit = availableUnits.find(unit => unit.id === currentUnitId) ?? {
    id: currentUnitId ?? -1,
    code: currentUnitCode ?? 'CĐCS',
    name: currentUnitName ?? 'CĐCS của bạn',
    companyName: currentUnitName ?? 'CĐCS của bạn',
    legalStatus: 'ACTIVE',
  }
  const roleScopedUnits = isAdmin ? availableUnits : [ownUnit]
  const mockRows = buildMockRows(roleScopedUnits, year, quarter)
  const visibleRows = unitId
    ? mockRows.filter(row => row.unitId === Number(unitId))
    : mockRows
  const rankedRows = [...visibleRows].sort(
    (left, right) => periodScore(right, periodMode, quarter) - periodScore(left, periodMode, quarter),
  )
  const selectedRow = rankedRows.find(row => row.unitId === selectedUnitId) ?? rankedRows[0]

  const summary = (() => {
    const scores = visibleRows.map(row => periodScore(row, periodMode, quarter))
    const previousScores = visibleRows.map(row => {
      if (periodMode === 'year') return Math.round(average(row.quarterlyScores.map(score => score - 3)))
      return row.quarterlyScores[Math.max(0, quarter - 2)]
    })
    return {
      average: average(scores),
      change: average(scores) - average(previousScores),
      reached: scores.filter(score => score >= 80).length,
      excellent: scores.filter(score => score >= 90).length,
      attention: scores.filter(score => score < 80).length,
      leader: rankedRows[0],
    }
  })()

  const trendValues = [0, 1, 2, 3].map(index => average(visibleRows.map(row => row.quarterlyScores[index])))
  const trendPoints = trendValues.map((value, index) => {
    const x = 50 + index * 184
    const y = 162 - (value - 60) * 3
    return `${x},${y}`
  }).join(' ')
  const distribution = {
    excellent: visibleRows.filter(row => periodScore(row, periodMode, quarter) >= 90).length,
    good: visibleRows.filter(row => {
      const score = periodScore(row, periodMode, quarter)
      return score >= 80 && score < 90
    }).length,
    passed: visibleRows.filter(row => {
      const score = periodScore(row, periodMode, quarter)
      return score >= 65 && score < 80
    }).length,
    attention: visibleRows.filter(row => periodScore(row, periodMode, quarter) < 65).length,
  }
  const totalDistribution = Math.max(visibleRows.length, 1)
  const excellentEnd = distribution.excellent / totalDistribution * 100
  const goodEnd = excellentEnd + distribution.good / totalDistribution * 100
  const passedEnd = goodEnd + distribution.passed / totalDistribution * 100
  const donutStyle = {
    '--kpi-donut': `conic-gradient(#1b9b70 0 ${excellentEnd}%, #2b65d8 ${excellentEnd}% ${goodEnd}%, #f0a038 ${goodEnd}% ${passedEnd}%, #de5a55 ${passedEnd}% 100%)`,
  } as CSSProperties
  const selectedScore = selectedRow ? periodScore(selectedRow, periodMode, quarter) : 0
  const reachedCategories = selectedRow?.categories.filter(category => category.score >= category.target).length ?? 0
  const periodLabel = periodMode === 'quarter' ? `Quý ${quarter}/${year}` : `Năm ${year}`

  return <section className="page-section kpi-report-page">
    <div className="kpi-report-heading">
      <div>
        <div className="kpi-report-heading__meta">
          <span className="kpi-live-dot" />
          <span>{isAdmin ? 'Trung tâm điều hành · Toàn hệ thống' : 'Báo cáo hiệu quả · CĐCS'}</span>
          <b>Dữ liệu mô phỏng</b>
        </div>
        <h1>Báo cáo KPI công đoàn</h1>
        <p>Theo dõi mức độ hoàn thành mục tiêu theo quý và cả năm, từ toàn hệ thống đến từng công đoàn cơ sở.</p>
      </div>
      <button className="button button--ghost kpi-print-button" onClick={() => window.print()}>
        <span aria-hidden="true">↗</span> Xuất báo cáo
      </button>
    </div>

    <div className="kpi-control-bar">
      <div className="kpi-period-switch" aria-label="Kiểu kỳ báo cáo">
        <button className={periodMode === 'quarter' ? 'is-active' : ''} onClick={() => setPeriodMode('quarter')}>Theo quý</button>
        <button className={periodMode === 'year' ? 'is-active' : ''} onClick={() => setPeriodMode('year')}>Theo năm</button>
      </div>
      <label className="kpi-control-field">
        <span>Năm báo cáo</span>
        <select value={year} onChange={event => setYear(Number(event.target.value))}>
          <option value={2026}>2026</option>
          <option value={2025}>2025</option>
          <option value={2024}>2024</option>
        </select>
      </label>
      {periodMode === 'quarter' && <div className="kpi-quarter-picker" aria-label="Chọn quý">
        {[1, 2, 3, 4].map(value => <button
          className={quarter === value ? 'is-active' : ''}
          key={value}
          onClick={() => setQuarter(value)}
        >Q{value}</button>)}
      </div>}
      <label className="kpi-control-field kpi-control-field--unit">
        <span>Phạm vi theo dõi</span>
        {isAdmin
          ? <select value={unitId} onChange={event => {
              setUnitId(event.target.value)
              setSelectedUnitId(event.target.value ? Number(event.target.value) : null)
            }}>
              <option value="">Tất cả công đoàn cơ sở</option>
              {roleScopedUnits.map(unit => <option key={unit.id} value={unit.id}>{unit.code} · {unit.name}</option>)}
            </select>
          : <strong>{currentUnitName ?? roleScopedUnits[0]?.name ?? 'CĐCS của bạn'}</strong>}
      </label>
      <div className="kpi-control-bar__stamp">
        <span>Kỳ đang xem</span>
        <strong>{periodLabel}</strong>
      </div>
    </div>

    <div className="kpi-summary-grid">
      <article className="kpi-summary-card kpi-summary-card--primary">
        <div className="kpi-summary-card__icon">KPI</div>
        <div><span>Điểm KPI bình quân</span><strong>{summary.average.toFixed(1)}</strong><small>/ 100 điểm</small></div>
        <b className={summary.change >= 0 ? 'is-positive' : 'is-negative'}>{formatChange(summary.change)} điểm</b>
      </article>
      <article className="kpi-summary-card">
        <div className="kpi-summary-card__icon kpi-summary-card__icon--green">✓</div>
        <div><span>Đơn vị đạt mục tiêu</span><strong>{summary.reached}<small>/{visibleRows.length}</small></strong><small>Từ 80 điểm trở lên</small></div>
        <b>{visibleRows.length ? Math.round(summary.reached / visibleRows.length * 100) : 0}%</b>
      </article>
      <article className="kpi-summary-card">
        <div className="kpi-summary-card__icon kpi-summary-card__icon--orange">★</div>
        <div><span>Đơn vị xuất sắc</span><strong>{summary.excellent}</strong><small>Từ 90 điểm trở lên</small></div>
        <b>{summary.leader?.unitCode ?? '—'}</b>
      </article>
      <article className="kpi-summary-card">
        <div className="kpi-summary-card__icon kpi-summary-card__icon--red">!</div>
        <div><span>Cần theo dõi</span><strong>{summary.attention}</strong><small>Chưa đạt mốc 80 điểm</small></div>
        <b>{summary.attention ? 'Cần hỗ trợ' : 'Ổn định'}</b>
      </article>
    </div>

    <div className="kpi-insight-grid">
      <article className="panel kpi-trend-panel">
        <header className="kpi-panel-heading">
          <div><span>Xu hướng toàn hệ thống</span><strong>Điểm KPI bình quân theo quý</strong></div>
          <div className="kpi-panel-legend"><i /> Năm {year}</div>
        </header>
        <div className="kpi-chart">
          <div className="kpi-chart__scale"><span>100</span><span>80</span><span>60</span></div>
          <svg viewBox="0 0 640 190" role="img" aria-label={`Xu hướng điểm KPI năm ${year}`} preserveAspectRatio="none">
            <defs>
              <linearGradient id="kpiArea" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#2864d7" stopOpacity=".2" />
                <stop offset="100%" stopColor="#2864d7" stopOpacity="0" />
              </linearGradient>
            </defs>
            <line x1="50" y1="42" x2="602" y2="42" />
            <line x1="50" y1="102" x2="602" y2="102" />
            <line x1="50" y1="162" x2="602" y2="162" />
            <polygon points={`50,162 ${trendPoints} 602,162`} fill="url(#kpiArea)" stroke="none" />
            <polyline points={trendPoints} className="kpi-chart__line" />
            {trendValues.map((value, index) => {
              const x = 50 + index * 184
              const y = 162 - (value - 60) * 3
              const active = periodMode === 'quarter' && quarter === index + 1
              return <g key={index} className={active ? 'is-active' : ''}>
                <circle cx={x} cy={y} r={active ? 7 : 5} />
                <text x={x} y={y - 14}>{value.toFixed(1)}</text>
                <text className="kpi-chart__quarter" x={x} y="185">Q{index + 1}</text>
              </g>
            })}
          </svg>
        </div>
        <footer>
          <span><i className="is-positive">↑</i> Tăng {Math.max(0, trendValues[3] - trendValues[0]).toFixed(1)} điểm so với đầu năm</span>
          <small>Kỳ có dữ liệu cập nhật gần nhất: Q3/{year}</small>
        </footer>
      </article>

      <article className="panel kpi-distribution-panel">
        <header className="kpi-panel-heading">
          <div><span>Cơ cấu xếp loại</span><strong>Phân bố kết quả {periodLabel}</strong></div>
        </header>
        <div className="kpi-donut-wrap">
          <div className="kpi-donut" style={donutStyle}>
            <div><strong>{visibleRows.length}</strong><span>đơn vị</span></div>
          </div>
          <div className="kpi-distribution-list">
            <div><i className="is-excellent" /><span>Xuất sắc</span><strong>{distribution.excellent}</strong></div>
            <div><i className="is-good" /><span>Tốt</span><strong>{distribution.good}</strong></div>
            <div><i className="is-passed" /><span>Đạt</span><strong>{distribution.passed}</strong></div>
            <div><i className="is-attention" /><span>Cần cải thiện</span><strong>{distribution.attention}</strong></div>
          </div>
        </div>
      </article>
    </div>

    <article className="panel kpi-ranking-panel">
      <header className="kpi-panel-heading">
        <div>
          <span>{isAdmin ? 'Theo dõi toàn hệ thống' : 'Kết quả công đoàn cơ sở'}</span>
          <strong>{isAdmin ? 'Xếp hạng KPI theo đơn vị' : `Chi tiết ${currentUnitName ?? selectedRow?.unitName ?? ''}`}</strong>
        </div>
        <small>Chọn một đơn vị để xem chi tiết nhóm chỉ tiêu</small>
      </header>
      <div className="table-wrap">
        <table className="kpi-ranking-table">
          <thead><tr><th>Hạng</th><th>Công đoàn cơ sở</th><th>Q1</th><th>Q2</th><th>Q3</th><th>Q4</th><th>Điểm {periodMode === 'quarter' ? `Q${quarter}` : 'năm'}</th><th>Xếp loại</th><th>Tiến độ báo cáo</th></tr></thead>
          <tbody>{rankedRows.map((row, index) => {
            const score = periodScore(row, periodMode, quarter)
            const previous = periodMode === 'quarter'
              ? row.quarterlyScores[Math.max(0, quarter - 2)]
              : average(row.quarterlyScores) - 3
            return <tr className={selectedRow?.unitId === row.unitId ? 'is-selected' : ''} key={row.unitId}>
              <td><span className={`kpi-rank kpi-rank--${Math.min(index + 1, 4)}`}>{index + 1}</span></td>
              <td><button className="kpi-unit-link" onClick={() => setSelectedUnitId(row.unitId)}><b>{row.unitCode}</b><span>{row.unitName}</span><small>{row.memberCount} đoàn viên</small></button></td>
              {row.quarterlyScores.map((quarterScore, quarterIndex) => <td key={quarterIndex}><span className={quarter === quarterIndex + 1 && periodMode === 'quarter' ? 'kpi-quarter-score is-current' : 'kpi-quarter-score'}>{quarterScore}</span></td>)}
              <td><div className="kpi-table-score"><strong>{score}</strong><span className={score - previous >= 0 ? 'is-positive' : 'is-negative'}>{score - previous >= 0 ? '↑' : '↓'} {Math.abs(score - previous).toFixed(1)}</span></div></td>
              <td><span className={`kpi-rating kpi-rating--${ratingTone(score)}`}>{ratingFor(score)}</span></td>
              <td><div className="kpi-report-progress"><div><i style={{ width: `${row.reportProgress}%` }} /></div><span>{row.reportProgress}%</span></div></td>
            </tr>
          })}</tbody>
        </table>
      </div>
    </article>

    {selectedRow && <div className="kpi-detail-grid">
      <article className="panel kpi-category-panel">
        <header className="kpi-panel-heading">
          <div><span>{selectedRow.unitCode} · {periodLabel}</span><strong>Hiệu quả theo nhóm chỉ tiêu</strong></div>
          <div className={`kpi-detail-score kpi-detail-score--${ratingTone(selectedScore)}`}><strong>{selectedScore}</strong><span>{ratingFor(selectedScore)}</span></div>
        </header>
        <div className="kpi-category-list">{selectedRow.categories.map(category => <div className="kpi-category-row" key={category.code}>
          <div className="kpi-category-row__heading"><div><strong>{category.label}</strong><span>{category.description}</span></div><div><b>{category.score}</b><small>/100</small></div></div>
          <div className="kpi-category-row__track" role="progressbar" aria-label={category.label} aria-valuemin={0} aria-valuemax={100} aria-valuenow={category.score}>
            <i className={category.score >= category.target ? 'is-met' : 'is-missed'} style={{ width: `${category.score}%` }} />
            <em style={{ left: `${category.target}%` }} />
          </div>
          <div className="kpi-category-row__meta"><span>Mục tiêu {category.target}</span><b className={category.change >= 0 ? 'is-positive' : 'is-negative'}>{category.change >= 0 ? '↑' : '↓'} {Math.abs(category.change)} điểm</b></div>
        </div>)}</div>
      </article>

      <aside className="panel kpi-followup-panel">
        <header className="kpi-panel-heading"><div><span>Gợi ý điều hành</span><strong>Ưu tiên kỳ tiếp theo</strong></div></header>
        <div className="kpi-goal-ring" style={{ '--goal-progress': `${reachedCategories / selectedRow.categories.length * 360}deg` } as CSSProperties}>
          <div><strong>{reachedCategories}/{selectedRow.categories.length}</strong><span>nhóm đạt mục tiêu</span></div>
        </div>
        <div className="kpi-followup-list">{[...selectedRow.categories]
          .sort((left, right) => (left.score - left.target) - (right.score - right.target))
          .slice(0, 3)
          .map((category, index) => <div key={category.code}><span>{index + 1}</span><div><strong>{category.label}</strong><small>{category.score >= category.target ? 'Duy trì chất lượng và bổ sung minh chứng' : `Còn thiếu ${category.target - category.score} điểm so với mục tiêu`}</small></div></div>)}</div>
        <footer><span>Cập nhật gần nhất</span><strong>{selectedRow.updatedAt}</strong></footer>
      </aside>
    </div>}
  </section>
}
