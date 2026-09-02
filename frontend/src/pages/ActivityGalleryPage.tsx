import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { api, apiAll, downloadFile, enumLabel, formatDate, loadFileUrl } from '../api'
import ListCard from '../components/ListCard'
import TableFilterBar, { FilterField } from '../components/TableFilterBar'
import { usePagedList } from '../hooks/usePagedList'
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

export default function ActivityGalleryPage({ units }: { units: UnionUnit[] }) {
  const [activityOptions, setActivityOptions] = useState<BaseRecord[]>([])
  const [allMedia, setAllMedia] = useState<ActivityMedia[]>([])
  const [activeView, setActiveView] = useState<LibraryView>('PHOTO')
  const [activityId, setActivityId] = useState('')
  const [title, setTitle] = useState('')
  const [file, setFile] = useState<File | null>(null)
  const [fileInputKey, setFileInputKey] = useState(0)
  const [status, setStatus] = useState('')
  const [unitFilter, setUnitFilter] = useState('')
  const [search, setSearch] = useState('')
  const [actionError, setActionError] = useState('')
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
    Promise.all([apiAll<BaseRecord>('/activities'), apiAll<ActivityMedia>('/activity-media')])
      .then(([activityRows, mediaRows]) => { setActivityOptions(activityRows); setAllMedia(mediaRows) })
      .catch(() => { setActivityOptions([]); setAllMedia([]) })
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
    const [activityRows, mediaRows] = await Promise.all([
      apiAll<BaseRecord>('/activities'), apiAll<ActivityMedia>('/activity-media'),
    ])
    setActivityOptions(activityRows)
    setAllMedia(mediaRows)
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
      await refreshAll()
    } catch (err) {
      const label = isPhotoView ? 'ảnh' : 'tài liệu'
      setActionError(err instanceof Error ? err.message : `Không thể tải ${label} chương trình`)
    } finally {
      setSaving(false)
    }
  }

  const removeMedia = async (item: ActivityMedia) => {
    if (!window.confirm(`Xóa tệp “${item.title ?? item.fileName}”?`)) return
    try {
      await api(`/activity-media/${item.id}`, { method: 'DELETE' })
      await refreshAll()
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Không thể xóa tệp chương trình')
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
      <select aria-label="Lọc theo trạng thái chương trình" value={status} onChange={event => setStatus(event.target.value)}>
        <option value="">Tất cả</option>
        <option value="IN_PROGRESS">Đang triển khai</option>
        <option value="COMPLETED">Đã hoàn tất</option>
      </select>
    </FilterField>
    <FilterField label="Tìm kiếm" search>
      <input value={search} onChange={event => setSearch(event.target.value)} placeholder="Mã / tên chương trình…" />
    </FilterField>
  </TableFilterBar>

  return <section className="page-section">
    <div className="page-heading">
      <div>
        <p className="eyebrow">Chương trình / Chứng cứ báo cáo</p>
        <h1>Ảnh & tài liệu báo cáo</h1>
        <p>Quản lý ảnh minh chứng và chứng từ bắt buộc theo từng chương trình trước khi USER nộp báo cáo.</p>
      </div>
    </div>

    {error && <div className="alert alert--danger">{error}</div>}

    <div className="notice">
      <strong>Chứng cứ bắt buộc của báo cáo</strong>
      <span>Mỗi báo cáo cần ít nhất 1 ảnh và 1 tài liệu. Danh sách tham gia và báo cáo được tải hoặc nhập tại mục “Báo cáo sau chương trình”.</span>
    </div>

    <div className="library-switch" role="tablist" aria-label="Loại thư viện chương trình">
      <button
        type="button"
        role="tab"
        aria-selected={isPhotoView}
        className={`library-switch__button${isPhotoView ? ' is-active' : ''}`}
        onClick={() => switchView('PHOTO')}
      >
        <span>Ảnh minh chứng</span>
        <small>{media.facets.metrics.photos ?? 0} ảnh</small>
      </button>
      <button
        type="button"
        role="tab"
        aria-selected={!isPhotoView}
        className={`library-switch__button${!isPhotoView ? ' is-active' : ''}`}
        onClick={() => switchView('DOCUMENT')}
      >
        <span>Chứng từ báo cáo</span>
        <small>{media.facets.metrics.documents ?? 0} tệp</small>
      </button>
    </div>

    <form className="data-card upload-strip library-upload" onSubmit={event => void upload(event)}>
      <div>
        <p className="eyebrow">Minh chứng bắt buộc</p>
        <strong>{isPhotoView ? 'Ảnh check-in, triển khai hoặc kết quả chương trình' : 'Hóa đơn, chứng từ, biên bản hoặc tài liệu kết quả'}</strong>
      </div>
      <select required value={activityId} onChange={event => setActivityId(event.target.value)} aria-label="Chương trình">
        <option value="">Chọn chương trình…</option>
        {activityOptions.map(item => <option key={item.id} value={item.id}>{String(item.activityCode)} · {String(item.name)}</option>)}
      </select>
      <input
        value={title}
        onChange={event => setTitle(event.target.value)}
        placeholder={isPhotoView ? 'Mô tả ảnh minh chứng…' : 'Tên chứng từ / tài liệu báo cáo…'}
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
      subtitle={isPhotoView ? 'Ảnh dùng làm minh chứng báo cáo' : 'Chứng từ và tài liệu đính kèm báo cáo'}
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
      unit="chương trình"
      title={`${activities.total} chương trình`}
      subtitle="Kiểm tra mức độ sẵn sàng trước khi nộp báo cáo chương trình"
      actions={<button className="button button--ghost" onClick={() => void refreshAll()}>Làm mới</button>}
    >
      {activities.loading
        ? <div className="empty-state">Đang kiểm tra chứng cứ báo cáo…</div>
        : !activities.rows.length
          ? <div className="empty-state">{filtersActive ? 'Không có chương trình phù hợp bộ lọc.' : 'Chưa có chương trình.'}</div>
          : <div className="report-evidence-grid">{activities.rows.map(item => {
              const evidence = allMedia.filter(mediaItem => mediaItem.activityId === item.id)
              const photoCount = evidence.filter(mediaItem => mediaItem.mediaType === 'PHOTO').length
              const documentCount = evidence.filter(mediaItem => mediaItem.mediaType === 'DOCUMENT').length
              const checks = [
                ['Ảnh minh chứng', photoCount > 0, `${photoCount} tệp`],
                ['Chứng từ', documentCount > 0, `${documentCount} tệp`],
                ['Danh sách tham dự', Boolean(item.participantList), item.participantList ? 'Đã có' : 'Chưa có'],
                ['Báo cáo', Boolean(item.quickFeedback), item.quickFeedback ? 'Đã có' : 'Chưa có'],
              ] as const

              return <article className="data-card report-evidence-card" key={item.id}>
                <div className="lifecycle-card__head">
                  <div>
                    <strong>{String(item.name)}</strong>
                    <span>{String(item.activityCode)} · {item.unionUnit?.code} · {formatDate(item.eventDate)}</span>
                  </div>
                  <b>{enumLabel(item.status)}</b>
                </div>
                <div className="report-evidence-checks">
                  {checks.map(([label, complete, detail]) => <div className={`report-evidence-check ${complete ? 'report-evidence-check--complete' : 'report-evidence-check--missing'}`} key={label}>
                    <span>{complete ? '✓' : '!'}</span>
                    <div>
                      <strong>{label}</strong>
                      <small>{detail}</small>
                    </div>
                  </div>)}
                </div>
                <div className="report-evidence-card__footer">
                  <span className={`status status--${item.reportCompleted ? 'success' : 'warning'}`}>
                    {item.reportCompleted ? 'Đã hoàn tất báo cáo' : 'Chưa hoàn tất báo cáo'}
                  </span>
                  <button type="button" className="button button--ghost" onClick={() => {
                    setActivityId(String(item.id))
                    window.scrollTo({ top: 0, behavior: 'smooth' })
                  }}>Chọn để tải tệp</button>
                </div>
              </article>
            })}</div>}
    </ListCard>
  </section>
}
