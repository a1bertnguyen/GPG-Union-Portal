import { Fragment, useEffect, useMemo, useState, type FormEvent, type ReactNode } from 'react'
import { api, downloadFile, enumLabel } from '../api'
import type { BaseRecord, UnionUnit } from '../types'
import { importSummary } from '../excel'
import ExcelImportActions from './ExcelImportActions'
import ListCard from './ListCard'
import TableFilterBar, { FilterField } from './TableFilterBar'
import { usePagedList } from '../hooks/usePagedList'

type Option = { value: string; label: string; disabled?: boolean }

export type FieldConfig = {
  name: string
  label: string
  type?: 'text' | 'date' | 'time' | 'number' | 'email' | 'textarea' | 'select' | 'unit' | 'checkbox' | 'file'
  required?: boolean
  placeholder?: string
  options?: Option[]
  step?: string
  wide?: boolean
  defaultValue?: string | boolean
  min?: string
  max?: string
  readOnly?: boolean
  hidden?: boolean
  section?: string
  sectionDescription?: string
  suggestions?: Option[]
  currency?: boolean
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

/** Tracking filter. The matching itself lives on the server; `value` is the key it understands. */
export type PresetFilter = { value: string; label: string }

type Props = {
  endpoint: string
  title: string
  description: string
  singular: string
  fields: FieldConfig[]
  columns: ColumnConfig[]
  units: UnionUnit[]
  notice?: ReactNode
  enableMemberExcel?: boolean
  excelResource?: string
  excelFilename?: string
  excelDownloadPath?: string
  excelDownloadLabel?: string
  canImportExcel?: boolean
  canDownloadExcel?: boolean
  readOnly?: boolean
  readOnlyMessage?: string
  wideForm?: boolean
  /**
   * Builds the metric cards from the whole-dataset numbers returned by `{endpoint}/facets`.
   * Labels, tones and money formatting stay here; the server only supplies raw counts and sums.
   */
  summaryBuilder?: (metrics: Record<string, number>) => SummaryCard[]
  presetFilters?: PresetFilter[]
  detailRenderer?: (item: BaseRecord, refresh: () => Promise<void>) => ReactNode
  detailActionLabel?: string
  canEditItem?: (item: BaseRecord) => boolean
  canDeleteItem?: (item: BaseRecord) => boolean
  openCreateInitially?: boolean
  onInitialCreateOpened?: () => void
  deriveForm?: (form: FormState, changedField: string) => FormState
  afterSave?: (saved: BaseRecord, form: FormState, isUpdate: boolean) => Promise<void>
}

export type FormState = Record<string, string | boolean | File | null>

const numberFromInput = (value: string | boolean | File | null | undefined) =>
  Number(String(value ?? '').replaceAll(',', '').replaceAll(' ', ''))

const commaNumber = (value: string | boolean | File | null | undefined) => {
  const raw = String(value ?? '').replaceAll(',', '').replaceAll(' ', '')
  if (!raw || Number.isNaN(Number(raw))) return raw
  return new Intl.NumberFormat('en-US', { maximumFractionDigits: 2 }).format(Number(raw))
}

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

export default function CrudPage({ endpoint, title, description, singular, fields, columns, units, notice, enableMemberExcel = false, excelResource, excelFilename = 'mau-du-lieu.xlsx', excelDownloadPath, excelDownloadLabel, canImportExcel = true, canDownloadExcel = canImportExcel, readOnly = false, readOnlyMessage, wideForm = false, summaryBuilder, presetFilters = [], detailRenderer, detailActionLabel = 'Mở', canEditItem, canDeleteItem, openCreateInitially = false, onInitialCreateOpened, deriveForm, afterSave }: Props) {
  const [formOpen, setFormOpen] = useState(openCreateInitially)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [form, setForm] = useState<FormState>(() => openCreateInitially
    ? Object.fromEntries(fields.map(field => [field.name, field.defaultValue ?? (field.type === 'checkbox' ? false : '')]))
    : {})
  const [formError, setFormError] = useState('')
  const [actionError, setActionError] = useState('')
  const [saving, setSaving] = useState(false)
  const [search, setSearch] = useState('')
  const [searchField, setSearchField] = useState('all')
  const [unitFilter, setUnitFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [presetFilter, setPresetFilter] = useState('')
  const [transferMessage, setTransferMessage] = useState('')
  const [viewingItem, setViewingItem] = useState<BaseRecord | null>(null)

  const filters = useMemo(() => ({
    q: search.trim() || undefined,
    searchField: searchField === 'all' ? undefined : searchField,
    unitId: unitFilter || undefined,
    status: statusFilter || undefined,
    preset: presetFilter || undefined,
  }), [search, searchField, unitFilter, statusFilter, presetFilter])

  const list = usePagedList<BaseRecord>({ endpoint, filters })
  const emptyForm = useMemo(() => Object.fromEntries(fields.map(field => [field.name, field.defaultValue ?? (field.type === 'checkbox' ? false : '')])), [fields])

  useEffect(() => {
    if (openCreateInitially) onInitialCreateOpened?.()
  }, [onInitialCreateOpened, openCreateInitially])

  const hasUnit = fields.some(field => field.type === 'unit')
  const searchableFields = useMemo(() => fields.filter(field => !field.hidden && field.type !== 'checkbox' && field.type !== 'file'), [fields])
  const selectedSearchField = searchableFields.find(field => field.name === searchField)
  const statusOptions = list.facets.statusValues
  const summaryCards = useMemo(() => summaryBuilder?.(list.facets.metrics) ?? [], [list.facets.metrics, summaryBuilder])
  const filtersActive = Boolean(search || unitFilter || statusFilter || presetFilter || searchField !== 'all')

  // Exports the filtered set rather than the page on screen, so the same params go to the server.
  const exportPath = useMemo(() => {
    const query = new URLSearchParams()
    if (search.trim()) query.set('q', search.trim())
    if (searchField !== 'all') query.set('searchField', searchField)
    if (unitFilter) query.set('unitId', unitFilter)
    if (statusFilter) query.set('status', statusFilter)
    if (presetFilter) query.set('preset', presetFilter)
    return `/members/export.xlsx${query.size ? `?${query}` : ''}`
  }, [presetFilter, search, searchField, statusFilter, unitFilter])

  const clearFilters = () => {
    setSearch('')
    setSearchField('all')
    setUnitFilter('')
    setStatusFilter('')
    setPresetFilter('')
  }

  const openCreate = () => {
    setEditingId(null)
    setForm(emptyForm)
    setFormError('')
    setFormOpen(true)
  }

  const openEdit = (item: BaseRecord) => {
    const values: FormState = {}
    fields.forEach(field => {
      if (field.type === 'file') values[field.name] = null
      else if (field.type === 'unit') values[field.name] = String(item.unionUnit?.id ?? '')
      else if (field.type === 'checkbox') values[field.name] = Boolean(item[field.name])
      else values[field.name] = String(item[field.name] ?? '')
    })
    setEditingId(item.id)
    setForm(values)
    setFormError('')
    setFormOpen(true)
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setSaving(true)
    setFormError('')
    const payload: Record<string, unknown> = {}
    fields.forEach(field => {
      const value = form[field.name]
      if (field.type === 'file') return
      if (field.type === 'number' || field.currency) payload[field.name] = value === '' ? null : numberFromInput(value)
      else if (field.type === 'unit') payload[field.name] = Number(value)
      else payload[field.name] = value === '' ? null : value
    })
    try {
      const saved = await api<BaseRecord>(`${endpoint}${editingId ? `/${editingId}` : ''}`, {
        method: editingId ? 'PUT' : 'POST',
        body: JSON.stringify(payload),
      })
      try {
        await afterSave?.(saved, form, Boolean(editingId))
      } catch (afterSaveError) {
        setActionError(afterSaveError instanceof Error
          ? `Đã lưu dữ liệu nhưng chưa tải được tệp: ${afterSaveError.message}`
          : 'Đã lưu dữ liệu nhưng chưa tải được tệp đính kèm')
      }
      setFormOpen(false)
      await list.reload()
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Không thể lưu dữ liệu')
    } finally {
      setSaving(false)
    }
  }

  const remove = async (item: BaseRecord) => {
    if (!window.confirm(`Xóa ${singular.toLowerCase()} này?`)) return
    try {
      await api(`${endpoint}/${item.id}`, { method: 'DELETE' })
      await list.reload()
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Không thể xóa dữ liệu')
    }
  }

  const updateField = (name: string, value: string | boolean | File | null) => {
    setForm(current => {
      const next = { ...current, [name]: value }
      return deriveForm?.(next, name) ?? next
    })
  }

  const actionColumn = !readOnly || Boolean(detailRenderer)
  const columnCount = columns.length + (actionColumn ? 1 : 0)
  const error = actionError || list.error

  return (
    <section className="page-section">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Quản lý nghiệp vụ</p>
          <h1>{title}</h1>
          <p>{description}</p>
        </div>
        <div className="page-actions" id="page-actions">
          {enableMemberExcel && <button className="button button--ghost" onClick={() => void downloadFile(exportPath, 'doan-vien.xlsx').catch(err => setActionError(err instanceof Error ? err.message : 'Không thể xuất Excel'))}>Xuất Excel</button>}
          {excelResource && (canImportExcel || canDownloadExcel) && <ExcelImportActions resource={excelResource} filename={excelFilename}
            downloadPath={excelDownloadPath} templateLabel={excelDownloadLabel}
            canImport={canImportExcel}
            onError={message => { setTransferMessage(''); setActionError(message) }} onImported={async result => {
              const summary = importSummary(result)
              if (result.errors.length) { setTransferMessage(''); setActionError(`${summary} Lỗi: ${result.errors.slice(0, 3).join(' · ')}`) }
              else { setActionError(''); setTransferMessage(summary) }
              await list.reload()
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

      <ListCard
        id="records"
        list={list}
        title={`${list.total} bản ghi`}
        subtitle={list.total !== list.facets.total ? `Trên tổng ${list.facets.total}` : undefined}
        actions={<>
          {filtersActive && <button className="button button--ghost" onClick={clearFilters}>Xóa lọc</button>}
          <button className="button button--ghost" onClick={() => void list.reload()}>Làm mới</button>
        </>}
        filters={<TableFilterBar>
          <FilterField label="Trường">
            <select aria-label="Chọn trường tìm kiếm" value={searchField} onChange={event => setSearchField(event.target.value)}>
              <option value="all">Tất cả trường</option>
              {searchableFields.map(field => <option key={field.name} value={field.name}>{field.label}</option>)}
            </select>
          </FilterField>
          {hasUnit && <FilterField label="CĐCS"><select aria-label="Lọc theo CĐCS" value={unitFilter} onChange={event => setUnitFilter(event.target.value)}><option value="">Tất cả CĐCS</option>{units.map(unit => <option key={unit.id} value={unit.id}>{unit.code} · {unit.name}</option>)}</select></FilterField>}
          {statusOptions.length > 1 && <FilterField label="Trạng thái"><select aria-label="Lọc theo trạng thái" value={statusFilter} onChange={event => setStatusFilter(event.target.value)}><option value="">Tất cả trạng thái</option>{statusOptions.map(value => <option key={value} value={value}>{enumLabel(value)}</option>)}</select></FilterField>}
          {presetFilters.length > 0 && <FilterField label="Theo dõi"><select aria-label="Lọc nhanh nghiệp vụ" value={presetFilter} onChange={event => setPresetFilter(event.target.value)}><option value="">Tất cả</option>{presetFilters.map(filter => <option key={filter.value} value={filter.value}>{filter.label}</option>)}</select></FilterField>}
          <FilterField label="Tìm kiếm" search><input aria-label="Tìm kiếm" value={search} placeholder={selectedSearchField ? `Theo ${selectedSearchField.label.toLocaleLowerCase('vi')}…` : 'Tên / mã…'} onChange={event => setSearch(event.target.value)} /></FilterField>
        </TableFilterBar>}
      >
        <div className="table-wrap">
          <table>
            <thead><tr>{columns.map(column => <th key={column.label}>{column.label}</th>)}{actionColumn && <th aria-label="Tác vụ" />}</tr></thead>
            <tbody>
              {list.loading && <tr><td colSpan={columnCount} className="empty-cell">Đang tải dữ liệu…</td></tr>}
              {!list.loading && list.rows.length === 0 && <tr><td colSpan={columnCount} className="empty-cell">{filtersActive ? 'Không có dữ liệu phù hợp bộ lọc.' : 'Chưa có dữ liệu.'}</td></tr>}
              {!list.loading && list.rows.map(item => (
                <tr key={item.id}>
                  {columns.map(column => <td key={column.label} className={column.className}>{column.render(item)}</td>)}
                  {actionColumn && <td className="actions-cell">
                    {detailRenderer && <button className="icon-button icon-button--view" onClick={() => setViewingItem(item)}>{detailActionLabel}</button>}
                    {!readOnly && (canEditItem?.(item) ?? true) && <button className="icon-button" onClick={() => openEdit(item)}>Sửa</button>}
                    {!readOnly && (canDeleteItem?.(item) ?? true) && <button className="icon-button icon-button--danger" onClick={() => void remove(item)}>Xóa</button>}
                  </td>}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </ListCard>

      {formOpen && (
        <div className="modal-backdrop" onMouseDown={() => setFormOpen(false)}>
          <div className={wideForm ? 'modal modal--wide' : 'modal'} onMouseDown={event => event.stopPropagation()}>
            <div className="modal__header">
              <div><p className="eyebrow">{editingId ? 'Cập nhật' : 'Tạo mới'}</p><h2>{singular}</h2></div>
              <button className="modal__close" onClick={() => setFormOpen(false)}>×</button>
            </div>
            <form onSubmit={event => void submit(event)} className={wideForm ? 'form-grid form-grid--wide' : 'form-grid'}>
                {fields.map(field => field.hidden ? null : (
                  <Fragment key={field.name}>
                    {field.section && <div className="form-section-heading field--wide">
                      <strong>{field.section}</strong>
                      {field.sectionDescription && <span>{field.sectionDescription}</span>}
                    </div>}
                    <label className={field.wide ? 'field field--wide' : 'field'}>
                      <span>{field.label}{field.required && <b> *</b>}</span>
                      {field.type === 'textarea' ? (
                        <textarea required={field.required} readOnly={field.readOnly} value={String(form[field.name] ?? '')} placeholder={field.placeholder}
                          onChange={event => updateField(field.name, event.target.value)} />
                      ) : field.type === 'select' ? (
                        <select required={field.required} disabled={field.readOnly} value={String(form[field.name] ?? '')}
                          onChange={event => updateField(field.name, event.target.value)}>
                          <option value="">Chọn…</option>
                          {field.options?.map(option => <option key={option.value} value={option.value} disabled={option.disabled}>{option.label}</option>)}
                        </select>
                      ) : field.type === 'unit' ? (
                        <select required={field.required} disabled={field.readOnly} value={String(form[field.name] ?? '')}
                          onChange={event => updateField(field.name, event.target.value)}>
                          <option value="">Chọn CĐCS…</option>
                          {units.map(unit => <option key={unit.id} value={unit.id}>{unit.code} · {unit.name}</option>)}
                        </select>
                      ) : field.type === 'checkbox' ? (
                        <input type="checkbox" className="checkbox" disabled={field.readOnly} checked={Boolean(form[field.name])}
                          onChange={event => updateField(field.name, event.target.checked)} />
                      ) : field.type === 'file' ? (
                        <input type="file" required={field.required} disabled={field.readOnly} accept=".pdf,.doc,.docx,.xls,.xlsx,image/*"
                          onChange={event => updateField(field.name, event.target.files?.[0] ?? null)} />
                      ) : (
                        <><input type={field.currency ? 'text' : field.type ?? 'text'} inputMode={field.currency ? 'decimal' : undefined} required={field.required} step={field.step} min={field.min} max={field.max} readOnly={field.readOnly}
                          list={field.suggestions?.length ? `field-suggestions-${field.name}` : undefined}
                          value={field.currency ? commaNumber(form[field.name]) : String(form[field.name] ?? '')} placeholder={field.placeholder}
                          onChange={event => updateField(field.name, event.target.value)} />
                        {field.suggestions?.length ? <datalist id={`field-suggestions-${field.name}`}>{field.suggestions.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}</datalist> : null}</>
                      )}
                    </label>
                  </Fragment>
                ))}
              {formError && <div className="alert alert--danger field--wide">{formError}</div>}
              <div className="form-actions field--wide">
                <button type="button" className="button button--ghost" onClick={() => setFormOpen(false)}>Hủy</button>
                <button type="submit" className="button button--primary" disabled={saving}>{saving ? 'Đang lưu…' : 'Lưu dữ liệu'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {viewingItem && detailRenderer && (
        <div className="modal-backdrop" onMouseDown={() => setViewingItem(null)}>
          <div className="modal modal--workspace" onMouseDown={event => event.stopPropagation()}>
            <div className="modal__header">
              <div><p className="eyebrow">Hồ sơ chi tiết</p><h2>{String(viewingItem.fullName ?? viewingItem.name ?? singular)}</h2></div>
              <button className="modal__close" onClick={() => setViewingItem(null)}>×</button>
            </div>
            {detailRenderer(viewingItem, list.reload)}
          </div>
        </div>
      )}
    </section>
  )
}
