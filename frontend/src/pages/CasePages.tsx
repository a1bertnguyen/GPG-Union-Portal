import { useEffect, useMemo, useState } from 'react'
import { api, apiAll, downloadFile, enumLabel, formatDate } from '../api'
import CrudPage, { type FormState } from '../components/CrudPage'
import type { BaseRecord, LaborCase, LaborCaseDocument, UnionUnit } from '../types'
import { caseColumns } from '../portal/crudColumns'
import { caseFields } from '../portal/crudFields'
import { casePresetFilters, caseSummary } from '../portal/crudSummaries'

const lockedStatuses = ['PENDING_APPROVAL', 'CLOSED']

export default function LaborCasePage({ units, isAdmin }: { units: UnionUnit[]; isAdmin: boolean }) {
  const [members, setMembers] = useState<BaseRecord[]>([])
  const [issueGroups, setIssueGroups] = useState<BaseRecord[]>([])

  useEffect(() => {
    Promise.all([apiAll<BaseRecord>('/members'), apiAll<BaseRecord>('/case-issue-groups')])
      .then(([memberRows, groupRows]) => { setMembers(memberRows); setIssueGroups(groupRows) })
      .catch(() => { setMembers([]); setIssueGroups([]) })
  }, [])

  const fields = useMemo(() => caseFields.map(field => {
    if (field.name === 'employeeCode') return {
      ...field,
      placeholder: 'Nhập mã NV để gợi ý thông tin',
      suggestions: members.map(member => ({
        value: String(member.employeeCode ?? ''),
        label: `${String(member.employeeCode ?? '')} · ${String(member.fullName ?? '')}`,
      })).filter(option => option.value),
    }
    if (field.name === 'issueGroup') return {
      ...field,
      options: issueGroups.map(group => ({
        value: String(group.name ?? ''),
        label: `${String(group.code ?? '')} · ${String(group.name ?? '')}`,
        disabled: group.active === false,
      })).filter(option => option.value),
    }
    if (field.name === 'responseDate') return { ...field, readOnly: true }
    if (!isAdmin && ['ownerName', 'deadline', 'status'].includes(field.name)) return { ...field, readOnly: true }
    if (field.name === 'status' && !isAdmin) return {
      ...field,
      readOnly: true,
      options: field.options?.filter(option => !lockedStatuses.includes(option.value)),
    }
    return field
  }), [isAdmin, issueGroups, members])

  const deriveForm = (form: FormState, changedField: string): FormState => {
    if (changedField !== 'employeeCode') return form
    const code = String(form.employeeCode ?? '').trim().toLocaleLowerCase('vi')
    const member = members.find(item => String(item.employeeCode ?? '').trim().toLocaleLowerCase('vi') === code)
    if (!member) return form
    return {
      ...form,
      requesterName: String(member.fullName ?? ''),
      jobTitle: String(member.jobTitle ?? member.professionalTitle ?? ''),
      workplace: String(member.workplace ?? ''),
      phone: String(member.phone ?? ''),
      startWorkDate: String(member.startWorkDate ?? ''),
      unionUnitId: String(member.unionUnit?.id ?? form.unionUnitId ?? ''),
    }
  }

  const uploadAttachment = async (saved: BaseRecord, form: FormState) => {
    const file = form.attachmentFile
    if (!(file instanceof File)) return
    const body = new FormData()
    body.append('caseId', String(saved.id))
    body.append('file', file)
    await api('/case-documents', { method: 'POST', body })
  }

  return <CrudPage
    endpoint="/cases"
    title="Kiến nghị"
    description="Theo dõi và xử lý kiến nghị người lao động."
    singular="Kiến nghị"
    fields={fields}
    columns={caseColumns}
    units={units}
    wideForm
    excelResource="cases"
    excelFilename="mau-kien-nghi.xlsx"
    excelDownloadPath="/spreadsheets/cases/export.xlsx"
    excelDownloadLabel="Xuất sổ kiến nghị"
    summaryBuilder={caseSummary}
    presetFilters={casePresetFilters}
    deriveForm={deriveForm}
    afterSave={uploadAttachment}
    detailActionLabel={isAdmin ? 'Giao việc / duyệt' : 'Xử lý / gửi duyệt'}
    detailRenderer={(item, refresh) => <LaborCaseDetail item={item} refresh={refresh} isAdmin={isAdmin} />}
    canEditItem={item => isAdmin || ['NEW', 'IN_PROGRESS', 'WAITING_RESPONSE'].includes(String(item.status))}
    canDeleteItem={item => isAdmin || !lockedStatuses.includes(String(item.status))}
  />
}

function LaborCaseDetail({ item, refresh, isAdmin }: {
  item: BaseRecord
  refresh: () => Promise<void>
  isAdmin: boolean
}) {
  const record = item as LaborCase
  const [status, setStatus] = useState(record.status)
  const [approvedBy, setApprovedBy] = useState(record.approvedBy)
  const [approvedAt, setApprovedAt] = useState(record.approvedAt)
  const [ownerName, setOwnerName] = useState(record.ownerName ?? '')
  const [deadline, setDeadline] = useState(record.deadline ?? '')
  const [responseDate, setResponseDate] = useState(record.responseDate)
  const [documents, setDocuments] = useState<LaborCaseDocument[]>([])
  const [attachment, setAttachment] = useState<File | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    apiAll<LaborCaseDocument>('/case-documents', { caseId: record.id })
      .then(setDocuments)
      .catch(err => setError(err instanceof Error ? err.message : 'Không thể tải tài liệu đính kèm'))
  }, [record.id])

  const transition = async (action: 'submit-approval' | 'approve') => {
    const assigning = action === 'approve' && status === 'NEW'
    if (assigning && (!ownerName.trim() || !deadline)) {
      setError('ADMIN cần chọn đầy đủ PIC và deadline trước khi duyệt tiếp nhận.')
      return
    }
    const prompt = assigning
      ? `Duyệt tiếp nhận và giao kiến nghị ${record.caseCode} cho ${ownerName}?`
      : action === 'approve'
        ? `Duyệt kết quả và đóng kiến nghị ${record.caseCode}?`
      : `Gửi kết quả kiến nghị ${record.caseCode} cho ADMIN duyệt?`
    if (!window.confirm(prompt)) return
    setBusy(true)
    setError('')
    try {
      const updated = await api<LaborCase>(`/cases/${record.id}/${action}`, {
        method: 'POST',
        body: assigning ? JSON.stringify({ ownerName: ownerName.trim(), deadline }) : undefined,
      })
      setStatus(updated.status)
      setOwnerName(updated.ownerName ?? '')
      setDeadline(updated.deadline ?? '')
      setResponseDate(updated.responseDate)
      setApprovedBy(updated.approvedBy)
      setApprovedAt(updated.approvedAt)
      await refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể chuyển trạng thái kiến nghị')
    } finally {
      setBusy(false)
    }
  }

  const canSubmit = !isAdmin && ['IN_PROGRESS', 'WAITING_RESPONSE'].includes(status)
  const canAssign = isAdmin && status === 'NEW'
  const canApprove = isAdmin && status === 'PENDING_APPROVAL'
  const uploadDocument = async () => {
    if (!attachment) return
    setBusy(true)
    setError('')
    const body = new FormData()
    body.append('caseId', String(record.id))
    body.append('file', attachment)
    try {
      const uploaded = await api<LaborCaseDocument>('/case-documents', { method: 'POST', body })
      setDocuments(current => [uploaded, ...current])
      setAttachment(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tải tệp đính kèm')
    } finally {
      setBusy(false)
    }
  }

  return <div className="member-detail">
    {error && <div className="alert alert--danger">{error}</div>}
    <div className="member-detail__identity">
      <div><span>Mã kiến nghị</span><strong>{record.caseCode}</strong></div>
      <div><span>Mã NV người được hỗ trợ</span><strong>{record.employeeCode ?? '—'}</strong></div>
      <div><span>Người được hỗ trợ</span><strong>{record.requesterName}</strong></div>
      <div><span>Chức danh</span><strong>{record.jobTitle ?? '—'}</strong></div>
      <div><span>Nơi làm việc</span><strong>{record.workplace ?? '—'}</strong></div>
      <div><span>Điện thoại</span><strong>{record.phone ?? '—'}</strong></div>
      <div><span>Ngày tiếp nhận</span><strong>{formatDate(record.receivedDate)}</strong></div>
      <div><span>PIC</span><strong>{ownerName || 'Chờ ADMIN giao'}</strong></div>
      <div><span>Hạn xử lý</span><strong>{formatDate(deadline)}</strong></div>
      <div><span>Ngày trả lời</span><strong>{formatDate(responseDate)}</strong></div>
      <div><span>Trạng thái</span><strong>{enumLabel(status)}</strong></div>
    </div>

    <section>
      <div className="section-title"><div><p className="eyebrow">Nội dung xử lý</p><h3>Kiến nghị và kết quả</h3></div></div>
      <div className="notice notice--compact"><div><strong>Kiến nghị</strong><span>{record.description}</span></div></div>
      <div className="notice notice--compact"><div><strong>Kết quả xử lý</strong><span>{record.resultText || 'Chưa cập nhật. USER cần sửa hồ sơ và nhập kết quả trước khi gửi duyệt.'}</span></div></div>
    </section>

    <section>
      <div className="section-title"><div><p className="eyebrow">Tài liệu đính kèm</p><h3>{documents.length ? `${documents.length} tệp đã tải` : 'Chưa có tệp'}</h3></div></div>
      {documents.length > 0 && <div className="report-evidence-files">{documents.map(document => <button type="button" key={document.id} onClick={() => void downloadFile(`/case-documents/${document.id}/download`, document.fileName)}>{document.fileName}</button>)}</div>}
      {status !== 'CLOSED' && <div className="upload-strip"><input type="file" accept=".pdf,.doc,.docx,.xls,.xlsx,image/*" onChange={event => setAttachment(event.target.files?.[0] ?? null)} /><button type="button" className="button button--ghost" disabled={busy || !attachment} onClick={() => void uploadDocument()}>{busy ? 'Đang tải…' : 'Tải tệp'}</button></div>}
    </section>

    {canAssign && <div className="case-assignment-panel">
      <div>
        <p className="eyebrow">ADMIN duyệt tiếp nhận</p>
        <h3>Giao PIC và deadline xử lý</h3>
        <span>Sau khi duyệt, trạng thái tự chuyển sang Đang xử lý và USER mới có thể nộp kết quả.</span>
      </div>
      <label><span>PIC phụ trách</span><input value={ownerName} onChange={event => setOwnerName(event.target.value)} placeholder="Nhập họ tên PIC" /></label>
      <label><span>Deadline</span><input type="date" min={new Date().toISOString().slice(0, 10)} value={deadline} onChange={event => setDeadline(event.target.value)} /></label>
      <button className="button button--primary" disabled={busy} onClick={() => void transition('approve')}>{busy ? 'Đang duyệt…' : 'Duyệt & giao xử lý'}</button>
    </div>}

    {canSubmit && <div className="notice notice--compact">
      <div><strong>Hoàn tất phần xử lý của USER</strong><span>Hãy sửa hồ sơ để nhập kết quả, tải tệp đính kèm và nêu Lý do quá hạn/ETA mới nếu đã trễ deadline. Ngày trả lời sẽ tự ghi là ngày gửi.</span></div>
      <button className="button button--primary" disabled={busy} onClick={() => void transition('submit-approval')}>{busy ? 'Đang gửi…' : 'Gửi ADMIN duyệt'}</button>
    </div>}

    {canApprove && <div className="notice notice--compact">
      <div><strong>Kết quả đang chờ duyệt</strong><span>Duyệt sẽ đóng hồ sơ và ghi nhận tài khoản ADMIN thực hiện.</span></div>
      <button className="button button--primary" disabled={busy} onClick={() => void transition('approve')}>{busy ? 'Đang duyệt…' : 'Duyệt & đóng hồ sơ'}</button>
    </div>}

    {status === 'CLOSED' && <div className="notice notice--compact">
      <strong>Hồ sơ đã được ADMIN duyệt</strong>
      <span>{approvedBy ? `${approvedBy}${approvedAt ? ` · ${new Date(approvedAt).toLocaleString('vi-VN')}` : ''}` : 'Đã đóng hồ sơ'}</span>
    </div>}
  </div>
}
