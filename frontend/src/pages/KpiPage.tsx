import { useEffect, useRef, useState } from 'react'
import { KpiHistoryPanel } from './KpiHistoryPanel'
import {
  downloadKpiEvidenceAttachment,
  loadKpiDashboard,
  loadKpiEvidence,
  loadKpiMetadata,
} from '../kpiApi'
import {
  classificationTone,
  defaultKpiPeriod,
  formatKpiNumber,
  formatKpiRate,
  KPI_PERIOD_TYPES,
  kpiPeriodLabel,
  kpiPeriodOptions,
  kpiStatusLabel,
  kpiYearOptions,
} from '../kpiModel'
import type {
  KpiDashboardView,
  KpiDetailView,
  KpiEvidenceAttachmentView,
  KpiEvidenceRecordView,
  KpiEvidenceView,
  KpiMetadataView,
  KpiPeriodType,
  KpiUnitResultView,
} from '../kpiModel'
import type { UnionUnit } from '../types'

type Props = {
  units: UnionUnit[]
  isAdmin: boolean
  currentUnitId?: number
  currentUnitCode?: string
  currentUnitName?: string
}

const timestampFormatter = new Intl.DateTimeFormat('vi-VN', {
  dateStyle: 'short',
  timeStyle: 'short',
})

function formatTimestamp(value: string | undefined): string {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : timestampFormatter.format(date)
}

function visualPercent(value: number | null): string {
  if (value === null || !Number.isFinite(value)) return '0%'
  return `${Math.max(0, Math.min(100, value))}%`
}

function formatMemberCount(value: number | null): string {
  return value === null ? '—' : value.toLocaleString('vi-VN')
}

function selectUnit(
  unit: KpiUnitResultView,
  setSelectedUnitId: (value: number) => void,
  setSelectedGroupCode: (value: string | null) => void,
  setSelectedKpiCode: (value: string | null) => void,
) {
  setSelectedUnitId(unit.unionUnitId)
  setSelectedGroupCode(unit.groups[0]?.groupCode ?? null)
  setSelectedKpiCode(null)
}

function KpiEvidencePanel({ detail }: { detail: KpiDetailView | undefined }) {
  const evidenceRequestRef = useRef<AbortController | null>(null)
  const [activeEvidence, setActiveEvidence] = useState<KpiEvidenceView | null>(null)
  const [evidenceRecord, setEvidenceRecord] = useState<KpiEvidenceRecordView | null>(null)
  const [evidenceLoading, setEvidenceLoading] = useState(false)
  const [evidenceError, setEvidenceError] = useState('')
  const [downloadingAttachmentId, setDownloadingAttachmentId] = useState<number | null>(null)
  const [downloadError, setDownloadError] = useState('')

  useEffect(() => () => evidenceRequestRef.current?.abort(), [])

  const openEvidence = (evidence: KpiEvidenceView) => {
    if (evidence.redacted || !evidence.evidenceUrl) return
    evidenceRequestRef.current?.abort()
    const controller = new AbortController()
    evidenceRequestRef.current = controller
    setActiveEvidence(evidence)
    setEvidenceRecord(null)
    setEvidenceLoading(true)
    setEvidenceError('')
    setDownloadError('')

    loadKpiEvidence(evidence.evidenceUrl, controller.signal).then(response => {
      if (controller.signal.aborted) return
      setEvidenceRecord(response)
    }).catch(reason => {
      if (controller.signal.aborted) return
      setEvidenceError(reason instanceof Error ? reason.message : 'Không thể tải chi tiết bản ghi chứng minh.')
    }).finally(() => {
      if (controller.signal.aborted || evidenceRequestRef.current !== controller) return
      evidenceRequestRef.current = null
      setEvidenceLoading(false)
    })
  }

  const downloadAttachment = async (attachment: KpiEvidenceAttachmentView) => {
    setDownloadingAttachmentId(attachment.id)
    setDownloadError('')
    try {
      await downloadKpiEvidenceAttachment(attachment)
    } catch (reason) {
      setDownloadError(reason instanceof Error ? reason.message : 'Không thể tải tệp chứng minh.')
    } finally {
      setDownloadingAttachmentId(null)
    }
  }

  if (!detail) {
    return <div className="kpi-evidence-empty">Chọn một KPI để xem cách tính và bản ghi chứng minh.</div>
  }

  return <div className="kpi-evidence-panel">
    <div className="kpi-evidence-heading">
      <div><span>{detail.kpiCode}</span><strong>{detail.name}</strong></div>
      <span className={`kpi-status kpi-status--${detail.resultStatus.toLowerCase()}`}>{kpiStatusLabel(detail.resultStatus)}</span>
    </div>
    <p>{detail.explanation || 'Engine chưa cung cấp giải thích cho KPI này.'}</p>
    <dl className="kpi-calculation-grid">
      <div><dt>Đã làm</dt><dd>{formatKpiNumber(detail.numerator)}</dd></div>
      <div><dt>Tổng cần làm</dt><dd>{formatKpiNumber(detail.denominator)}</dd></div>
      <div><dt>Mục tiêu</dt><dd>{formatKpiNumber(detail.targetValue)}</dd></div>
      <div><dt>Điểm chuẩn hóa</dt><dd>{formatKpiNumber(detail.normalizedScore)}</dd></div>
      <div><dt>Trọng số cấu hình</dt><dd>{formatKpiNumber(detail.weight)}</dd></div>
      <div><dt>Trọng số hợp lệ</dt><dd>{formatKpiNumber(detail.eligibleWeight)}</dd></div>
      <div><dt>Điểm đạt được</dt><dd>{formatKpiNumber(detail.earnedPoints)}</dd></div>
    </dl>
    <div className="kpi-evidence-list">
      <div className="kpi-evidence-list__title">
        <strong>Bản ghi chứng minh</strong>
        <span>{detail.evidence.length} liên kết nguồn</span>
      </div>
      {detail.evidence.length === 0
        ? <div className="kpi-evidence-empty">KPI này chưa có bản ghi chứng minh được phép hiển thị.</div>
        : detail.evidence.map(evidence => {
            const canOpen = !evidence.redacted && Boolean(evidence.evidenceUrl)
            const isActive = activeEvidence?.evidenceId === evidence.evidenceId
            const content = <>
              <div>
                <strong>{evidence.sourceModule}</strong>
                <span>{evidence.redacted ? 'Mã hồ sơ đã ẩn theo quyền truy cập' : `Mã nguồn: ${evidence.sourceRecordId}`}</span>
              </div>
              <div className="kpi-evidence-tags">
                <span>{kpiStatusLabel(evidence.role)}</span>
                <span className={`is-${evidence.validationStatus.toLowerCase()}`}>{kpiStatusLabel(evidence.validationStatus)}</span>
                <span className={canOpen ? 'kpi-evidence-open-label' : ''}>
                  {evidence.redacted ? 'Đã ẩn' : canOpen ? 'Xem chi tiết' : 'Chỉ đối soát'}
                </span>
              </div>
            </>
            return canOpen
              ? <button
                  type="button"
                  aria-controls="kpi-evidence-record"
                  aria-expanded={isActive}
                  className={`kpi-evidence-item kpi-evidence-item--button${isActive ? ' is-active' : ''}`}
                  key={evidence.evidenceId}
                  onClick={() => openEvidence(evidence)}
                >{content}</button>
              : <div className="kpi-evidence-item" key={evidence.evidenceId}>{content}</div>
          })}
    </div>
    {activeEvidence && <section
      id="kpi-evidence-record"
      className="kpi-evidence-record"
      aria-busy={evidenceLoading}
      aria-live="polite"
    >
      {evidenceLoading && <div className="kpi-evidence-record__state" role="status">Đang tải chi tiết bản ghi…</div>}
      {!evidenceLoading && evidenceError && <div className="kpi-evidence-record__state kpi-evidence-record__state--error" role="alert">
        <span>{evidenceError}</span>
        <button type="button" onClick={() => openEvidence(activeEvidence)}>Thử lại</button>
      </div>}
      {!evidenceLoading && !evidenceError && evidenceRecord && <>
        <header>
          <div><span>{evidenceRecord.sourceModule}</span><strong>{evidenceRecord.title}</strong></div>
          <small>Mã nguồn: {evidenceRecord.sourceRecordId}</small>
        </header>
        {evidenceRecord.fields.length === 0
          ? <div className="kpi-evidence-record__state">Bản ghi không có trường chi tiết được phép hiển thị.</div>
          : <dl className="kpi-evidence-fields">{evidenceRecord.fields.map((field, index) => <div key={`${field.label}-${index}`}>
              <dt>{field.label}</dt>
              <dd>{field.value || '—'}</dd>
            </div>)}</dl>}
        <div className="kpi-evidence-attachments">
          <div><strong>Tệp đính kèm</strong><span>{evidenceRecord.attachments.length} tệp</span></div>
          {evidenceRecord.attachments.length === 0
            ? <small>Không có tệp đính kèm được phép tải.</small>
            : evidenceRecord.attachments.map(attachment => <button
                type="button"
                aria-label={`Tải tệp ${attachment.fileName}`}
                disabled={downloadingAttachmentId !== null}
                key={attachment.id}
                onClick={() => void downloadAttachment(attachment)}
              >
                <span>{attachment.fileName}</span>
                <small>{downloadingAttachmentId === attachment.id ? 'Đang tải…' : 'Tải xuống'}</small>
              </button>)}
          {downloadError && <div className="kpi-evidence-download-error" role="alert">{downloadError}</div>}
        </div>
      </>}
    </section>}
  </div>
}

function SimpleKpiPanel({ unit, periodLabel }: { unit: KpiUnitResultView; periodLabel: string }) {
  const groupNames = new Map(unit.groups.map(group => [group.groupCode, group.name]))
  const [activeGroupCode, setActiveGroupCode] = useState(unit.groups[0]?.groupCode ?? '')
  const activeGroup = unit.groups.find(group => group.groupCode === activeGroupCode) ?? unit.groups[0]
  const visibleDetails = unit.details.filter(detail => detail.groupCode === activeGroup?.groupCode)
  return <article className="panel kpi-simple-panel">
    <header className="kpi-panel-heading">
      <div><span>{unit.unionUnitCode} · {periodLabel}</span><strong>KPI tính nhanh theo từng nội dung</strong></div>
      <div className={`kpi-simple-score kpi-simple-score--${classificationTone(unit.finalClassification)}`}>
        <strong>{formatKpiNumber(unit.finalScore)}<small>/100</small></strong>
        <span>{unit.runStatus === 'FINAL' ? unit.finalClassification : `Tạm tính · ${unit.finalClassification}`}</span>
      </div>
    </header>
    <p className="kpi-simple-hint"><b>Đã làm</b> là số hồ sơ đã hoàn tất. <b>Tổng cần làm</b> là tổng số hồ sơ phải xử lý. Mỗi thẻ dưới đây là một dashboard nhỏ; bấm vào thẻ để xem riêng nhóm đó.</p>
    <div className="kpi-mini-dashboard-grid" aria-label="Các dashboard KPI theo nhóm">
      {unit.groups.map(group => {
        const groupDetails = unit.details.filter(detail => detail.groupCode === group.groupCode)
        return <button type="button" key={group.groupCode} aria-pressed={activeGroup?.groupCode === group.groupCode} className={`kpi-mini-dashboard${activeGroup?.groupCode === group.groupCode ? ' is-active' : ''}`} onClick={() => setActiveGroupCode(group.groupCode)}>
          <div className="kpi-mini-dashboard__heading"><strong>{group.name}</strong><span>{kpiStatusLabel(group.status)}</span></div>
          <b className="kpi-mini-dashboard__score">{group.score === null ? '—' : formatKpiNumber(group.score)}<small>/100</small></b>
          <div className="kpi-group-progress" role="progressbar" aria-label={`Điểm nhóm ${group.name}`} aria-valuemin={0} aria-valuemax={100} aria-valuenow={group.score ?? undefined}><i style={{ width: visualPercent(group.score) }} /></div>
          <small>{groupDetails.length} chỉ tiêu · {formatKpiNumber(group.earnedPoints)} điểm đóng góp</small>
        </button>
      })}
    </div>
    <div className="kpi-simple-group-title"><strong>{activeGroup?.name ?? 'Nội dung KPI'}</strong><span>{visibleDetails.length} chỉ tiêu</span></div>
    <div className="table-wrap"><table className="kpi-simple-table"><thead><tr><th>Nội dung</th><th>Đã làm / Tổng cần làm</th><th>Tỷ lệ</th><th>Điểm</th><th>Trạng thái</th></tr></thead><tbody>
      {visibleDetails.map(detail => {
        const ratio = detail.numerator !== null && detail.denominator !== null && detail.denominator > 0
          ? detail.numerator / detail.denominator
          : null
        return <tr key={detail.kpiCode}>
          <td><strong>{groupNames.get(detail.groupCode) ?? detail.groupCode}</strong><small>{detail.name}</small></td>
          <td>{formatKpiNumber(detail.numerator)} <span>/</span> {formatKpiNumber(detail.denominator)}</td>
          <td><strong>{formatKpiRate(ratio)}</strong></td>
          <td><strong>{formatKpiNumber(detail.earnedPoints)}</strong><small>/{formatKpiNumber(detail.eligibleWeight)}</small></td>
          <td><span className={`kpi-status kpi-status--${detail.resultStatus.toLowerCase()}`}>{kpiStatusLabel(detail.resultStatus)}</span></td>
        </tr>
      })}
    </tbody></table></div>
  </article>
}

export default function KpiPage({ units, isAdmin, currentUnitId, currentUnitCode, currentUnitName }: Props) {
  const [today] = useState(() => new Date())
  const [periodType, setPeriodType] = useState<KpiPeriodType>('MONTH')
  const [year, setYear] = useState(today.getFullYear())
  const [period, setPeriod] = useState(defaultKpiPeriod('MONTH', today))
  const [unitId, setUnitId] = useState(isAdmin ? '' : String(currentUnitId ?? ''))
  const [data, setData] = useState<KpiDashboardView | null>(null)
  const [metadata, setMetadata] = useState<KpiMetadataView | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [retryKey, setRetryKey] = useState(0)
  const [selectedUnitId, setSelectedUnitId] = useState<number | null>(currentUnitId ?? null)
  const [selectedGroupCode, setSelectedGroupCode] = useState<string | null>(null)
  const [selectedKpiCode, setSelectedKpiCode] = useState<string | null>(null)
  const selectionRef = useRef({ periodType, year, period })

  useEffect(() => {
    selectionRef.current = { periodType, year, period }
  }, [periodType, year, period])

  useEffect(() => {
    const controller = new AbortController()
    loadKpiMetadata(controller.signal).then(response => {
      if (controller.signal.aborted) return
      const availableYears = kpiYearOptions(today, response.versions)
      const currentSelection = selectionRef.current
      const nextYear = availableYears.includes(currentSelection.year)
        ? currentSelection.year
        : (availableYears[0] ?? today.getFullYear())
      let nextType = currentSelection.periodType
      let nextOptions = kpiPeriodOptions(nextType, nextYear, today, response.versions)
      if (nextOptions.length === 0) {
        const fallback = KPI_PERIOD_TYPES
          .map(option => ({
            type: option.value,
            options: kpiPeriodOptions(option.value, nextYear, today, response.versions),
          }))
          .find(candidate => candidate.options.length > 0)
        if (!fallback) {
          setMetadata(response)
          setData(null)
          setLoading(false)
          return
        }
        nextType = fallback.type
        nextOptions = fallback.options
      }
      const nextPeriod = nextOptions.some(option => option.value === currentSelection.period)
        ? currentSelection.period
        : (nextOptions.at(-1)?.value ?? 1)

      setMetadata(response)
      setLoading(true)
      setError('')
      setYear(nextYear)
      setPeriodType(nextType)
      setPeriod(nextPeriod)
    }).catch(() => {
      // The dashboard remains usable for the current year when metadata is temporarily unavailable.
    })
    return () => controller.abort()
  }, [today])

  useEffect(() => {
    const selectablePeriods = kpiPeriodOptions(periodType, year, today, metadata?.versions)
    if (!selectablePeriods.some(option => option.value === period)) return
    const controller = new AbortController()
    queueMicrotask(() => {
      if (controller.signal.aborted) return
      setLoading(true)
      setError('')
    })

    loadKpiDashboard({
      periodType,
      year,
      period,
      unitId: unitId ? Number(unitId) : undefined,
    }, controller.signal).then(response => {
      if (controller.signal.aborted) return
      setError('')
      setData(response)
      setSelectedUnitId(current => response.results.some(result => result.unionUnitId === current)
        ? current
        : (response.results[0]?.unionUnitId ?? null))
    }).catch(reason => {
      if (controller.signal.aborted) return
      setData(null)
      setError(reason instanceof Error ? reason.message : 'Không thể tải kết quả KPI.')
    }).finally(() => {
      if (!controller.signal.aborted) setLoading(false)
    })

    return () => controller.abort()
  }, [metadata, periodType, year, period, unitId, retryKey, today])

  const results = data?.results ?? []
  const selectedUnit = results.find(result => result.unionUnitId === selectedUnitId) ?? results[0]
  const selectedGroup = selectedUnit?.groups.find(group => group.groupCode === selectedGroupCode)
    ?? selectedUnit?.groups[0]
  const selectedDetails = selectedUnit?.details.filter(detail => detail.groupCode === selectedGroup?.groupCode) ?? []
  const selectedDetail = selectedDetails.find(detail => detail.kpiCode === selectedKpiCode) ?? selectedDetails[0]
  const chosenPeriodLabel = data
    ? `${kpiPeriodLabel(data.periodType, year, period)} · ${data.periodStart} – ${data.periodEnd}`
    : kpiPeriodLabel(periodType, year, period)
  const ownUnitLabel = [currentUnitCode, currentUnitName].filter(Boolean).join(' · ') || 'CĐCS của bạn'
  const availablePeriodOptions = kpiPeriodOptions(periodType, year, today, metadata?.versions)

  const prepareReload = () => {
    setLoading(true)
    setError('')
  }

  const onPeriodTypeChange = (nextType: KpiPeriodType) => {
    if (nextType === periodType) return
    const nextOptions = kpiPeriodOptions(nextType, year, today, metadata?.versions)
    if (nextOptions.length === 0) return
    prepareReload()
    setPeriodType(nextType)
    const preferredPeriod = defaultKpiPeriod(nextType, today)
    setPeriod(nextOptions.some(option => option.value === preferredPeriod)
      ? preferredPeriod
      : (nextOptions.at(-1)?.value ?? 1))
    setSelectedGroupCode(null)
    setSelectedKpiCode(null)
  }

  return <section className="page-section kpi-report-page">
    <details className="kpi-history-collapsible">
      <summary>Đối soát nhân sự và xem lịch sử KPI</summary>
      <KpiHistoryPanel key={`${year}:${selectedUnit?.unionUnitId ?? ''}`} year={year} unitId={selectedUnit?.unionUnitId} isAdmin={isAdmin} onChanged={() => setRetryKey(k => k + 1)} />
    </details>
    <div className="kpi-report-heading">
      <div>
        <div className="kpi-report-heading__meta">
          <span className="kpi-live-dot" />
          <span>{isAdmin ? 'Điều hành KPI toàn hệ thống' : 'Kết quả KPI công đoàn cơ sở'}</span>
        </div>
        <h1>Báo cáo KPI công đoàn</h1>
        <p>Chọn kỳ để xem nhanh số đã làm, tổng cần làm, tỷ lệ và điểm của từng nội dung.</p>
      </div>
      <button type="button" className="button button--ghost kpi-print-button" onClick={() => window.print()}>
        <span aria-hidden="true">↗</span> Xuất báo cáo
      </button>
    </div>

    <div className="kpi-control-bar">
      <div className="kpi-period-switch" aria-label="Kiểu kỳ KPI">
        {KPI_PERIOD_TYPES.map(option => <button
          type="button"
          aria-pressed={periodType === option.value}
          className={periodType === option.value ? 'is-active' : ''}
          disabled={metadata !== null && kpiPeriodOptions(option.value, year, today, metadata.versions).length === 0}
          key={option.value}
          onClick={() => onPeriodTypeChange(option.value)}
        >{option.label}</button>)}
      </div>
      <label className="kpi-control-field">
        <span>Năm</span>
        <select value={year} onChange={event => {
          const nextYear = Number(event.target.value)
          let nextType = periodType
          let nextOptions = kpiPeriodOptions(nextType, nextYear, today, metadata?.versions)
          if (nextOptions.length === 0 && metadata !== null) {
            const fallback = KPI_PERIOD_TYPES
              .map(option => ({
                type: option.value,
                options: kpiPeriodOptions(option.value, nextYear, today, metadata.versions),
              }))
              .find(candidate => candidate.options.length > 0)
            if (!fallback) return
            nextType = fallback.type
            nextOptions = fallback.options
          }
          prepareReload()
          setYear(nextYear)
          setPeriodType(nextType)
          setPeriod(nextOptions.some(option => option.value === period)
            ? period
            : (nextOptions.at(-1)?.value ?? 1))
        }}>
          {kpiYearOptions(today, metadata?.versions).map(option => <option key={option} value={option}>{option}</option>)}
        </select>
      </label>
      {periodType !== 'YEAR' && <label className="kpi-control-field kpi-control-field--period">
        <span>Kỳ</span>
        <select value={period} onChange={event => {
          prepareReload()
          setPeriod(Number(event.target.value))
        }}>
          {availablePeriodOptions.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
        </select>
      </label>}
      <label className="kpi-control-field kpi-control-field--unit">
        <span>Phạm vi</span>
        {isAdmin
          ? <select value={unitId} onChange={event => {
              prepareReload()
              setUnitId(event.target.value)
              setSelectedUnitId(event.target.value ? Number(event.target.value) : null)
              setSelectedGroupCode(null)
              setSelectedKpiCode(null)
            }}>
              <option value="">Tất cả công đoàn cơ sở</option>
              {units.map(unit => <option key={unit.id} value={unit.id}>{unit.code} · {unit.name}</option>)}
            </select>
          : <strong>{ownUnitLabel}</strong>}
      </label>
      <div className="kpi-control-bar__stamp">
        <span>Kỳ đang xem</span>
        <strong>{chosenPeriodLabel}</strong>
      </div>
    </div>

    {loading && <div className="loading-panel" role="status">Đang nạp và tính KPI từ dữ liệu nghiệp vụ…</div>}

    {!loading && error && <div className="kpi-load-error" role="alert">
      <div><strong>Không thể tải kết quả KPI</strong><span>{error}</span></div>
      <button type="button" className="button button--primary" onClick={() => {
        prepareReload()
        setRetryKey(value => value + 1)
      }}>Thử lại</button>
    </div>}

    {!loading && !error && data && <>
      <div className="kpi-run-meta">
        <span>Phiên bản <strong>{data.versionId}</strong></span>
        <span>Chốt lúc <strong>{formatTimestamp(data.cutoffAt)}</strong></span>
        <span>Tổng hợp lúc <strong>{formatTimestamp(data.generatedAt)}</strong></span>
      </div>

      <div className="kpi-summary-grid">
        <article className="kpi-summary-card kpi-summary-card--primary">
          <div className="kpi-summary-card__icon">KPI</div>
          <div><span>Điểm bình quân</span><strong>{formatKpiNumber(data.summary.averageScore)}</strong><small>/ 100 điểm</small></div>
        </article>
        <article className="kpi-summary-card">
          <div className="kpi-summary-card__icon kpi-summary-card__icon--green">✓</div>
          <div><span>Kết quả chính thức</span><strong>{data.summary.finalUnitCount}</strong><small>đơn vị được xếp hạng</small></div>
        </article>
        <article className="kpi-summary-card">
          <div className="kpi-summary-card__icon kpi-summary-card__icon--orange">…</div>
          <div><span>Kết quả tạm tính</span><strong>{data.summary.provisionalUnitCount}</strong><small>chưa vào bảng hạng</small></div>
        </article>
        <article className="kpi-summary-card">
          <div className="kpi-summary-card__icon kpi-summary-card__icon--green">★</div>
          <div><span>Xuất sắc chính thức</span><strong>{data.summary.excellentCount}</strong><small>chỉ tính kết quả FINAL</small></div>
        </article>
        <article className="kpi-summary-card">
          <div className="kpi-summary-card__icon kpi-summary-card__icon--red">!</div>
          <div><span>Cần theo dõi</span><strong>{data.summary.attentionCount}</strong><small>cần xử lý cảnh báo</small></div>
        </article>
      </div>

      {results.length === 0
        ? <div className="kpi-empty-state">
            <strong>Chưa có kết quả KPI cho kỳ này</strong>
            <span>Hãy kiểm tra dữ liệu nguồn, ngày chốt hoặc phạm vi CĐCS đã chọn.</span>
          </div>
        : <>
          <article className="panel kpi-ranking-panel">
            <header className="kpi-panel-heading">
              <div>
                <span>{isAdmin ? 'Cùng kỳ · cùng phiên bản' : 'Kết quả của đơn vị'}</span>
                <strong>{isAdmin ? 'Kết quả KPI cùng kỳ' : ownUnitLabel}</strong>
              </div>
              <small>Chỉ kết quả FINAL mới có hạng; kết quả khác là dự báo</small>
            </header>
            <div className="table-wrap">
              <table className="kpi-ranking-table">
                <thead><tr><th>Hạng</th><th>Công đoàn cơ sở</th><th>Đoàn viên</th><th>Chất lượng dữ liệu</th><th>Điểm gốc</th><th>Thưởng</th><th>Phạt</th><th>Điểm hiện tại</th><th>Xếp loại</th><th>Trạng thái</th><th>Báo cáo đúng hạn</th></tr></thead>
                <tbody>{results.map(result => <tr
                  className={`${selectedUnit?.unionUnitId === result.unionUnitId ? 'is-selected' : ''}${result.runStatus !== 'FINAL' ? ' is-provisional' : ''}`}
                  key={result.unionUnitId}
                >
                  <td>{result.rank === null
                    ? <span className="kpi-rank kpi-rank--empty" title="Chưa được xếp hạng">—</span>
                    : <span className={`kpi-rank kpi-rank--${Math.min(result.rank, 4)}`}>{result.rank}</span>}</td>
                  <td><button type="button" aria-pressed={selectedUnit?.unionUnitId === result.unionUnitId} className="kpi-unit-link" onClick={() => selectUnit(result, setSelectedUnitId, setSelectedGroupCode, setSelectedKpiCode)}><b>{result.unionUnitCode}</b><span>{result.unionUnitName}</span><small>{result.activeMemberCount === null ? 'Chưa có snapshot đoàn viên của kỳ' : `${formatMemberCount(result.activeMemberCount)} đoàn viên đang hoạt động`}</small></button></td>
                  <td>{formatMemberCount(result.activeMemberCount)}</td>
                  <td><strong>{formatKpiRate(result.dataQualityRate)}</strong></td>
                  <td>{formatKpiNumber(result.baseScore)}</td>
                  <td className="is-positive">+{formatKpiNumber(result.bonusPoints)}</td>
                  <td className="is-negative">−{formatKpiNumber(result.penaltyPoints)}</td>
                  <td><strong className="kpi-final-score">{formatKpiNumber(result.finalScore)}</strong></td>
                  <td><span className={`kpi-rating kpi-rating--${classificationTone(result.finalClassification)}`}>{result.runStatus === 'FINAL' ? result.finalClassification : `Dự báo · ${result.finalClassification}`}</span>{result.rawClassification !== result.finalClassification && <small className="kpi-raw-rating">Điểm gốc: {result.rawClassification}</small>}</td>
                  <td><span className={`kpi-run-status kpi-run-status--${result.runStatus.toLowerCase()}`}>{kpiStatusLabel(result.runStatus)}</span></td>
                  <td>{formatKpiRate(result.reportOnTimeRate)}</td>
                </tr>)}</tbody>
              </table>
            </div>
          </article>

          {selectedUnit && <>
            <SimpleKpiPanel unit={selectedUnit} periodLabel={chosenPeriodLabel} />
            <details className="kpi-advanced-details">
              <summary>Xem nhóm KPI, chứng cứ và cảnh báo chi tiết</summary>
            <article className="panel kpi-group-panel">
              <header className="kpi-panel-heading">
                <div><span>{selectedUnit.unionUnitCode} · {chosenPeriodLabel}</span><strong>Điểm 7 nhóm KPI</strong></div>
                <div className={`kpi-detail-score kpi-detail-score--${classificationTone(selectedUnit.finalClassification)}`}><strong>{formatKpiNumber(selectedUnit.finalScore)}</strong><span>{selectedUnit.runStatus === 'FINAL' ? selectedUnit.finalClassification : `Dự báo · ${selectedUnit.finalClassification}`}</span></div>
              </header>
              <div className="kpi-group-grid">{selectedUnit.groups.map(group => <button
                type="button"
                aria-pressed={selectedGroup?.groupCode === group.groupCode}
                className={`kpi-group-card${selectedGroup?.groupCode === group.groupCode ? ' is-selected' : ''}`}
                key={group.groupCode}
                onClick={() => {
                  setSelectedGroupCode(group.groupCode)
                  setSelectedKpiCode(null)
                }}
              >
                <div className="kpi-group-card__heading"><b>{group.groupCode}</b><span>{kpiStatusLabel(group.status)}</span></div>
                <strong>{group.score === null ? 'NA' : formatKpiNumber(group.score)}</strong>
                <small>{group.name}</small>
                <div className="kpi-group-progress" role="progressbar" aria-label={group.name} aria-valuemin={0} aria-valuemax={100} aria-valuenow={group.score ?? undefined} aria-valuetext={group.score === null ? 'Không phát sinh' : `${formatKpiNumber(group.score)} trên 100`}>
                  <i style={{ width: visualPercent(group.score) }} />
                </div>
                <span>{formatKpiNumber(group.earnedPoints)} điểm · trọng số {formatKpiNumber(group.eligibleWeight)}/{formatKpiNumber(group.configuredWeight)}</span>
              </button>)}</div>
            </article>

            <div className="kpi-detail-grid">
              <article className="panel kpi-category-panel">
                <header className="kpi-panel-heading">
                  <div><span>{selectedGroup?.groupCode ?? 'KPI'}</span><strong>{selectedGroup?.name ?? 'Chi tiết chỉ tiêu'}</strong></div>
                  <small>Chọn KPI để truy xuất dữ liệu nguồn</small>
                </header>
                <div className="kpi-detail-workspace">
                  <div className="kpi-detail-list">{selectedDetails.length === 0
                    ? <div className="kpi-evidence-empty">Nhóm này chưa có KPI chi tiết.</div>
                    : selectedDetails.map(detail => <button
                        type="button"
                        aria-pressed={selectedDetail?.kpiCode === detail.kpiCode}
                        className={`kpi-detail-row${selectedDetail?.kpiCode === detail.kpiCode ? ' is-selected' : ''}`}
                        key={detail.kpiCode}
                        onClick={() => setSelectedKpiCode(detail.kpiCode)}
                      >
                        <div><b>{detail.kpiCode}</b><span>{detail.name}</span></div>
                        <div><strong>{formatKpiNumber(detail.earnedPoints)}</strong><small>/ {formatKpiNumber(detail.eligibleWeight)} điểm</small></div>
                        <span className={`kpi-status kpi-status--${detail.resultStatus.toLowerCase()}`}>{kpiStatusLabel(detail.resultStatus)}</span>
                      </button>)}</div>
                  <KpiEvidencePanel key={selectedDetail?.resultId ?? selectedDetail?.kpiCode ?? 'empty'} detail={selectedDetail} />
                </div>
              </article>

              <aside className="panel kpi-followup-panel">
                <header className="kpi-panel-heading"><div><span>Kiểm soát chất lượng</span><strong>Cảnh báo & hành động</strong></div><small>{selectedUnit.warnings.length} cảnh báo</small></header>
                {selectedUnit.adjustments.length > 0 && <section className="kpi-adjustment-audit" aria-label="Nhật ký điều chỉnh KPI đã duyệt">
                  <div className="kpi-adjustment-audit__heading"><strong>Nhật ký điều chỉnh đã duyệt</strong><span>{selectedUnit.adjustments.length} mục</span></div>
                  {selectedUnit.adjustments.map(adjustment => <div className="kpi-adjustment-audit__item" key={adjustment.adjustmentId}>
                    <div><b>{adjustment.adjustmentType === 'BONUS' ? 'Điểm thưởng' : `Điểm phạt ${adjustment.penaltyCode ?? ''}`}</b><strong>{adjustment.adjustmentType === 'BONUS' ? '+' : '−'}{formatKpiNumber(adjustment.points)}</strong></div>
                    <p>{adjustment.redacted ? 'Chi tiết lý do được ẩn theo quyền truy cập.' : adjustment.reason}</p>
                    {adjustment.adjustmentType === 'BONUS' && <small>Hiệu quả: {adjustment.effectivenessVerified ? 'đã xác minh' : 'chưa xác minh'} · Không trùng KPI: {adjustment.nonDuplicateVerified ? 'đã xác minh' : 'chưa xác minh'}</small>}
                    {!adjustment.redacted && adjustment.evidenceModule && <small>Minh chứng: {adjustment.evidenceModule} · {adjustment.evidenceRecordId}</small>}
                    <small>Phê duyệt lúc {formatTimestamp(adjustment.approvedAt)}</small>
                    {!adjustment.redacted && <small>Đề nghị: {adjustment.requestedBy} · Duyệt: {adjustment.approvedBy}</small>}
                  </div>)}
                </section>}
                <div className="kpi-warning-list">{selectedUnit.warnings.length === 0
                  ? <div className="kpi-warning-empty"><strong>Không có cảnh báo</strong><span>Dữ liệu kỳ này không phát sinh cảnh báo cần xử lý.</span></div>
                  : selectedUnit.warnings.map((warning, index) => <div className={`kpi-warning-item kpi-warning-item--${warning.severity.toLowerCase()}`} key={`${warning.code}-${warning.sourceRecordId ?? index}`}>
                      <div><b>{warning.code}</b><span>{kpiStatusLabel(warning.severity)}</span></div>
                      <strong>{warning.message}</strong>
                      {(warning.sourceModule || warning.sourceRecordId) && <small>
                        Kiểm tra {warning.sourceModule ?? 'hồ sơ nguồn'}{warning.sourceRecordId && !warning.redacted ? ` · ${warning.sourceRecordId}` : ''}
                      </small>}
                      {warning.recommendedAction && <small><b>Hành động:</b> {warning.recommendedAction}</small>}
                      {warning.dueAt && <small>Hạn xử lý: {warning.dueAt}</small>}
                    </div>)}</div>
                <footer><span>Chất lượng dữ liệu</span><strong>{formatKpiRate(selectedUnit.dataQualityRate)}</strong></footer>
              </aside>
            </div>
            </details>
          </>}
        </>}
    </>}
  </section>
}
