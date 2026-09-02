import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { api, apiAll, downloadFile, enumLabel, formatDate } from '../api'
import ListCard from '../components/ListCard'
import TableFilterBar, { FilterField } from '../components/TableFilterBar'
import { usePagedList } from '../hooks/usePagedList'
import type { BaseRecord, MemberChange, MemberCompliance, MemberDocument, UnionUnit } from '../types'

const documentTypes = ['JOIN_APPLICATION', 'DECISION', 'BCH_DOCUMENT'] as const
const today = () => new Date().toISOString().slice(0, 10)
const fileSize = (bytes: number) => bytes < 1024 * 1024 ? `${Math.ceil(bytes / 1024)} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`

type WorkspaceProps = { units: UnionUnit[] }

export function MemberChangesPage({ units }: WorkspaceProps) {
  const [members, setMembers] = useState<BaseRecord[]>([])
  const [memberId, setMemberId] = useState('')
  const [changeType, setChangeType] = useState('CẬP NHẬT THÔNG TIN')
  const [effectiveDate, setEffectiveDate] = useState(today())
  const [description, setDescription] = useState('')
  const [search, setSearch] = useState('')
  const [unitFilter, setUnitFilter] = useState('')
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)

  const filters = useMemo(() => ({
    q: search.trim() || undefined,
    unitId: unitFilter || undefined,
  }), [search, unitFilter])

  const list = usePagedList<MemberChange>({ endpoint: '/member-changes', filters })
  const filtersActive = Boolean(search || unitFilter)

  // The member picker needs every member the account can see, not one page of them.
  const loadMembers = useCallback(async () => {
    try {
      setMembers(await apiAll<BaseRecord>('/members'))
    } catch {
      setMembers([])
    }
  }, [])
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { void loadMembers() }, [loadMembers])

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setSaving(true)
    setFormError('')
    try {
      await api('/member-changes', { method: 'POST', body: JSON.stringify({ memberId: Number(memberId), changeType, effectiveDate, description }) })
      setDescription('')
      await list.reload()
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Không thể cập nhật thông tin')
    } finally {
      setSaving(false)
    }
  }

  const error = formError || list.error

  return <section className="page-section">
    <div className="page-heading"><div><p className="eyebrow">Đoàn viên / Tổng quan</p><h1>Cập nhật thông tin đoàn viên</h1><p>Ghi nhận thay đổi nhân sự, tình trạng công đoàn và lịch sử cập nhật theo từng đoàn viên.</p></div></div>
    {error && <div className="alert alert--danger">{error}</div>}
    <div className="workspace-grid">
      <form className="data-card compact-form-card" onSubmit={event => void submit(event)}>
        <div><p className="eyebrow">Cập nhật thông tin</p><h2>Ghi nhận thay đổi mới</h2></div>
        <label className="field"><span>Đoàn viên *</span><select required value={memberId} onChange={event => setMemberId(event.target.value)}><option value="">Chọn đoàn viên…</option>{members.map(member => <option key={member.id} value={member.id}>{String(member.employeeCode)} · {String(member.fullName)}</option>)}</select></label>
        <label className="field"><span>Loại cập nhật *</span><select required value={changeType} onChange={event => setChangeType(event.target.value)}><option>CẬP NHẬT THÔNG TIN</option><option>THAY ĐỔI ĐƠN VỊ</option><option>THAY ĐỔI CHỨC DANH</option><option>THAY ĐỔI TRẠNG THÁI</option><option>RỜI CÔNG ĐOÀN</option></select></label>
        <label className="field"><span>Ngày hiệu lực *</span><input type="date" required value={effectiveDate} onChange={event => setEffectiveDate(event.target.value)} /></label>
        <label className="field"><span>Nội dung *</span><textarea required value={description} onChange={event => setDescription(event.target.value)} placeholder="Mô tả nội dung trước/sau hoặc lý do thay đổi…" /></label>
        <button className="button button--primary" disabled={saving}>{saving ? 'Đang lưu…' : 'Lưu cập nhật'}</button>
      </form>

      <ListCard
        list={list}
        unit="lần cập nhật"
        title={`${list.total} lần cập nhật`}
        subtitle={list.total === list.facets.total ? undefined : `Trên tổng ${list.facets.total}`}
        actions={<>
          {filtersActive && <button className="button button--ghost" onClick={() => { setSearch(''); setUnitFilter('') }}>Xóa lọc</button>}
          <button className="button button--ghost" onClick={() => void list.reload()}>Làm mới</button>
        </>}
        filters={<TableFilterBar>
          <FilterField label="Công ty"><select aria-label="Lọc theo CĐCS" value={unitFilter} onChange={event => setUnitFilter(event.target.value)}><option value="">Tất cả</option>{units.map(unit => <option key={unit.id} value={unit.id}>{unit.code}</option>)}</select></FilterField>
          <FilterField label="Tìm kiếm" search><input value={search} onChange={event => setSearch(event.target.value)} placeholder="Tên / mã NV / nội dung…" /></FilterField>
        </TableFilterBar>}
      >
        {list.loading
          ? <div className="empty-state">Đang tải lịch sử…</div>
          : list.rows.length
            ? <div className="timeline-list">{list.rows.map(change => <article key={change.id}><i /><div><strong>{change.changeType}</strong><span>{change.employeeCode} · {change.memberName} · {change.unionUnit.code}</span><p>{change.description}</p></div><time>{formatDate(change.effectiveDate)}</time></article>)}</div>
            : <div className="empty-state">Chưa có cập nhật thông tin phù hợp.</div>}
      </ListCard>
    </div>
  </section>
}

export function MemberDocumentsPage({ units }: WorkspaceProps) {
  const [members, setMembers] = useState<BaseRecord[]>([])
  const [memberId, setMemberId] = useState('')
  const [memberSearch, setMemberSearch] = useState('')
  const [memberSuggestionsOpen, setMemberSuggestionsOpen] = useState(false)
  const [documentType, setDocumentType] = useState<(typeof documentTypes)[number]>('JOIN_APPLICATION')
  const [file, setFile] = useState<File | null>(null)
  const [fileInputKey, setFileInputKey] = useState(0)
  const [search, setSearch] = useState('')
  const [unitFilter, setUnitFilter] = useState('')
  const [complianceFilter, setComplianceFilter] = useState('')
  const [actionError, setActionError] = useState('')
  const [saving, setSaving] = useState(false)

  const filters = useMemo(() => ({
    q: search.trim() || undefined,
    unitId: unitFilter || undefined,
    preset: complianceFilter || undefined,
  }), [search, unitFilter, complianceFilter])

  // One card per member with their required-document status, resolved and paged on the server.
  const list = usePagedList<MemberCompliance>({ endpoint: '/member-documents/compliance', filters })
  const filtersActive = Boolean(search || unitFilter || complianceFilter)

  const loadMembers = useCallback(async () => {
    try {
      setMembers(await apiAll<BaseRecord>('/members'))
    } catch {
      setMembers([])
    }
  }, [])
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { void loadMembers() }, [loadMembers])

  const matchingMembers = useMemo(() => {
    const needle = memberSearch.trim().toLocaleLowerCase('vi')
    if (!needle) return members
    return members.filter(member => `${String(member.employeeCode ?? '')} ${String(member.fullName ?? '')}`
      .toLocaleLowerCase('vi').includes(needle))
  }, [memberSearch, members])

  const selectMember = (member: BaseRecord) => {
    setMemberId(String(member.id))
    setMemberSearch(`${String(member.employeeCode ?? '')} · ${String(member.fullName ?? '')}`)
    setMemberSuggestionsOpen(false)
  }

  const upload = async (event: FormEvent) => {
    event.preventDefault()
    if (!file) return
    if (!memberId) {
      setActionError('Hãy chọn một đoàn viên từ danh sách gợi ý trước khi tải tệp')
      return
    }
    setSaving(true)
    setActionError('')
    const body = new FormData()
    body.append('memberId', memberId)
    body.append('documentType', documentType)
    body.append('file', file)
    try {
      await api('/member-documents', { method: 'POST', body })
      setFile(null)
      setFileInputKey(value => value + 1)
      await list.reload()
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Không thể tải tài liệu')
    } finally {
      setSaving(false)
    }
  }

  const removeDocument = async (document: MemberDocument) => {
    if (!window.confirm(`Xóa tài liệu “${document.fileName}”?`)) return
    try {
      await api(`/member-documents/${document.id}`, { method: 'DELETE' })
      await list.reload()
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Không thể xóa tài liệu')
    }
  }

  const error = actionError || list.error

  return <section className="page-section">
    <div className="page-heading"><div><p className="eyebrow">Đoàn viên / Tổng quan</p><h1>Tài liệu đoàn viên</h1><p>Tìm đoàn viên theo mã NV hoặc tên, rồi tải Đơn gia nhập, Quyết định và Tài liệu BCH.</p></div></div>
    {error && <div className="alert alert--danger">{error}</div>}
    <form className="data-card upload-strip" onSubmit={event => void upload(event)}>
      <div><p className="eyebrow">Bổ sung hồ sơ</p><strong>Tải tài liệu vào hồ sơ đoàn viên</strong></div>
      <div className="member-document-picker">
        <input required value={memberSearch} placeholder="Tìm mã NV hoặc tên đoàn viên…" autoComplete="off" aria-autocomplete="list" aria-expanded={memberSuggestionsOpen}
          onFocus={() => setMemberSuggestionsOpen(true)} onBlur={() => setMemberSuggestionsOpen(false)}
          onChange={event => {
            const next = event.target.value
            setMemberSearch(next)
            const exact = members.find(member => String(member.employeeCode ?? '').toLocaleLowerCase('vi') === next.trim().toLocaleLowerCase('vi'))
            setMemberId(exact ? String(exact.id) : '')
            setMemberSuggestionsOpen(true)
          }} />
        {memberSuggestionsOpen && <div className="member-document-suggestions" role="listbox">
          {matchingMembers.length ? matchingMembers.slice(0, 8).map(member => <button type="button" role="option" key={member.id} onMouseDown={event => event.preventDefault()} onClick={() => selectMember(member)}><strong>{String(member.employeeCode ?? '')}</strong><span>{String(member.fullName ?? '')} · {member.unionUnit?.code ?? '—'}</span></button>) : <span>Không tìm thấy đoàn viên phù hợp.</span>}
        </div>}
      </div>
      <select value={documentType} onChange={event => setDocumentType(event.target.value as typeof documentType)}>{documentTypes.map(type => <option key={type} value={type}>{enumLabel(type)}</option>)}</select>
      <input key={fileInputKey} aria-label="Chọn tài liệu" type="file" accept=".pdf,.doc,.docx,image/*" required onChange={event => setFile(event.target.files?.[0] ?? null)} />
      <button className="button button--primary" disabled={saving}>{saving ? 'Đang tải…' : 'Tải lên'}</button>
    </form>

    <ListCard
      list={list}
      unit="đoàn viên"
      className="list-card--grid"
      title={`${list.total} đoàn viên`}
      subtitle={`Thiếu hồ sơ: ${list.facets.metrics.missing ?? 0} · Đủ hồ sơ: ${list.facets.metrics.complete ?? 0}`}
      actions={<>
        {filtersActive && <button className="button button--ghost" onClick={() => { setSearch(''); setUnitFilter(''); setComplianceFilter('') }}>Xóa lọc</button>}
        <button className="button button--ghost" onClick={() => void list.reload()}>Làm mới</button>
      </>}
      filters={<TableFilterBar>
        <FilterField label="Công ty"><select aria-label="Lọc theo CĐCS" value={unitFilter} onChange={event => setUnitFilter(event.target.value)}><option value="">Tất cả</option>{units.map(unit => <option key={unit.id} value={unit.id}>{unit.code}</option>)}</select></FilterField>
        <FilterField label="Trạng thái"><select aria-label="Lọc tình trạng hồ sơ" value={complianceFilter} onChange={event => setComplianceFilter(event.target.value)}><option value="">Tất cả</option><option value="missing">Còn thiếu</option><option value="complete">Đủ hồ sơ</option></select></FilterField>
        <FilterField label="Tìm kiếm" search><input value={search} onChange={event => setSearch(event.target.value)} placeholder="Tên / mã NV…" /></FilterField>
      </TableFilterBar>}
    >
      {list.loading
        ? <div className="empty-state">Đang tải hồ sơ…</div>
        : !list.rows.length
          ? <div className="empty-state">{filtersActive ? 'Không có đoàn viên phù hợp bộ lọc.' : 'Chưa có đoàn viên.'}</div>
          : <div className="document-compliance-grid">{list.rows.map(row => <article key={row.memberId}>
              <div className="document-card__head">
                <div><strong>{row.memberName}</strong><span>{row.employeeCode} · {row.unionUnit?.code}</span></div>
                <b className={row.missing.length ? 'compliance-badge compliance-badge--missing' : 'compliance-badge'}>{row.missing.length ? `Thiếu ${row.missing.length}` : 'Đủ hồ sơ'}</b>
              </div>
              <div className="required-docs">{documentTypes.map(type => {
                const doc = row.documents.find(item => item.documentType === type)
                return <div key={type} className={doc ? 'required-doc required-doc--done' : 'required-doc'}>
                  <span>{doc ? '✓' : '!'}</span>
                  <div><strong>{enumLabel(type)}</strong>{doc
                    ? <div className="required-doc__actions">
                        <button type="button" onClick={() => void downloadFile(`/member-documents/${doc.id}/download`, doc.fileName)}>{doc.fileName} · {fileSize(doc.fileSize)}</button>
                        <button type="button" className="icon-button icon-button--danger" onClick={() => void removeDocument(doc)}>Xóa</button>
                      </div>
                    : <small>Chưa có tệp</small>}</div>
                </div>
              })}</div>
            </article>)}</div>}
    </ListCard>
  </section>
}

export function MemberDetailPanel({ member, refreshMembers }: { member: BaseRecord; refreshMembers: () => Promise<void> }) {
  const [changes, setChanges] = useState<MemberChange[]>([])
  const [documents, setDocuments] = useState<MemberDocument[]>([])
  const [welfare, setWelfare] = useState<BaseRecord[]>([])
  const [activities, setActivities] = useState<BaseRecord[]>([])
  const [documentType, setDocumentType] = useState<(typeof documentTypes)[number]>('JOIN_APPLICATION')
  const [file, setFile] = useState<File | null>(null)
  const [error, setError] = useState('')

  // This panel summarises one member's whole history, so it asks for complete lists rather than pages.
  const load = useCallback(async () => {
    try {
      const [changeData, documentData, welfareData, activityData] = await Promise.all([
        apiAll<MemberChange>('/member-changes', { memberId: member.id }),
        apiAll<MemberDocument>('/member-documents', { memberId: member.id }),
        apiAll<BaseRecord>('/welfare', { unitId: member.unionUnit?.id }),
        apiAll<BaseRecord>('/activities', { unitId: member.unionUnit?.id }),
      ])
      setChanges(changeData)
      setDocuments(documentData)
      setWelfare(welfareData.filter(item => String(item.beneficiaryName ?? '').toLocaleLowerCase('vi') === String(member.fullName ?? '').toLocaleLowerCase('vi')))
      setActivities(activityData)
      setError('')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tải hồ sơ chi tiết')
    }
  }, [member])
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { void load() }, [load])

  const upload = async (event: FormEvent) => {
    event.preventDefault()
    if (!file) return
    const body = new FormData()
    body.append('memberId', String(member.id))
    body.append('documentType', documentType)
    body.append('file', file)
    try {
      await api('/member-documents', { method: 'POST', body })
      setFile(null)
      await load()
      await refreshMembers()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tải tài liệu')
    }
  }

  const removeDocument = async (document: MemberDocument) => {
    if (!window.confirm(`Xóa tài liệu “${document.fileName}”?`)) return
    try {
      await api(`/member-documents/${document.id}`, { method: 'DELETE' })
      await load()
      await refreshMembers()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể xóa tài liệu')
    }
  }

  return <div className="member-detail">
    {error && <div className="alert alert--danger">{error}</div>}
    <div className="member-detail__identity">
      <div><span>Mã nhân viên</span><strong>{String(member.employeeCode)}</strong></div>
      <div><span>CĐCS</span><strong>{member.unionUnit?.code ?? '—'}</strong></div>
      <div><span>Công ty</span><strong>{String(member.company ?? '—')}</strong></div>
      <div><span>Nơi làm việc</span><strong>{String(member.workplace ?? '—')}</strong></div>
      <div><span>Chức danh công đoàn</span><strong>{String(member.proposedUnionTitle ?? '—')}</strong></div>
      <div><span>Chức vụ chuyên môn</span><strong>{String(member.professionalTitle ?? '—')}</strong></div>
      <div><span>Vị trí công việc</span><strong>{String(member.jobTitle ?? '—')}</strong></div>
      <div><span>Ngày gia nhập CĐ</span><strong>{formatDate(member.joinDate)}</strong></div>
    </div>
    <section>
      <div className="section-title"><div><p className="eyebrow">Thông tin cá nhân</p><h3>Thông tin trích ngang</h3></div></div>
      <div className="member-detail__identity">
        <div><span>Giới tính</span><strong>{enumLabel(member.gender)}</strong></div>
        <div><span>Dân tộc</span><strong>{String(member.ethnicity ?? '—')}</strong></div>
        <div><span>Nơi sinh</span><strong>{String(member.placeOfBirth ?? '—')}</strong></div>
        <div><span>CCCD</span><strong>{String(member.nationalId ?? '—')}</strong></div>
        <div><span>Đảng viên</span><strong>{member.partyMember ? 'Có' : 'Không'}</strong></div>
        <div><span>Học vấn</span><strong>{String(member.education ?? '—')}</strong></div>
        <div><span>Điện thoại</span><strong>{String(member.phone ?? '—')}</strong></div>
        <div><span>Email</span><strong>{String(member.email ?? '—')}</strong></div>
      </div>
    </section>
    <section><div className="section-title"><div><p className="eyebrow">Hồ sơ bắt buộc</p><h3>Tài liệu đi kèm</h3></div><b>{documents.length}/{documentTypes.length} nhóm đã có</b></div>
      <div className="required-docs required-docs--horizontal">{documentTypes.map(type => { const doc = documents.find(item => item.documentType === type); return <div key={type} className={doc ? 'required-doc required-doc--done' : 'required-doc'}><span>{doc ? '✓' : '!'}</span><div><strong>{enumLabel(type)}</strong>{doc ? <div className="required-doc__actions"><button type="button" onClick={() => void downloadFile(`/member-documents/${doc.id}/download`, doc.fileName)}>{doc.fileName}</button><button type="button" className="icon-button icon-button--danger" onClick={() => void removeDocument(doc)}>Xóa</button></div> : <small>Bắt buộc · chưa có</small>}</div></div> })}</div>
      <form className="inline-upload" onSubmit={event => void upload(event)}><select value={documentType} onChange={event => setDocumentType(event.target.value as typeof documentType)}>{documentTypes.map(type => <option key={type} value={type}>{enumLabel(type)}</option>)}</select><input type="file" required accept=".pdf,.doc,.docx,image/*" onChange={event => setFile(event.target.files?.[0] ?? null)} /><button className="button button--primary">Tải tài liệu</button></form>
    </section>
    <div className="member-history-grid"><section><div className="section-title"><div><p className="eyebrow">Lịch sử</p><h3>Cập nhật thông tin</h3></div></div><div className="compact-history">{changes.length ? changes.slice(0, 6).map(change => <article key={change.id}><strong>{change.changeType}</strong><span>{formatDate(change.effectiveDate)} · {change.description}</span></article>) : <p>Chưa có cập nhật thông tin.</p>}</div></section>
      <section><div className="section-title"><div><p className="eyebrow">Lịch sử</p><h3>Chính sách / hoạt động</h3></div></div><div className="compact-history">{welfare.map(item => <article key={`w-${item.id}`}><strong>{enumLabel(item.welfareType)}</strong><span>{formatDate(item.eventDate)} · {enumLabel(item.status)}</span></article>)}{activities.slice(0, 4).map(item => <article key={`a-${item.id}`}><strong>{String(item.name)}</strong><span>{formatDate(item.eventDate)} · {enumLabel(item.status)}</span></article>)}{!welfare.length && !activities.length && <p>Chưa có lịch sử liên quan.</p>}</div></section>
    </div>
  </div>
}
