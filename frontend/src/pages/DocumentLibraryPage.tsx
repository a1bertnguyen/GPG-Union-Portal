import { useMemo, useState, type FormEvent } from 'react'
import { api, downloadFile, formatDate } from '../api'
import ListCard from '../components/ListCard'
import TableFilterBar, { FilterField } from '../components/TableFilterBar'
import { usePagedList } from '../hooks/usePagedList'
import type { DocumentLibraryItem, UnionUnit } from '../types'

const fileSize = (bytes: number) => bytes < 1024 * 1024
  ? `${Math.max(1, Math.ceil(bytes / 1024))} KB`
  : `${(bytes / 1024 / 1024).toFixed(1)} MB`

export default function DocumentLibraryPage({ units, isAdmin, currentUnitName }: {
  units: UnionUnit[]
  isAdmin: boolean
  currentUnitName?: string
}) {
  const [search, setSearch] = useState('')
  const [unitFilter, setUnitFilter] = useState('')
  const [uploadUnitId, setUploadUnitId] = useState('')
  const [category, setCategory] = useState('Văn bản hướng dẫn')
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [file, setFile] = useState<File | null>(null)
  const [fileInputKey, setFileInputKey] = useState(0)
  const [saving, setSaving] = useState(false)
  const [actionError, setActionError] = useState('')
  const [message, setMessage] = useState('')

  const filters = useMemo(() => ({
    q: search.trim() || undefined,
    unitId: isAdmin && unitFilter ? unitFilter : undefined,
  }), [isAdmin, search, unitFilter])
  const list = usePagedList<DocumentLibraryItem>({ endpoint: '/document-library', filters, withFacets: false })

  const upload = async (event: FormEvent) => {
    event.preventDefault()
    if (!file) return
    setSaving(true)
    setActionError('')
    setMessage('')
    const body = new FormData()
    body.append('unionUnitId', uploadUnitId)
    body.append('category', category)
    body.append('title', title)
    body.append('description', description)
    body.append('file', file)
    try {
      await api('/document-library', { method: 'POST', body })
      setTitle('')
      setDescription('')
      setFile(null)
      setFileInputKey(value => value + 1)
      setMessage('Đã đưa tài liệu vào kho của CĐCS được chọn.')
      await list.reload()
    } catch (error) {
      setActionError(error instanceof Error ? error.message : 'Không thể tải tài liệu lên')
    } finally {
      setSaving(false)
    }
  }

  const remove = async (item: DocumentLibraryItem) => {
    if (!window.confirm(`Xóa tài liệu “${item.title}”?`)) return
    try {
      await api(`/document-library/${item.id}`, { method: 'DELETE' })
      await list.reload()
    } catch (error) {
      setActionError(error instanceof Error ? error.message : 'Không thể xóa tài liệu')
    }
  }

  const error = actionError || list.error
  const filterBar = <TableFilterBar>
    {isAdmin && <FilterField label="CĐCS">
      <select value={unitFilter} onChange={event => setUnitFilter(event.target.value)}>
        <option value="">Tất cả CĐCS</option>
        {units.map(unit => <option value={unit.id} key={unit.id}>{unit.code} · {unit.name}</option>)}
      </select>
    </FilterField>}
    <FilterField label="Tìm tài liệu" search>
      <input value={search} onChange={event => setSearch(event.target.value)} placeholder="Tên, nhóm, mô tả hoặc tên tệp…" />
    </FilterField>
  </TableFilterBar>

  return <section className="page-section">
    <div className="page-heading">
      <div>
        <p className="eyebrow">Văn bản dùng chung</p>
        <h1>Kho tài liệu CĐCS</h1>
        <p>{isAdmin
          ? 'ADMIN tải tài liệu cho từng công đoàn cụ thể; mỗi USER chỉ nhìn thấy tài liệu của CĐCS được gán.'
          : `Tài liệu ADMIN đã phân phối cho ${currentUnitName ?? 'CĐCS của bạn'}. Bạn có thể tải tệp về máy để sử dụng.`}</p>
      </div>
    </div>

    {error && <div className="alert alert--danger">{error}</div>}
    {message && <div className="alert alert--success">{message}</div>}

    {isAdmin && <form className="data-card document-upload-panel" onSubmit={event => void upload(event)}>
      <div className="document-upload-panel__intro">
        <p className="eyebrow">Chỉ ADMIN</p>
        <strong>Phân phối tài liệu cho một CĐCS</strong>
        <span>Giới hạn 20 MB/tệp. USER của đơn vị khác không thể xem hoặc tải xuống.</span>
      </div>
      <label><span>CĐCS nhận tài liệu</span><select required value={uploadUnitId} onChange={event => setUploadUnitId(event.target.value)}>
        <option value="">Chọn CĐCS…</option>
        {units.map(unit => <option value={unit.id} key={unit.id}>{unit.code} · {unit.name}</option>)}
      </select></label>
      <label><span>Nhóm tài liệu</span><input required value={category} onChange={event => setCategory(event.target.value)} /></label>
      <label><span>Tên hiển thị</span><input required value={title} onChange={event => setTitle(event.target.value)} placeholder="VD: Hướng dẫn quyết toán tháng 8" /></label>
      <label className="field--wide"><span>Mô tả</span><textarea value={description} onChange={event => setDescription(event.target.value)} placeholder="Mục đích sử dụng hoặc lưu ý…" /></label>
      <label className="field--wide"><span>Tệp tài liệu</span><input key={fileInputKey} type="file" required onChange={event => setFile(event.target.files?.[0] ?? null)} /></label>
      <div className="form-actions field--wide"><button className="button button--primary" disabled={saving}>{saving ? 'Đang tải…' : 'Đưa vào kho tài liệu'}</button></div>
    </form>}

    <ListCard
      list={list}
      unit="tài liệu"
      title={`${list.total} tài liệu`}
      subtitle={isAdmin ? 'Kho tài liệu đã phân phối theo CĐCS' : 'Chỉ hiển thị dữ liệu của CĐCS bạn'}
      filters={filterBar}
      actions={<button className="button button--ghost" onClick={() => void list.reload()}>Làm mới</button>}
    >
      {list.loading
        ? <div className="empty-state">Đang tải kho tài liệu…</div>
        : !list.rows.length
          ? <div className="empty-state">Chưa có tài liệu phù hợp.</div>
          : <div className="document-library-grid">{list.rows.map(item => <article className="data-card document-library-card" key={item.id}>
              <div className="document-library-card__icon">TỆP</div>
              <div className="document-library-card__content">
                <span>{item.category} · {item.unionUnit.code}</span>
                <strong>{item.title}</strong>
                <p>{item.description || 'Không có mô tả bổ sung.'}</p>
                <small>{item.fileName} · {fileSize(item.fileSize)} · {formatDate(item.createdAt)}</small>
                <div className="document-library-card__actions">
                  <button className="button button--ghost" onClick={() => void downloadFile(`/document-library/${item.id}/download`, item.fileName)}>Tải về máy</button>
                  {isAdmin && <button className="icon-button icon-button--danger" onClick={() => void remove(item)}>Xóa</button>}
                </div>
              </div>
            </article>)}</div>}
    </ListCard>
  </section>
}
