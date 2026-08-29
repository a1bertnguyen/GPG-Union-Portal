import { enumLabel, formatDate, formatMoney } from '../../api'
import type { BaseRecord, EngagementSummary, UnionUnit } from '../../types'
import { AlertList, BreakdownList, DashboardTable, KpiLine, MetricGrid, PanelHeading } from './DashboardWidgets'
import { grouped, groupedAmount, sum } from './dashboardAggregates'

export function ActivitiesDashboard({ records, month }: { records: BaseRecord[]; month: string }) {
  const monthly = records.filter(item => String(item.eventDate ?? '').startsWith(month))
  const completed = monthly.filter(item => item.status === 'COMPLETED')
  const missingReports = completed.filter(item => !item.reportCompleted)
  const plannedBudget = sum(monthly, 'plannedBudget')
  const actualCost = sum(monthly, 'actualCost')
  const participants = sum(monthly, 'participantCount')
  const scores = monthly.map(item => Number(item.usefulnessScore ?? 0)).filter(Boolean)
  const averageScore = scores.length ? (scores.reduce((total, value) => total + value, 0) / scores.length).toFixed(1) : '—'
  const upcoming = monthly.filter(item => ['PLANNED', 'APPROVED', 'IN_PROGRESS'].includes(String(item.status ?? ''))).sort((left, right) => String(left.eventDate ?? '').localeCompare(String(right.eventDate ?? ''))).slice(0, 10)
  return <div id="dashboard-activities">
    <MetricGrid cards={[
      ['Chương trình trong kỳ', monthly.length, `Kỳ ${month}`, 'blue'],
      ['Người tham dự', participants, 'Tổng lượt tham gia', 'teal'],
      ['Điểm hữu ích', averageScore === '—' ? averageScore : `${averageScore}/5`, 'Từ phản hồi sau chương trình', 'green'],
      ['Thiếu báo cáo sau CT', missingReports.length, 'Cần hoàn thiện đầu ra', 'orange'],
    ]} />
    <div className="dashboard-grid">
      <article className="panel"><PanelHeading eyebrow="Ngân sách" title="Kế hoạch và thực tế" /><div className="money-row"><div><span>Dự kiến</span><strong>{formatMoney(plannedBudget)}</strong></div><div><span>Thực tế</span><strong>{formatMoney(actualCost)}</strong></div></div><KpiLine label="Tỷ lệ sử dụng ngân sách" value={plannedBudget ? Math.round(actualCost * 100 / plannedBudget) : 0} tone="blue" /></article>
      <article className="panel"><PanelHeading eyebrow="Trạng thái" title="Tiến độ chương trình" /><BreakdownList rows={grouped(monthly, 'status')} /></article>
    </div>
    <DashboardTable id="activity-priorities" title="Chương trình đang triển khai" columns={['Mã', 'Chương trình', 'Đơn vị', 'Ngày', 'Ngân sách', 'Trạng thái']} rows={upcoming.map(item => [String(item.activityCode ?? '—'), String(item.name ?? '—'), item.unionUnit?.code ?? '—', formatDate(item.eventDate), formatMoney(Number(item.plannedBudget ?? 0)), enumLabel(item.status)])} />
  </div>
}

export function FinanceDashboard({ records, units, month }: { records: BaseRecord[]; units: UnionUnit[]; month: string }) {
  const monthly = records.filter(item => String(item.transactionDate ?? '').startsWith(month))
  const income = sum(monthly.filter(item => item.entryType === 'INCOME'), 'amount')
  const advance = sum(monthly.filter(item => item.entryType === 'ADVANCE'), 'amount')
  const expense = sum(monthly.filter(item => ['EXPENSE', 'ADVANCE'].includes(String(item.entryType ?? ''))), 'amount')
  const balance = income - expense
  const incomplete = monthly.filter(item => item.documentStatus === 'INCOMPLETE').length
  const unitRows = units.map(unit => {
    const entries = monthly.filter(item => item.unionUnit?.id === unit.id)
    const unitIncome = sum(entries.filter(item => item.entryType === 'INCOME'), 'amount')
    const unitAdvance = sum(entries.filter(item => item.entryType === 'ADVANCE'), 'amount')
    const unitExpense = sum(entries.filter(item => ['EXPENSE', 'ADVANCE'].includes(String(item.entryType ?? ''))), 'amount')
    return [unit.code, formatMoney(unitIncome), formatMoney(unitExpense), formatMoney(unitAdvance), formatMoney(unitIncome - unitExpense), String(entries.filter(item => item.documentStatus === 'INCOMPLETE').length)]
  }).filter(row => row[1] !== formatMoney(0) || row[2] !== formatMoney(0) || row[3] !== formatMoney(0))
  return <div id="dashboard-finance">
    <MetricGrid cards={[
      ['Tổng thu', formatMoney(income), `Kỳ ${month}`, 'green'],
      ['Tổng chi', formatMoney(expense), 'Đã bao gồm tạm ứng', 'orange'],
      ['Tạm ứng', formatMoney(advance), 'Là một phần của tổng Chi', 'blue'],
      ['Chênh lệch số dư', formatMoney(balance), 'Thu trừ tổng Chi', 'teal'],
      ['Chứng từ chưa đủ', incomplete, `${monthly.length} phiếu trong kỳ`, 'orange'],
    ]} />
    <div className="dashboard-grid">
      <article className="panel" id="finance-breakdown"><PanelHeading eyebrow="Cơ cấu thu – chi" title="Theo nhóm nghiệp vụ" /><BreakdownList rows={groupedAmount(monthly, 'category').map(([label, value]) => [label, formatMoney(value)])} /></article>
      <article className="panel"><PanelHeading eyebrow="Kiểm soát hồ sơ" title="Mức độ hoàn thiện" /><KpiLine label="Chứng từ hợp lệ/không yêu cầu" value={monthly.length ? Math.round((monthly.length - incomplete) * 100 / monthly.length) : 100} tone="green" /><div className="boundary-note"><strong>Ranh giới hệ thống</strong><span>Không chuyển tiền, không truy vấn số dư ngân hàng và không lưu thông tin tài khoản thanh toán.</span></div></article>
    </div>
    <DashboardTable id="finance-units" title="Thu • Chi • Tạm ứng theo đơn vị" columns={['Đơn vị', 'Thu', 'Chi', 'Tạm ứng', 'Chênh lệch số dư', 'Chứng từ thiếu']} rows={unitRows} />
  </div>
}

export function VoiceDashboard({ data }: { data: EngagementSummary }) {
  const maxNeed = Math.max(...data.topNeeds.map(item => item.count), 1)
  return <div id="dashboard-voice">
    <MetricGrid cards={[
      ['Tỷ lệ phản hồi', `${data.surveyResponseRate}%`, `${data.totalResponses} phản hồi trong kỳ`, 'blue'],
      ['Điểm kết nối', data.averageRating ? `${data.averageRating}/5` : '—', 'Mục tiêu từ 3,5/5', 'teal'],
      ['Kiến nghị có phản hồi', `${data.caseResponseRate}%`, 'Mục tiêu từ 90%', 'green'],
      ['Khảo sát đang mở', data.activeSurveyCount, `${data.totalSurveyCount} chiến dịch`, 'orange'],
    ]} />
    <div className="dashboard-grid">
      <article className="panel" id="voice-needs"><PanelHeading eyebrow="Top nhu cầu" title="NLĐ đang quan tâm" /><div className="need-list">{data.topNeeds.map(item => <div key={item.category}><div><strong>{item.category}</strong><span>{item.count} ý kiến</span></div><div className="progress-line"><div style={{ width: `${item.count * 100 / maxNeed}%` }} /></div></div>)}{!data.topNeeds.length && <div className="empty-state">Chưa có phản hồi trong kỳ.</div>}</div></article>
      <article className="panel"><PanelHeading eyebrow="Chỉ số cần theo dõi" title="Kết nối người lao động" count={data.alerts.length} /><AlertList alerts={data.alerts} empty="Các chỉ số đang trong ngưỡng theo dõi." /></article>
    </div>
  </div>
}
