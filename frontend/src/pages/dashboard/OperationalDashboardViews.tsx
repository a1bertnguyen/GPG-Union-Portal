import { enumLabel, formatDate } from '../../api'
import type { BaseRecord, DashboardSummary } from '../../types'
import { AlertList, BreakdownList, DashboardTable, KpiLine, MetricGrid, PanelHeading, RecordList } from './DashboardWidgets'
import { grouped } from './dashboardAggregates'

export function ExecutiveDashboard({ data, month }: { data: DashboardSummary; month: string }) {
  const reportCompletion = data.unitCount ? Math.round((data.unitCount - data.pendingReportCount) * 100 / data.unitCount) : 100
  return <div id="dashboard-executive">
    <MetricGrid cards={[
      ['CĐCS đang theo dõi', data.unitCount, 'Phạm vi tài khoản hiện tại', 'blue'],
      ['Đoàn viên', data.unionMemberCount, `${data.activeMemberCount} NLĐ đang hoạt động`, 'teal'],
      ['Chăm lo hoàn tất', `${data.welfareCompletionRate}%`, 'Mục tiêu từ 95%', 'green'],
      ['Kiến nghị đang mở', data.openCaseCount, `${data.overdueCaseCount} kiến nghị quá hạn`, 'orange'],
    ]} />
    <div className="task-strip" id="executive-priorities">
      <div><strong>{data.overdueCaseCount}</strong><span>Kiến nghị quá hạn</span><small>Cần PIC và ETA mới</small></div>
      <div><strong>{data.pendingReportCount}</strong><span>Báo cáo chưa nộp</span><small>Kỳ {month}</small></div>
      <div><strong>{reportCompletion}%</strong><span>Đơn vị đã báo cáo</span><small>Mục tiêu 100%</small></div>
    </div>
    <div className="dashboard-grid">
      <article className="panel">
        <PanelHeading eyebrow="Tiến độ hệ thống" title="Mức độ hoàn thành" />
        <KpiLine label="Chăm lo hoàn tất" value={data.welfareCompletionRate} tone="green" />
        <KpiLine label="Đơn vị đã nộp báo cáo" value={reportCompletion} tone="blue" />
        <KpiLine label="Kiến nghị trong hạn" value={data.openCaseCount ? 100 - Math.round(data.overdueCaseCount * 100 / data.openCaseCount) : 100} tone="teal" />
      </article>
      <article className="panel">
        <PanelHeading eyebrow="Điểm cần quyết định" title="Ưu tiên điều hành" count={data.alerts.length} />
        <AlertList alerts={data.alerts} empty="Không có vấn đề vượt ngưỡng trong kỳ." />
      </article>
    </div>
  </div>
}

export function WelfareDashboard({ records, month }: { records: BaseRecord[]; month: string }) {
  const monthly = records.filter(item => String(item.eventDate ?? '').startsWith(month))
  const completed = monthly.filter(item => item.status === 'COMPLETED').length
  const incompleteDocuments = monthly.filter(item => item.documentStatus === 'INCOMPLETE').length
  const active = monthly.filter(item => !['COMPLETED', 'CANCELLED'].includes(String(item.status ?? '')))
  const completion = monthly.length ? Math.round(completed * 100 / monthly.length) : 100
  return <div id="dashboard-welfare">
    <MetricGrid cards={[
      ['Hồ sơ trong kỳ', monthly.length, `Kỳ ${month}`, 'blue'],
      ['Đã hoàn tất', `${completion}%`, `${completed} hồ sơ`, 'green'],
      ['Đang xử lý', active.length, 'Cần theo dõi đến khi đóng', 'teal'],
      ['Thiếu chứng từ', incompleteDocuments, 'Cần bổ sung hồ sơ', 'orange'],
    ]} />
    <div className="dashboard-grid">
      <article className="panel" id="welfare-breakdown"><PanelHeading eyebrow="Cơ cấu chăm lo" title="Hồ sơ theo nhóm" /><BreakdownList rows={grouped(monthly, 'welfareType')} /></article>
      <article className="panel"><PanelHeading eyebrow="Danh sách ưu tiên" title="Hồ sơ chưa hoàn tất" count={active.length} /><RecordList records={active.slice(0, 8)} primary="beneficiaryName" secondary="recordCode" meta="eventDate" empty="Không còn hồ sơ đang xử lý." /></article>
    </div>
  </div>
}

export function CasesDashboard({ records, month }: { records: BaseRecord[]; month: string }) {
  const today = new Date().toISOString().slice(0, 10)
  const tomorrowDate = new Date(); tomorrowDate.setDate(tomorrowDate.getDate() + 1)
  const tomorrow = tomorrowDate.toISOString().slice(0, 10)
  const monthly = records.filter(item => String(item.receivedDate ?? '').startsWith(month))
  const monthlyOpen = monthly.filter(item => item.status !== 'CLOSED')
  const open = records.filter(item => item.status !== 'CLOSED')
  const due24 = open.filter(item => String(item.deadline ?? '') >= today && String(item.deadline ?? '') <= tomorrow)
  const overdue = open.filter(item => String(item.deadline ?? '') < today)
  const high = open.filter(item => ['HIGH', 'CRITICAL'].includes(String(item.severity ?? '')))
  const manyAffected = open.filter(item => Number(item.affectedPeople ?? 0) >= 10)
  const urgentEscalation = open.filter(item => Number(item.affectedPeople ?? 0) >= 10
    || ['HIGH', 'CRITICAL'].includes(String(item.severity ?? '')))
  const groupCounts = new Map<string, number>()
  open.forEach(item => groupCounts.set(String(item.issueGroup ?? ''), (groupCounts.get(String(item.issueGroup ?? '')) ?? 0) + 1))
  const repeated = open.filter(item => (groupCounts.get(String(item.issueGroup ?? '')) ?? 0) > 1)
  const responded = monthly.filter(item => Boolean(item.resultText)).length
  const responseRate = monthly.length ? Math.round(responded * 100 / monthly.length) : 100
  const priorities = [...open].sort((left, right) => String(left.deadline ?? '').localeCompare(String(right.deadline ?? ''))).slice(0, 10)
  const publicStatus = (status: unknown) => {
    const value = String(status ?? '')
    if (['CLASSIFYING', 'ASSIGNED', 'IN_PROGRESS'].includes(value)) return 'IN_PROGRESS'
    if (['WAITING_RESPONSE', 'PENDING_APPROVAL'].includes(value)) return 'WAITING_RESPONSE'
    return value
  }
  return <div id="dashboard-cases">
    {urgentEscalation.length > 0 && <div className="alert alert--danger case-escalation-alert">
      <strong>Báo ngay CĐ GPG / Ban CSNLĐ</strong>
      <span>{urgentEscalation.length} kiến nghị đang mở ảnh hưởng nhiều NLĐ hoặc có rủi ro cao. Không chờ báo cáo tháng.</span>
    </div>}
    <div className="today-alert-grid today-alert-grid--static">
      <article><span>Đến hạn 24h</span><strong>{due24.length}</strong><small>Cần ưu tiên phản hồi</small></article>
      <article className="today-alert--danger"><span>Quá hạn</span><strong>{overdue.length}</strong><small>Cần lý do và ETA mới</small></article>
      <article><span>Kiến nghị lặp lại</span><strong>{repeated.length}</strong><small>Theo nhóm vấn đề</small></article>
      <article className="today-alert--orange"><span>Ảnh hưởng nhiều NLĐ</span><strong>{manyAffected.length}</strong><small>Từ 10 người trở lên</small></article>
    </div>
    <MetricGrid cards={[
      ['Kiến nghị trong kỳ', monthly.length, `Tiếp nhận trong ${month}`, 'blue'],
      ['Đang mở trong kỳ', monthlyOpen.length, 'Chưa ở trạng thái đóng', 'teal'],
      ['Quá hạn', overdue.length, 'Cần cập nhật nguyên nhân và ETA', 'orange'],
      ['Có kết quả phản hồi', `${responseRate}%`, `${responded} hồ sơ có kết quả`, 'green'],
    ]} />
    <div className="dashboard-grid">
      <article className="panel"><PanelHeading eyebrow="Phân loại rủi ro" title="Mức độ kiến nghị" /><BreakdownList rows={grouped(monthly, 'severity')} /></article>
      <article className="panel"><PanelHeading eyebrow="SLA & trách nhiệm" title="Tình trạng xử lý" /><KpiLine label="Có kết quả xử lý" value={responseRate} tone="green" /><KpiLine label="Kiến nghị trong hạn" value={open.length ? 100 - Math.round(overdue.length * 100 / open.length) : 100} tone="blue" /><KpiLine label="Không thuộc mức cao/nghiêm trọng" value={open.length ? 100 - Math.round(high.length * 100 / open.length) : 100} tone="teal" /></article>
    </div>
    <DashboardTable id="case-priorities" title="Kiến nghị cần ưu tiên" columns={['Mã', 'Nhóm vấn đề', 'Mức độ', 'PIC', 'Deadline', 'Trạng thái']} rows={priorities.map(item => [String(item.caseCode ?? '—'), String(item.issueGroup ?? '—'), enumLabel(item.severity), String(item.ownerName ?? '—'), formatDate(item.deadline), enumLabel(publicStatus(item.status))])} />
  </div>
}
