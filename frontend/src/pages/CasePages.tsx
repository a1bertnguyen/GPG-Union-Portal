import { useMemo, useState } from 'react'
import { api, enumLabel, formatDate } from '../api'
import CrudPage from '../components/CrudPage'
import type { BaseRecord, LaborCase, UnionUnit } from '../types'
import { caseColumns } from '../portal/crudColumns'
import { caseFields } from '../portal/crudFields'
import { casePresetFilters, caseSummary } from '../portal/crudSummaries'

const lockedStatuses = ['PENDING_APPROVAL', 'CLOSED']

export default function LaborCasePage({ units, isAdmin }: { units: UnionUnit[]; isAdmin: boolean }) {
  const fields = useMemo(() => caseFields.map(field => {
    if (field.name === 'responseDate') return { ...field, readOnly: true }
    if (!isAdmin && ['ownerName', 'deadline', 'status'].includes(field.name)) return { ...field, readOnly: true }
    if (field.name === 'status' && !isAdmin) return {
      ...field,
      readOnly: true,
      options: field.options?.filter(option => !lockedStatuses.includes(option.value)),
    }
    return field
  }), [isAdmin])

  return <CrudPage
    endpoint="/cases"
    title="Xử lý kiến nghị & vụ việc"
    description={isAdmin
      ? 'Duyệt tiếp nhận để giao PIC/deadline, sau đó kiểm tra kết quả USER gửi và duyệt đóng hồ sơ.'
      : 'Tạo vụ việc để ADMIN giao PIC/deadline; sau khi xử lý, nộp kết quả và tài liệu để ADMIN duyệt.'}
    singular="Vụ việc"
    fields={fields}
    columns={caseColumns}
    units={units}
    wideForm
    excelResource="cases"
    excelFilename="mau-xu-ly-vu-viec.xlsx"
    summaryBuilder={caseSummary}
    presetFilters={casePresetFilters}
    notice={<div className="notice notice--compact">
      <strong>Hỗ trợ trực tiếp file Book1</strong>
      <span>USER có thể nhập 10 cột MãNV, Họ và tên, Chức danh, Nơi làm việc, ngày vào/nghỉ, điện thoại, Yêu cầu, Ngày yêu cầu và Ngày trả lời. Hồ sơ mới tự gắn vào CĐCS của USER.</span>
    </div>}
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
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const transition = async (action: 'submit-approval' | 'approve') => {
    const assigning = action === 'approve' && status === 'NEW'
    if (assigning && (!ownerName.trim() || !deadline)) {
      setError('ADMIN cần chọn đầy đủ PIC và deadline trước khi duyệt tiếp nhận.')
      return
    }
    const prompt = assigning
      ? `Duyệt tiếp nhận và giao vụ việc ${record.caseCode} cho ${ownerName}?`
      : action === 'approve'
        ? `Duyệt kết quả và đóng vụ việc ${record.caseCode}?`
      : `Gửi kết quả vụ việc ${record.caseCode} cho ADMIN duyệt?`
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
      setError(err instanceof Error ? err.message : 'Không thể chuyển trạng thái vụ việc')
    } finally {
      setBusy(false)
    }
  }

  const canSubmit = !isAdmin && ['IN_PROGRESS', 'WAITING_RESPONSE'].includes(status)
  const canAssign = isAdmin && status === 'NEW'
  const canApprove = isAdmin && status === 'PENDING_APPROVAL'

  return <div className="member-detail">
    {error && <div className="alert alert--danger">{error}</div>}
    <div className="member-detail__identity">
      <div><span>Mã vụ việc</span><strong>{record.caseCode}</strong></div>
      <div><span>Mã nhân viên</span><strong>{record.employeeCode ?? '—'}</strong></div>
      <div><span>Người yêu cầu</span><strong>{record.requesterName}</strong></div>
      <div><span>Chức danh</span><strong>{record.jobTitle ?? '—'}</strong></div>
      <div><span>Nơi làm việc</span><strong>{record.workplace ?? '—'}</strong></div>
      <div><span>Điện thoại</span><strong>{record.phone ?? '—'}</strong></div>
      <div><span>Ngày yêu cầu</span><strong>{formatDate(record.receivedDate)}</strong></div>
      <div><span>PIC</span><strong>{ownerName || 'Chờ ADMIN giao'}</strong></div>
      <div><span>Hạn xử lý</span><strong>{formatDate(deadline)}</strong></div>
      <div><span>Ngày trả lời</span><strong>{formatDate(responseDate)}</strong></div>
      <div><span>Trạng thái</span><strong>{enumLabel(status)}</strong></div>
    </div>

    <section>
      <div className="section-title"><div><p className="eyebrow">Nội dung xử lý</p><h3>Yêu cầu và kết quả</h3></div></div>
      <div className="notice notice--compact"><div><strong>Yêu cầu</strong><span>{record.description}</span></div></div>
      <div className="notice notice--compact"><div><strong>Kết quả / phản hồi</strong><span>{record.resultText || 'Chưa cập nhật. USER cần sửa hồ sơ và nhập kết quả trước khi gửi duyệt.'}</span></div></div>
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
      <div><strong>Hoàn tất phần xử lý của USER</strong><span>Hãy sửa hồ sơ để nhập Kết quả/phản hồi, Tài liệu đính kèm và Lý do quá hạn/ETA mới nếu đã trễ deadline. Ngày trả lời sẽ tự ghi là ngày gửi.</span></div>
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
