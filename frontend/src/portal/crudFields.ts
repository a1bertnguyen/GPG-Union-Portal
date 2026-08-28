import { enumLabel } from '../api'
import type { FieldConfig } from '../components/CrudPage'
import {
  memberCompanyOptions,
  memberEducationOptions,
  memberPoliticalTheoryOptions,
  memberWorkplaceOptions,
} from './memberCatalog'

const option = (...values: string[]) => values.map(value => ({ value, label: enumLabel(value) }))

export const unitFields: FieldConfig[] = [
  { name: 'code', label: 'Mã CĐCS', required: true }, { name: 'name', label: 'Tên CĐCS', required: true },
  { name: 'companyName', label: 'Công ty', required: true }, { name: 'location', label: 'Địa điểm' },
  { name: 'chairperson', label: 'Chủ tịch CĐCS' }, { name: 'contactPerson', label: 'Đầu mối' },
  { name: 'termStart', label: 'Bắt đầu nhiệm kỳ', type: 'date' }, { name: 'termEnd', label: 'Kết thúc nhiệm kỳ', type: 'date' },
  { name: 'decisionNumber', label: 'Số quyết định' },
  { name: 'legalStatus', label: 'Tình trạng pháp lý', type: 'select', required: true, options: option('ACTIVE', 'INACTIVE'), defaultValue: 'ACTIVE' },
]

export const memberFields: FieldConfig[] = [
  { name: 'employeeCode', label: 'Mã nhân viên', required: true, placeholder: 'VD: NV0339' },
  { name: 'fullName', label: 'Họ và tên', required: true },
  { name: 'unionUnitId', label: 'CĐCS', type: 'unit', required: true },
  { name: 'company', label: 'Công ty', type: 'select', required: true, options: memberCompanyOptions },
  { name: 'workplace', label: 'Nơi làm việc', type: 'select', required: true, options: memberWorkplaceOptions },
  { name: 'proposedUnionTitle', label: 'Chức danh công đoàn', placeholder: 'Nhập chức danh công đoàn' },
  { name: 'professionalTitle', label: 'Chức vụ chuyên môn', placeholder: 'Nhập chức vụ chuyên môn' },
  { name: 'jobTitle', label: 'Vị trí công việc', placeholder: 'Nhập vị trí công việc nếu khác chức vụ chuyên môn' },
  { name: 'gender', label: 'Giới tính', type: 'select', options: option('MALE', 'FEMALE') },
  { name: 'ethnicity', label: 'Dân tộc', placeholder: 'VD: Kinh' },
  { name: 'placeOfBirth', label: 'Nơi sinh' },
  { name: 'nationalId', label: 'CCCD', placeholder: 'Nhập đủ số, kể cả số 0 đầu' },
  { name: 'partyMember', label: 'Đảng viên', type: 'checkbox', defaultValue: false },
  { name: 'education', label: 'Học vấn', type: 'select', options: memberEducationOptions },
  { name: 'specialization', label: 'Chuyên môn' },
  { name: 'politicalTheory', label: 'Trình độ chính trị', type: 'select', options: memberPoliticalTheoryOptions },
  { name: 'foreignLanguage', label: 'Ngoại ngữ' },
  { name: 'phone', label: 'Điện thoại', placeholder: 'Giữ nguyên số 0 đầu' },
  { name: 'joinDate', label: 'Ngày gia nhập công đoàn', type: 'date' },
  { name: 'startWorkDate', label: 'Ngày vào làm', type: 'date' },
  { name: 'email', label: 'Email', type: 'email' },
  { name: 'currentResidence', label: 'Nơi ở hiện tại', type: 'textarea', wide: true },
  { name: 'membershipStatus', label: 'Tình trạng công đoàn', type: 'select', required: true, options: option('MEMBER', 'NOT_JOINED', 'LEFT'), defaultValue: 'MEMBER' },
  { name: 'employmentStatus', label: 'Trạng thái nhân sự', type: 'select', required: true, options: option('ACTIVE', 'INACTIVE'), defaultValue: 'ACTIVE' },
]

export const welfareFields: FieldConfig[] = [
  { name: 'recordCode', label: 'Mã hồ sơ', required: true },
  { name: 'welfareType', label: 'Loại chăm lo', type: 'select', required: true, options: option('BIRTHDAY', 'FUNERAL', 'WEDDING', 'VISIT', 'CHILDBIRTH', 'HARDSHIP'), defaultValue: 'BIRTHDAY' },
  { name: 'policyName', label: 'Chính sách / định mức áp dụng' },
  { name: 'unionUnitId', label: 'CĐCS', type: 'unit', required: true }, { name: 'beneficiaryName', label: 'Người thụ hưởng', required: true },
  { name: 'eventDate', label: 'Ngày sự kiện', type: 'date', required: true }, { name: 'deadline', label: 'Hạn hoàn tất', type: 'date' },
  { name: 'status', label: 'Trạng thái', type: 'select', required: true, options: option('NEW', 'PENDING_APPROVAL', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'), defaultValue: 'NEW' },
  { name: 'amount', label: 'Số tiền', type: 'number', step: '1000', required: true, defaultValue: '0' },
  { name: 'standardAmount', label: 'Định mức', type: 'number', step: '1000' },
  { name: 'documentStatus', label: 'Hồ sơ / chứng từ', type: 'select', required: true, options: option('COMPLETE', 'INCOMPLETE', 'NOT_REQUIRED'), defaultValue: 'INCOMPLETE' },
  { name: 'receiptStatus', label: 'Biên nhận / quyết toán', type: 'select', required: true, options: option('COMPLETE', 'INCOMPLETE', 'NOT_REQUIRED'), defaultValue: 'INCOMPLETE' },
  { name: 'hasImage', label: 'Đã có hình ảnh', type: 'checkbox', defaultValue: false },
  { name: 'notes', label: 'Ghi chú', type: 'textarea', wide: true },
]

export const caseFields: FieldConfig[] = [
  { name: 'caseCode', label: 'Mã vụ việc', required: true }, { name: 'receivedDate', label: 'Ngày nhận', type: 'date', required: true },
  { name: 'unionUnitId', label: 'Đơn vị', type: 'unit', required: true }, { name: 'requesterName', label: 'Người gửi', required: true },
  { name: 'source', label: 'Kênh tiếp nhận' }, { name: 'issueGroup', label: 'Nhóm vấn đề', required: true },
  { name: 'severity', label: 'Mức độ', type: 'select', required: true, options: option('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'), defaultValue: 'MEDIUM' },
  { name: 'ownerName', label: 'PIC', required: true }, { name: 'deadline', label: 'Deadline', type: 'date', required: true },
  { name: 'status', label: 'Trạng thái', type: 'select', required: true, options: option('NEW', 'VERIFYING', 'CLASSIFYING', 'ASSIGNED', 'IN_PROGRESS', 'WAITING_RESPONSE', 'CLOSED'), defaultValue: 'NEW' },
  { name: 'affectedPeople', label: 'Số NLĐ ảnh hưởng', type: 'number', required: true, defaultValue: '1' },
  { name: 'description', label: 'Mô tả', type: 'textarea', required: true, wide: true },
  { name: 'attachmentNote', label: 'Tài liệu đính kèm / liên kết', type: 'textarea', wide: true },
  { name: 'resultText', label: 'Kết quả / phản hồi', type: 'textarea', wide: true },
  { name: 'overdueReason', label: 'Lý do quá hạn / ETA mới', type: 'textarea', wide: true },
]

export const activityFields: FieldConfig[] = [
  { name: 'activityCode', label: 'Mã hoạt động', required: true }, { name: 'name', label: 'Tên chương trình', required: true },
  { name: 'unionUnitId', label: 'Đơn vị', type: 'unit', required: true }, { name: 'eventDate', label: 'Ngày tổ chức', type: 'date', required: true },
  { name: 'status', label: 'Trạng thái', type: 'select', required: true, options: option('PLANNED', 'APPROVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'), defaultValue: 'PLANNED' },
  { name: 'plannedBudget', label: 'Ngân sách dự kiến', type: 'number', step: '1000', required: true, defaultValue: '0' },
  { name: 'actualCost', label: 'Chi phí thực tế', type: 'number', step: '1000', required: true, defaultValue: '0' },
  { name: 'participantCount', label: 'Số người tham dự', type: 'number', required: true, defaultValue: '0' },
  { name: 'participantList', label: 'Danh sách tham dự', type: 'textarea', wide: true },
  { name: 'checkInCount', label: 'Số người check-in', type: 'number', required: true, defaultValue: '0' },
  { name: 'usefulnessScore', label: 'Điểm hữu ích (0–5)', type: 'number', step: '0.1' },
  { name: 'quickFeedback', label: 'Phản hồi nhanh', type: 'textarea', wide: true },
  { name: 'issues', label: 'Vấn đề phát sinh', type: 'textarea', wide: true },
  { name: 'reportCompleted', label: 'Đã có báo cáo sau CT', type: 'checkbox', defaultValue: false },
  { name: 'documentStatus', label: 'Chứng từ sau chương trình', type: 'select', required: true, options: option('COMPLETE', 'INCOMPLETE', 'NOT_REQUIRED'), defaultValue: 'INCOMPLETE' },
  { name: 'objective', label: 'Mục tiêu', type: 'textarea', wide: true },
  { name: 'followUpOwner', label: 'PIC follow-up' }, { name: 'followUpDeadline', label: 'Deadline follow-up', type: 'date' },
  { name: 'lessonsLearned', label: 'Bài học sau chương trình', type: 'textarea', wide: true },
]

export const financeFields: FieldConfig[] = [
  { name: 'entryCode', label: 'Mã phiếu', required: true }, { name: 'unionUnitId', label: 'Đơn vị', type: 'unit', required: true },
  { name: 'transactionDate', label: 'Ngày giao dịch', type: 'date', required: true },
  { name: 'entryType', label: 'Loại', type: 'select', required: true, options: option('INCOME', 'EXPENSE'), defaultValue: 'EXPENSE' },
  { name: 'category', label: 'Nhóm thu/chi', required: true }, { name: 'amount', label: 'Số tiền', type: 'number', step: '1000', required: true },
  { name: 'documentNumber', label: 'Số chứng từ' },
  { name: 'documentStatus', label: 'Tình trạng chứng từ', type: 'select', required: true, options: option('COMPLETE', 'INCOMPLETE', 'NOT_REQUIRED'), defaultValue: 'INCOMPLETE' },
  { name: 'description', label: 'Nội dung', type: 'textarea', required: true, wide: true },
]
