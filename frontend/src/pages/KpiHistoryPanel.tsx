import { useEffect, useState } from 'react'
import { api, apiAll } from '../api'
import type { KpiUnitResultView } from '../kpiModel'
import './KpiHistoryPanel.css'

type Person = { memberId: number; employeeCode: string; fullName: string; unionMember: boolean }
type Population = { id: number; year: number; revision: number; status: string; reconciliationNote: string; members: Person[] }
type Source = { module: string; id: string; fields: Record<string, string | null> }
type Statistic = { groupCode: string; code: string; label: string; dimensionType: string; dimensionKey: string; numerator: number | null; denominator: number | null; measure: string; status: string; numeratorIds: string[]; denominatorIds: string[]; excludedIds: string[] }
type Activity = { activeEmployeeCount: number | null; activeUnionMemberCount: number | null; statistics: Statistic[]; sources: Source[]; blockers: string[] }
type Run = { id: number; unionUnitId: number; unitNameSnapshot: string | null; periodStart: string; versionId: string; revision: number; runStatus: string; finalScore: number; calculatedAt: string; calculatedBy: string }
type RunDetail = { run: Run; result: KpiUnitResultView; activity: Activity | null }
type Readiness = { unitId: number; ready: boolean; blockers: string[] }
const groups = { GOV: 'Tổ chức', DATA: 'Nhân sự', REP: 'Báo cáo', CARE: 'Chăm lo', GRV: 'Kiến nghị', ACT: 'Hoạt động', FIN: 'Tài chính' }
const typeNames: Record<string, string> = { BIRTHDAY: 'Sinh nhật', FUNERAL: 'Hiếu', WEDDING: 'Hỷ', VISIT: 'Thăm hỏi', CHILDBIRTH: 'Sinh con', HARDSHIP: 'Khó khăn', INCOME: 'Thu', EXPENSE: 'Chi', ADVANCE: 'Tạm ứng' }
const number = (n: number | null) => n === null ? 'Chưa có dữ liệu' : n.toLocaleString('vi-VN', { maximumFractionDigits: 2 })
export function KpiHistoryPanel({ year, unitId, isAdmin, onChanged }: { year: number; unitId?: number; isAdmin: boolean; onChanged: () => void }) {
  const [tab, setTab] = useState<'live' | 'history' | 'population'>('live')
  const [group, setGroup] = useState('CARE')
  const [dimension, setDimension] = useState('YEAR')
  const [activity, setActivity] = useState<Activity | null>(null)
  const [runs, setRuns] = useState<Run[]>([])
  const [populations, setPopulations] = useState<Population[]>([])
  const [people, setPeople] = useState<Person[]>([])
  const [readiness, setReadiness] = useState<Readiness[]>([])
  const [selected, setSelected] = useState<RunDetail | null>(null)
  const [detail, setDetail] = useState<Statistic | null>(null)
  const [allRevisions, setAllRevisions] = useState(false)
  const [loading, setLoading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [note, setNote] = useState('')
  const [ids, setIds] = useState('')
  const [reload, setReload] = useState(0)
  const [recordId, setRecordId] = useState('')
  const [memberId, setMemberId] = useState('')
  const [reason, setReason] = useState('')
  const [cancelSource, setCancelSource] = useState('')

  useEffect(() => {
    if (!unitId) return
    const controller = new AbortController()
    queueMicrotask(() => { if (!controller.signal.aborted) { setLoading(true); setError(''); setSelected(null); setDetail(null) } })
    const options = { signal: controller.signal }
    Promise.all([
      api<Activity>(`/kpi/statistics?year=${year}&unitId=${unitId}`, options),
      api<Run[]>(`/kpi/history?fromYear=${Math.max(2000, year - 5)}&toYear=${year}&unitId=${unitId}&includeSuperseded=${allRevisions}`, options),
      api<Population[]>(`/kpi/populations?year=${year}&unitId=${unitId}`, options),
      api<Readiness[]>(`/kpi/readiness?year=${year}&unitId=${unitId}`, options),
      apiAll<{ id: number; employeeCode: string; fullName: string; membershipStatus: string }>('/members', { unitId }, options),
    ]).then(([a, r, p, ready, employees]) => {
      if (controller.signal.aborted) return
      setActivity(a); setRuns(r); setPopulations(p); setReadiness(ready)
      setPeople(employees.map(e => ({ memberId: e.id, employeeCode: e.employeeCode, fullName: e.fullName, unionMember: e.membershipStatus === 'MEMBER' })))
    }).catch(e => { if (!controller.signal.aborted) { setActivity(null); setError(e instanceof Error ? e.message : 'Không tải được lịch sử') } })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [year, unitId, reload, allRevisions])

  async function mutate(path: string, method: string, body?: unknown) {
    setBusy(true); setError(''); setMessage('')
    try {
      await api(path, { method, ...(body === undefined ? {} : { body: JSON.stringify(body) }) })
      setMessage('Đã lưu. Số liệu đang được tải lại.'); setReload(n => n + 1); onChanged()
    } catch (e) { setError(e instanceof Error ? e.message : 'Không thể lưu') }
    finally { setBusy(false) }
  }
  async function openRun(id: number) {
    setBusy(true); setError(''); setDetail(null)
    try { setSelected(await api<RunDetail>(`/kpi/runs/${id}`)) }
    catch (e) { setError(e instanceof Error ? e.message : 'Không thể mở bản chốt') }
    finally { setBusy(false) }
  }
  const view = tab === 'history' ? selected?.activity : activity
  const rows = view?.statistics.filter(s => s.groupCode === group && s.dimensionType === dimension) ?? []
  const sources = view?.sources ?? []
  const birthdayRows = activity?.sources.filter(s => s.module === 'CHAM_SOC_NLD' && s.fields.welfare_type === 'BIRTHDAY') ?? []
  const cancelled = activity?.sources.filter(s => s.fields.status === 'CANCELLED') ?? []
  const first = populations[0]
  if (!unitId) return <p>Chọn một CĐCS để xem lịch sử hoạt động.</p>
  return <section className="kpi-history-panel" aria-label="Lịch sử hoạt động và chốt KPI">
    <h2>Ghi nhận hoạt động · KPI năm {year}</h2>
    <p>Thống kê theo CĐCS. Số liệu trực tiếp là dự thảo; chỉ bản chốt năm mới là lịch sử chính thức.</p>
    <div className="kpi-history-actions" role="tablist" aria-label="Chế độ xem">
      {([['live', 'Thống kê hoạt động'], ['history', 'Lịch sử đã chốt'], ['population', 'Đối soát nhân sự']] as const).map(([key, title]) =>
        <button key={key} role="tab" aria-selected={tab === key} onClick={() => { setTab(key); setDetail(null) }}>{title}</button>)}
    </div>
    {error && <p role="alert">{error}</p>}
    {message && <p role="status">{message}</p>}
    {loading ? <p role="status">Đang tải số liệu…</p> : <>
      {tab === 'live' && <details>
        <summary>Điều kiện chốt năm: {readiness[0]?.ready ? 'Đã đủ điều kiện' : 'Cần bổ sung dữ liệu'}</summary>
        <ul>{readiness[0]?.blockers.map((b, i) => <li key={i}>{b}</li>)}</ul>
        {isAdmin && <button disabled={busy || year >= new Date().getFullYear()} onClick={() => {
          if (window.confirm(`Chốt KPI năm ${year} cho tất cả CĐCS? Đơn vị thiếu dữ liệu sẽ lưu bản tạm, không xếp hạng chính thức.`)) void mutate(`/kpi/lock?year=${year}`, 'POST')
        }}>Chốt năm cho tất cả CĐCS</button>}
      </details>}
      {tab === 'history' && <>
        <label><input type="checkbox" checked={allRevisions} onChange={e => setAllRevisions(e.target.checked)} /> Hiện tất cả phiên bản, kể cả bản đã thay thế</label>
        <div className="kpi-history-scroll"><table><thead><tr><th>Năm</th><th>Phiên bản</th><th>Lần chốt</th><th>Trạng thái</th><th>Điểm</th><th>Người chốt</th><th /></tr></thead>
          <tbody>{runs.map(r => <tr key={r.id}><td>{r.periodStart.slice(0, 4)}</td><td>{r.versionId}</td><td>{r.revision}</td><td>{r.runStatus}</td><td>{number(r.finalScore)}</td><td>{r.calculatedBy}</td><td><button disabled={busy} onClick={() => void openRun(r.id)}>Xem bản chốt</button></td></tr>)}</tbody></table></div>
        {!runs.length && <p>Chưa có bản chốt trong 6 năm đã chọn.</p>}
        {selected && <p>Bản chốt #{selected.run.id} · {selected.result.unionUnitName} · {selected.result.finalClassification} · {new Date(selected.run.calculatedAt).toLocaleString('vi-VN')}. Dữ liệu bên dưới lấy từ snapshot, không tính lại từ hồ sơ hiện tại.</p>}
      </>}
      {tab === 'population' ? <>
        <p>Mẫu số là toàn bộ người lao động cuối năm, gồm người chưa tham gia công đoàn. Không dùng tổng số hiện tại để tự động xác nhận năm cũ.</p>
        <label>Nguồn và nội dung đối soát<textarea value={note} maxLength={2000} onChange={e => setNote(e.target.value)} placeholder="Ví dụ: danh sách nhân sự ngày 31/12 đã đối chiếu phòng nhân sự" /></label>
        <label>ID nhân sự thuộc danh sách cuối năm (phân cách bằng dấu phẩy; bỏ trống để lấy danh sách ACTIVE hiện tại làm bản nháp)<textarea value={ids} onChange={e => setIds(e.target.value)} /></label>
        <button disabled={busy || !note.trim() || (!!ids.trim() && !/^\d+(\s*,\s*\d+)*$/.test(ids.trim()))} onClick={() => void mutate('/kpi/populations', 'POST', { unitId, year, reconciliationNote: note, memberIds: ids.trim() ? ids.split(',').map(Number) : null })}>Lập bản đối soát mới</button>
        {populations.map(p => <div key={p.id} className="kpi-history-population"><strong>Bản {p.revision} · {p.status} · {p.members.length} nhân sự</strong><p>{p.reconciliationNote}</p>
          {p.status === 'DRAFT' && <button disabled={busy} onClick={() => void mutate(`/kpi/populations/${p.id}/submit`, 'POST')}>Gửi duyệt</button>}
          {p.status === 'SUBMITTED' && isAdmin && <button disabled={busy || year >= new Date().getFullYear()} onClick={() => void mutate(`/kpi/populations/${p.id}/approve`, 'POST')}>Phê duyệt danh sách cuối năm</button>}
          <details><summary>Xem danh sách đã ghi nhận</summary><ul>{p.members.map(p => <li key={p.memberId}>{p.memberId} · {p.employeeCode} · {p.fullName} · {p.unionMember ? 'Đoàn viên' : 'Chưa là đoàn viên'}</li>)}</ul></details>
        </div>)}
        <h3>Liên kết hồ sơ sinh nhật với nhân sự</h3>
        <p>Chọn thủ công; không ghép tự động theo tên người hưởng.</p>
        <label>Hồ sơ<select value={recordId} onChange={e => setRecordId(e.target.value)}><option value="">Chọn hồ sơ</option>{birthdayRows.map(s => <option key={s.id} value={s.id}>{s.fields.record_code} · {s.fields.event_date} · Nhân sự: {s.fields.member_id ?? 'chưa liên kết'}</option>)}</select></label>
        <label>Nhân sự<select value={memberId} onChange={e => setMemberId(e.target.value)}><option value="">Chọn nhân sự</option>{(first?.members ?? people).map(p => <option key={p.memberId} value={p.memberId}>{p.employeeCode} · {p.fullName}</option>)}</select></label>
        <button disabled={busy || !recordId || !memberId} onClick={() => void mutate(`/kpi/welfare/${recordId}/link`, 'PUT', { memberId: Number(memberId) })}>Lưu liên kết</button>
        {!!cancelled.length && <><h3>Đối soát lý do hủy</h3>
          <label>Hồ sơ hủy<select value={cancelSource} onChange={e => setCancelSource(e.target.value)}><option value="">Chọn hồ sơ</option>{cancelled.map(s => <option key={s.module + s.id} value={s.module + ':' + s.id}>{s.module} #{s.id} · {s.fields.cancellation_reason ?? 'Thiếu lý do'}</option>)}</select></label>
          <label>Lý do<input value={reason} maxLength={1000} onChange={e => setReason(e.target.value)} /></label>
          <button disabled={busy || !cancelSource || !reason.trim()} onClick={() => { const [module, id] = cancelSource.split(':'); void mutate(`/kpi/${module === 'HOAT_DONG' ? 'activities' : 'welfare'}/${id}/cancellation`, 'PUT', { reason }) }}>Lưu lý do</button>
        </>}
      </> : view && <>
        <p>Nhân sự cuối năm đã duyệt: {number(view.activeEmployeeCount)} · Đoàn viên: {number(view.activeUnionMemberCount)}</p>
        <div className="kpi-history-actions">
          <label>Nhóm KPI<select value={group} onChange={e => { setGroup(e.target.value); setDetail(null) }}>{Object.entries(groups).map(([key, name]) => <option key={key} value={key}>{name}</option>)}</select></label>
          <label>Chi tiết<select value={dimension} onChange={e => { setDimension(e.target.value); setDetail(null) }}><option value="YEAR">Tổng năm</option><option value="MONTH">12 tháng</option><option value="KPI">Chỉ tiêu tính điểm</option><option value="ISSUE_GROUP">Nhóm kiến nghị</option></select></label>
        </div>
        <div className="kpi-history-scroll"><table><thead><tr><th>Kỳ/nhóm</th><th>Nội dung</th><th>Đã thực hiện / phát sinh</th><th>Tỷ lệ</th><th>Trạng thái</th><th /></tr></thead><tbody>
          {rows.map((s, i) => <tr key={s.code + s.dimensionKey + i}><td>{s.dimensionKey}</td><td>{typeNames[s.label] ?? s.label}</td><td>{number(s.numerator)}{s.denominator !== null ? ' / ' + number(s.denominator) : s.measure === 'VND' ? ' ₫' : ''}</td><td>{s.denominator && s.numerator !== null ? number(s.numerator / s.denominator * 100) + '%' : '—'}</td><td>{s.status}</td><td><button onClick={() => setDetail(s)}>Chứng minh</button></td></tr>)}
        </tbody></table></div>
        {!rows.length && <p>Không có thống kê cho chiều này. Chọn “Chỉ tiêu tính điểm” để xem công thức của nhóm.</p>}
        {detail && <details open><summary>{detail.label}: bản ghi được tính</summary>
          <p>Tử số: {detail.numeratorIds.join(', ') || 'Không có'}<br />Mẫu số: {detail.denominatorIds.join(', ') || 'Không có / hạn chế truy cập'}<br />Đã loại: {detail.excludedIds.join(', ') || 'Không có'}</p>
          {sources.filter(s => [...detail.numeratorIds, ...detail.denominatorIds, ...detail.excludedIds].includes(s.module + ':' + s.id)).map(s => <details key={s.module + s.id}><summary>{s.module} #{s.id}</summary><dl>{Object.entries(s.fields).map(([k, v]) => <div key={k}><dt>{k}</dt><dd>{v ?? '—'}</dd></div>)}</dl></details>)}
        </details>}
      </>}
    </>}
  </section>
}
