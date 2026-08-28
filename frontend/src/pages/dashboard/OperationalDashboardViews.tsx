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
      ['Vụ việc đang mở', data.openCaseCount, `${data.overdueCaseCount} vụ quá hạn`, 'orange'],
    ]} />
    <div className="task-strip" id="executive-priorities">
      <div><strong>{data.overdueCaseCount}</strong><span>Vụ việc quá hạn</span><small>Cần PIC và ETA mới</small></div>
      <div><strong>{data.pendingReportCount}</strong><span>Báo cáo chưa nộp</span><small>Kỳ {month}</small></div>
      <div><strong>{reportCompletion}%</strong><span>Đơn vị đã báo cáo</span><small>Mục tiêu 100%</small></div>
    </div>
    <div className="dashboard-grid">
      <article className="panel">
        <PanelHeading eyebrow="Tiến độ hệ thống" title="Mức độ hoàn thành" />
        <KpiLine label="Chăm lo hoàn tất" value={data.welfareCompletionRate} tone="green" />
        <KpiLine label="Đơn vị đã nộp báo cáo" value={reportCompletion} tone="blue" />
        <KpiLine label="Vụ việc trong hạn" value={data.openCaseCount ? 100 - Math.round(data.overdueCaseCount * 100 / data.openCaseCount) : 100} tone="teal" />
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
  const monthly = records.filter(item => String(item.receivedDate ?? '').startsWith(month))
  const open = monthly.filter(item => item.status !== 'CLOSED')
  const overdue = open.filter(item => String(item.deadline ?? '') < new Date().toISOString().slice(0, 10))
  const high = open.filter(item => ['HIGH', 'CRITICAL'].includes(String(item.severity ?? '')))
  const responded = monthly.filter(item => Boolean(item.resultText)).length
  const responseRate = monthly.length ? Math.round(responded * 100 / monthly.length) : 100
  const priorities = [...open].sort((left, right) => String(left.deadline ?? '').localeCompare(String(right.deadline ?? ''))).slice(0, 10)
  return <div id="dashboard-cases">
    <MetricGrid cards={[
      ['Vụ việc trong kỳ', monthly.length, `Tiếp nhận trong ${month}`, 'blue'],
      ['Đang mở', open.length, 'Chưa ở trạng thái đóng', 'teal'],
      ['Quá hạn', overdue.length, 'Cần cập nhật nguyên nhân và ETA', 'orange'],
      ['Có kết quả phản hồi', `${responseRate}%`, `${responded} hồ sơ có kết quả`, 'green'],
    ]} />
    <div className="dashboard-grid">
      <article className="panel"><PanelHeading eyebrow="Phân loại rủi ro" title="Mức độ vụ việc" /><BreakdownList rows={grouped(monthly, 'severity')} /></article>
      <article className="panel"><PanelHeading eyebrow="SLA & trách nhiệm" title="Tình trạng xử lý" /><KpiLine label="Có kết quả phản hồi" value={responseRate} tone="green" /><KpiLine label="Vụ việc trong hạn" value={open.length ? 100 - Math.round(overdue.length * 100 / open.length) : 100} tone="blue" /><KpiLine label="Không thuộc mức cao/nghiêm trọng" value={open.length ? 100 - Math.round(high.length * 100 / open.length) : 100} tone="teal" /></article>
    </div>
    <DashboardTable id="case-priorities" title="Vụ việc cần ưu tiên" columns={['Mã', 'Nhóm vấn đề', 'Mức độ', 'PIC', 'Deadline', 'Trạng thái']} rows={priorities.map(item => [String(item.caseCode ?? '—'), String(item.issueGroup ?? '—'), enumLabel(item.severity), String(item.ownerName ?? '—'), formatDate(item.deadline), enumLabel(item.status)])} />
  </div>
}
