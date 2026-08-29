import { useEffect, useMemo, useState, type ChangeEvent } from 'react'
import { api, apiAll, downloadFile, enumLabel, formatDate, formatMoney } from '../api'
import type { ActivityMedia, BaseRecord, UnionUnit } from '../types'

type ProgramReport = BaseRecord & {
  activityCode: string
  name: string
  unionUnit: UnionUnit
  eventDate: string
  eventTime?: string
  location?: string
  programPic?: string
  status: string
  objective?: string
  plannedBudget: number
  actualCost: number
  invitedCount: number
  participantCount: number
  participantList?: string
  employeeGroup?: string
  checkInCount: number
  actualContent?: string
  planDifference?: string
  workersReached: number
  usefulnessScore?: number
  quickFeedback?: string
  issues?: string
  outputProposal?: string
  communicationContent?: string
  strengths?: string
  weaknesses?: string
  reportCompleted: boolean
  documentStatus: string
  followUpIssue?: string
  followUpOwner?: string
  followUpDeadline?: string
  followUpStatus?: string
  lessonsLearned?: string
}

type ReportForm = {
  name: string
  eventDate: string
  eventTime: string
  location: string
  programPic: string
  objective: string
  plannedBudget: string
  actualCost: string
  invitedCount: string
  participantCount: string
  participantList: string
  employeeGroup: string
  checkInCount: string
  actualContent: string
  planDifference: string
  workersReached: string
  usefulnessScore: string
  quickFeedback: string
  issues: string
  outputProposal: string
  communicationContent: string
  strengths: string
  weaknesses: string
  followUpIssue: string
  followUpOwner: string
  followUpDeadline: string
  followUpStatus: string
  lessonsLearned: string
  documentStatus: string
}

const value = (input: unknown) => input == null ? '' : String(input)
const normalizeSearch = (input: unknown) => value(input).normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('vi').replace(/đ/g, 'd')

const toForm = (item: ProgramReport): ReportForm => ({
  name: value(item.name), eventDate: value(item.eventDate), eventTime: value(item.eventTime).slice(0, 5),
  location: value(item.location), programPic: value(item.programPic), objective: value(item.objective),
  plannedBudget: value(item.plannedBudget ?? 0), actualCost: value(item.actualCost ?? 0),
  invitedCount: value(item.invitedCount ?? 0), participantCount: value(item.participantCount ?? 0),
  participantList: value(item.participantList), employeeGroup: value(item.employeeGroup),
  checkInCount: value(item.checkInCount ?? 0), actualContent: value(item.actualContent),
  planDifference: value(item.planDifference), workersReached: value(item.workersReached ?? 0),
  usefulnessScore: value(item.usefulnessScore), quickFeedback: value(item.quickFeedback), issues: value(item.issues),
  outputProposal: value(item.outputProposal), communicationContent: value(item.communicationContent),
  strengths: value(item.strengths), weaknesses: value(item.weaknesses), followUpIssue: value(item.followUpIssue),
  followUpOwner: value(item.followUpOwner), followUpDeadline: value(item.followUpDeadline),
  followUpStatus: value(item.followUpStatus), lessonsLearned: value(item.lessonsLearned),
  documentStatus: value(item.documentStatus || 'INCOMPLETE'),
})

const requiredText: Array<[keyof ReportForm, string]> = [
  ['eventTime', 'Giờ tổ chức'], ['location', 'Địa điểm'], ['programPic', 'PIC chương trình'],
  ['objective', 'Mục tiêu'], ['employeeGroup', 'Nhóm NLĐ'], ['actualContent', 'Nội dung thực tế'],
  ['planDifference', 'Khác biệt so kế hoạch'], ['quickFeedback', 'Phản hồi'], ['issues', 'Vấn đề ghi nhận'],
  ['outputProposal', 'Đề xuất'], ['communicationContent', 'Nội dung truyền thông'],
  ['participantList', 'Danh sách tham dự'], ['usefulnessScore', 'Điểm hữu ích'],
  ['strengths', 'Điều làm tốt'], ['weaknesses', 'Điều chưa tốt'], ['lessonsLearned', 'Bài học'],
  ['followUpIssue', 'Vấn đề follow-up'], ['followUpStatus', 'Tình trạng follow-up'],
]

function FormField({ label, wide = false, children }: { label: string; wide?: boolean; children: React.ReactNode }) {
  return <label className={`field${wide ? ' field--wide' : ''}`}><span>{label}</span>{children}</label>
}

export default function ActivityReportsPage({ isAdmin }: { isAdmin: boolean }) {
  const [activities, setActivities] = useState<ProgramReport[]>([])
  const [selectedId, setSelectedId] = useState('')
  const [reportSearch, setReportSearch] = useState('')
  const [searchOpen, setSearchOpen] = useState(false)
  const [form, setForm] = useState<ReportForm | null>(null)
  const [media, setMedia] = useState<ActivityMedia[]>([])
  const [photo, setPhoto] = useState<File | null>(null)
  const [document, setDocument] = useState<File | null>(null)
  const [photoKey, setPhotoKey] = useState(0)
  const [documentKey, setDocumentKey] = useState(0)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const selected = useMemo(() => activities.find(item => String(item.id) === selectedId), [activities, selectedId])
  const matchingActivities = useMemo(() => {
    const query = normalizeSearch(reportSearch.trim())
    if (!query) return activities
    return activities.filter(item => normalizeSearch([
      item.activityCode, item.name, item.unionUnit.code, item.unionUnit.name,
      item.reportCompleted ? 'đã nộp' : 'chưa nộp',
    ].join(' ')).includes(query))
  }, [activities, reportSearch])
  const visibleActivities = useMemo(() => {
    if (!reportSearch.trim()) return activities
    const current = activities.find(item => String(item.id) === selectedId)
    return current && !matchingActivities.some(item => item.id === current.id)
      ? [current, ...matchingActivities] : matchingActivities
  }, [activities, matchingActivities, reportSearch, selectedId])
  const photos = media.filter(item => item.mediaType === 'PHOTO')
  const documents = media.filter(item => item.mediaType === 'DOCUMENT')

  const chooseActivity = (id: string, rows: ProgramReport[] = activities) => {
    const item = rows.find(row => String(row.id) === id)
    setSelectedId(id)
    setForm(item ? toForm(item) : null)
    setMedia([])
    setMessage('')
    setError('')
    if (item) {
      apiAll<ActivityMedia>('/activity-media', { activityId: item.id })
        .then(setMedia)
        .catch(err => setError(err instanceof Error ? err.message : 'Không thể tải ảnh và chứng từ'))
    }
  }

  const chooseSuggestion = (item: ProgramReport) => {
    chooseActivity(String(item.id))
    setReportSearch(`${item.activityCode} · ${item.name}`)
    setSearchOpen(false)
  }

  const loadActivities = async (preferredId?: number) => {
    const rows = await apiAll<ProgramReport>('/activities')
    setActivities(rows)
    const wanted = preferredId ? String(preferredId) : selectedId
    const preferred = rows.find(item => isAdmin ? item.reportCompleted : !item.reportCompleted)
    const nextId = wanted && rows.some(item => String(item.id) === wanted)
      ? wanted : rows.length ? String(preferred?.id ?? rows[0].id) : ''
    chooseActivity(nextId, rows)
  }

  useEffect(() => {
    let active = true
    apiAll<ProgramReport>('/activities').then(rows => {
      if (!active) return
      setActivities(rows)
      const preferred = rows.find(item => isAdmin ? item.reportCompleted : !item.reportCompleted)
      const nextId = rows.length ? String(preferred?.id ?? rows[0].id) : ''
      const item = rows.find(row => String(row.id) === nextId)
      setSelectedId(nextId)
      setForm(item ? toForm(item) : null)
      if (item) {
        apiAll<ActivityMedia>('/activity-media', { activityId: item.id })
          .then(items => { if (active) setMedia(items) })
          .catch(err => { if (active) setError(err instanceof Error ? err.message : 'Không thể tải ảnh và chứng từ') })
      }
    }).catch(err => {
      if (active) setError(err instanceof Error ? err.message : 'Không thể tải chương trình')
    }).finally(() => {
      if (active) setLoading(false)
    })
    return () => { active = false }
  }, [isAdmin])

  const set = (name: keyof ReportForm) => (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    setForm(current => current ? { ...current, [name]: event.target.value } : current)
  }

  const reportMissing = useMemo(() => {
    if (!form) return ['Chưa chọn chương trình']
    const missing = requiredText.filter(([name]) => !form[name].trim()).map(([, label]) => label)
    if (Number(form.invitedCount) <= 0) missing.push('Số người mời')
    if (form.documentStatus !== 'COMPLETE') missing.push('Tình trạng chứng từ đầy đủ')
    if (!photos.length) missing.push('Ít nhất 1 ảnh')
    if (!documents.length) missing.push('Ít nhất 1 chứng từ')
    return missing
  }, [documents.length, form, photos.length])

  const closeMissing = useMemo(() => {
    if (!form) return reportMissing
    return [...reportMissing,
      ...(!form.followUpOwner.trim() ? ['PIC follow-up'] : []),
      ...(!form.followUpDeadline ? ['Deadline follow-up'] : []),
    ]
  }, [form, reportMissing])

  const participationRate = form && Number(form.invitedCount) > 0
    ? Number(form.participantCount) / Number(form.invitedCount) * 100 : 0
  const costPerPerson = form && Number(form.participantCount) > 0
    ? Number(form.actualCost) / Number(form.participantCount) : 0
  const budgetDifference = form ? Number(form.plannedBudget) - Number(form.actualCost) : 0

  const payload = (reportCompleted: boolean, status: string) => {
    if (!form || !selected) return null
    return {
      activityCode: selected.activityCode, name: form.name.trim(), unionUnitId: selected.unionUnit.id,
      eventDate: form.eventDate, eventTime: form.eventTime || null, location: form.location || null,
      programPic: form.programPic || null, status, objective: form.objective || null,
      plannedBudget: Number(form.plannedBudget || 0), actualCost: Number(form.actualCost || 0),
      invitedCount: Number(form.invitedCount || 0), participantCount: Number(form.participantCount || 0),
      participantList: form.participantList || null, employeeGroup: form.employeeGroup || null,
      checkInCount: Number(form.checkInCount || 0), actualContent: form.actualContent || null,
      planDifference: form.planDifference || null, workersReached: Number(form.workersReached || 0),
      usefulnessScore: form.usefulnessScore === '' ? null : Number(form.usefulnessScore),
      quickFeedback: form.quickFeedback || null, issues: form.issues || null,
      outputProposal: form.outputProposal || null, communicationContent: form.communicationContent || null,
      strengths: form.strengths || null, weaknesses: form.weaknesses || null,
      reportCompleted, documentStatus: form.documentStatus, followUpIssue: form.followUpIssue || null,
      followUpOwner: form.followUpOwner || null, followUpDeadline: form.followUpDeadline || null,
      followUpStatus: form.followUpStatus || null, lessonsLearned: form.lessonsLearned || null,
    }
  }

  const save = async (mode: 'draft' | 'submit' | 'close') => {
    if (!selected) return
    setSaving(true)
    setError('')
    setMessage('')
    const reportCompleted = mode !== 'draft' || Boolean(selected.reportCompleted)
    const nextStatus = mode === 'close' ? 'COMPLETED'
      : mode === 'submit' && ['PLANNED', 'APPROVED'].includes(selected.status) ? 'IN_PROGRESS'
        : selected.status
    try {
      await api(`/activities/${selected.id}`, { method: 'PUT', body: JSON.stringify(payload(reportCompleted, nextStatus)) })
      await loadActivities(selected.id)
      setMessage(mode === 'draft' ? 'Đã lưu nội dung báo cáo.' : mode === 'submit' ? 'Đã nộp báo cáo chương trình.' : 'Đã nộp báo cáo và đóng chương trình.')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể lưu báo cáo chương trình')
    } finally {
      setSaving(false)
    }
  }

  const upload = async (mediaType: 'PHOTO' | 'DOCUMENT') => {
    if (!selected) return
    const file = mediaType === 'PHOTO' ? photo : document
    if (!file) return
    setUploading(mediaType)
    setError('')
    const body = new FormData()
    body.append('activityId', String(selected.id))
    body.append('mediaType', mediaType)
    body.append('title', mediaType === 'PHOTO' ? 'Ảnh báo cáo chương trình' : 'Chứng từ báo cáo chương trình')
    body.append('file', file)
    try {
      await api('/activity-media', { method: 'POST', body })
      const rows = await apiAll<ActivityMedia>('/activity-media', { activityId: selected.id })
      setMedia(rows)
      if (mediaType === 'PHOTO') { setPhoto(null); setPhotoKey(key => key + 1) }
      else {
        setDocument(null)
        setDocumentKey(key => key + 1)
        setForm(current => current ? { ...current, documentStatus: 'COMPLETE' } : current)
      }
      setMessage(mediaType === 'PHOTO' ? 'Đã tải ảnh chương trình.' : 'Đã tải chứng từ chương trình.')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tải tệp báo cáo')
    } finally {
      setUploading('')
    }
  }

  if (loading) return <section className="page-section"><div className="loading-panel">Đang tải mẫu báo cáo chương trình…</div></section>

  return <section className="page-section activity-report-page">
    <div className="page-heading">
      <div><p className="eyebrow">Hoạt động / Báo cáo chương trình</p><h1>{isAdmin ? 'Xem báo cáo chương trình' : 'Nộp báo cáo chương trình'}</h1><p>{isAdmin ? 'Xem nội dung, KPI và chứng cứ do các CĐCS gửi về.' : 'Hoàn thiện 8 nhóm nội dung, chứng cứ bắt buộc và KPI trước khi nộp hoặc đóng chương trình.'}</p></div>
      <div className="report-program-controls">
        <div className="report-program-search">
          <label className="report-program-picker"><span>Tìm báo cáo</span><input value={reportSearch} onFocus={() => setSearchOpen(true)} onBlur={() => setSearchOpen(false)} onChange={event => { setReportSearch(event.target.value); setSearchOpen(true) }} onKeyDown={event => {
            if (event.key === 'Escape') setSearchOpen(false)
            if (event.key === 'Enter' && matchingActivities[0]) { event.preventDefault(); chooseSuggestion(matchingActivities[0]) }
          }} placeholder="Mã, tên hoặc CĐCS…" autoComplete="off" aria-autocomplete="list" aria-expanded={searchOpen && Boolean(reportSearch.trim())} /></label>
          {searchOpen && reportSearch.trim() && <div className="report-program-suggestions" role="listbox">
            {matchingActivities.length ? matchingActivities.slice(0, 8).map(item => <button type="button" role="option" aria-selected={String(item.id) === selectedId} key={item.id} onMouseDown={event => event.preventDefault()} onClick={() => chooseSuggestion(item)}>
              <strong>{item.activityCode} · {item.name}</strong>
              <span>{item.unionUnit.code} · {formatDate(item.eventDate)}</span>
              <small>{item.reportCompleted ? 'Đã nộp báo cáo' : 'Chưa nộp báo cáo'}</small>
            </button>) : <div className="report-program-suggestions__empty">Không tìm thấy chương trình phù hợp.</div>}
          </div>}
        </div>
        <label className="report-program-picker"><span>Chương trình · {matchingActivities.length}/{activities.length}</span><select value={selectedId} onChange={event => chooseActivity(event.target.value)}><option value="">Chọn chương trình…</option>{visibleActivities.map(item => <option key={item.id} value={item.id}>{item.activityCode} · {item.name}{isAdmin ? ` · ${item.unionUnit.code} · ${item.reportCompleted ? 'Đã nộp' : 'Chưa nộp'}` : ''}</option>)}</select></label>
      </div>
    </div>

    {error && <div className="alert alert--danger">{error}</div>}
    {message && <div className="alert alert--success">{message}</div>}
    {!selected || !form ? <div className="empty-state data-card">Chưa có chương trình để lập báo cáo.</div> : <>
      <div className="activity-report-summary data-card">
        <div><span>Mã duy nhất</span><strong>{selected.activityCode}</strong><small>{selected.unionUnit.code} · {formatDate(selected.eventDate)}</small></div>
        <div><span>Trạng thái chương trình</span><strong>{enumLabel(selected.status)}</strong><small>{selected.reportCompleted ? 'Báo cáo đã nộp' : 'Báo cáo chưa nộp'}</small></div>
        <div><span>Điều kiện nộp</span><strong>{reportMissing.length ? `Thiếu ${reportMissing.length}` : 'Đã đủ'}</strong><small>{reportMissing.slice(0, 2).join(' · ') || 'Đủ nội dung và chứng cứ'}</small></div>
      </div>

      <div className="metric-grid metric-grid--compact activity-report-kpis">
        <article className="metric-card metric-card--teal"><span>Tỷ lệ tham gia</span><strong>{participationRate.toFixed(1)}%</strong><small>{form.participantCount || 0}/{form.invitedCount || 0} người</small></article>
        <article className="metric-card metric-card--blue"><span>Điểm hữu ích</span><strong>{form.usefulnessScore || '—'}</strong><small>Thang điểm 5</small></article>
        <article className="metric-card metric-card--orange"><span>Chi phí / người</span><strong>{formatMoney(costPerPerson)}</strong><small>Theo số người tham dự</small></article>
        <article className="metric-card metric-card--green"><span>Chênh lệch ngân sách</span><strong>{formatMoney(budgetDifference)}</strong><small>Dự kiến trừ thực tế</small></article>
      </div>

      <form className="activity-report-form" onSubmit={event => event.preventDefault()}>
        <fieldset disabled={isAdmin}>
        <section className="data-card report-section"><header><b>1</b><div><h2>Thông tin chương trình</h2><p>Tên • mục tiêu • đơn vị • ngày/giờ • địa điểm • PIC</p></div></header><div className="form-grid">
          <FormField label="Tên chương trình *"><input value={form.name} onChange={set('name')} /></FormField>
          <FormField label="Đơn vị"><input readOnly value={`${selected.unionUnit.code} · ${selected.unionUnit.name}`} /></FormField>
          <FormField label="Ngày tổ chức *"><input type="date" value={form.eventDate} onChange={set('eventDate')} /></FormField>
          <FormField label="Giờ tổ chức *"><input type="time" value={form.eventTime} onChange={set('eventTime')} /></FormField>
          <FormField label="Địa điểm *"><input value={form.location} onChange={set('location')} /></FormField>
          <FormField label="PIC chương trình *"><input value={form.programPic} onChange={set('programPic')} /></FormField>
          <FormField label="Mục tiêu *" wide><textarea value={form.objective} onChange={set('objective')} /></FormField>
        </div></section>

        <section className="data-card report-section"><header><b>2</b><div><h2>Quy mô & đối tượng</h2><p>Số mời • số tham dự • tỷ lệ tham gia • nhóm NLĐ</p></div></header><div className="form-grid">
          <FormField label="Số người mời *"><input type="number" min="0" value={form.invitedCount} onChange={set('invitedCount')} /></FormField>
          <FormField label="Số người tham dự *"><input type="number" min="0" value={form.participantCount} onChange={set('participantCount')} /></FormField>
          <FormField label="Số người check-in"><input type="number" min="0" value={form.checkInCount} onChange={set('checkInCount')} /></FormField>
          <FormField label="Tỷ lệ tham gia"><input readOnly value={`${participationRate.toFixed(1)}%`} /></FormField>
          <FormField label="Nhóm NLĐ / đối tượng *" wide><textarea value={form.employeeGroup} onChange={set('employeeGroup')} /></FormField>
        </div></section>

        <section className="data-card report-section"><header><b>3</b><div><h2>Nội dung thực tế</h2><p>Các nội dung đã triển khai • khác biệt so kế hoạch</p></div></header><div className="form-grid">
          <FormField label="Nội dung đã triển khai *" wide><textarea value={form.actualContent} onChange={set('actualContent')} /></FormField>
          <FormField label="Khác biệt so với kế hoạch *" wide><textarea value={form.planDifference} onChange={set('planDifference')} placeholder="Ghi ‘Không có’ nếu triển khai đúng kế hoạch." /></FormField>
        </div></section>

        <section className="data-card report-section"><header><b>4</b><div><h2>Kết quả đầu ra</h2><p>Số NLĐ được tiếp cận • phản hồi • vấn đề ghi nhận • đề xuất</p></div></header><div className="form-grid">
          <FormField label="Số NLĐ được tiếp cận *"><input type="number" min="0" value={form.workersReached} onChange={set('workersReached')} /></FormField>
          <FormField label="Phản hồi *"><textarea value={form.quickFeedback} onChange={set('quickFeedback')} /></FormField>
          <FormField label="Vấn đề ghi nhận *"><textarea value={form.issues} onChange={set('issues')} placeholder="Ghi ‘Không phát sinh’ nếu không có." /></FormField>
          <FormField label="Đề xuất *"><textarea value={form.outputProposal} onChange={set('outputProposal')} /></FormField>
        </div></section>

        <section className="data-card report-section"><header><b>5</b><div><h2>Ngân sách</h2><p>Được duyệt • thực tế • chênh lệch • tình trạng chứng từ</p></div></header><div className="form-grid">
          <FormField label="Ngân sách được duyệt *"><input type="number" min="0" step="1000" value={form.plannedBudget} onChange={set('plannedBudget')} /></FormField>
          <FormField label="Chi phí thực tế *"><input type="number" min="0" step="1000" value={form.actualCost} onChange={set('actualCost')} /></FormField>
          <FormField label="Chênh lệch"><input readOnly value={formatMoney(budgetDifference)} /></FormField>
          <FormField label="Tình trạng chứng từ *"><select value={form.documentStatus} onChange={set('documentStatus')}><option value="INCOMPLETE">Chưa đủ</option><option value="COMPLETE">Đủ</option></select></FormField>
        </div></section>

        <section className="data-card report-section"><header><b>6</b><div><h2>Hình ảnh / tài liệu</h2><p>Ảnh • chứng từ • danh sách tham dự • nội dung truyền thông</p></div></header><div className="form-grid">
          <FormField label="Danh sách tham dự *" wide><textarea value={form.participantList} onChange={set('participantList')} placeholder="Nhập danh sách hoặc đường dẫn tệp danh sách." /></FormField>
          <FormField label="Nội dung truyền thông *" wide><textarea value={form.communicationContent} onChange={set('communicationContent')} /></FormField>
          <div className={`report-upload-box${photos.length ? ' is-complete' : ''}`}><span>Ảnh chương trình * · {photos.length} tệp</span>{isAdmin ? <div className="report-evidence-files">{photos.length ? photos.map(item => <a href="#" key={item.id} onClick={event => { event.preventDefault(); void downloadFile(`/activity-media/${item.id}/download`, item.fileName) }}>{item.title ?? item.fileName}</a>) : <small>Chưa có ảnh được nộp.</small>}</div> : <><input key={photoKey} type="file" accept="image/*" onChange={event => setPhoto(event.target.files?.[0] ?? null)} /><button type="button" className="button button--ghost" disabled={!photo || Boolean(uploading)} onClick={() => void upload('PHOTO')}>{uploading === 'PHOTO' ? 'Đang tải…' : 'Tải ảnh'}</button></>}</div>
          <div className={`report-upload-box${documents.length ? ' is-complete' : ''}`}><span>Chứng từ * · {documents.length} tệp</span>{isAdmin ? <div className="report-evidence-files">{documents.length ? documents.map(item => <a href="#" key={item.id} onClick={event => { event.preventDefault(); void downloadFile(`/activity-media/${item.id}/download`, item.fileName) }}>{item.title ?? item.fileName}</a>) : <small>Chưa có chứng từ được nộp.</small>}</div> : <><input key={documentKey} type="file" accept=".pdf,.doc,.docx,.xls,.xlsx,image/*" onChange={event => setDocument(event.target.files?.[0] ?? null)} /><button type="button" className="button button--ghost" disabled={!document || Boolean(uploading)} onClick={() => void upload('DOCUMENT')}>{uploading === 'DOCUMENT' ? 'Đang tải…' : 'Tải chứng từ'}</button></>}</div>
        </div></section>

        <section className="data-card report-section"><header><b>7</b><div><h2>Đánh giá</h2><p>Điểm hài lòng/hữu ích • điều làm tốt • điều chưa tốt • bài học</p></div></header><div className="form-grid">
          <FormField label="Điểm hữu ích (0–5) *"><input type="number" min="0" max="5" step="0.1" value={form.usefulnessScore} onChange={set('usefulnessScore')} /></FormField>
          <FormField label="Điều làm tốt *"><textarea value={form.strengths} onChange={set('strengths')} /></FormField>
          <FormField label="Điều chưa tốt *"><textarea value={form.weaknesses} onChange={set('weaknesses')} /></FormField>
          <FormField label="Bài học *"><textarea value={form.lessonsLearned} onChange={set('lessonsLearned')} /></FormField>
        </div></section>

        <section className="data-card report-section"><header><b>8</b><div><h2>Hành động sau chương trình</h2><p>Vấn đề cần follow-up • PIC • deadline • tình trạng</p></div></header><div className="form-grid">
          <FormField label="Vấn đề cần follow-up *" wide><textarea value={form.followUpIssue} onChange={set('followUpIssue')} placeholder="Ghi ‘Không phát sinh’ nếu không có." /></FormField>
          <FormField label="PIC follow-up (bắt buộc để đóng)"><input value={form.followUpOwner} onChange={set('followUpOwner')} /></FormField>
          <FormField label="Deadline follow-up (bắt buộc để đóng)"><input type="date" value={form.followUpDeadline} onChange={set('followUpDeadline')} /></FormField>
          <FormField label="Tình trạng follow-up *"><select value={form.followUpStatus} onChange={set('followUpStatus')}><option value="">Chọn tình trạng…</option><option value="PENDING">Chưa thực hiện</option><option value="IN_PROGRESS">Đang thực hiện</option><option value="COMPLETED">Hoàn tất</option></select></FormField>
        </div></section>
        </fieldset>
      </form>

      {isAdmin ? <div className="notice activity-report-view-notice">
        <strong>Chế độ xem của ADMIN</strong>
        <span>{selected.reportCompleted ? 'Báo cáo này đã được CĐCS nộp. ADMIN có thể xem nội dung và tải chứng cứ, không chỉnh sửa báo cáo của USER.' : 'CĐCS chưa nộp báo cáo này; dữ liệu đang hiển thị là nội dung hiện có.'}</span>
      </div> : <div className="activity-report-submit data-card">
        <div><strong>Kiểm tra trước khi nộp</strong><span>{reportMissing.length ? `Còn thiếu: ${reportMissing.join(' • ')}` : 'Đủ ảnh • chứng từ • danh sách • phản hồi và nội dung báo cáo.'}</span>{!reportMissing.length && closeMissing.length > 0 && <small>Để đóng chương trình, bổ sung: {closeMissing.join(' • ')}</small>}</div>
        <div><button type="button" className="button button--ghost" disabled={saving} onClick={() => void save('draft')}>Lưu nội dung</button><button type="button" className="button button--primary" disabled={saving || reportMissing.length > 0} onClick={() => void save('submit')}>Nộp báo cáo</button><button type="button" className="button button--primary report-close-button" disabled={saving || closeMissing.length > 0} onClick={() => void save('close')}>Đóng chương trình</button></div>
      </div>}
    </>}
  </section>
}
