import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { api, apiAll, formatDate } from '../api'
import type { UnionUnit } from '../types'
import './InventoryPage.css'

type Tab = 'catalog' | 'receipts' | 'issues'
type ModalKind = 'item' | 'receipt' | 'issue'

type UnitReference = {
  unionUnitId?: number
  unitCode?: string
  companyName?: string
  unionUnit?: UnionUnit
}

type InventoryItem = UnitReference & {
  id: number
  itemId?: number
  itemCode: string
  itemName: string
  category?: string
  unitOfMeasure?: string
  supplierName?: string
  supplier?: string
  notes?: string
  note?: string
  minimumStock?: number
  receivedQuantity?: number
  issuedQuantity?: number
  stockQuantity?: number
}

type InventoryReceipt = UnitReference & {
  id: number
  itemId: number
  itemCode: string
  itemName: string
  receiptDate: string
  quantity: number
  supplierName?: string
  supplier?: string
  referenceNo?: string
  notes?: string
  note?: string
}

type InventoryIssue = UnitReference & {
  id: number
  itemId: number
  itemCode: string
  itemName: string
  memberId: number
  employeeCode?: string
  recipientName?: string
  jobTitle?: string
  professionalTitle?: string
  workplace?: string
  email?: string
  phone?: string
  gender?: string
  placeOfBirth?: string
  currentResidence?: string
  startWorkDate?: string
  issueDate: string
  programName?: string
  quantity: number
  referenceNo?: string
  notes?: string
  note?: string
}

type InventorySummary = {
  itemCount: number
  totalReceived: number
  totalIssued: number
  stockQuantity: number
  lowStockCount: number
  outOfStockCount: number
}

type Recipient = {
  id?: number
  memberId?: number
  employeeCode: string
  fullName?: string
  recipientName?: string
  membershipStatus?: string
  active?: boolean
  employmentStatus?: string
  companyName?: string
  jobTitle?: string
  professionalTitle?: string
  workplace?: string
  email?: string
  phone?: string
  gender?: string
  placeOfBirth?: string
  currentResidence?: string
  startWorkDate?: string
}

type ItemForm = {
  unitId: string
  itemCode: string
  itemName: string
  category: string
  unitOfMeasure: string
  supplier: string
  minimumStock: string
  note: string
}

type ReceiptForm = {
  unitId: string
  itemId: string
  receiptDate: string
  quantity: string
  supplier: string
  referenceNo: string
  note: string
}

type IssueForm = {
  unitId: string
  itemId: string
  memberId: string
  issueDate: string
  quantity: string
  programName: string
  referenceNo: string
  note: string
}

type ModalState = { kind: ModalKind; id?: number } | null

const today = () => new Date().toISOString().slice(0, 10)
const asInteger = (value: string) => Number.parseInt(value, 10)
const amount = (value: number | undefined) => new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(Number(value ?? 0))
const text = (value: unknown) => String(value ?? '').trim()
const unitIdOf = (row: UnitReference) => Number(row.unionUnitId ?? row.unionUnit?.id ?? 0) || undefined
const recipientIdOf = (recipient: Recipient) => Number(recipient.memberId ?? recipient.id ?? 0)
const recipientNameOf = (recipient: Recipient) => recipient.recipientName ?? recipient.fullName ?? ''
const recipientLabel = (recipient: Recipient) => [recipient.employeeCode, recipientNameOf(recipient)].filter(Boolean).join(' · ')
const searchable = (value: unknown) => text(value).toLocaleLowerCase('vi')

function unitInfo(units: UnionUnit[], unitId?: number, fallbackCode?: string, fallbackCompany?: string) {
  const unit = units.find(candidate => candidate.id === unitId)
  return {
    code: fallbackCode ?? unit?.code ?? '—',
    company: fallbackCompany ?? unit?.companyName ?? unit?.name ?? 'Chưa xác định',
    unit,
  }
}

function CompanyScope({ units, isAdmin, currentUnitId, unitId, onChange }: {
  units: UnionUnit[]
  isAdmin: boolean
  currentUnitId?: number
  unitId: string
  onChange: (value: string) => void
}) {
  const effectiveId = isAdmin ? Number(unitId || 0) || undefined : currentUnitId ?? (Number(unitId || 0) || undefined)
  const info = unitInfo(units, effectiveId)
  return <div className="inventory-company-scope">
    {isAdmin ? <label className="field">
      <span>Công đoàn cơ sở <b>*</b></span>
      <select required value={unitId} onChange={event => onChange(event.target.value)}>
        <option value="">Chọn CĐCS…</option>
        {units.map(unit => <option value={unit.id} key={unit.id}>{unit.code} · {unit.name}</option>)}
      </select>
    </label> : <div className="inventory-scope-readonly">
      <span>Công đoàn cơ sở</span>
      <strong>{info.code === '—' ? 'Theo CĐCS của tài khoản' : `${info.code} · ${info.unit?.name ?? ''}`}</strong>
    </div>}
    <div className="inventory-company-readonly" aria-live="polite">
      <span>Công ty</span>
      <strong>{info.company}</strong>
      <small>Tự lấy theo CĐCS, không nhập trực tiếp.</small>
    </div>
  </div>
}

function RecipientProfile({ recipient, fallback }: { recipient?: Recipient; fallback?: InventoryIssue }) {
  if (!recipient && !fallback) return <div className="inventory-recipient-empty">Chọn cán bộ nhân viên để tự điền thông tin vào phiếu xuất quà.</div>
  const data = recipient ?? fallback
  const name = recipient ? recipientNameOf(recipient) : fallback?.recipientName
  const code = recipient?.employeeCode ?? fallback?.employeeCode
  return <div className="inventory-recipient-profile" aria-live="polite">
    <div className="inventory-recipient-profile__heading"><div><span>Người nhận đã chọn</span><strong>{name || 'Chưa có tên'}</strong></div><b>{code || '—'}</b></div>
    <div className="inventory-recipient-profile__grid">
      <div><span>Chức vụ</span><strong>{data?.jobTitle || data?.professionalTitle || '—'}</strong></div>
      <div><span>Nơi làm việc</span><strong>{data?.workplace || '—'}</strong></div>
      <div><span>Liên hệ</span><strong>{data?.phone || data?.email || '—'}</strong></div>
      <div><span>Ngày vào làm</span><strong>{formatDate(data?.startWorkDate)}</strong></div>
      <div><span>Giới tính</span><strong>{data?.gender || '—'}</strong></div>
      <div><span>Địa chỉ hiện tại</span><strong>{data?.currentResidence || '—'}</strong></div>
    </div>
    <small>Thông tin này chỉ để kiểm tra trực quan; khi lưu phiếu, hệ thống ghi snapshot từ mã đoàn viên đã chọn.</small>
  </div>
}

export default function InventoryPage({ units, isAdmin, currentUnitId }: {
  units: UnionUnit[]
  isAdmin: boolean
  currentUnitId?: number
}) {
  const [tab, setTab] = useState<Tab>('catalog')
  const [items, setItems] = useState<InventoryItem[]>([])
  const [receipts, setReceipts] = useState<InventoryReceipt[]>([])
  const [issues, setIssues] = useState<InventoryIssue[]>([])
  const [summary, setSummary] = useState<InventorySummary | null>(null)
  const [recipients, setRecipients] = useState<Recipient[]>([])
  const [recipientError, setRecipientError] = useState('')
  const [modal, setModal] = useState<ModalState>(null)
  const [itemForm, setItemForm] = useState<ItemForm>(() => emptyItemForm(isAdmin, currentUnitId))
  const [receiptForm, setReceiptForm] = useState<ReceiptForm>(() => emptyReceiptForm(isAdmin, currentUnitId))
  const [issueForm, setIssueForm] = useState<IssueForm>(() => emptyIssueForm(isAdmin, currentUnitId))
  const [recipientQuery, setRecipientQuery] = useState('')
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [formError, setFormError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [itemRows, receiptRows, issueRows, stockSummary] = await Promise.all([
        apiAll<InventoryItem>('/inventory/items'),
        apiAll<InventoryReceipt>('/inventory/receipts'),
        apiAll<InventoryIssue>('/inventory/issues'),
        api<InventorySummary>('/inventory/summary'),
      ])
      setItems(itemRows)
      setReceipts(receiptRows)
      setIssues(issueRows)
      setSummary(stockSummary)
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Không thể tải dữ liệu tồn kho')
    } finally {
      setLoading(false)
    }
  }, [])

  // The list is remote state and must be synchronized when this workspace opens.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { void load() }, [load])

  const formUnitId = modal?.kind === 'item'
    ? itemForm.unitId
    : modal?.kind === 'receipt'
      ? receiptForm.unitId
      : issueForm.unitId
  const selectedUnitId = isAdmin ? Number(formUnitId || 0) || undefined : currentUnitId ?? (Number(formUnitId || 0) || undefined)

  useEffect(() => {
    if (modal?.kind !== 'issue' || !selectedUnitId) return
    let disposed = false
    const loadRecipients = async () => {
      setRecipientError('')
      try {
        const rows = await apiAll<Recipient>('/inventory/recipients', { unitId: selectedUnitId })
        if (!disposed) setRecipients(rows)
      } catch {
        try {
          const rows = await apiAll<Recipient>('/members', { unitId: selectedUnitId })
          if (!disposed) setRecipients(rows.filter(isActiveUnionMember))
        } catch (memberLoadError) {
          if (!disposed) {
            setRecipients([])
            setRecipientError(memberLoadError instanceof Error ? memberLoadError.message : 'Không thể tải danh sách đoàn viên')
          }
        }
      }
    }
    void loadRecipients()
    return () => { disposed = true }
  }, [modal?.kind, selectedUnitId])

  const availableItems = useMemo(() => items.filter(item => !selectedUnitId || unitIdOf(item) === selectedUnitId), [items, selectedUnitId])
  const selectedItemId = modal?.kind === 'receipt' ? Number(receiptForm.itemId) : Number(issueForm.itemId)
  const selectedItem = items.find(item => item.id === selectedItemId)
  const selectedRecipient = recipients.find(recipient => String(recipientIdOf(recipient)) === issueForm.memberId)
  const recipientSuggestions = useMemo(() => {
    const query = searchable(recipientQuery)
    return recipients.filter(recipient => !query || [recipient.employeeCode, recipientNameOf(recipient), recipient.jobTitle, recipient.workplace]
      .some(value => searchable(value).includes(query))).slice(0, 7)
  }, [recipientQuery, recipients])

  const filteredItems = useMemo(() => filterRows(items, search, item => [item.itemCode, item.itemName, item.category, item.companyName, item.unitCode]), [items, search])
  const filteredReceipts = useMemo(() => filterRows(receipts, search, receipt => [receipt.referenceNo, receipt.id, receipt.itemCode, receipt.itemName, receipt.companyName, receipt.supplierName]), [receipts, search])
  const filteredIssues = useMemo(() => filterRows(issues, search, issue => [issue.referenceNo, issue.id, issue.itemCode, issue.itemName, issue.recipientName, issue.employeeCode, issue.companyName, issue.programName]), [issues, search])

  const openItem = (item?: InventoryItem) => {
    setFormError('')
    setItemForm(item ? {
      unitId: String(unitIdOf(item) ?? currentUnitId ?? ''), itemCode: item.itemCode ?? '', itemName: item.itemName ?? '', category: item.category ?? '',
      unitOfMeasure: item.unitOfMeasure ?? '', supplier: item.supplierName ?? item.supplier ?? '', minimumStock: item.minimumStock === undefined ? '' : String(item.minimumStock),
      note: item.notes ?? item.note ?? '',
    } : emptyItemForm(isAdmin, currentUnitId))
    setModal({ kind: 'item', id: item?.id })
  }

  const openReceipt = (receipt?: InventoryReceipt) => {
    setFormError('')
    setReceiptForm(receipt ? {
      unitId: String(unitIdOf(receipt) ?? currentUnitId ?? ''), itemId: String(receipt.itemId), receiptDate: receipt.receiptDate ?? today(), quantity: String(receipt.quantity ?? ''),
      supplier: receipt.supplierName ?? receipt.supplier ?? '', referenceNo: receipt.referenceNo ?? '', note: receipt.notes ?? receipt.note ?? '',
    } : emptyReceiptForm(isAdmin, currentUnitId))
    setModal({ kind: 'receipt', id: receipt?.id })
  }

  const openIssue = (issue?: InventoryIssue) => {
    setFormError('')
    setIssueForm(issue ? {
      unitId: String(unitIdOf(issue) ?? currentUnitId ?? ''), itemId: String(issue.itemId), memberId: String(issue.memberId ?? ''), issueDate: issue.issueDate ?? today(),
      quantity: String(issue.quantity ?? ''), programName: issue.programName ?? '', referenceNo: issue.referenceNo ?? '', note: issue.notes ?? issue.note ?? '',
    } : emptyIssueForm(isAdmin, currentUnitId))
    setRecipientQuery(issue ? [issue.employeeCode, issue.recipientName].filter(Boolean).join(' · ') : '')
    setModal({ kind: 'issue', id: issue?.id })
  }

  const closeModal = () => {
    if (saving) return
    setModal(null)
    setFormError('')
  }

  const saveItem = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const payload = {
      ...(isAdmin ? { unitId: asInteger(itemForm.unitId) } : {}),
      itemCode: itemForm.itemCode.trim(), itemName: itemForm.itemName.trim(), category: itemForm.category.trim() || null,
      unitOfMeasure: itemForm.unitOfMeasure.trim() || null, supplier: itemForm.supplier.trim() || null,
      minimumStock: itemForm.minimumStock.trim() ? asInteger(itemForm.minimumStock) : null, note: itemForm.note.trim() || null,
    }
    await save('/inventory/items', payload)
  }

  const saveReceipt = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const payload = {
      ...(isAdmin ? { unitId: asInteger(receiptForm.unitId) } : {}),
      itemId: asInteger(receiptForm.itemId), receiptDate: receiptForm.receiptDate, quantity: asInteger(receiptForm.quantity),
      supplier: receiptForm.supplier.trim() || null, referenceNo: receiptForm.referenceNo.trim() || null, note: receiptForm.note.trim() || null,
    }
    await save('/inventory/receipts', payload)
  }

  const saveIssue = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!issueForm.memberId) {
      setFormError('Chọn một cán bộ nhân viên từ danh sách gợi ý trước khi lưu phiếu xuất.')
      return
    }
    const payload = {
      ...(isAdmin ? { unitId: asInteger(issueForm.unitId) } : {}),
      itemId: asInteger(issueForm.itemId), memberId: asInteger(issueForm.memberId), issueDate: issueForm.issueDate, quantity: asInteger(issueForm.quantity),
      programName: issueForm.programName.trim() || null, referenceNo: issueForm.referenceNo.trim() || null, note: issueForm.note.trim() || null,
    }
    await save('/inventory/issues', payload)
  }

  async function save(path: string, payload: Record<string, unknown>) {
    if (isAdmin && !selectedUnitId) {
      setFormError('ADMIN cần chọn CĐCS trước khi lưu phiếu.')
      return
    }
    setSaving(true)
    setFormError('')
    try {
      await api(`${path}${modal?.id ? `/${modal.id}` : ''}`, { method: modal?.id ? 'PUT' : 'POST', body: JSON.stringify(payload) })
      setModal(null)
      await load()
    } catch (saveError) {
      setFormError(saveError instanceof Error ? saveError.message : 'Không thể lưu phiếu tồn kho')
    } finally {
      setSaving(false)
    }
  }

  const remove = async (path: string, id: number, label: string) => {
    if (!window.confirm(`Xóa ${label}?`)) return
    setError('')
    try {
      await api(`${path}/${id}`, { method: 'DELETE' })
      await load()
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : 'Không thể xóa dữ liệu tồn kho')
    }
  }

  const changeIssueUnit = (unitId: string) => {
    setIssueForm(current => ({ ...current, unitId, itemId: '', memberId: '' }))
    setRecipientQuery('')
    setRecipients([])
  }

  const chooseRecipient = (recipient: Recipient) => {
    setIssueForm(current => ({ ...current, memberId: String(recipientIdOf(recipient)) }))
    setRecipientQuery(recipientLabel(recipient))
  }

  const changeRecipientQuery = (value: string) => {
    setRecipientQuery(value)
    const exact = recipients.find(recipient => {
      const candidate = searchable(value)
      return candidate === searchable(recipient.employeeCode) || candidate === searchable(recipientLabel(recipient))
    })
    setIssueForm(current => ({ ...current, memberId: exact ? String(recipientIdOf(exact)) : '' }))
  }

  const tabMeta: Array<{ id: Tab; label: string; count: number }> = [
    { id: 'catalog', label: 'Tổng quan & vật phẩm', count: summary?.itemCount ?? items.length },
    { id: 'receipts', label: 'Phiếu nhập', count: receipts.length },
    { id: 'issues', label: 'Phiếu xuất quà', count: issues.length },
  ]
  const activeCount = tab === 'catalog' ? filteredItems.length : tab === 'receipts' ? filteredReceipts.length : filteredIssues.length
  const hasProduct = items.some(item => !selectedUnitId || unitIdOf(item) === selectedUnitId)

  return <section className="page-section inventory-page">
    <div className="page-heading">
      <div><p className="eyebrow">Nghiệp vụ công đoàn</p><h1>Tồn kho vật phẩm</h1><p>Quản lý danh mục, phiếu nhập và cấp phát quà theo từng CĐCS. Cột Công ty luôn lấy từ CĐCS của phiếu.</p></div>
      <div className="page-actions"><button className="button button--ghost" onClick={() => void load()} disabled={loading}>Làm mới</button>
        <button className="button button--primary" onClick={() => tab === 'catalog' ? openItem() : tab === 'receipts' ? openReceipt() : openIssue()}>
          {tab === 'catalog' ? '+ Thêm vật phẩm' : tab === 'receipts' ? '+ Lập phiếu nhập' : '+ Lập phiếu xuất'}
        </button>
      </div>
    </div>

    {error && <div className="alert alert--danger" role="alert">{error}</div>}

    <div className="inventory-summary-grid" aria-label="Tổng hợp tồn kho">
      <article><span>Vật phẩm</span><strong>{amount(summary?.itemCount ?? items.length)}</strong><small>Danh mục đang quản lý</small></article>
      <article><span>Đã nhập</span><strong>{amount(summary?.totalReceived)}</strong><small>Tổng số lượng ghi nhận</small></article>
      <article><span>Đã cấp phát</span><strong>{amount(summary?.totalIssued)}</strong><small>Phiếu xuất quà</small></article>
      <article className="inventory-summary-grid__stock"><span>Tồn hiện tại</span><strong>{amount(summary?.stockQuantity)}</strong><small>Cộng dồn theo CĐCS</small></article>
      <article className={(summary?.outOfStockCount ?? 0) > 0 ? 'is-alert' : ''}><span>Cần chú ý</span><strong>{amount((summary?.lowStockCount ?? 0) + (summary?.outOfStockCount ?? 0))}</strong><small>{amount(summary?.outOfStockCount)} hết hàng · {amount(summary?.lowStockCount)} sắp thiếu</small></article>
    </div>

    <div className="inventory-tabs" role="tablist" aria-label="Nghiệp vụ tồn kho">
      {tabMeta.map(entry => <button key={entry.id} type="button" role="tab" aria-selected={tab === entry.id} className={tab === entry.id ? 'is-active' : ''}
        onClick={() => { setTab(entry.id); setSearch('') }}><span>{entry.label}</span><b>{amount(entry.count)}</b></button>)}
    </div>

    <article className="panel inventory-panel">
      <div className="inventory-panel__heading">
        <div><p className="eyebrow">{tab === 'catalog' ? 'Danh mục dùng chung' : tab === 'receipts' ? 'Tăng tồn theo phiếu' : 'Cấp phát cho đoàn viên'}</p>
          <h2>{tab === 'catalog' ? 'Vật phẩm và số dư tồn' : tab === 'receipts' ? 'Sổ phiếu nhập kho' : 'Sổ phiếu xuất quà'}</h2></div>
        <div className="inventory-panel__tools"><label><span className="sr-only">Tìm kiếm</span><input value={search} onChange={event => setSearch(event.target.value)} placeholder={tab === 'issues' ? 'Mã phiếu, nhân viên, chương trình…' : 'Mã, tên, CĐCS…'} /></label>
          <button className="button button--ghost" onClick={() => tab === 'catalog' ? openItem() : tab === 'receipts' ? openReceipt() : openIssue()}>{tab === 'catalog' ? '+ Vật phẩm' : tab === 'receipts' ? '+ Phiếu nhập' : '+ Phiếu xuất'}</button>
        </div>
      </div>
      {loading ? <div className="empty-state">Đang tải tồn kho…</div> : tab === 'catalog'
        ? <ItemTable items={filteredItems} units={units} onEdit={openItem} onDelete={item => void remove('/inventory/items', item.id, `vật phẩm “${item.itemName}”`)} />
        : tab === 'receipts'
          ? <ReceiptTable receipts={filteredReceipts} units={units} onEdit={openReceipt} onDelete={receipt => void remove('/inventory/receipts', receipt.id, `phiếu nhập ${receipt.referenceNo || `#${receipt.id}`}`)} />
          : <IssueTable issues={filteredIssues} units={units} onEdit={openIssue} onDelete={issue => void remove('/inventory/issues', issue.id, `phiếu xuất ${issue.referenceNo || `#${issue.id}`}`)} />}
      {!loading && activeCount === 0 && <p className="inventory-empty-note">{search ? 'Không có bản ghi phù hợp với nội dung tìm kiếm.' : 'Chưa có bản ghi. Hãy tạo phiếu đầu tiên để bắt đầu theo dõi tồn kho.'}</p>}
    </article>

    {modal && <div className="modal-backdrop" onMouseDown={closeModal}>
      <div className="modal modal--wide inventory-modal" role="dialog" aria-modal="true" aria-label={modal.kind === 'item' ? 'Vật phẩm tồn kho' : modal.kind === 'receipt' ? 'Phiếu nhập kho' : 'Phiếu xuất quà'} onMouseDown={event => event.stopPropagation()}>
        <div className="modal__header"><div><p className="eyebrow">{modal.id ? 'Cập nhật dữ liệu' : 'Tạo mới'}</p><h2>{modal.kind === 'item' ? 'Vật phẩm tồn kho' : modal.kind === 'receipt' ? 'Phiếu nhập kho' : 'Phiếu xuất quà cho CBNV'}</h2></div><button className="modal__close" type="button" onClick={closeModal}>×</button></div>
        {modal.kind === 'item' ? <form className="form-grid form-grid--wide inventory-form" onSubmit={event => void saveItem(event)}>
          <CompanyScope units={units} isAdmin={isAdmin} currentUnitId={currentUnitId} unitId={itemForm.unitId} onChange={unitId => setItemForm(current => ({ ...current, unitId }))} />
          <label className="field"><span>Mã vật phẩm <b>*</b></span><input required value={itemForm.itemCode} onChange={event => setItemForm(current => ({ ...current, itemCode: event.target.value }))} placeholder="VD: QUA-TET-001" /></label>
          <label className="field"><span>Tên vật phẩm <b>*</b></span><input required value={itemForm.itemName} onChange={event => setItemForm(current => ({ ...current, itemName: event.target.value }))} placeholder="VD: Giỏ quà Tết" /></label>
          <label className="field"><span>Nhóm vật phẩm</span><input value={itemForm.category} onChange={event => setItemForm(current => ({ ...current, category: event.target.value }))} placeholder="Quà tặng, văn phòng phẩm…" /></label>
          <label className="field"><span>Đơn vị tính</span><input value={itemForm.unitOfMeasure} onChange={event => setItemForm(current => ({ ...current, unitOfMeasure: event.target.value }))} placeholder="Phần, hộp, cái…" /></label>
          <label className="field"><span>Nhà cung cấp mặc định</span><input value={itemForm.supplier} onChange={event => setItemForm(current => ({ ...current, supplier: event.target.value }))} placeholder="Tên nhà cung cấp" /></label>
          <label className="field"><span>Mức tồn tối thiểu</span><input type="number" min="0" step="1" value={itemForm.minimumStock} onChange={event => setItemForm(current => ({ ...current, minimumStock: event.target.value }))} placeholder="Để trống nếu không cảnh báo" /></label>
          <label className="field field--wide"><span>Ghi chú</span><textarea value={itemForm.note} onChange={event => setItemForm(current => ({ ...current, note: event.target.value }))} placeholder="Quy cách, điều kiện bảo quản hoặc ghi chú khác…" /></label>
          <FormActions error={formError} saving={saving} onCancel={closeModal} />
        </form> : modal.kind === 'receipt' ? <form className="form-grid form-grid--wide inventory-form" onSubmit={event => void saveReceipt(event)}>
          <CompanyScope units={units} isAdmin={isAdmin} currentUnitId={currentUnitId} unitId={receiptForm.unitId} onChange={unitId => setReceiptForm(current => ({ ...current, unitId, itemId: '' }))} />
          <label className="field field--wide"><span>Vật phẩm nhập <b>*</b></span><select required value={receiptForm.itemId} onChange={event => setReceiptForm(current => ({ ...current, itemId: event.target.value }))}><option value="">Chọn vật phẩm…</option>{availableItems.map(item => <option value={item.id} key={item.id}>{item.itemCode} · {item.itemName} · tồn {amount(item.stockQuantity)} {item.unitOfMeasure ?? ''}</option>)}</select></label>
          {selectedItem && <SelectedItemNote item={selectedItem} />}
          {!hasProduct && <div className="inventory-inline-alert field--wide">CĐCS này chưa có vật phẩm. Hãy tạo danh mục vật phẩm trước.</div>}
          <label className="field"><span>Ngày nhập <b>*</b></span><input required type="date" value={receiptForm.receiptDate} onChange={event => setReceiptForm(current => ({ ...current, receiptDate: event.target.value }))} /></label>
          <label className="field"><span>Số lượng nhập <b>*</b></span><input required type="number" min="1" step="1" value={receiptForm.quantity} onChange={event => setReceiptForm(current => ({ ...current, quantity: event.target.value }))} /></label>
          <label className="field"><span>Nhà cung cấp</span><input value={receiptForm.supplier} onChange={event => setReceiptForm(current => ({ ...current, supplier: event.target.value }))} placeholder="Tự do nhập theo phiếu" /></label>
          <label className="field"><span>Số hóa đơn / chứng từ</span><input value={receiptForm.referenceNo} onChange={event => setReceiptForm(current => ({ ...current, referenceNo: event.target.value }))} placeholder="VD: HĐ-2026-001" /></label>
          <label className="field field--wide"><span>Ghi chú</span><textarea value={receiptForm.note} onChange={event => setReceiptForm(current => ({ ...current, note: event.target.value }))} placeholder="Lý do nhập, tình trạng vật phẩm…" /></label>
          <FormActions error={formError} saving={saving} onCancel={closeModal} />
        </form> : <form className="form-grid form-grid--wide inventory-form" onSubmit={event => void saveIssue(event)}>
          <CompanyScope units={units} isAdmin={isAdmin} currentUnitId={currentUnitId} unitId={issueForm.unitId} onChange={changeIssueUnit} />
          <label className="field field--wide"><span>Vật phẩm cấp phát <b>*</b></span><select required value={issueForm.itemId} onChange={event => setIssueForm(current => ({ ...current, itemId: event.target.value }))}><option value="">Chọn vật phẩm…</option>{availableItems.map(item => <option value={item.id} key={item.id}>{item.itemCode} · {item.itemName} · còn {amount(item.stockQuantity)} {item.unitOfMeasure ?? ''}</option>)}</select></label>
          {selectedItem && <SelectedItemNote item={selectedItem} warning />}
          {!hasProduct && <div className="inventory-inline-alert field--wide">CĐCS này chưa có vật phẩm. Hãy tạo danh mục và lập phiếu nhập trước khi xuất quà.</div>}
          <div className="inventory-member-picker field--wide">
            <label className="field"><span>CBNV nhận quà <b>*</b></span><input required list="inventory-recipient-suggestions" value={recipientQuery} onChange={event => changeRecipientQuery(event.target.value)} placeholder="Nhập mã nhân viên hoặc họ tên để gợi ý" />
              <datalist id="inventory-recipient-suggestions">{recipients.map(recipient => <option key={recipientIdOf(recipient)} value={recipientLabel(recipient)}>{recipient.jobTitle || recipient.workplace || ''}</option>)}</datalist>
            </label>
            {recipientError && <div className="inventory-inline-alert">{recipientError}</div>}
            {!!recipientSuggestions.length && <div className="inventory-recipient-suggestions" aria-label="Gợi ý cán bộ nhân viên">{recipientSuggestions.map(recipient => <button type="button" key={recipientIdOf(recipient)} className={String(recipientIdOf(recipient)) === issueForm.memberId ? 'is-selected' : ''} onClick={() => chooseRecipient(recipient)}><span><strong>{recipientLabel(recipient)}</strong><small>{recipient.jobTitle || recipient.professionalTitle || 'Chưa có chức vụ'} · {recipient.workplace || 'Chưa có nơi làm việc'}</small></span><b>Chọn</b></button>)}</div>}
            <RecipientProfile recipient={selectedRecipient} fallback={modal.id ? issues.find(issue => issue.id === modal.id) : undefined} />
          </div>
          <label className="field"><span>Ngày cấp phát <b>*</b></span><input required type="date" value={issueForm.issueDate} onChange={event => setIssueForm(current => ({ ...current, issueDate: event.target.value }))} /></label>
          <label className="field"><span>Số lượng cấp <b>*</b></span><input required type="number" min="1" step="1" value={issueForm.quantity} onChange={event => setIssueForm(current => ({ ...current, quantity: event.target.value }))} /></label>
          <label className="field"><span>Chương trình / dịp tặng</span><input value={issueForm.programName} onChange={event => setIssueForm(current => ({ ...current, programName: event.target.value }))} placeholder="VD: Tết Nguyên đán 2026" /></label>
          <label className="field"><span>Số phiếu / chứng từ</span><input value={issueForm.referenceNo} onChange={event => setIssueForm(current => ({ ...current, referenceNo: event.target.value }))} placeholder="VD: PXQ-2026-001" /></label>
          <label className="field field--wide"><span>Ghi chú</span><textarea value={issueForm.note} onChange={event => setIssueForm(current => ({ ...current, note: event.target.value }))} placeholder="Ghi nhận ký nhận hoặc điều kiện cấp phát…" /></label>
          <FormActions error={formError} saving={saving} onCancel={closeModal} />
        </form>}
      </div>
    </div>}
  </section>
}

function ItemTable({ items, units, onEdit, onDelete }: { items: InventoryItem[]; units: UnionUnit[]; onEdit: (item: InventoryItem) => void; onDelete: (item: InventoryItem) => void }) {
  return <div className="table-wrap inventory-table-wrap"><table><thead><tr><th>Vật phẩm</th><th>CĐCS / Công ty</th><th>Phân loại</th><th>Nhập</th><th>Xuất</th><th>Tồn</th><th aria-label="Tác vụ" /></tr></thead><tbody>
    {items.map(item => { const unit = unitInfo(units, unitIdOf(item), item.unitCode, item.companyName); const stock = Number(item.stockQuantity ?? 0); const isOut = stock <= 0; const isLow = !isOut && item.minimumStock !== undefined && stock <= item.minimumStock
      return <tr key={item.id}><td><strong>{item.itemName}</strong><small>{item.itemCode}</small></td><td><strong>{unit.code}</strong><small>{unit.company}</small></td><td>{item.category || '—'}<small>{item.unitOfMeasure || 'Chưa có đơn vị tính'}</small></td><td>{amount(item.receivedQuantity)}</td><td>{amount(item.issuedQuantity)}</td><td><strong className={isOut ? 'inventory-stock is-out' : isLow ? 'inventory-stock is-low' : 'inventory-stock'}>{amount(stock)} {item.unitOfMeasure ?? ''}</strong></td><td className="actions-cell"><button className="icon-button" onClick={() => onEdit(item)}>Sửa</button><button className="icon-button icon-button--danger" onClick={() => onDelete(item)}>Xóa</button></td></tr>
    })}</tbody></table></div>
}

function ReceiptTable({ receipts, units, onEdit, onDelete }: { receipts: InventoryReceipt[]; units: UnionUnit[]; onEdit: (receipt: InventoryReceipt) => void; onDelete: (receipt: InventoryReceipt) => void }) {
  return <div className="table-wrap inventory-table-wrap"><table><thead><tr><th>Phiếu nhập</th><th>Ngày nhập</th><th>Vật phẩm</th><th>CĐCS / Công ty</th><th>Số lượng</th><th>Nhà cung cấp</th><th aria-label="Tác vụ" /></tr></thead><tbody>
    {receipts.map(receipt => { const unit = unitInfo(units, unitIdOf(receipt), receipt.unitCode, receipt.companyName); return <tr key={receipt.id}><td><strong>{receipt.referenceNo || `#${receipt.id}`}</strong><small>Phiếu nhập kho</small></td><td>{formatDate(receipt.receiptDate)}</td><td><strong>{receipt.itemName}</strong><small>{receipt.itemCode}</small></td><td><strong>{unit.code}</strong><small>{unit.company}</small></td><td><strong>{amount(receipt.quantity)}</strong></td><td>{receipt.supplierName || receipt.supplier || '—'}</td><td className="actions-cell"><button className="icon-button" onClick={() => onEdit(receipt)}>Sửa</button><button className="icon-button icon-button--danger" onClick={() => onDelete(receipt)}>Xóa</button></td></tr> })}
  </tbody></table></div>
}

function IssueTable({ issues, units, onEdit, onDelete }: { issues: InventoryIssue[]; units: UnionUnit[]; onEdit: (issue: InventoryIssue) => void; onDelete: (issue: InventoryIssue) => void }) {
  return <div className="table-wrap inventory-table-wrap"><table><thead><tr><th>Phiếu xuất</th><th>Ngày cấp</th><th>CBNV nhận quà</th><th>Vật phẩm</th><th>CĐCS / Công ty</th><th>Số lượng</th><th>Chương trình</th><th aria-label="Tác vụ" /></tr></thead><tbody>
    {issues.map(issue => { const unit = unitInfo(units, unitIdOf(issue), issue.unitCode, issue.companyName); return <tr key={issue.id}><td><strong>{issue.referenceNo || `#${issue.id}`}</strong><small>Phiếu xuất quà</small></td><td>{formatDate(issue.issueDate)}</td><td><strong>{issue.recipientName || '—'}</strong><small>{issue.employeeCode || `Mã đoàn viên ${issue.memberId}`}</small></td><td><strong>{issue.itemName}</strong><small>{issue.itemCode}</small></td><td><strong>{unit.code}</strong><small>{unit.company}</small></td><td><strong>{amount(issue.quantity)}</strong></td><td>{issue.programName || '—'}</td><td className="actions-cell"><button className="icon-button" onClick={() => onEdit(issue)}>Sửa</button><button className="icon-button icon-button--danger" onClick={() => onDelete(issue)}>Xóa</button></td></tr> })}
  </tbody></table></div>
}

function SelectedItemNote({ item, warning = false }: { item: InventoryItem; warning?: boolean }) {
  return <div className={`inventory-item-note field--wide${warning && Number(item.stockQuantity ?? 0) <= 0 ? ' is-danger' : ''}`}><div><span>Vật phẩm đã chọn</span><strong>{item.itemCode} · {item.itemName}</strong></div><div><span>Tồn hiện tại</span><strong>{amount(item.stockQuantity)} {item.unitOfMeasure ?? ''}</strong></div><small>{warning ? 'Hệ thống sẽ kiểm tra tồn kho khi lưu phiếu xuất.' : 'Thông tin tồn được cập nhật sau khi lưu phiếu nhập.'}</small></div>
}

function FormActions({ error, saving, onCancel }: { error: string; saving: boolean; onCancel: () => void }) {
  return <><>{error && <div className="alert alert--danger field--wide" role="alert">{error}</div>}</><div className="form-actions field--wide"><button className="button button--ghost" type="button" onClick={onCancel} disabled={saving}>Hủy</button><button className="button button--primary" type="submit" disabled={saving}>{saving ? 'Đang lưu…' : 'Lưu phiếu'}</button></div></>
}

function emptyItemForm(isAdmin: boolean, currentUnitId?: number): ItemForm {
  return { unitId: isAdmin ? '' : String(currentUnitId ?? ''), itemCode: '', itemName: '', category: '', unitOfMeasure: '', supplier: '', minimumStock: '', note: '' }
}

function emptyReceiptForm(isAdmin: boolean, currentUnitId?: number): ReceiptForm {
  return { unitId: isAdmin ? '' : String(currentUnitId ?? ''), itemId: '', receiptDate: today(), quantity: '', supplier: '', referenceNo: '', note: '' }
}

function emptyIssueForm(isAdmin: boolean, currentUnitId?: number): IssueForm {
  return { unitId: isAdmin ? '' : String(currentUnitId ?? ''), itemId: '', memberId: '', issueDate: today(), quantity: '', programName: '', referenceNo: '', note: '' }
}

function filterRows<T>(rows: T[], query: string, fields: (row: T) => unknown[]) {
  const needle = searchable(query)
  return needle ? rows.filter(row => fields(row).some(value => searchable(value).includes(needle))) : rows
}

function isActiveUnionMember(recipient: Recipient) {
  if (recipient.membershipStatus !== undefined && recipient.membershipStatus !== 'MEMBER') return false
  if (recipient.employmentStatus !== undefined && recipient.employmentStatus !== 'ACTIVE') return false
  return recipient.active !== false
}
