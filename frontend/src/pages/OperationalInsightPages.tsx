import { useEffect, useMemo, useState } from 'react'
import { api, enumLabel, formatDate, formatMoney } from '../api'
import { StatusBadge } from '../components/CrudPage'
import ListCard from '../components/ListCard'
import TableFilterBar, { FilterField } from '../components/TableFilterBar'
import { usePagedList } from '../hooks/usePagedList'
import type { BaseRecord, CaseGroupCount, UnionUnit } from '../types'

type CommonProps = { units: UnionUnit[] }
const today = () => new Date().toISOString().slice(0, 10)
const isOpenCase = (item: BaseRecord) => item.status !== 'CLOSED'
const needsImmediateEscalation = (item: BaseRecord) => isOpenCase(item)
  && (['HIGH', 'CRITICAL'].includes(String(item.severity ?? '')) || Number(item.affectedPeople ?? 0) >= 10)
const reportStatus = (status: unknown) => {
  const value = String(status ?? '')
  if (['CLASSIFYING', 'ASSIGNED', 'IN_PROGRESS'].includes(value)) return 'IN_PROGRESS'
  if (['WAITING_RESPONSE', 'PENDING_APPROVAL'].includes(value)) return 'WAITING_RESPONSE'
  return value
}

export function CasesInsightPage({ units, mode, isAdmin, unitCode }: CommonProps & {
  mode: 'reports' | 'analytics'
  isAdmin: boolean
  unitCode?: string
}) {
  const [unitFilter, setUnitFilter] = useState('')
  const [tracking, setTracking] = useState('')
  const [search, setSearch] = useState('')
  const [groups, setGroups] = useState<CaseGroupCount[]>([])
  const [groupError, setGroupError] = useState('')

  const filters = useMemo(() => ({
    q: search.trim() || undefined,
    unitId: unitFilter || undefined,
    preset: tracking || undefined,
  }), [search, unitFilter, tracking])

  const list = usePagedList<BaseRecord>({ endpoint: '/cases', filters })
  const metrics = list.facets.metrics

  // The analysis bars group over the whole filtered set, so they come from a dedicated rollup
  // endpoint rather than whatever rows happen to be on the current page.
  const query = JSON.stringify(filters)
  useEffect(() => {
    if (mode !== 'analytics') return
    const params = new URLSearchParams()
    Object.entries(JSON.parse(query) as Record<string, string | undefined>).forEach(([key, value]) => {
      if (value) params.set(key, value)
    })
    api<CaseGroupCount[]>(`/cases/issue-groups${params.size ? `?${params}` : ''}`)
      .then(result => { setGroups(result); setGroupError('') })
      .catch(err => setGroupError(err instanceof Error ? err.message : 'Không thể tổng hợp nhóm vấn đề'))
  }, [mode, query])

  const filtersActive = Boolean(search || unitFilter || tracking)
  const error = list.error || groupError
  const maxGroup = Math.max(...groups.map(group => group.count), 1)

  const filterBar = <TableFilterBar>
    {isAdmin
      ? <FilterField label="CĐCS"><select aria-label="Lọc theo CĐCS" value={unitFilter} onChange={event => setUnitFilter(event.target.value)}><option value="">Tất cả</option>{units.map(unit => <option key={unit.id} value={unit.id}>{unit.code}</option>)}</select></FilterField>
      : <FilterField label="CĐCS"><strong>{unitCode ?? 'Đơn vị của tài khoản'}</strong></FilterField>}
    <FilterField label="Theo dõi"><select aria-label="Lọc nhanh nghiệp vụ" value={tracking} onChange={event => setTracking(event.target.value)}><option value="">Tất cả</option><option value="due24">Đến hạn 24h</option><option value="overdue">Quá hạn</option><option value="repeated">Vụ việc lặp lại</option><option value="many">Ảnh hưởng nhiều NLĐ</option></select></FilterField>
    <FilterField label="Tìm kiếm" search><input value={search} onChange={event => setSearch(event.target.value)} placeholder="Mã / nhóm vấn đề / PIC…" /></FilterField>
  </TableFilterBar>

  return <section className="page-section">
    <div className="page-heading"><div><p className="eyebrow">Vụ việc / Dashboard con</p><h1>{mode === 'reports' ? 'Báo cáo vụ việc' : 'Phân tích vụ việc'}</h1><p>{mode === 'reports' ? 'Bảng theo dõi tiến độ đã ẩn tên người lao động; mỗi dòng dùng mã vụ việc duy nhất.' : 'Nhận diện nhóm vấn đề lặp lại, mức ảnh hưởng và điểm nghẽn quá hạn.'}</p></div></div>
    {error && <div className="alert alert--danger">{error}</div>}

    {Number(metrics.urgentEscalation ?? 0) > 0 && <div className="alert alert--danger case-escalation-alert">
      <strong>Báo ngay CĐ GPG / Ban CSNLĐ</strong>
      <span>{metrics.urgentEscalation} vụ việc đang mở ảnh hưởng nhiều NLĐ hoặc có rủi ro cao. Không chờ báo cáo tháng.</span>
    </div>}

    <div className="metric-grid metric-grid--compact insight-metrics">
      <article className="metric-card metric-card--blue"><span>Đến hạn 24h</span><strong>{metrics.due24 ?? 0}</strong><small>Cần ưu tiên phản hồi</small></article>
      <article className="metric-card metric-card--orange"><span>Quá hạn</span><strong>{metrics.overdue ?? 0}</strong><small>Cần lý do và ETA mới</small></article>
      <article className="metric-card metric-card--teal"><span>Lặp lại</span><strong>{metrics.repeated ?? 0}</strong><small>Cùng nhóm vấn đề</small></article>
      <article className="metric-card metric-card--green"><span>Ảnh hưởng nhiều NLĐ</span><strong>{metrics.wideImpact ?? 0}</strong><small>Từ 10 người trở lên</small></article>
    </div>

    {mode === 'reports' ? <>
      <div className="workflow-stepper workflow-stepper--case">{['Mới', 'Đang xác minh', 'Đang xử lý', 'Chờ phản hồi', 'Đã đóng'].map((step, index) => <div key={step}><span>{index + 1}</span><strong>{step}</strong></div>)}</div>
      <ListCard
        list={list}
        title={`${list.total} vụ việc`}
        subtitle={list.total === list.facets.total ? undefined : `Trên tổng ${list.facets.total}`}
        actions={<>
          {filtersActive && <button className="button button--ghost" onClick={() => { setSearch(''); setUnitFilter(''); setTracking('') }}>Xóa lọc</button>}
          <button className="button button--ghost" onClick={() => void list.reload()}>Làm mới</button>
        </>}
        filters={filterBar}
      >
        <div className="table-wrap case-report-table"><table><thead><tr><th>Mã</th><th>Ngày nhận</th><th>Đơn vị</th><th>Nhóm vấn đề</th><th>Mức độ</th><th>PIC</th><th>Deadline</th><th>Trạng thái</th><th>Kết quả / phản hồi</th></tr></thead><tbody>
          {list.loading && <tr><td colSpan={9} className="empty-cell">Đang tải dữ liệu…</td></tr>}
          {!list.loading && !list.rows.length && <tr><td colSpan={9} className="empty-cell">{filtersActive ? 'Không có vụ việc phù hợp bộ lọc.' : 'Chưa có vụ việc.'}</td></tr>}
          {!list.loading && list.rows.map(item => {
            const overdue = isOpenCase(item) && String(item.deadline ?? '') < today()
            const escalation = needsImmediateEscalation(item)
            return <tr key={item.id} className={escalation ? 'case-report-row--urgent' : undefined}>
              <td><strong>{String(item.caseCode)}</strong></td>
              <td>{formatDate(item.receivedDate)}</td>
              <td>{item.unionUnit?.code ?? '—'}</td>
              <td>{String(item.issueGroup ?? '—')}<small className="table-subtext">{Number(item.affectedPeople ?? 0)} NLĐ ảnh hưởng</small></td>
              <td><StatusBadge value={item.severity} />{escalation && <small className="table-subtext table-subtext--danger">Báo ngay CĐ GPG / Ban CSNLĐ</small>}</td>
              <td>{String(item.ownerName ?? '—')}</td>
              <td>{formatDate(item.deadline)}{overdue && <small className={item.overdueReason ? 'table-subtext' : 'table-subtext table-subtext--danger'}>{String(item.overdueReason ?? 'Thiếu lý do / ETA mới')}</small>}</td>
              <td><StatusBadge value={reportStatus(item.status)} /></td>
              <td>{String(item.resultText ?? '—')}</td>
            </tr>
          })}
        </tbody></table></div>
      </ListCard>
    </> : <>
      {filterBar}
      <div className="dashboard-grid insight-grid">
        <article className="panel data-card">
          <div className="panel__heading"><div><p className="eyebrow">Nhóm vấn đề</p><h2>Tần suất và mức ảnh hưởng</h2></div></div>
          {groups.length === 0
            ? <div className="empty-state">Chưa có nhóm vấn đề phù hợp bộ lọc.</div>
            : <div className="analysis-bars">{groups.map(group => <div key={group.issueGroup}>
                <div><strong>{group.issueGroup}</strong><span>{group.count} vụ · {group.affectedPeople} NLĐ</span></div>
                <div className="progress-line"><div style={{ width: `${Math.max(8, group.count / maxGroup * 100)}%` }} /></div>
                <small>{group.overdue} vụ quá hạn</small>
              </div>)}</div>}
        </article>
        <article className="panel data-card">
          <div className="panel__heading"><div><p className="eyebrow">Cảnh báo phân tích</p><h2>Điểm cần can thiệp</h2></div></div>
          <div className="control-checklist">
            <div><span>!</span><strong>Vụ việc lặp lại</strong><small>{metrics.repeated ?? 0} hồ sơ thuộc nhóm xuất hiện từ 2 lần</small></div>
            <div><span>!</span><strong>Quá hạn xử lý</strong><small>{metrics.overdue ?? 0} hồ sơ chưa đóng đã quá deadline</small></div>
            <div><span>!</span><strong>Ảnh hưởng diện rộng</strong><small>{metrics.wideImpact ?? 0} hồ sơ ảnh hưởng từ 10 NLĐ</small></div>
          </div>
        </article>
      </div>
    </>}
  </section>
}

export function WelfareInsightPage({ units, mode }: CommonProps & { mode: 'policies' | 'documents' }) {
  const [unitFilter, setUnitFilter] = useState('')
  const [tracking, setTracking] = useState('')
  const [search, setSearch] = useState('')

  const filters = useMemo(() => ({
    q: search.trim() || undefined,
    unitId: unitFilter || undefined,
    preset: tracking || undefined,
  }), [search, unitFilter, tracking])

  const list = usePagedList<BaseRecord>({ endpoint: '/welfare', filters })
  const filtersActive = Boolean(search || unitFilter || tracking)

  const checks = (item: BaseRecord) => [
    { label: 'Đúng đối tượng', ok: Boolean(item.beneficiaryName) },
    { label: 'Đúng định mức', ok: !item.standardAmount || Number(item.amount) <= Number(item.standardAmount) },
    { label: 'Đúng hạn', ok: !item.deadline || String(item.deadline) >= today() || item.status === 'COMPLETED' },
    { label: 'Đủ chứng từ', ok: item.documentStatus === 'COMPLETE' || item.documentStatus === 'NOT_REQUIRED' },
    { label: 'Có hình ảnh/biên nhận', ok: Boolean(item.hasImage) && (item.receiptStatus === 'COMPLETE' || item.receiptStatus === 'NOT_REQUIRED') },
  ]

  return <section className="page-section">
    <div className="page-heading"><div><p className="eyebrow">Chăm lo / Dashboard con</p><h1>{mode === 'policies' ? 'Chính sách chăm lo' : 'Kiểm soát chứng từ'}</h1><p>{mode === 'policies' ? 'Theo dõi yêu cầu mới, ngày đến hạn và mức chi theo từng chính sách.' : 'Kiểm soát đúng đối tượng, đúng định mức, đúng hạn và đủ hồ sơ thanh toán.'}</p></div></div>
    {list.error && <div className="alert alert--danger">{list.error}</div>}

    <ListCard
      list={list}
      unit="hồ sơ"
      title={`${list.total} hồ sơ chăm lo`}
      subtitle={list.total === list.facets.total ? undefined : `Trên tổng ${list.facets.total}`}
      className={mode === 'policies' ? 'list-card--grid' : undefined}
      actions={<>
        {filtersActive && <button className="button button--ghost" onClick={() => { setSearch(''); setUnitFilter(''); setTracking('') }}>Xóa lọc</button>}
        <button className="button button--ghost" onClick={() => void list.reload()}>Làm mới</button>
      </>}
      filters={<TableFilterBar>
        <FilterField label="Công ty"><select aria-label="Lọc theo CĐCS" value={unitFilter} onChange={event => setUnitFilter(event.target.value)}><option value="">Tất cả</option>{units.map(unit => <option key={unit.id} value={unit.id}>{unit.code}</option>)}</select></FilterField>
        <FilterField label="Theo dõi"><select aria-label="Lọc nhanh nghiệp vụ" value={tracking} onChange={event => setTracking(event.target.value)}><option value="">Tất cả</option><option value="due">Đến hạn</option><option value="new">Yêu cầu mới</option></select></FilterField>
        <FilterField label="Tìm kiếm" search><input value={search} onChange={event => setSearch(event.target.value)} placeholder="Mã / người thụ hưởng…" /></FilterField>
      </TableFilterBar>}
    >
      {list.loading
        ? <div className="empty-state">Đang tải dữ liệu…</div>
        : !list.rows.length
          ? <div className="empty-state">{filtersActive ? 'Không có hồ sơ phù hợp bộ lọc.' : 'Chưa có hồ sơ chăm lo.'}</div>
          : mode === 'policies'
            ? <div className="policy-grid">{list.rows.map(item => <article className="data-card" key={item.id}>
                <div className="policy-card__head"><div><span>{enumLabel(item.welfareType)}</span><strong>{String(item.beneficiaryName)}</strong><small>{item.unionUnit?.code} · {String(item.recordCode)}</small></div><StatusBadge value={item.status} /></div>
                <dl>
                  <div><dt>Chính sách</dt><dd>{String(item.policyName ?? 'Chưa cập nhật')}</dd></div>
                  <div><dt>Mức đề xuất</dt><dd>{formatMoney(item.amount as number)}</dd></div>
                  <div><dt>Định mức</dt><dd>{item.standardAmount ? formatMoney(item.standardAmount as number) : 'Chưa thiết lập'}</dd></div>
                  <div><dt>Đến hạn</dt><dd>{formatDate(item.deadline ?? item.eventDate)}</dd></div>
                </dl>
              </article>)}</div>
            : <div className="table-wrap"><table><thead><tr><th>Hồ sơ</th><th>Đối tượng / định mức</th><th>Deadline</th><th>Chứng từ</th><th>Kết quả kiểm soát</th></tr></thead><tbody>
                {list.rows.map(item => {
                  const results = checks(item)
                  const passed = results.filter(check => check.ok).length
                  return <tr key={item.id}>
                    <td><strong>{String(item.recordCode)}</strong><small className="table-subtext">{enumLabel(item.welfareType)} · {String(item.beneficiaryName)}</small></td>
                    <td>{formatMoney(item.amount as number)}<small className="table-subtext">Định mức: {item.standardAmount ? formatMoney(item.standardAmount as number) : 'chưa có'}</small></td>
                    <td>{formatDate(item.deadline ?? item.eventDate)}</td>
                    <td><StatusBadge value={item.documentStatus} /><small className="table-subtext">Biên nhận: {enumLabel(item.receiptStatus)}</small></td>
                    <td><b className={passed === results.length ? 'compliance-badge' : 'compliance-badge compliance-badge--missing'}>{passed}/{results.length} đạt</b><small className="table-subtext">{results.filter(check => !check.ok).map(check => check.label).join(' · ') || 'Đủ điều kiện'}</small></td>
                  </tr>
                })}
              </tbody></table></div>}
    </ListCard>
  </section>
}
