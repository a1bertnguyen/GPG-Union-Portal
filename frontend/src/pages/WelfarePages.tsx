import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { api, apiAll, downloadFile, enumLabel, formatDate, formatMoney } from '../api'
import CrudPage, { type FormState } from '../components/CrudPage'
import type { BaseRecord, UnionUnit, WelfareDocument, WelfarePolicy, WelfareRecord } from '../types'
import { welfareColumns, welfarePolicyColumns } from '../portal/crudColumns'
import { welfareFields, welfarePolicyFields } from '../portal/crudFields'
import { welfarePolicySummary, welfarePresetFilters, welfareSummary } from '../portal/crudSummaries'

const deadlineFrom = (dateValue: string, weeks: number) => {
  if (!dateValue || weeks < 1 || weeks > 8) return ''
  const date = new Date(`${dateValue}T00:00:00Z`)
  if (Number.isNaN(date.getTime())) return ''
  date.setUTCDate(date.getUTCDate() + weeks * 7)
  return date.toISOString().slice(0, 10)
}

const documentTypes = ['SUPPORTING_DOCUMENT', 'RECEIPT', 'IMAGE'] as const
const fileSize = (bytes: number) => bytes < 1024 * 1024
  ? `${Math.ceil(bytes / 1024)} KB`
  : `${(bytes / 1024 / 1024).toFixed(1)} MB`

export function WelfarePolicyPage({ units, isAdmin }: { units: UnionUnit[]; isAdmin: boolean }) {
  return <CrudPage
    endpoint="/welfare-policies"
    title="Danh mục chính sách chăm lo"
    description={isAdmin
      ? 'Quản lý định mức, điều kiện áp dụng và thời hạn xử lý từ 1 đến tối đa 8 tuần. Dữ liệu có thể nhập hoặc xuất theo bảng Excel chính sách.'
      : 'Tra cứu chính sách, định mức và thời hạn xử lý đang được ADMIN ban hành.'}
    singular="Chính sách chăm lo"
    fields={welfarePolicyFields}
    columns={welfarePolicyColumns}
    units={units}
    wideForm
    excelResource="welfare-policies"
    excelFilename="bang-chinh-sach-cham-lo.xlsx"
    excelDownloadPath="/welfare-policies/export.xlsx"
    excelDownloadLabel="Xuất Excel"
    canImportExcel={isAdmin}
    canDownloadExcel
    readOnly={!isAdmin}
    readOnlyMessage="USER chỉ được xem và xuất danh mục; chỉ ADMIN được thêm, sửa, xóa hoặc nhập Excel chính sách chăm lo."
    summaryBuilder={welfarePolicySummary}
  />
}

export function WelfareRecordPage({ units, isAdmin }: { units: UnionUnit[]; isAdmin: boolean }) {
  const [policies, setPolicies] = useState<WelfarePolicy[]>([])
  const [policyError, setPolicyError] = useState('')

  useEffect(() => {
    apiAll<WelfarePolicy>('/welfare-policies')
      .then(data => { setPolicies(data); setPolicyError('') })
      .catch(error => { setPolicies([]); setPolicyError(error instanceof Error ? error.message : 'Không thể tải danh mục chính sách') })
  }, [])

  const fields = useMemo(() => welfareFields.map(field => {
    if (field.name === 'policyId') return {
        ...field,
        options: policies.map(policy => ({
          value: String(policy.id),
          label: `${policy.code} · ${policy.name} · ${formatMoney(policy.supportAmount)} · ${policy.processingWeeks} tuần${policy.active ? '' : ' · Ngừng áp dụng'}`,
          disabled: !policy.active,
        })),
      }
    if (field.name === 'status') return { ...field, defaultValue: 'PENDING_APPROVAL', readOnly: !isAdmin }
    if (['documentStatus', 'receiptStatus', 'hasImage'].includes(field.name)) return { ...field, readOnly: true }
    return field
  }), [isAdmin, policies])

  const deriveForm = useCallback((form: FormState, changedField: string) => {
    if (!['policyId', 'eventDate'].includes(changedField)) return form
    const policy = policies.find(item => String(item.id) === String(form.policyId ?? ''))
    if (!policy) {
      return changedField === 'policyId'
        ? { ...form, welfareType: '', standardAmount: '', deadline: '' }
        : form
    }
    const next = {
      ...form,
      welfareType: policy.welfareType,
      standardAmount: String(policy.supportAmount),
      deadline: deadlineFrom(String(form.eventDate ?? ''), policy.processingWeeks),
    }
    return changedField === 'policyId' ? { ...next, amount: String(policy.supportAmount) } : next
  }, [policies])

  const notice = policyError
    ? <div className="notice notice--compact"><strong>Không tải được chính sách</strong><span>{policyError}</span></div>
    : policies.every(policy => !policy.active)
      ? <div className="notice notice--compact"><strong>Chưa có chính sách đang áp dụng</strong><span>Hãy thêm hoặc nhập Excel ở Chăm lo → Chính sách trước khi tạo phiếu mới.</span></div>
      : undefined

  return <CrudPage
    endpoint="/welfare"
    title="Chăm lo, phúc lợi & chính sách"
    description={isAdmin
      ? 'Duyệt yêu cầu do USER gửi, theo dõi chứng từ và tiến độ hoàn tất theo thời hạn của chính sách.'
      : 'Gửi yêu cầu chăm lo, chọn đúng chính sách và nộp chứng từ trực tiếp trên hồ sơ để ADMIN duyệt.'}
    singular="Hồ sơ chăm lo"
    fields={fields}
    columns={welfareColumns}
    units={units}
    wideForm
    excelResource="welfare"
    excelFilename="mau-cham-lo.xlsx"
    canImportExcel={isAdmin}
    summaryBuilder={welfareSummary}
    presetFilters={welfarePresetFilters}
    deriveForm={deriveForm}
    notice={notice}
    detailActionLabel={isAdmin ? 'Hồ sơ / duyệt' : 'Nộp chứng từ'}
    detailRenderer={(item, refresh) => <WelfareDetailPanel item={item} refresh={refresh} isAdmin={isAdmin} />}
    canEditItem={item => isAdmin || item.status === 'PENDING_APPROVAL'}
    canDeleteItem={item => isAdmin || item.status === 'PENDING_APPROVAL'}
  />
}

function WelfareDetailPanel({ item, refresh, isAdmin }: {
  item: BaseRecord
  refresh: () => Promise<void>
  isAdmin: boolean
}) {
  const record = item as WelfareRecord
  const [documents, setDocuments] = useState<WelfareDocument[]>([])
  const [documentType, setDocumentType] = useState<(typeof documentTypes)[number]>('SUPPORTING_DOCUMENT')
  const [file, setFile] = useState<File | null>(null)
  const [fileInputKey, setFileInputKey] = useState(0)
  const [status, setStatus] = useState(record.status)
  const [saving, setSaving] = useState(false)
  const [approving, setApproving] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    try {
      setDocuments(await api<WelfareDocument[]>(`/welfare-documents?welfareRecordId=${record.id}`))
      setError('')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tải chứng từ chăm lo')
    }
  }, [record.id])

  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { void load() }, [load])

  const upload = async (event: FormEvent) => {
    event.preventDefault()
    if (!file) return
    setSaving(true)
    setError('')
    const body = new FormData()
    body.append('welfareRecordId', String(record.id))
    body.append('documentType', documentType)
    body.append('file', file)
    try {
      await api('/welfare-documents', { method: 'POST', body })
      setFile(null)
      setFileInputKey(value => value + 1)
      await Promise.all([load(), refresh()])
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tải chứng từ')
    } finally {
      setSaving(false)
    }
  }

  const remove = async (document: WelfareDocument) => {
    if (!window.confirm(`Xóa chứng từ “${document.fileName}”?`)) return
    try {
      await api(`/welfare-documents/${document.id}`, { method: 'DELETE' })
      await Promise.all([load(), refresh()])
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể xóa chứng từ')
    }
  }

  const approve = async () => {
    if (!window.confirm(`Duyệt yêu cầu ${record.recordCode}?`)) return
    setApproving(true)
    setError('')
    try {
      const approved = await api<WelfareRecord>(`/welfare/${record.id}/approve`, { method: 'POST' })
      setStatus(approved.status)
      await refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể duyệt yêu cầu')
    } finally {
      setApproving(false)
    }
  }

  const canChangeDocuments = isAdmin || !['COMPLETED', 'CANCELLED'].includes(status)

  return <div className="member-detail">
    {error && <div className="alert alert--danger">{error}</div>}
    <div className="member-detail__identity">
      <div><span>Mã hồ sơ</span><strong>{record.recordCode}</strong></div>
      <div><span>Người thụ hưởng</span><strong>{record.beneficiaryName}</strong></div>
      <div><span>Chính sách</span><strong>{record.policyName ?? enumLabel(record.welfareType)}</strong></div>
      <div><span>Ngày phát hiện</span><strong>{formatDate(record.eventDate)}</strong></div>
      <div><span>Hạn hoàn tất</span><strong>{formatDate(record.deadline)}</strong></div>
      <div><span>Trạng thái</span><strong>{enumLabel(status)}</strong></div>
    </div>

    {isAdmin && status === 'PENDING_APPROVAL' && <div className="notice notice--compact">
      <div><strong>Yêu cầu đang chờ duyệt</strong><span>Kiểm tra chính sách và chứng từ. Khi duyệt, hệ thống tự tạo phiếu chi PC-CL-{record.id} theo định mức {formatMoney(record.standardAmount)}.</span></div>
      <button className="button button--primary" disabled={approving} onClick={() => void approve()}>{approving ? 'Đang duyệt…' : 'Duyệt yêu cầu'}</button>
    </div>}

    {isAdmin && status === 'IN_PROGRESS' && <div className="notice notice--compact">
      <strong>Đã tạo phiếu chi tự động</strong>
      <span>Mã phiếu PC-CL-{record.id} đã được ghi vào Tài chính nội bộ theo số tiền của chính sách.</span>
    </div>}

    <section>
      <div className="section-title"><div><p className="eyebrow">Chứng từ điện tử</p><h3>Tệp đã nộp</h3></div><b>{documents.length} tệp</b></div>
      <div className="required-docs required-docs--horizontal">{documentTypes.map(type => {
        const matching = documents.filter(document => document.documentType === type)
        return <div key={type} className={matching.length ? 'required-doc required-doc--done' : 'required-doc'}>
          <span>{matching.length ? '✓' : '!'}</span>
          <div><strong>{enumLabel(type)}</strong>{matching.length
            ? matching.map(document => <div className="required-doc__actions" key={document.id}>
                <button type="button" onClick={() => void downloadFile(`/welfare-documents/${document.id}/download`, document.fileName)}>{document.fileName} · {fileSize(document.fileSize)}</button>
                {canChangeDocuments && <button type="button" className="icon-button icon-button--danger" onClick={() => void remove(document)}>Xóa</button>}
              </div>)
            : <small>Chưa có tệp</small>}</div>
        </div>
      })}</div>
      {canChangeDocuments && <form className="inline-upload" onSubmit={event => void upload(event)}>
        <select value={documentType} onChange={event => setDocumentType(event.target.value as typeof documentType)}>{documentTypes.map(type => <option key={type} value={type}>{enumLabel(type)}</option>)}</select>
        <input key={fileInputKey} type="file" required accept=".pdf,.doc,.docx,image/*" onChange={event => setFile(event.target.files?.[0] ?? null)} />
        <button className="button button--primary" disabled={saving}>{saving ? 'Đang tải…' : 'Nộp chứng từ'}</button>
      </form>}
      <p className="muted">Hệ thống tự cập nhật trạng thái hồ sơ, biên nhận và hình ảnh trong danh mục chăm lo theo loại tệp đã nộp.</p>
    </section>
  </div>
}
