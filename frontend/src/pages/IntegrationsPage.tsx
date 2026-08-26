import { useMemo, useState } from 'react'
import { currentMonth, downloadFile, enumLabel, formatDate } from '../api'
import { StatusBadge } from '../components/CrudPage'
import ExcelImportActions from '../components/ExcelImportActions'
import ListCard from '../components/ListCard'
import TableFilterBar, { FilterField } from '../components/TableFilterBar'
import { usePagedList } from '../hooks/usePagedList'
import { importSummary } from '../excel'
import type { IntegrationRun, UnionUnit } from '../types'

type Props = { units: UnionUnit[] }

export default function IntegrationsPage({ units }: Props) {
  const [month, setMonth] = useState(currentMonth())
  const [unitId, setUnitId] = useState('')
  const [message, setMessage] = useState('')
  const [actionError, setActionError] = useState('')
  const [search, setSearch] = useState('')
  const [searchField, setSearchField] = useState('all')
  const [statusFilter, setStatusFilter] = useState('')

  const filters = useMemo(() => ({
    q: search.trim() || undefined,
    searchField: searchField === 'all' ? undefined : searchField,
    status: statusFilter || undefined,
  }), [search, searchField, statusFilter])

  const runs = usePagedList<IntegrationRun>({ endpoint: '/integrations/runs', filters })
  const filtersActive = Boolean(search || searchField !== 'all' || statusFilter)
  const error = actionError || runs.error

  const exportFinance = async () => {
    const query = new URLSearchParams()
    if (month) query.set('month', month)
    if (unitId) query.set('unitId', unitId)
    try {
      await downloadFile(`/integrations/finance/export.csv?${query}`, `tai-chinh-noi-bo-${month}.csv`)
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Không thể xuất CSV tài chính')
    }
  }

  const imported = async (result: Parameters<typeof importSummary>[0]) => {
    const summary = importSummary(result)
    if (result.errors.length) setActionError(`${summary} ${result.errors.slice(0, 3).join(' · ')}`)
    else { setActionError(''); setMessage(summary) }
    await runs.reload()
  }

  return (
    <section className="page-section">
      <div className="page-heading">
        <div><p className="eyebrow">Quản trị dữ liệu</p><h1>Tích hợp dữ liệu nội bộ</h1><p>Nhập Excel theo lô cho mọi phân hệ, có kiểm tra lỗi, cập nhật theo mã và nhật ký truy vết.</p></div>
      </div>

      <div className="notice integration-safety"><strong>Ranh giới tài chính</strong><span>Chỉ nhập, xuất và tính toán dữ liệu kế toán trong hệ thống. Không có kết nối ngân hàng, ví điện tử, cổng thanh toán hoặc lệnh chuyển tiền.</span></div>
      {message && <div className="alert alert--success">{message}</div>}
      {error && <div className="alert alert--danger">{error}</div>}

      <div className="integration-grid" id="integration-import">
        <article className="panel integration-card">
          <div className="integration-card__icon">HR</div>
          <div><p className="eyebrow">HR master</p><h2>Nhập hồ sơ nhân sự</h2><p>Cập nhật đầy đủ trường đoàn viên theo mã nhân viên và mã CĐCS. Dòng lỗi được bỏ qua và ghi vào lịch sử.</p></div>
          <div className="integration-actions">
            <ExcelImportActions resource="members" filename="mau-doan-vien.xlsx" importLabel="Nhập HR Excel" onImported={imported} onError={setActionError} />
          </div>
        </article>

        <article className="panel integration-card">
          <div className="integration-card__icon integration-card__icon--finance">TC</div>
          <div><p className="eyebrow">Kế toán nội bộ</p><h2>Nhập phiếu thu – chi</h2><p>Cập nhật đầy đủ trường theo mã phiếu bằng Excel; chỉ ghi nhận nội bộ, không phát sinh giao dịch tiền.</p></div>
          <div className="integration-actions"><ExcelImportActions resource="finance" filename="mau-tai-chinh-noi-bo.xlsx" importLabel="Nhập tài chính Excel" onImported={imported} onError={setActionError} /></div>
        </article>

        <article className="panel integration-card integration-card--wide">
          <div><p className="eyebrow">Đối soát file</p><h2>Xuất dữ liệu tài chính</h2><p>CSV chỉ dùng đối soát nhanh; nhập dữ liệu chính thức bằng file Excel mẫu ở trên.</p></div>
          <div className="report-filters">
            <label><span>Tháng</span><input type="month" value={month} onChange={event => setMonth(event.target.value)} /></label>
            <label><span>CĐCS</span><select value={unitId} onChange={event => setUnitId(event.target.value)}><option value="">Toàn hệ thống</option>{units.map(unit => <option key={unit.id} value={unit.id}>{unit.code}</option>)}</select></label>
            <button className="button button--ghost" onClick={() => void exportFinance()}>Xuất CSV tài chính</button>
          </div>
        </article>
      </div>

      <ListCard
        id="integration-history"
        className="integration-history"
        list={runs}
        unit="lượt"
        title="Lịch sử nhập dữ liệu"
        subtitle={runs.total === runs.facets.total
          ? `${runs.facets.total} lượt đã ghi nhận`
          : `${runs.total} trên tổng ${runs.facets.total} lượt`}
        actions={<>
          {filtersActive && <button className="button button--ghost" onClick={() => { setSearch(''); setSearchField('all'); setStatusFilter('') }}>Xóa lọc</button>}
          <button className="button button--ghost" onClick={() => void runs.reload()}>Làm mới</button>
        </>}
        filters={<TableFilterBar>
          <FilterField label="Trường"><select aria-label="Chọn trường tìm kiếm lịch sử nhập" value={searchField} onChange={event => setSearchField(event.target.value)}><option value="all">Tất cả trường</option><option value="integrationType">Loại dữ liệu</option><option value="fileName">Tên tệp</option><option value="status">Kết quả</option><option value="startedBy">Người thực hiện</option><option value="completedAt">Thời gian</option><option value="errorSummary">Chi tiết lỗi</option></select></FilterField>
          {runs.facets.statusValues.length > 1 && <FilterField label="Kết quả"><select aria-label="Lọc theo kết quả nhập" value={statusFilter} onChange={event => setStatusFilter(event.target.value)}><option value="">Tất cả</option>{runs.facets.statusValues.map(value => <option key={value} value={value}>{enumLabel(value)}</option>)}</select></FilterField>}
          <FilterField label="Tìm kiếm" search><input aria-label="Tìm lịch sử nhập" value={search} placeholder="Tên tệp / người nhập…" onChange={event => setSearch(event.target.value)} /></FilterField>
        </TableFilterBar>}
      >
        <div className="table-wrap"><table><thead><tr><th>Loại</th><th>Tệp</th><th>Kết quả</th><th>Số dòng</th><th>Người thực hiện</th><th>Hoàn tất</th><th>Chi tiết lỗi</th></tr></thead><tbody>
          {runs.loading && <tr><td colSpan={7} className="empty-cell">Đang tải lịch sử…</td></tr>}
          {!runs.loading && !runs.rows.length && <tr><td colSpan={7} className="empty-cell">{filtersActive ? 'Không có lượt nhập phù hợp bộ lọc.' : 'Chưa có lượt tích hợp dữ liệu.'}</td></tr>}
          {!runs.loading && runs.rows.map(run => <tr key={run.id}><td><strong>{enumLabel(run.integrationType)}</strong></td><td>{run.fileName}</td><td><StatusBadge value={run.status} /></td><td>{run.successfulRows}/{run.totalRows}</td><td>{run.startedBy}</td><td>{formatDate(run.completedAt.slice(0, 10))}</td><td><small className="table-subtext">{run.errorSummary ?? 'Không có lỗi'}</small></td></tr>)}
        </tbody></table></div>
      </ListCard>
    </section>
  )
}
