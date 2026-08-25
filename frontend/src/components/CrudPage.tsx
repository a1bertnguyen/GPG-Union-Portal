import { useCallback, useEffect, useMemo, useState, type FormEvent, type ReactNode } from 'react'
import { api, downloadFile, enumLabel } from '../api'
import type { BaseRecord, UnionUnit } from '../types'
import { importSummary } from '../excel'
import ExcelImportActions from './ExcelImportActions'

type Option = { value: string; label: string }

export type FieldConfig = {
  name: string
  label: string
  type?: 'text' | 'date' | 'number' | 'email' | 'textarea' | 'select' | 'unit' | 'checkbox'
  required?: boolean
  placeholder?: string
  options?: Option[]
  step?: string
  wide?: boolean
  defaultValue?: string | boolean
}

export type ColumnConfig = {
  label: string
  render: (item: BaseRecord) => ReactNode
  className?: string
}

export type SummaryCard = {
  label: string
  value: string | number
  note?: string
  tone?: 'blue' | 'teal' | 'green' | 'orange'
}

type Props = {
  endpoint: string
  title: string
  description: string
  singular: string
  fields: FieldConfig[]
  columns: ColumnConfig[]
  units: UnionUnit[]
  notice?: ReactNode
  enableMemberCsv?: boolean
  excelResource?: string
  excelFilename?: string
  canImportExcel?: boolean
  readOnly?: boolean
  readOnlyMessage?: string
  statusField?: string
  summaryBuilder?: (items: BaseRecord[]) => SummaryCard[]
  footer?: ReactNode
}

type FormState = Record<string, string | boolean>
export function StatusBadge({ value }: { value: unknown }) {
  const raw = String(value ?? '')
  const tone = ['COMPLETED', 'COMPLETE', 'ACTIVE', 'MEMBER', 'APPROVED', 'SUBMITTED', 'CLOSED', 'INCOME'].includes(raw)
    ? 'success'
    : ['HIGH', 'CRITICAL', 'INCOMPLETE', 'EXPENSE', 'CANCELLED', 'FAILED'].includes(raw)
      ? 'danger'
      : ['PENDING_APPROVAL', 'WAITING_RESPONSE', 'MEDIUM', 'DRAFT', 'PLANNED', 'PARTIAL'].includes(raw)
        ? 'warning'
        : 'neutral'
  return <span className={`status status--${tone}`}>{enumLabel(value)}</span>
}

export default function CrudPage({ endpoint, title, description, singular, fields, columns, units, notice, enableMemberCsv = false, excelResource, excelFilename = 'mau-du-lieu.xlsx', canImportExcel = true, readOnly = false, readOnlyMessage, statusField = 'status', summaryBuilder, footer }: Props) {
  const [items, setItems] = useState<BaseRecord[]>([])
  const [loading, setLoading] = useState(true)
  const [formOpen, setFormOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [form, setForm] = useState<FormState>({})
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [search, setSearch] = useState('')
  const [searchField, setSearchField] = useState('all')
  const [unitFilter, setUnitFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [transferMessage, setTransferMessage] = useState('')

  const emptyForm = useMemo(() => Object.fromEntries(fields.map(field => [field.name, field.defaultValue ?? (field.type === 'checkbox' ? false : '')])), [fields])

  const load = useCallback(async () => {
    try {
      setItems(await api<BaseRecord[]>(endpoint))
      setError('')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tải dữ liệu')
    } finally {
      setLoading(false)
    }
  }, [endpoint])

  // Loading remote data is the intended synchronization performed by this effect.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { void load() }, [load])

  const hasUnit = fields.some(field => field.type === 'unit')
  const searchableFields = useMemo(() => fields.filter(field => field.type !== 'checkbox'), [fields])
  const selectedSearchField = searchableFields.find(field => field.name === searchField)
  const statusOptions = useMemo(() => [...new Set(items.map(item => String(item[statusField] ?? '')).filter(Boolean))], [items, statusField])
  const summaryCards = useMemo(() => summaryBuilder?.(items) ?? [], [items, summaryBuilder])
  const visibleItems = useMemo(() => {
    const query = search.trim().toLocaleLowerCase('vi')
    return items.filter(item => {
      if (unitFilter && String(item.unionUnit?.id ?? '') !== unitFilter) return false
      if (statusFilter && String(item[statusField] ?? '') !== statusFilter) return false
      if (!query) return true
      if (searchField !== 'all') {
        const field = searchableFields.find(candidate => candidate.name === searchField)
        const rawValue = item[searchField]
        const value = field?.type === 'unit'
          ? `${item.unionUnit?.code ?? ''} ${item.unionUnit?.name ?? ''} ${item.unionUnit?.companyName ?? ''}`
          : `${String(rawValue ?? '')} ${enumLabel(rawValue)}`
        return value.toLocaleLowerCase('vi').includes(query)
      }
      const primitiveValues = Object.values(item)
        .filter(value => ['string', 'number', 'boolean'].includes(typeof value))
        .flatMap(value => [String(value), enumLabel(value)])
        .join(' ')
      const unitValues = item.unionUnit ? `${item.unionUnit.code} ${item.unionUnit.name} ${item.unionUnit.companyName}` : ''
      return `${primitiveValues} ${unitValues}`.toLocaleLowerCase('vi').includes(query)
    })
  }, [items, search, searchField, searchableFields, unitFilter, statusFilter, statusField])

  const exportPath = useMemo(() => {
    const query = new URLSearchParams()
    if (search.trim()) query.set('q', search.trim())
    if (unitFilter) query.set('unitId', unitFilter)
    return `/members/export.csv${query.size ? `?${query}` : ''}`
  }, [search, unitFilter])

  const openCreate = () => {
    setEditingId(null)
    setForm(emptyForm)
    setError('')
    setFormOpen(true)
  }

  const openEdit = (item: BaseRecord) => {
    const values: FormState = {}
    fields.forEach(field => {
      if (field.type === 'unit') values[field.name] = String(item.unionUnit?.id ?? '')
      else if (field.type === 'checkbox') values[field.name] = Boolean(item[field.name])
      else values[field.name] = String(item[field.name] ?? '')
    })
    setEditingId(item.id)
    setForm(values)
    setError('')
    setFormOpen(true)
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setSaving(true)
    setError('')
    const payload: Record<string, unknown> = {}
    fields.forEach(field => {
      const value = form[field.name]
      if (field.type === 'number') payload[field.name] = value === '' ? null : Number(value)
      else if (field.type === 'unit') payload[field.name] = Number(value)
      else payload[field.name] = value === '' ? null : value
    })
    try {
      await api(`${endpoint}${editingId ? `/${editingId}` : ''}`, {
        method: editingId ? 'PUT' : 'POST',
        body: JSON.stringify(payload),
      })
      setFormOpen(false)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể lưu dữ liệu')
    } finally {
      setSaving(false)
    }
  }

  const remove = async (item: BaseRecord) => {
    if (!window.confirm(`Xóa ${singular.toLowerCase()} này?`)) return
    try {
      await api(`${endpoint}/${item.id}`, { method: 'DELETE' })
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể xóa dữ liệu')
    }
  }

  return (
    <section className="page-section">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Quản lý nghiệp vụ</p>
          <h1>{title}</h1>
          <p>{description}</p>
        </div>
        <div className="page-actions" id="page-actions">
          {enableMemberCsv && <>
            <button className="button button--ghost" onClick={() => void downloadFile(exportPath, 'doan-vien.csv').catch(err => setError(err instanceof Error ? err.message : 'Không thể xuất CSV'))}>Xuất CSV</button>
          </>}
          {excelResource && canImportExcel && <ExcelImportActions resource={excelResource} filename={excelFilename}
            onError={message => { setTransferMessage(''); setError(message) }} onImported={async result => {
              const summary = importSummary(result)
              if (result.errors.length) { setTransferMessage(''); setError(`${summary} Lỗi: ${result.errors.slice(0, 3).join(' · ')}`) }
              else { setError(''); setTransferMessage(summary) }
              await load()
            }} />}
          {!readOnly && <button className="button button--primary" onClick={openCreate}>+ Thêm {singular.toLowerCase()}</button>}
        </div>
      </div>

      {notice}
      {readOnly && <div className="notice notice--compact"><strong>Chỉ xem</strong><span>{readOnlyMessage ?? 'Tài khoản của bạn không có quyền thay đổi dữ liệu tại màn hình này.'}</span></div>}
      {transferMessage && <div className="alert alert--success">{transferMessage}</div>}
      {error && !formOpen && <div className="alert alert--danger">{error}</div>}

      {summaryCards.length > 0 && <div className="metric-grid metric-grid--compact" id="module-summary">
        {summaryCards.map(card => <article className={`metric-card metric-card--${card.tone ?? 'blue'}`} key={card.label}>
          <span>{card.label}</span><strong>{card.value}</strong><small>{card.note ?? 'Theo dữ liệu hiện tại'}</small>
        </article>)}
      </div>}

      <div className="data-card" id="records">
        <div className="data-card__header">
          <div className="record-count"><strong>{visibleItems.length} bản ghi</strong>{visibleItems.length !== items.length && <span>trên tổng {items.length}</span>}</div>
          <div className="table-filters">
            <select aria-label="Chọn trường tìm kiếm" value={searchField} onChange={event => setSearchField(event.target.value)}>
              <option value="all">Tất cả trường</option>
              {searchableFields.map(field => <option key={field.name} value={field.name}>{field.label}</option>)}
            </select>
            <input aria-label="Tìm kiếm" value={search} placeholder={selectedSearchField ? `Tìm theo ${selectedSearchField.label.toLocaleLowerCase('vi')}…` : 'Nhập từ khóa tìm kiếm…'} onChange={event => setSearch(event.target.value)} />
            {hasUnit && <select aria-label="Lọc theo CĐCS" value={unitFilter} onChange={event => setUnitFilter(event.target.value)}><option value="">Tất cả CĐCS</option>{units.map(unit => <option key={unit.id} value={unit.id}>{unit.code} · {unit.name}</option>)}</select>}
            {statusOptions.length > 1 && <select aria-label="Lọc theo trạng thái" value={statusFilter} onChange={event => setStatusFilter(event.target.value)}><option value="">Tất cả trạng thái</option>{statusOptions.map(value => <option key={value} value={value}>{enumLabel(value)}</option>)}</select>}
            {(search || unitFilter || statusFilter || searchField !== 'all') && <button className="button button--ghost" onClick={() => { setSearch(''); setSearchField('all'); setUnitFilter(''); setStatusFilter('') }}>Xóa lọc</button>}
            <button className="button button--ghost" onClick={() => void load()}>Làm mới</button>
          </div>
        </div>
        <div className="table-wrap">
          <table>
            <thead><tr>{columns.map(column => <th key={column.label}>{column.label}</th>)}{!readOnly && <th aria-label="Tác vụ" />}</tr></thead>
            <tbody>
              {loading && <tr><td colSpan={columns.length + (readOnly ? 0 : 1)} className="empty-cell">Đang tải dữ liệu…</td></tr>}
              {!loading && visibleItems.length === 0 && <tr><td colSpan={columns.length + (readOnly ? 0 : 1)} className="empty-cell">{items.length ? 'Không có dữ liệu phù hợp bộ lọc.' : 'Chưa có dữ liệu.'}</td></tr>}
              {!loading && visibleItems.map(item => (
                <tr key={item.id}>
                  {columns.map(column => <td key={column.label} className={column.className}>{column.render(item)}</td>)}
                  {!readOnly && <td className="actions-cell">
                    <button className="icon-button" onClick={() => openEdit(item)}>Sửa</button>
                    <button className="icon-button icon-button--danger" onClick={() => void remove(item)}>Xóa</button>
                  </td>}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {footer}

      {formOpen && (
        <div className="modal-backdrop" onMouseDown={() => setFormOpen(false)}>
          <div className="modal" onMouseDown={event => event.stopPropagation()}>
            <div className="modal__header">
              <div><p className="eyebrow">{editingId ? 'Cập nhật' : 'Tạo mới'}</p><h2>{singular}</h2></div>
              <button className="modal__close" onClick={() => setFormOpen(false)}>×</button>
            </div>
            <form onSubmit={event => void submit(event)} className="form-grid">
              {fields.map(field => (
                <label key={field.name} className={field.wide ? 'field field--wide' : 'field'}>
                  <span>{field.label}{field.required && <b> *</b>}</span>
                  {field.type === 'textarea' ? (
                    <textarea required={field.required} value={String(form[field.name] ?? '')} placeholder={field.placeholder}
                      onChange={event => setForm(current => ({ ...current, [field.name]: event.target.value }))} />
                  ) : field.type === 'select' ? (
                    <select required={field.required} value={String(form[field.name] ?? '')}
                      onChange={event => setForm(current => ({ ...current, [field.name]: event.target.value }))}>
                      <option value="">Chọn…</option>
                      {field.options?.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
                    </select>
                  ) : field.type === 'unit' ? (
                    <select required={field.required} value={String(form[field.name] ?? '')}
                      onChange={event => setForm(current => ({ ...current, [field.name]: event.target.value }))}>
                      <option value="">Chọn CĐCS…</option>
                      {units.map(unit => <option key={unit.id} value={unit.id}>{unit.code} · {unit.name}</option>)}
                    </select>
                  ) : field.type === 'checkbox' ? (
                    <input type="checkbox" className="checkbox" checked={Boolean(form[field.name])}
                      onChange={event => setForm(current => ({ ...current, [field.name]: event.target.checked }))} />
                  ) : (
                    <input type={field.type ?? 'text'} required={field.required} step={field.step}
                      value={String(form[field.name] ?? '')} placeholder={field.placeholder}
                      onChange={event => setForm(current => ({ ...current, [field.name]: event.target.value }))} />
                  )}
                </label>
              ))}
              {error && <div className="alert alert--danger field--wide">{error}</div>}
              <div className="form-actions field--wide">
                <button type="button" className="button button--ghost" onClick={() => setFormOpen(false)}>Hủy</button>
                <button type="submit" className="button button--primary" disabled={saving}>{saving ? 'Đang lưu…' : 'Lưu dữ liệu'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </section>
  )
}
