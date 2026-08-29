import { useCallback, useEffect, useMemo, useState } from 'react'
import { api, apiAll, enumLabel, formatDate } from '../api'
import type { PageKey } from '../components/sidebar/navigation'
import type { BaseRecord } from '../types'

type Props = { unitName?: string; isAdmin: boolean; onNavigate: (page: PageKey) => void }

const iso = (date: Date) => date.toISOString().slice(0, 10)
const isOpen = (item: BaseRecord) => !['CLOSED', 'COMPLETED', 'CANCELLED'].includes(String(item.status ?? ''))

export default function HomeDashboardPage({ unitName, isAdmin, onNavigate }: Props) {
  const [cases, setCases] = useState<BaseRecord[]>([])
  const [welfare, setWelfare] = useState<BaseRecord[]>([])
  const [activities, setActivities] = useState<BaseRecord[]>([])
  const [reports, setReports] = useState<BaseRecord[]>([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    try {
      // The "today" panel scans whole lists for due dates, so it needs complete sets rather than pages.
      // `/reports` is not a paginated list endpoint and still returns a plain array.
      const [caseData, welfareData, activityData, reportData] = await Promise.all([
        apiAll<BaseRecord>('/cases'), apiAll<BaseRecord>('/welfare'), apiAll<BaseRecord>('/activities'),
        api<BaseRecord[]>('/reports'),
      ])
      setCases(caseData); setWelfare(welfareData); setActivities(activityData); setReports(reportData); setError('')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tải công việc hôm nay')
    } finally {
      setLoading(false)
    }
  }, [])

  // Remote data loading is the synchronization intentionally performed by this effect.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { void load() }, [load])

  const summary = useMemo(() => {
    const today = iso(new Date())
    const tomorrowDate = new Date(); tomorrowDate.setDate(tomorrowDate.getDate() + 1)
    const tomorrow = iso(tomorrowDate)
    const nextWeekDate = new Date(); nextWeekDate.setDate(nextWeekDate.getDate() + 7)
    const nextWeek = iso(nextWeekDate)
    const openCases = cases.filter(isOpen)
    const due24 = openCases.filter(item => String(item.deadline ?? '') >= today && String(item.deadline ?? '') <= tomorrow)
    const overdue = openCases.filter(item => String(item.deadline ?? '') < today)
    const groupCounts = new Map<string, number>()
    openCases.forEach(item => groupCounts.set(String(item.issueGroup ?? ''), (groupCounts.get(String(item.issueGroup ?? '')) ?? 0) + 1))
    const repeated = openCases.filter(item => (groupCounts.get(String(item.issueGroup ?? '')) ?? 0) > 1)
    const manyAffected = openCases.filter(item => Number(item.affectedPeople ?? 0) >= 10)
    const urgentEscalation = openCases.filter(item => Number(item.affectedPeople ?? 0) >= 10
      || ['HIGH', 'CRITICAL'].includes(String(item.severity ?? '')))
    const birthday = welfare.filter(item => item.welfareType === 'BIRTHDAY' && isOpen(item)
      && String(item.eventDate ?? '') >= today && String(item.eventDate ?? '') <= nextWeek)
    const incompleteCare = welfare.filter(item => ['FUNERAL', 'WEDDING'].includes(String(item.welfareType ?? ''))
      && (item.documentStatus === 'INCOMPLETE' || item.receiptStatus === 'INCOMPLETE'))
    const unsettled = welfare.filter(item => isOpen(item) && item.receiptStatus === 'INCOMPLETE')
    const runningActivities = activities.filter(item => item.status === 'IN_PROGRESS')
    const followUps = activities.filter(item => item.followUpDeadline && String(item.followUpDeadline) <= today && !item.reportCompleted)
    const pendingReports = reports.filter(item => item.status === 'DRAFT')
    return { due24, overdue, repeated, manyAffected, urgentEscalation, birthday, incompleteCare, unsettled, runningActivities, followUps, pendingReports }
  }, [activities, cases, reports, welfare])

  const tasks = [
    ...summary.urgentEscalation.map(item => ({ tone: 'danger', title: `${item.caseCode} · cần báo ngay`, detail: 'CĐ GPG / Ban CSNLĐ · không chờ báo cáo tháng', page: 'caseReports' as PageKey })),
    ...summary.overdue.filter(item => !summary.urgentEscalation.includes(item)).map(item => ({ tone: 'danger', title: `${item.caseCode} · ${item.issueGroup}`, detail: `Quá hạn từ ${formatDate(item.deadline)} · PIC ${item.ownerName}`, page: 'cases' as PageKey })),
    ...summary.due24.map(item => ({ tone: 'warning', title: `${item.caseCode} · đến hạn trong 24h`, detail: `${item.issueGroup} · ${Number(item.affectedPeople ?? 0)} NLĐ ảnh hưởng`, page: 'cases' as PageKey })),
    ...summary.birthday.map(item => ({ tone: 'info', title: `Sinh nhật · ${item.beneficiaryName}`, detail: `Ngày ${formatDate(item.eventDate)} · cần chuẩn bị trước 7 ngày`, page: 'welfare' as PageKey })),
    ...summary.incompleteCare.map(item => ({ tone: 'warning', title: `${enumLabel(item.welfareType)} · ${item.beneficiaryName}`, detail: 'Hồ sơ/biên nhận chưa hoàn tất', page: 'welfareDocuments' as PageKey })),
    ...summary.followUps.map(item => ({ tone: 'info', title: `Follow-up · ${item.name}`, detail: `Hạn ${formatDate(item.followUpDeadline)} · ${item.followUpOwner ?? 'chưa có PIC'}`, page: 'activities' as PageKey })),
  ].slice(0, 10)

  return <section className="page-section home-dashboard">
    <div className="page-heading">
      <div><p className="eyebrow">{isAdmin ? 'Bảng điều hành toàn hệ thống' : 'Bảng điều hành CĐCS'}</p><h1>Công việc cần xử lý hôm nay</h1><p>{isAdmin ? 'Toàn hệ thống' : unitName ?? 'Công đoàn thành viên'} · tập trung các việc đến hạn, cảnh báo và hồ sơ còn thiếu.</p></div>
      <button className="button button--ghost" onClick={() => void load()}>Làm mới</button>
    </div>
    {error && <div className="alert alert--danger">{error}</div>}
    {summary.urgentEscalation.length > 0 && <button className="alert alert--danger case-escalation-alert case-escalation-alert--button" onClick={() => onNavigate('caseReports')}>
      <strong>Báo ngay CĐ GPG / Ban CSNLĐ</strong>
      <span>{summary.urgentEscalation.length} vụ việc đang mở ảnh hưởng nhiều NLĐ hoặc có rủi ro cao. Không chờ báo cáo tháng.</span>
    </button>}
    <div className="today-alert-grid">
      <button onClick={() => onNavigate('cases')}><span>Đến hạn 24h</span><strong>{summary.due24.length}</strong><small>Cần ưu tiên phản hồi</small></button>
      <button className="today-alert--danger" onClick={() => onNavigate('cases')}><span>Quá hạn</span><strong>{summary.overdue.length}</strong><small>Cần cập nhật lý do và ETA</small></button>
      <button onClick={() => onNavigate('caseAnalytics')}><span>Vụ việc lặp lại</span><strong>{summary.repeated.length}</strong><small>Theo nhóm vấn đề</small></button>
      <button className="today-alert--orange" onClick={() => onNavigate('caseAnalytics')}><span>Ảnh hưởng nhiều NLĐ</span><strong>{summary.manyAffected.length}</strong><small>Từ 10 người trở lên</small></button>
    </div>
    <div className="dashboard-grid dashboard-grid--today">
      <article className="panel data-card">
        <div className="panel__heading"><div><p className="eyebrow">Ưu tiên hôm nay</p><h2>Danh sách cần xử lý</h2></div><span className="tag">{tasks.length} việc</span></div>
        {loading ? <div className="empty-state">Đang tải dữ liệu…</div> : tasks.length ? <div className="today-task-list">
          {tasks.map((task, index) => <button key={`${task.title}-${index}`} onClick={() => onNavigate(task.page)} className={`today-task today-task--${task.tone}`}>
            <i /><span><strong>{task.title}</strong><small>{task.detail}</small></span><b>›</b>
          </button>)}
        </div> : <div className="empty-state">Không có công việc khẩn cần xử lý hôm nay.</div>}
      </article>
      <aside className="today-side-stack">
        <article className="panel data-card"><div className="panel__heading"><div><p className="eyebrow">Chăm lo</p><h2>Nhắc việc tự động</h2></div></div>
          <div className="today-mini-list"><button onClick={() => onNavigate('welfarePolicies')}><span>Sinh nhật trong 7 ngày</span><strong>{summary.birthday.length}</strong></button><button onClick={() => onNavigate('welfareDocuments')}><span>Hiếu hỷ thiếu hồ sơ</span><strong>{summary.incompleteCare.length}</strong></button><button onClick={() => onNavigate('welfareDocuments')}><span>Chưa quyết toán</span><strong>{summary.unsettled.length}</strong></button></div>
        </article>
        <article className="panel data-card"><div className="panel__heading"><div><p className="eyebrow">Hoạt động & báo cáo</p><h2>Đang theo dõi</h2></div></div>
          <div className="today-mini-list"><button onClick={() => onNavigate('activities')}><span>Đang triển khai</span><strong>{summary.runningActivities.length}</strong></button><button onClick={() => onNavigate('activities')}><span>Follow-up đến hạn</span><strong>{summary.followUps.length}</strong></button><button onClick={() => onNavigate('reports')}><span>Báo cáo bản nháp</span><strong>{summary.pendingReports.length}</strong></button></div>
        </article>
      </aside>
    </div>
  </section>
}
