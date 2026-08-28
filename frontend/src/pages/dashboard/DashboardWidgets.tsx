import { enumLabel, formatDate } from '../../api'
import type { BaseRecord, DashboardSummary } from '../../types'

export type MetricCardData = [string, string | number, string, string]

export function MetricGrid({ cards }: { cards: MetricCardData[] }) {
  return <div className="metric-grid metric-grid--compact">{cards.map(([label, value, note, tone]) => <article className={`metric-card metric-card--${tone}`} key={label}><span>{label}</span><strong>{value}</strong><small>{note}</small></article>)}</div>
}

export function PanelHeading({ eyebrow, title, count }: { eyebrow: string; title: string; count?: number }) {
  return <div className="panel__heading"><div><p className="eyebrow">{eyebrow}</p><h2>{title}</h2></div>{count !== undefined && <span className="alert-count">{count}</span>}</div>
}

export function KpiLine({ label, value, tone }: { label: string; value: number; tone: string }) {
  const safeValue = Math.max(0, Math.min(value, 100))
  return <div className="kpi-line"><div><span>{label}</span><strong>{Math.round(value)}%</strong></div><div className="progress-line"><div className={`progress-fill progress-fill--${tone}`} style={{ width: `${safeValue}%` }} /></div></div>
}

export function AlertList({ alerts, empty }: { alerts: DashboardSummary['alerts']; empty: string }) {
  return <div className="alert-list">{alerts.length ? alerts.map((alert, index) => <div key={`${alert.title}-${index}`} className={`alert-item alert-item--${alert.level}`}><i /><div><strong>{alert.title}</strong><span>{alert.detail}</span></div></div>) : <div className="empty-state">{empty}</div>}</div>
}

export function BreakdownList({ rows }: { rows: Array<[string, string | number]> }) {
  return <div className="care-list">{rows.map(([label, value]) => <div key={label}><strong>{enumLabel(label)}</strong><span>{value}</span></div>)}{!rows.length && <div className="empty-state">Chưa có dữ liệu trong kỳ.</div>}</div>
}

export function RecordList({ records, primary, secondary, meta, empty }: { records: BaseRecord[]; primary: string; secondary: string; meta: string; empty: string }) {
  return <div className="todo-list">{records.map(item => <div key={item.id}><i /><strong>{String(item[primary] ?? '—')}</strong><span>{String(item[secondary] ?? '—')} · {formatDate(item[meta])}</span></div>)}{!records.length && <div className="empty-state">{empty}</div>}</div>
}

export function DashboardTable({ id, title, columns, rows }: { id: string; title: string; columns: string[]; rows: string[][] }) {
  return <div className="data-card dashboard-table" id={id}><div className="data-card__header"><div className="record-count"><strong>{title}</strong><span>{rows.length} bản ghi đang hiển thị</span></div></div><div className="table-wrap"><table><thead><tr>{columns.map(column => <th key={column}>{column}</th>)}</tr></thead><tbody>{rows.map((row, rowIndex) => <tr key={`${row[0]}-${rowIndex}`}>{row.map((cell, index) => <td key={`${columns[index]}-${cell}`}>{index === 0 ? <strong>{cell}</strong> : cell}</td>)}</tr>)}{!rows.length && <tr><td className="empty-cell" colSpan={columns.length}>Không có dữ liệu phù hợp kỳ đã chọn.</td></tr>}</tbody></table></div></div>
}
