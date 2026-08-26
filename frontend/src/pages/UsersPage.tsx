import { useMemo, useState, type FormEvent } from 'react'
import { api } from '../api'
import { StatusBadge } from '../components/CrudPage'
import ExcelImportActions from '../components/ExcelImportActions'
import ListCard from '../components/ListCard'
import TableFilterBar, { FilterField } from '../components/TableFilterBar'
import { usePagedList } from '../hooks/usePagedList'
import { importSummary } from '../excel'
import type { UnionUnit, UserAccount } from '../types'

type Props = { units: UnionUnit[] }
type AccountForm = {
  username: string
  fullName: string
  role: 'ADMIN' | 'USER'
  unionUnitId: string
  active: boolean
  password: string
}

const emptyForm: AccountForm = {
  username: '', fullName: '', role: 'USER', unionUnitId: '', active: true, password: '',
}

export default function UsersPage({ units }: Props) {
  const [form, setForm] = useState<AccountForm | null>(null)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')
  const [actionError, setActionError] = useState('')
  const [message, setMessage] = useState('')
  const [search, setSearch] = useState('')
  const [searchField, setSearchField] = useState('all')
  const [statusFilter, setStatusFilter] = useState('')

  const filters = useMemo(() => ({
    q: search.trim() || undefined,
    searchField: searchField === 'all' ? undefined : searchField,
    status: statusFilter || undefined,
  }), [search, searchField, statusFilter])

  const list = usePagedList<UserAccount>({ endpoint: '/admin/users', filters })
  const adminCount = list.facets.metrics.activeAdmins ?? 0
  const userCount = list.facets.metrics.activeUsers ?? 0
  const filtersActive = Boolean(search || searchField !== 'all' || statusFilter)
  const error = actionError || list.error

  const openCreate = () => {
    setEditingId(null)
    setForm({ ...emptyForm, unionUnitId: String(units[0]?.id ?? '') })
    setFormError('')
  }

  const openEdit = (account: UserAccount) => {
    setEditingId(account.id)
    setForm({
      username: account.username,
      fullName: account.fullName,
      role: account.role,
      unionUnitId: String(account.unionUnitId ?? units[0]?.id ?? ''),
      active: account.active,
      password: '',
    })
    setFormError('')
  }

  const save = async (event: FormEvent) => {
    event.preventDefault()
    if (!form) return
    setSaving(true)
    setFormError('')
    try {
      await api(`/admin/users${editingId ? `/${editingId}` : ''}`, {
        method: editingId ? 'PUT' : 'POST',
        body: JSON.stringify({
          ...form,
          unionUnitId: form.role === 'USER' ? Number(form.unionUnitId) : null,
          password: form.password || null,
        }),
      })
      setForm(null)
      await list.reload()
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Không thể lưu tài khoản')
    } finally {
      setSaving(false)
    }
  }

  const remove = async (account: UserAccount) => {
    if (!window.confirm(`Xóa tài khoản “${account.username}”? Thao tác này không thể hoàn tác.`)) return
    try {
      await api(`/admin/users/${account.id}`, { method: 'DELETE' })
      setMessage(`Đã xóa tài khoản ${account.username}.`)
      setActionError('')
      await list.reload()
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Không thể xóa tài khoản')
    }
  }

  return (
    <section className="page-section">
      <div className="page-heading">
        <div><p className="eyebrow">Quản trị hệ thống</p><h1>Tài khoản & phân quyền</h1><p>ADMIN quản lý toàn hệ thống; USER vận hành dữ liệu trong CĐCS được gán.</p></div>
        <div className="page-actions">
          <ExcelImportActions resource="users" filename="mau-tai-khoan.xlsx" importLabel="Nhập tài khoản" templateLabel="Mẫu Excel"
            onError={setActionError} onImported={async result => {
              const summary = importSummary(result)
              if (result.errors.length) setActionError(`${summary} Lỗi: ${result.errors.slice(0, 3).join(' · ')}`)
              else { setActionError(''); setMessage(summary) }
              await list.reload()
            }} />
          <button className="button button--primary" onClick={openCreate}>+ Thêm tài khoản</button>
        </div>
      </div>

      <div className="role-summary" id="role-summary">
        <div><span className="role-chip role-chip--admin">ADMIN</span><strong>{adminCount} đang hoạt động</strong><small>Toàn quyền, quản lý tài khoản, CĐCS và tích hợp.</small></div>
        <div><span className="role-chip role-chip--user">USER</span><strong>{userCount} đang hoạt động</strong><small>Thao tác nghiệp vụ trong đúng CĐCS được gán.</small></div>
      </div>

      {message && <div className="alert alert--success">{message}</div>}
      {error && !form && <div className="alert alert--danger">{error}</div>}

      <ListCard
        id="users-list"
        list={list}
        unit="tài khoản"
        title={`${list.total} tài khoản`}
        subtitle={list.total === list.facets.total
          ? 'Có thể khóa hoặc xóa tài khoản; tài khoản đang đăng nhập và ADMIN cuối cùng được bảo vệ'
          : `Trên tổng ${list.facets.total} tài khoản`}
        actions={<>
          {filtersActive && <button className="button button--ghost" onClick={() => { setSearch(''); setSearchField('all'); setStatusFilter('') }}>Xóa lọc</button>}
          <button className="button button--ghost" onClick={() => void list.reload()}>Làm mới</button>
        </>}
        filters={<TableFilterBar>
          <FilterField label="Trường"><select aria-label="Chọn trường tìm kiếm tài khoản" value={searchField} onChange={event => setSearchField(event.target.value)}><option value="all">Tất cả trường</option><option value="username">Tên đăng nhập</option><option value="fullName">Họ tên</option><option value="role">Vai trò</option><option value="unit">CĐCS</option><option value="active">Trạng thái</option></select></FilterField>
          <FilterField label="Trạng thái"><select aria-label="Lọc theo trạng thái tài khoản" value={statusFilter} onChange={event => setStatusFilter(event.target.value)}><option value="">Tất cả</option><option value="ACTIVE">Đang hoạt động</option><option value="INACTIVE">Đã khóa</option></select></FilterField>
          <FilterField label="Tìm kiếm" search><input aria-label="Tìm kiếm tài khoản" value={search} placeholder="Tên / tài khoản…" onChange={event => setSearch(event.target.value)} /></FilterField>
        </TableFilterBar>}
      >
        <div className="table-wrap"><table><thead><tr><th>Người dùng</th><th>Vai trò</th><th>Phạm vi</th><th>Trạng thái</th><th>Đăng nhập gần nhất</th><th /></tr></thead><tbody>
          {list.loading && <tr><td colSpan={6} className="empty-cell">Đang tải tài khoản…</td></tr>}
          {!list.loading && !list.rows.length && <tr><td colSpan={6} className="empty-cell">{filtersActive ? 'Không có tài khoản phù hợp bộ lọc.' : 'Chưa có tài khoản.'}</td></tr>}
          {!list.loading && list.rows.map(account => <tr key={account.id}>
            <td><strong>{account.fullName}</strong><small className="table-subtext">{account.username}</small></td>
            <td><span className={`role-chip role-chip--${account.role.toLowerCase()}`}>{account.role}</span></td>
            <td>{account.role === 'ADMIN' ? 'Toàn hệ thống' : `${account.unionUnitCode} · ${account.unionUnitName}`}</td>
            <td><StatusBadge value={account.active ? 'ACTIVE' : 'INACTIVE'} /></td>
            <td>{account.lastLoginAt ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(account.lastLoginAt)) : 'Chưa đăng nhập'}</td>
            <td className="actions-cell"><button className="icon-button" onClick={() => openEdit(account)}>Chỉnh sửa</button><button className="icon-button icon-button--danger" onClick={() => void remove(account)}>Xóa</button></td>
          </tr>)}
        </tbody></table></div>
      </ListCard>

      {form && <div className="modal-backdrop" onMouseDown={() => setForm(null)}><div className="modal modal--compact" onMouseDown={event => event.stopPropagation()}>
        <div className="modal__header"><div><p className="eyebrow">{editingId ? 'Cập nhật' : 'Tạo mới'}</p><h2>Tài khoản nội bộ</h2></div><button className="modal__close" onClick={() => setForm(null)}>×</button></div>
        <form className="form-grid" onSubmit={event => void save(event)}>
          <label className="field"><span>Tên đăng nhập *</span><input required value={form.username} onChange={event => setForm(current => current && ({ ...current, username: event.target.value }))} /></label>
          <label className="field"><span>Họ tên *</span><input required value={form.fullName} onChange={event => setForm(current => current && ({ ...current, fullName: event.target.value }))} /></label>
          <label className="field"><span>Vai trò *</span><select value={form.role} onChange={event => setForm(current => current && ({ ...current, role: event.target.value as 'ADMIN' | 'USER' }))}><option value="USER">USER · Theo CĐCS</option><option value="ADMIN">ADMIN · Toàn hệ thống</option></select></label>
          {form.role === 'USER' && <label className="field"><span>CĐCS *</span><select required value={form.unionUnitId} onChange={event => setForm(current => current && ({ ...current, unionUnitId: event.target.value }))}><option value="">Chọn CĐCS…</option>{units.map(unit => <option key={unit.id} value={unit.id}>{unit.code} · {unit.name}</option>)}</select></label>}
          <label className="field field--wide"><span>{editingId ? 'Mật khẩu mới (để trống nếu giữ nguyên)' : 'Mật khẩu khởi tạo *'}</span><input type="password" minLength={8} required={!editingId} autoComplete="new-password" value={form.password} onChange={event => setForm(current => current && ({ ...current, password: event.target.value }))} /></label>
          <label className="field inline-check"><input className="checkbox" type="checkbox" checked={form.active} onChange={event => setForm(current => current && ({ ...current, active: event.target.checked }))} /><span>Tài khoản đang hoạt động</span></label>
          {formError && <div className="alert alert--danger field--wide">{formError}</div>}
          <div className="form-actions field--wide"><button type="button" className="button button--ghost" onClick={() => setForm(null)}>Hủy</button><button className="button button--primary" disabled={saving}>{saving ? 'Đang lưu…' : 'Lưu tài khoản'}</button></div>
        </form>
      </div></div>}
    </section>
  )
}
