import { useEffect, useMemo, useState, type ComponentProps, type FormEvent } from 'react'
import { api, apiAll, downloadFile, enumLabel, formatDate, formatMoney, loadFileUrl } from '../api'
import ExcelImportActions from '../components/ExcelImportActions'
import ListCard from '../components/ListCard'
import TableFilterBar, { FilterField } from '../components/TableFilterBar'
import { usePagedList } from '../hooks/usePagedList'
import { importSummary } from '../excel'
import type { ActivityMedia, BaseRecord, UnionUnit } from '../types'

type LibraryView = 'PHOTO' | 'DOCUMENT'

function AuthenticatedImage({ item }: { item: ActivityMedia }) {
  const [src, setSrc] = useState('')
  useEffect(() => {
    let active = true
    let objectUrl = ''
    loadFileUrl(`/activity-media/${item.id}/download`)
      .then(url => {
        objectUrl = url
        if (active) setSrc(url)
        else URL.revokeObjectURL(url)
      })
      .catch(() => undefined)
    return () => {
      active = false
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [item.id])
  return src ? <img src={src} alt={item.title ?? item.activityName} /> : <div className="gallery-placeholder">ẢNH</div>
}

export default function ActivityGalleryPage({ units, onCreateActivity }: { units: UnionUnit[]; onCreateActivity: () => void }) {
  const [activityOptions, setActivityOptions] = useState<BaseRecord[]>([])
  const [activeView, setActiveView] = useState<LibraryView>('PHOTO')
  const [activityId, setActivityId] = useState('')
  const [title, setTitle] = useState('')
  const [file, setFile] = useState<File | null>(null)
  const [fileInputKey, setFileInputKey] = useState(0)
  const [status, setStatus] = useState('')
  const [unitFilter, setUnitFilter] = useState('')
  const [search, setSearch] = useState('')
  const [actionError, setActionError] = useState('')
  const [transferMessage, setTransferMessage] = useState('')
  const [saving, setSaving] = useState(false)

  // `status` is the activity lifecycle state on this screen; the media list carries it separately from
  // its own `status`, which the server reads as the media type.
  const mediaFilters = useMemo(() => ({
    q: search.trim() || undefined,
    unitId: unitFilter || undefined,
    activityStatus: status || undefined,
    status: activeView,
  }), [search, unitFilter, status, activeView])

  const activityFilters = useMemo(() => ({
    q: search.trim() || undefined,
    unitId: unitFilter || undefined,
    status: status || undefined,
  }), [search, unitFilter, status])

  const media = usePagedList<ActivityMedia>({ endpoint: '/activity-media', filters: mediaFilters })
  const activities = usePagedList<BaseRecord>({ endpoint: '/activities', filters: activityFilters })

  // The upload form needs every activity the account can see, not the page below it.
  useEffect(() => {
    apiAll<BaseRecord>('/activities').then(setActivityOptions).catch(() => setActivityOptions([]))
  }, [])

  const isPhotoView = activeView === 'PHOTO'
  const filtersActive = Boolean(search || unitFilter || status)
  const error = actionError || media.error || activities.error

  const switchView = (view: LibraryView) => {
    setActiveView(view)
    setTitle('')
    setFile(null)
    setFileInputKey(value => value + 1)
    setActionError('')
  }

  const refreshAll = async () => {
    await Promise.all([media.reload(), activities.reload()])
  }

  const upload = async (event: FormEvent) => {
    event.preventDefault()
    if (!file) return
    setSaving(true)
    setActionError('')
    const body = new FormData()
    body.append('activityId', activityId)
    body.append('mediaType', activeView)
    body.append('title', title)
    body.append('file', file)
    try {
      await api('/activity-media', { method: 'POST', body })
      setFile(null)
      setTitle('')
      setFileInputKey(value => value + 1)
      await media.reload()
    } catch (err) {
      const label = isPhotoView ? 'ảnh' : 'tài liệu'
      setActionError(err instanceof Error ? err.message : `Không thể tải ${label} hoạt động`)
    } finally {
      setSaving(false)
    }
  }

  const removeMedia = async (item: ActivityMedia) => {
    if (!window.confirm(`Xóa tệp “${item.title ?? item.fileName}”?`)) return
    try {
      await api(`/activity-media/${item.id}`, { method: 'DELETE' })
      await media.reload()
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Không thể xóa tệp hoạt động')
    }
  }

  const handlePlanImported = async (result: Parameters<NonNullable<ComponentProps<typeof ExcelImportActions>['onImported']>>[0]) => {
    await refreshAll()
    setActivityOptions(await apiAll<BaseRecord>('/activities').catch(() => []))
    const summary = importSummary(result)
    if (result.errors.length) {
      setTransferMessage('')
      setActionError(`${summary} Lỗi: ${result.errors.slice(0, 3).join(' · ')}`)
    } else {
      setActionError('')
      setTransferMessage(summary)
    }
  }

  const filterBar = <TableFilterBar>
    <FilterField label="Công ty">
      <select aria-label="Lọc theo CĐCS" value={unitFilter} onChange={event => setUnitFilter(event.target.value)}>
        <option value="">Tất cả</option>
        {units.map(unit => <option key={unit.id} value={unit.id}>{unit.code}</option>)}
      </select>
    </FilterField>
    <FilterField label="Trạng thái">
      <select aria-label="Lọc theo trạng thái hoạt động" value={status} onChange={event => setStatus(event.target.value)}>
        <option value="">Tất cả</option>
        <option value="IN_PROGRESS">Đang triển khai</option>
        <option value="COMPLETED">Đã hoàn tất</option>
      </select>
    </FilterField>
    <FilterField label="Tìm kiếm" search>
      <input value={search} onChange={event => setSearch(event.target.value)} placeholder="Mã / tên hoạt động…" />
    </FilterField>
  </TableFilterBar>

  return <section className="page-section">
    <div className="page-heading">
      <div>
        <p className="eyebrow">Hoạt động / Thư viện</p>
        <h1>Thư viện hoạt động</h1>
        <p>Ảnh sự kiện và hồ sơ, chứng từ được quản lý riêng theo từng hoạt động.</p>
      </div>
      <div className="page-actions">
        <ExcelImportActions
          resource="activities"
          filename="mau-ke-hoach-hoat-dong.xlsx"
          templateLabel="Tải mẫu kế hoạch"
          importLabel="Nhập kế hoạch Excel"
          onError={message => { setTransferMessage(''); setActionError(message) }}
          onImported={handlePlanImported}
        />
        <button type="button" className="button button--primary" onClick={onCreateActivity}>+ Tạo hoạt động mới</button>
      </div>
    </div>

    {error && <div className="alert alert--danger">{error}</div>}
    {transferMessage && <div className="alert alert--success">{transferMessage}</div>}

    <div className="notice">
      <strong>Khung chương trình được tạo từ dữ liệu hoạt động</strong>
      <span>Tạo thủ công bằng nút “Tạo hoạt động mới” hoặc nhập nhiều kế hoạch từ file Excel. Sau đó chọn hoạt động để tải ảnh và tài liệu liên quan.</span>
    </div>

    <div className="library-switch" role="tablist" aria-label="Loại thư viện hoạt động">
      <button
        type="button"
        role="tab"
        aria-selected={isPhotoView}
        className={`library-switch__button${isPhotoView ? ' is-active' : ''}`}
        onClick={() => switchView('PHOTO')}
      >
        <span>Ảnh hoạt động</span>
        <small>{media.facets.metrics.photos ?? 0} ảnh</small>
      </button>
      <button
        type="button"
        role="tab"
        aria-selected={!isPhotoView}
        className={`library-switch__button${!isPhotoView ? ' is-active' : ''}`}
        onClick={() => switchView('DOCUMENT')}
      >
        <span>Tài liệu / chứng từ</span>
        <small>{media.facets.metrics.documents ?? 0} tệp</small>
      </button>
    </div>

    <form className="data-card upload-strip library-upload" onSubmit={event => void upload(event)}>
      <div>
        <p className="eyebrow">{isPhotoView ? 'Tải ảnh' : 'Tải tài liệu'}</p>
        <strong>{isPhotoView ? 'Ảnh check-in hoặc khoảnh khắc hoạt động' : 'Kế hoạch, kịch bản, biên bản hoặc chứng từ hoạt động'}</strong>
      </div>
      <select required value={activityId} onChange={event => setActivityId(event.target.value)} aria-label="Hoạt động">
        <option value="">Chọn hoạt động…</option>
        {activityOptions.map(item => <option key={item.id} value={item.id}>{String(item.activityCode)} · {String(item.name)}</option>)}
      </select>
      <input
        value={title}
        onChange={event => setTitle(event.target.value)}
        placeholder={isPhotoView ? 'Chú thích ảnh…' : 'Tên hoặc mô tả tài liệu…'}
        aria-label={isPhotoView ? 'Chú thích ảnh' : 'Tên tài liệu'}
      />
      <input
        key={fileInputKey}
        type="file"
        required
        accept={isPhotoView ? 'image/*' : '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.zip,.rar,image/*'}
        onChange={event => setFile(event.target.files?.[0] ?? null)}
        aria-label={isPhotoView ? 'Chọn ảnh' : 'Chọn tài liệu'}
      />
      <button className="button button--primary" disabled={saving}>
        {saving ? 'Đang tải…' : isPhotoView ? 'Tải ảnh lên' : 'Tải tài liệu lên'}
      </button>
    </form>

    <ListCard
      list={media}
      className="list-card--grid"
      unit={isPhotoView ? 'ảnh' : 'tệp'}
      title={`${media.total} ${isPhotoView ? 'ảnh' : 'tệp'}`}
      subtitle={isPhotoView ? 'Kho ảnh hoạt động' : 'Kho tài liệu và chứng từ'}
      actions={<>
        {filtersActive && <button className="button button--ghost" onClick={() => { setSearch(''); setUnitFilter(''); setStatus('') }}>Xóa lọc</button>}
        <button className="button button--ghost" onClick={() => void refreshAll()}>Làm mới</button>
      </>}
      filters={filterBar}
    >
      {media.loading
        ? <div className="empty-state">Đang tải thư viện…</div>
        : !media.rows.length
          ? <div className="empty-state">{isPhotoView ? 'Chưa có ảnh phù hợp bộ lọc.' : 'Chưa có tài liệu hoặc chứng từ phù hợp bộ lọc.'}</div>
          : <div className="gallery-grid">{media.rows.map(item => <article className="gallery-card data-card" key={item.id}>
              {isPhotoView
                ? <AuthenticatedImage item={item} />
                : <div className="gallery-placeholder gallery-placeholder--document">TỆP</div>}
              <div>
                <span>{enumLabel(item.mediaType)} · {item.unionUnit.code}</span>
                <strong>{item.title ?? item.fileName}</strong>
                <small>{item.activityName}</small>
                <small>{item.fileName}</small>
                <div className="gallery-card__actions">
                  <button type="button" onClick={() => void downloadFile(`/activity-media/${item.id}/download`, item.fileName)}>Tải xuống</button>
                  <button type="button" className="icon-button icon-button--danger" onClick={() => void removeMedia(item)}>Xóa</button>
                </div>
              </div>
            </article>)}</div>}
    </ListCard>

    <ListCard
      list={activities}
      className="list-card--grid list-card--spaced"
      unit="hoạt động"
      title={`${activities.total} hoạt động`}
      subtitle="Theo dõi trước, trong và sau chương trình"
      actions={<button className="button button--ghost" onClick={() => void activities.reload()}>Làm mới</button>}
    >
      {activities.loading
        ? <div className="empty-state">Đang tải hoạt động…</div>
        : !activities.rows.length
          ? <div className="empty-state">{filtersActive ? 'Không có hoạt động phù hợp bộ lọc.' : 'Chưa có hoạt động.'}</div>
          : <div className="activity-lifecycle-grid">{activities.rows.map(item => <article className="data-card lifecycle-card" key={item.id}>
              <div className="lifecycle-card__head">
                <div>
                  <strong>{String(item.name)}</strong>
                  <span>{String(item.activityCode)} · {item.unionUnit?.code} · {formatDate(item.eventDate)}</span>
                </div>
                <b>{enumLabel(item.status)}</b>
              </div>
              <div className="lifecycle-columns">
                <section>
                  <span>Trước chương trình</span>
                  <small>Mục tiêu: {String(item.objective ?? 'chưa có')}</small>
                  <small>Ngân sách: {formatMoney(item.plannedBudget as number)}</small>
                  <small>Danh sách: {item.participantList ? 'đã có' : 'chưa có'}</small>
                  <small>Phê duyệt: {item.status === 'PLANNED' ? 'đang chờ' : 'đã qua bước duyệt'}</small>
                </section>
                <section>
                  <span>Trong chương trình</span>
                  <small>Check-in: {Number(item.checkInCount ?? 0)}/{Number(item.participantCount ?? 0)}</small>
                  <small>Phản hồi: {String(item.quickFeedback ?? 'chưa có')}</small>
                  <small>Phát sinh: {String(item.issues ?? 'không ghi nhận')}</small>
                </section>
                <section>
                  <span>Sau chương trình</span>
                  <small>Chi phí: {formatMoney(item.actualCost as number)}</small>
                  <small>Chứng từ: {enumLabel(item.documentStatus)}</small>
                  <small>Đánh giá: {item.usefulnessScore ? `${item.usefulnessScore}/5` : 'chưa có'}</small>
                  <small>Follow-up: {String(item.followUpOwner ?? 'chưa giao')}</small>
                  <small>Bài học: {String(item.lessonsLearned ?? 'chưa có')}</small>
                </section>
              </div>
            </article>)}</div>}
    </ListCard>
  </section>
}
