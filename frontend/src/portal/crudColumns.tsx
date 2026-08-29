import { enumLabel, formatDate, formatMoney } from '../api'
import { StatusBadge, type ColumnConfig } from '../components/CrudPage'
import type { BaseRecord } from '../types'

const text = (item: BaseRecord, key: string) => String(item[key] ?? '—')

export const unitColumns: ColumnConfig[] = [
  { label: 'Mã', render: item => <strong>{text(item, 'code')}</strong> }, { label: 'CĐCS', render: item => text(item, 'name') },
  { label: 'Công ty', render: item => text(item, 'companyName') }, { label: 'Địa điểm', render: item => text(item, 'location') },
  { label: 'Chủ tịch', render: item => text(item, 'chairperson') }, { label: 'Trạng thái', render: item => <StatusBadge value={item.legalStatus} /> },
]

export const memberColumns: ColumnConfig[] = [
  { label: 'Mã NV', render: item => <strong>{text(item, 'employeeCode')}</strong> }, { label: 'Họ tên', render: item => text(item, 'fullName') },
  { label: 'Công ty', render: item => text(item, 'company') }, { label: 'Nơi làm việc', render: item => text(item, 'workplace') },
  { label: 'CĐCS', render: item => item.unionUnit?.code ?? '—' }, { label: 'Chức danh CĐ', render: item => text(item, 'proposedUnionTitle') },
  { label: 'Chức vụ chuyên môn', render: item => text(item, 'professionalTitle') }, { label: 'Tình trạng', render: item => <StatusBadge value={item.membershipStatus} /> },
]

export const welfareColumns: ColumnConfig[] = [
  { label: 'Mã', render: item => <strong>{text(item, 'recordCode')}</strong> }, { label: 'Loại', render: item => enumLabel(item.welfareType) },
  { label: 'Người thụ hưởng', render: item => text(item, 'beneficiaryName') }, { label: 'Đơn vị', render: item => item.unionUnit?.code ?? '—' },
  { label: 'Đến hạn', render: item => formatDate(item.deadline ?? item.eventDate) }, { label: 'Số tiền', render: item => formatMoney(item.amount as number) },
  { label: 'Trạng thái', render: item => <StatusBadge value={item.status} /> }, { label: 'Chứng từ', render: item => <StatusBadge value={item.documentStatus} /> },
]

export const welfarePolicyColumns: ColumnConfig[] = [
  { label: 'Mã', render: item => <strong>{text(item, 'code')}</strong> },
  { label: 'Nguồn', render: item => enumLabel(item.source) },
  { label: 'TT', render: item => text(item, 'sequenceNumber') },
  { label: 'Loại', render: item => enumLabel(item.welfareType) },
  { label: 'Nội dung', render: item => text(item, 'name') },
  { label: 'Mức hỗ trợ', render: item => formatMoney(item.supportAmount as number) },
  { label: 'Thời hạn', render: item => `${text(item, 'processingWeeks')} tuần` },
  { label: 'Trạng thái', render: item => <StatusBadge value={item.active ? 'ACTIVE' : 'INACTIVE'} /> },
]

export const caseColumns: ColumnConfig[] = [
  { label: 'Mã', render: item => <strong>{text(item, 'caseCode')}</strong> }, { label: 'Mã NV', render: item => text(item, 'employeeCode') },
  { label: 'Người gửi', render: item => text(item, 'requesterName') }, { label: 'Nơi làm việc', render: item => text(item, 'workplace') },
  { label: 'Nhóm vấn đề', render: item => text(item, 'issueGroup') }, { label: 'Mức độ', render: item => <StatusBadge value={item.severity} /> },
  { label: 'PIC', render: item => text(item, 'ownerName') }, { label: 'Deadline', render: item => formatDate(item.deadline) },
  { label: 'Trạng thái', render: item => <StatusBadge value={item.status} /> },
]

export const activityColumns: ColumnConfig[] = [
  { label: 'Mã', render: item => <strong>{text(item, 'activityCode')}</strong> }, { label: 'Chương trình', render: item => text(item, 'name') },
  { label: 'Đơn vị', render: item => item.unionUnit?.code ?? '—' }, { label: 'Ngày', render: item => formatDate(item.eventDate) },
  { label: 'Trạng thái', render: item => <StatusBadge value={item.status} /> }, { label: 'Check-in', render: item => `${text(item, 'checkInCount')}/${text(item, 'participantCount')}` },
  { label: 'Chi phí', render: item => formatMoney(item.actualCost as number) }, { label: 'Báo cáo', render: item => item.reportCompleted ? <StatusBadge value="COMPLETE" /> : <StatusBadge value="INCOMPLETE" /> },
]

export const financeColumns: ColumnConfig[] = [
  { label: 'Mã phiếu', render: item => <strong>{text(item, 'entryCode')}</strong> }, { label: 'Ngày', render: item => formatDate(item.transactionDate) },
  { label: 'Đơn vị', render: item => item.unionUnit?.code ?? '—' }, { label: 'Loại', render: item => <StatusBadge value={item.entryType} /> },
  { label: 'Nội dung', render: item => text(item, 'description') }, { label: 'Số tiền', render: item => <strong>{formatMoney(item.amount as number)}</strong> },
  { label: 'Chứng từ', render: item => <StatusBadge value={item.documentStatus} /> },
]
