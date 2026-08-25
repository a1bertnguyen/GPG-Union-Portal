import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { api, currentMonth, formatDate } from '../api'
import { StatusBadge } from '../components/CrudPage'
import ExcelImportActions from '../components/ExcelImportActions'
import { importSummary } from '../excel'
import type { EngagementSummary, PulseSurvey, UnionUnit } from '../types'

type Props = { units: UnionUnit[]; canManage?: boolean }

type SurveyForm = {
  surveyCode: string
  title: string
  unionUnitId: string
  questionText: string
  startDate: string
  endDate: string
  status: string
  targetResponses: string
}

const needOptions = ['Sức khỏe tinh thần', 'Điều kiện làm việc', 'Phúc lợi', 'Đào tạo/kỹ năng', 'Kết nối nội bộ', 'Chương trình gia đình', 'Khác']

export default function EngagementPage({ units, canManage = true }: Props) {
  const [month, setMonth] = useState(currentMonth())
  const [unitId, setUnitId] = useState('')
  const [summary, setSummary] = useState<EngagementSummary | null>(null)
  const [surveys, setSurveys] = useState<PulseSurvey[]>([])
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [saving, setSaving] = useState(false)
  const [surveyForm, setSurveyForm] = useState<SurveyForm | null>(null)
  const [responseSurvey, setResponseSurvey] = useState<PulseSurvey | null>(null)
  const [rating, setRating] = useState('5')
  const [needCategory, setNeedCategory] = useState(needOptions[0])
  const [suggestion, setSuggestion] = useState('')
  const [anonymous, setAnonymous] = useState(true)
  const [respondentName, setRespondentName] = useState('')
  const [surveySearch, setSurveySearch] = useState('')
  const [surveySearchField, setSurveySearchField] = useState('all')

  const load = useCallback(async () => {
    const query = new URLSearchParams({ month })
    const surveyQuery = new URLSearchParams()
    if (unitId) {
      query.set('unitId', unitId)
      surveyQuery.set('unitId', unitId)
    }
    try {
      const [summaryResult, surveyResult] = await Promise.all([
        api<EngagementSummary>(`/engagement?${query}`),
        api<PulseSurvey[]>(`/surveys${surveyQuery.size ? `?${surveyQuery}` : ''}`),
      ])
      setSummary(summaryResult)
      setSurveys(surveyResult)
      setError('')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tải dữ liệu tiếng nói NLĐ')
    }
  }, [month, unitId])

  // Fetching the selected reporting slice is the synchronization performed by this effect.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { void load() }, [load])

  const openCreate = () => {
    const [year, monthNumber] = month.split('-').map(Number)
    const lastDay = new Date(year, monthNumber, 0).getDate()
    setSurveyForm({
      surveyCode: `KS-${month.replace('-', '')}`,
      title: '',
      unionUnitId: unitId || String(units[0]?.id ?? ''),
      questionText: '',
      startDate: `${month}-01`,
      endDate: `${month}-${String(lastDay).padStart(2, '0')}`,
      status: 'ACTIVE',
      targetResponses: '10',
    })
    setError('')
  }

  const createSurvey = async (event: FormEvent) => {
    event.preventDefault()
    if (!surveyForm) return
    setSaving(true)
    try {
      await api('/surveys', {
        method: 'POST',
        body: JSON.stringify({ ...surveyForm, unionUnitId: Number(surveyForm.unionUnitId), targetResponses: Number(surveyForm.targetResponses) }),
      })
      setSurveyForm(null)
      setMessage('Đã tạo chiến dịch khảo sát mới.')
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tạo khảo sát')
    } finally {
      setSaving(false)
    }
  }

  const closeSurvey = async (survey: PulseSurvey) => {
    if (!window.confirm(`Đóng khảo sát “${survey.title}”?`)) return
    try {
      await api(`/surveys/${survey.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          surveyCode: survey.surveyCode,
          title: survey.title,
          unionUnitId: survey.unionUnit.id,
          questionText: survey.questionText,
          startDate: survey.startDate,
          endDate: survey.endDate,
          status: 'CLOSED',
          targetResponses: survey.targetResponses,
        }),
      })
      setMessage('Khảo sát đã được đóng.')
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể đóng khảo sát')
    }
  }

  const openResponse = (survey: PulseSurvey) => {
    setResponseSurvey(survey)
    setRating('5')
    setNeedCategory(needOptions[0])
    setSuggestion('')
    setAnonymous(true)
    setRespondentName('')
    setError('')
  }

  const sendResponse = async (event: FormEvent) => {
    event.preventDefault()
    if (!responseSurvey) return
    setSaving(true)
    try {
      await api(`/surveys/${responseSurvey.id}/responses`, {
        method: 'POST',
        body: JSON.stringify({ rating: Number(rating), needCategory, suggestion: suggestion || null, anonymous, respondentName: anonymous ? null : respondentName }),
      })
      setResponseSurvey(null)
      setMessage('Cảm ơn bạn. Phản hồi đã được ghi nhận.')
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể gửi phản hồi')
    } finally {
      setSaving(false)
    }
  }

  const metrics = summary ? [
    ['Tỷ lệ phản hồi khảo sát', `${summary.surveyResponseRate}%`, `${summary.totalResponses} phản hồi trong kỳ`, 'blue'],
    ['Điểm kết nối', summary.averageRating ? `${summary.averageRating}/5` : '—', 'Mục tiêu từ 3,5/5', 'teal'],
    ['Kiến nghị có phản hồi', `${summary.caseResponseRate}%`, 'Mục tiêu từ 90%', 'green'],
    ['Điểm hữu ích hoạt động', summary.averageActivityScore ? `${summary.averageActivityScore}/5` : '—', `${summary.activeSurveyCount} khảo sát đang mở`, 'orange'],
  ] : []

  const maxNeed = Math.max(...(summary?.topNeeds.map(item => item.count) ?? [1]), 1)
  const visibleSurveys = useMemo(() => {
    const query = surveySearch.trim().toLocaleLowerCase('vi')
    if (!query) return surveys
    return surveys.filter(survey => {
      const values: Record<string, string> = {
        surveyCode: survey.surveyCode,
        title: survey.title,
        questionText: survey.questionText,
        unit: `${survey.unionUnit.code} ${survey.unionUnit.name}`,
        status: survey.status,
      }
      return (surveySearchField === 'all' ? Object.values(values).join(' ') : values[surveySearchField] ?? '').toLocaleLowerCase('vi').includes(query)
    })
  }, [surveySearch, surveySearchField, surveys])

  return (
    <section className="page-section">
      <div className="page-heading">
        <div><p className="eyebrow">Khảo sát & kết nối</p><h1>Tiếng nói NLĐ & kết nối</h1><p>Tổ chức khảo sát nhanh, tiếp nhận phản hồi và theo dõi KPI kết nối theo từng CĐCS.</p></div>
        <div className="page-actions">
          <label className="month-picker"><span>Tháng</span><input type="month" value={month} onChange={event => setMonth(event.target.value)} /></label>
          <label className="month-picker"><span>CĐCS</span><select value={unitId} onChange={event => setUnitId(event.target.value)}><option value="">Toàn hệ thống</option>{units.map(unit => <option key={unit.id} value={unit.id}>{unit.code}</option>)}</select></label>
          {canManage && <ExcelImportActions resource="surveys" filename="mau-khao-sat.xlsx" importLabel="Nhập khảo sát" templateLabel="Mẫu khảo sát"
            onError={setError} onImported={async result => {
              const summary = importSummary(result)
              if (result.errors.length) setError(`${summary} Lỗi: ${result.errors.slice(0, 3).join(' · ')}`)
              else { setError(''); setMessage(summary) }
              await load()
            }} />}
          {canManage && <button className="button button--primary" onClick={openCreate}>+ Tạo khảo sát</button>}
        </div>
      </div>

      {message && <div className="alert alert--success">{message}</div>}
      {error && !surveyForm && !responseSurvey && <div className="alert alert--danger">{error}</div>}

      <div className="metric-grid" id="voice-overview">
        {metrics.map(([label, value, note, tone]) => <article key={label} className={`metric-card metric-card--${tone}`}><span>{label}</span><strong>{value}</strong><small>{note}</small></article>)}
      </div>

      <div className="dashboard-grid">
        <article className="panel">
          <div className="panel__heading"><div><p className="eyebrow">Voice analytics</p><h2>Top nhu cầu NLĐ</h2></div><span className="tag">{summary?.month}</span></div>
          {!summary?.topNeeds.length ? <div className="empty-state">Chưa có phản hồi trong kỳ.</div> : <div className="need-list">{summary.topNeeds.map(item => <div key={item.category}><div><strong>{item.category}</strong><span>{item.count} ý kiến</span></div><div className="progress-line"><div style={{ width: `${item.count * 100 / maxNeed}%` }} /></div></div>)}</div>}
        </article>
        <article className="panel">
          <div className="panel__heading"><div><p className="eyebrow">Tự động</p><h2>Điểm cần chú ý</h2></div><span className="alert-count">{summary?.alerts.length ?? 0}</span></div>
          {!summary?.alerts.length ? <div className="empty-state">Các KPI đang trong ngưỡng theo dõi.</div> : <div className="alert-list">{summary.alerts.map((alert, index) => <div key={`${alert.title}-${index}`} className={`alert-item alert-item--${alert.level}`}><i /><div><strong>{alert.title}</strong><span>{alert.detail}</span></div></div>)}</div>}
        </article>
      </div>

      <div className="data-card" id="voice-surveys">
        <div className="data-card__header"><div className="record-count"><strong>{visibleSurveys.length} chiến dịch khảo sát</strong><span>{visibleSurveys.length === surveys.length ? 'Phản hồi được tổng hợp theo kỳ đã chọn' : `Trên tổng ${surveys.length} chiến dịch`}</span></div><div className="table-filters"><select aria-label="Chọn trường tìm kiếm khảo sát" value={surveySearchField} onChange={event => setSurveySearchField(event.target.value)}><option value="all">Tất cả trường</option><option value="surveyCode">Mã khảo sát</option><option value="title">Tên chiến dịch</option><option value="questionText">Câu hỏi</option><option value="unit">CĐCS</option><option value="status">Trạng thái</option></select><input aria-label="Tìm kiếm khảo sát" value={surveySearch} placeholder="Nhập từ khóa tìm kiếm…" onChange={event => setSurveySearch(event.target.value)} />{(surveySearch || surveySearchField !== 'all') && <button className="button button--ghost" onClick={() => { setSurveySearch(''); setSurveySearchField('all') }}>Xóa lọc</button>}
          {canManage && <ExcelImportActions resource="survey-responses" filename="mau-phan-hoi-khao-sat.xlsx" importLabel="Nhập phản hồi" templateLabel="Mẫu phản hồi"
            onError={setError} onImported={async result => {
              const summary = importSummary(result)
              if (result.errors.length) setError(`${summary} Lỗi: ${result.errors.slice(0, 3).join(' · ')}`)
              else { setError(''); setMessage(summary) }
              await load()
            }} />}
          <button className="button button--ghost" onClick={() => void load()}>Làm mới</button>
        </div></div>
        <div className="table-wrap"><table><thead><tr><th>Mã</th><th>Khảo sát</th><th>CĐCS</th><th>Thời gian</th><th>Phản hồi</th><th>Trạng thái</th><th /></tr></thead><tbody>
          {!visibleSurveys.length && <tr><td colSpan={7} className="empty-cell">{surveys.length ? 'Không có khảo sát phù hợp bộ lọc.' : 'Chưa có chiến dịch khảo sát.'}</td></tr>}
          {visibleSurveys.map(survey => <tr key={survey.id}><td><strong>{survey.surveyCode}</strong></td><td><strong>{survey.title}</strong><small className="table-subtext">{survey.questionText}</small></td><td>{survey.unionUnit.code}</td><td>{formatDate(survey.startDate)} – {formatDate(survey.endDate)}</td><td><strong>{survey.responseCount}/{survey.targetResponses}</strong><small className="table-subtext">{survey.responseRate}%</small></td><td><StatusBadge value={survey.status} /></td><td className="actions-cell">{survey.status === 'ACTIVE' && <><button className="icon-button" onClick={() => openResponse(survey)}>Phản hồi</button>{canManage && <button className="icon-button icon-button--danger" onClick={() => void closeSurvey(survey)}>Đóng</button>}</>}</td></tr>)}
        </tbody></table></div>
      </div>

      {surveyForm && <div className="modal-backdrop" onMouseDown={() => setSurveyForm(null)}><div className="modal" onMouseDown={event => event.stopPropagation()}><div className="modal__header"><div><p className="eyebrow">Chiến dịch mới</p><h2>Tạo khảo sát nhanh</h2></div><button className="modal__close" onClick={() => setSurveyForm(null)}>×</button></div><form className="form-grid" onSubmit={event => void createSurvey(event)}>
        <label className="field"><span>Mã khảo sát *</span><input required value={surveyForm.surveyCode} onChange={event => setSurveyForm(current => current && ({ ...current, surveyCode: event.target.value }))} /></label>
        <label className="field"><span>CĐCS *</span><select required value={surveyForm.unionUnitId} onChange={event => setSurveyForm(current => current && ({ ...current, unionUnitId: event.target.value }))}><option value="">Chọn CĐCS…</option>{units.map(unit => <option key={unit.id} value={unit.id}>{unit.code} · {unit.name}</option>)}</select></label>
        <label className="field field--wide"><span>Tên chiến dịch *</span><input required value={surveyForm.title} onChange={event => setSurveyForm(current => current && ({ ...current, title: event.target.value }))} /></label>
        <label className="field field--wide"><span>Câu hỏi chính *</span><textarea required value={surveyForm.questionText} onChange={event => setSurveyForm(current => current && ({ ...current, questionText: event.target.value }))} /></label>
        <label className="field"><span>Từ ngày *</span><input required type="date" value={surveyForm.startDate} onChange={event => setSurveyForm(current => current && ({ ...current, startDate: event.target.value }))} /></label>
        <label className="field"><span>Đến ngày *</span><input required type="date" value={surveyForm.endDate} onChange={event => setSurveyForm(current => current && ({ ...current, endDate: event.target.value }))} /></label>
        <label className="field"><span>Mục tiêu phản hồi *</span><input required min="1" type="number" value={surveyForm.targetResponses} onChange={event => setSurveyForm(current => current && ({ ...current, targetResponses: event.target.value }))} /></label>
        <label className="field"><span>Trạng thái *</span><select value={surveyForm.status} onChange={event => setSurveyForm(current => current && ({ ...current, status: event.target.value }))}><option value="DRAFT">Bản nháp</option><option value="ACTIVE">Đang hoạt động</option></select></label>
        {error && <div className="alert alert--danger field--wide">{error}</div>}
        <div className="form-actions field--wide"><button type="button" className="button button--ghost" onClick={() => setSurveyForm(null)}>Hủy</button><button className="button button--primary" disabled={saving}>{saving ? 'Đang lưu…' : 'Tạo khảo sát'}</button></div>
      </form></div></div>}

      {responseSurvey && <div className="modal-backdrop" onMouseDown={() => setResponseSurvey(null)}><div className="modal modal--compact" onMouseDown={event => event.stopPropagation()}><div className="modal__header"><div><p className="eyebrow">{responseSurvey.unionUnit.code}</p><h2>{responseSurvey.title}</h2></div><button className="modal__close" onClick={() => setResponseSurvey(null)}>×</button></div><form className="form-grid" onSubmit={event => void sendResponse(event)}>
        <div className="survey-question field--wide">{responseSurvey.questionText}</div>
        <label className="field"><span>Điểm đánh giá (1–5) *</span><select value={rating} onChange={event => setRating(event.target.value)}>{[5, 4, 3, 2, 1].map(score => <option key={score} value={score}>{score} / 5</option>)}</select></label>
        <label className="field"><span>Nhu cầu ưu tiên *</span><select value={needCategory} onChange={event => setNeedCategory(event.target.value)}>{needOptions.map(item => <option key={item}>{item}</option>)}</select></label>
        <label className="field field--wide"><span>Ý kiến / đề xuất</span><textarea value={suggestion} onChange={event => setSuggestion(event.target.value)} placeholder="Nội dung cần công đoàn lắng nghe…" /></label>
        <label className="field inline-check"><input className="checkbox" type="checkbox" checked={anonymous} onChange={event => setAnonymous(event.target.checked)} /><span>Gửi phản hồi ẩn danh</span></label>
        {!anonymous && <label className="field"><span>Họ tên *</span><input required value={respondentName} onChange={event => setRespondentName(event.target.value)} /></label>}
        {error && <div className="alert alert--danger field--wide">{error}</div>}
        <div className="form-actions field--wide"><button type="button" className="button button--ghost" onClick={() => setResponseSurvey(null)}>Hủy</button><button className="button button--primary" disabled={saving}>{saving ? 'Đang gửi…' : 'Gửi phản hồi'}</button></div>
      </form></div></div>}
    </section>
  )
}
