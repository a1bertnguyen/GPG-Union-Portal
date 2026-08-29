import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { api, downloadFile, enumLabel, formatDate, formatMoney } from '../api'
import CrudPage from '../components/CrudPage'
import type { BaseRecord, FinanceDocument, UnionUnit } from '../types'
import { financeColumns } from '../portal/crudColumns'
import { financeFields } from '../portal/crudFields'
import { financeSummary } from '../portal/crudSummaries'

const fileSize = (bytes: number) => bytes < 1024 * 1024
  ? `${Math.ceil(bytes / 1024)} KB`
  : `${(bytes / 1024 / 1024).toFixed(1)} MB`

export default function FinanceEntryPage({ units }: { units: UnionUnit[] }) {
  const fields = useMemo(() => financeFields.map(field => field.name === 'documentStatus'
    ? { ...field, readOnly: true }
    : field), [])

  return <CrudPage
    endpoint="/finance"
    title="Thu • Chi • Tạm ứng"
    description="Quản lý từng phiếu nghiệp vụ và nộp chứng từ trực tiếp theo đúng CĐCS. Tạm ứng được theo dõi riêng và đồng thời được tính vào tổng Chi."
    singular="Phiếu tài chính"
    fields={fields}
    columns={financeColumns}
    units={units}
    wideForm
    excelResource="finance"
    excelFilename="mau-tai-chinh-noi-bo.xlsx"
    summaryBuilder={financeSummary}
    notice={<div className="notice"><strong>Phạm vi quản lý phiếu</strong><span>Hệ thống chỉ ghi nhận nghiệp vụ và chứng từ; không chuyển tiền hoặc kết nối tài khoản ngân hàng.</span></div>}
    detailActionLabel="Nộp chứng từ"
    detailRenderer={(item, refresh) => <FinanceDetailPanel item={item} refresh={refresh} />}
  />
}

function FinanceDetailPanel({ item, refresh }: { item: BaseRecord; refresh: () => Promise<void> }) {
  const [documents, setDocuments] = useState<FinanceDocument[]>([])
  const [file, setFile] = useState<File | null>(null)
  const [fileInputKey, setFileInputKey] = useState(0)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    try {
      setDocuments(await api<FinanceDocument[]>(`/finance-documents?financeEntryId=${item.id}`))
      setError('')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tải chứng từ tài chính')
    }
  }, [item.id])

  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { void load() }, [load])

  const upload = async (event: FormEvent) => {
    event.preventDefault()
    if (!file) return
    setSaving(true)
    setError('')
    const body = new FormData()
    body.append('financeEntryId', String(item.id))
    body.append('file', file)
    try {
      await api('/finance-documents', { method: 'POST', body })
      setFile(null)
      setFileInputKey(value => value + 1)
      await Promise.all([load(), refresh()])
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể nộp chứng từ')
    } finally {
      setSaving(false)
    }
  }

  const remove = async (document: FinanceDocument) => {
    if (!window.confirm(`Xóa chứng từ “${document.fileName}”?`)) return
    try {
      await api(`/finance-documents/${document.id}`, { method: 'DELETE' })
      await Promise.all([load(), refresh()])
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể xóa chứng từ')
    }
  }

  return <div className="member-detail">
    {error && <div className="alert alert--danger">{error}</div>}
    <div className="member-detail__identity">
      <div><span>Mã phiếu</span><strong>{String(item.entryCode)}</strong></div>
      <div><span>Loại phiếu</span><strong>{enumLabel(item.entryType)}</strong></div>
      <div><span>Ngày giao dịch</span><strong>{formatDate(item.transactionDate)}</strong></div>
      <div><span>Đơn vị</span><strong>{item.unionUnit?.code ?? '—'}</strong></div>
      <div><span>Số tiền</span><strong>{formatMoney(Number(item.amount ?? 0))}</strong></div>
      <div><span>Trạng thái chứng từ</span><strong>{enumLabel(item.documentStatus)}</strong></div>
    </div>

    <section>
      <div className="section-title"><div><p className="eyebrow">Chứng từ điện tử</p><h3>Tệp của phiếu {String(item.entryCode)}</h3></div><b>{documents.length} tệp</b></div>
      {documents.length
        ? <div className="required-docs required-docs--horizontal">{documents.map(document => <div className="required-doc required-doc--done" key={document.id}>
            <span>✓</span>
            <div><strong>Chứng từ</strong><div className="required-doc__actions">
              <button type="button" onClick={() => void downloadFile(`/finance-documents/${document.id}/download`, document.fileName)}>{document.fileName} · {fileSize(document.fileSize)}</button>
              <button type="button" className="icon-button icon-button--danger" onClick={() => void remove(document)}>Xóa</button>
            </div></div>
          </div>)}</div>
        : <div className="empty-state">Phiếu chưa có chứng từ.</div>}
      <form className="inline-upload" onSubmit={event => void upload(event)}>
        <input key={fileInputKey} type="file" required accept=".pdf,.doc,.docx,.xls,.xlsx,image/*" onChange={event => setFile(event.target.files?.[0] ?? null)} />
        <button className="button button--primary" disabled={saving}>{saving ? 'Đang tải…' : 'Nộp chứng từ'}</button>
      </form>
      <p className="muted">Có ít nhất một tệp, trạng thái chứng từ của phiếu sẽ tự chuyển thành “Đủ”.</p>
    </section>
  </div>
}
