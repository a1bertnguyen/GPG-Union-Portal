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
  { name: 'policyId', label: 'Chính sách áp dụng', type: 'select', required: true, options: [] },
  { name: 'welfareType', label: 'Loại chăm lo', type: 'select', required: true, readOnly: true, options: option('BIRTHDAY', 'FUNERAL', 'WEDDING', 'VISIT', 'CHILDBIRTH', 'HARDSHIP') },
  { name: 'unionUnitId', label: 'CĐCS', type: 'unit', required: true }, { name: 'beneficiaryName', label: 'Người thụ hưởng', required: true },
  { name: 'eventDate', label: 'Ngày phát hiện vụ việc', type: 'date', required: true }, { name: 'deadline', label: 'Hạn hoàn tất (tự tính)', type: 'date', required: true, readOnly: true },
  { name: 'status', label: 'Trạng thái', type: 'select', required: true, options: option('NEW', 'PENDING_APPROVAL', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'), defaultValue: 'NEW' },
  { name: 'amount', label: 'Số tiền', type: 'number', step: '1000', required: true, defaultValue: '0' },
  { name: 'standardAmount', label: 'Định mức theo chính sách', type: 'number', step: '1000', readOnly: true },
  { name: 'documentStatus', label: 'Hồ sơ / chứng từ', type: 'select', required: true, options: option('COMPLETE', 'INCOMPLETE', 'NOT_REQUIRED'), defaultValue: 'INCOMPLETE' },
  { name: 'receiptStatus', label: 'Biên nhận / quyết toán', type: 'select', required: true, options: option('COMPLETE', 'INCOMPLETE', 'NOT_REQUIRED'), defaultValue: 'INCOMPLETE' },
  { name: 'hasImage', label: 'Đã có hình ảnh', type: 'checkbox', defaultValue: false },
  { name: 'notes', label: 'Ghi chú', type: 'textarea', wide: true },
]

export const welfarePolicyFields: FieldConfig[] = [
  { name: 'code', label: 'Mã chính sách', required: true, placeholder: 'VD: CD-01-01' },
  { name: 'source', label: 'Nguồn hỗ trợ', type: 'select', required: true, options: option('UNION', 'COMPANY'), defaultValue: 'UNION' },
  { name: 'sequenceNumber', label: 'TT', type: 'number', min: '1', required: true, defaultValue: '1' },
  { name: 'welfareType', label: 'Loại chăm lo', type: 'select', required: true, options: option('BIRTHDAY', 'FUNERAL', 'WEDDING', 'VISIT', 'CHILDBIRTH', 'HARDSHIP'), defaultValue: 'HARDSHIP' },
  { name: 'name', label: 'Nội dung chính sách', required: true, wide: true },
  { name: 'supportAmount', label: 'Mức hỗ trợ (VNĐ)', type: 'number', min: '0', step: '1000', required: true, defaultValue: '0' },
  { name: 'processingWeeks', label: 'Thời hạn xử lý (tuần)', type: 'number', min: '1', max: '8', required: true, defaultValue: '1' },
  { name: 'active', label: 'Đang áp dụng', type: 'checkbox', defaultValue: true },
  { name: 'eligibilityNotes', label: 'Đối tượng / điều kiện / ghi chú', type: 'textarea', wide: true },
]

export const caseFields: FieldConfig[] = [
  { name: 'caseCode', label: 'Mã vụ việc', required: true }, { name: 'receivedDate', label: 'Ngày nhận', type: 'date', required: true },
  { name: 'unionUnitId', label: 'Đơn vị', type: 'unit', required: true }, { name: 'requesterName', label: 'Người gửi', required: true },
  { name: 'employeeCode', label: 'Mã NV' }, { name: 'jobTitle', label: 'Chức danh' },
  { name: 'workplace', label: 'Nơi làm việc' }, { name: 'phone', label: 'Điện thoại' },
  { name: 'startWorkDate', label: 'Ngày vào làm', type: 'date' }, { name: 'leaveDate', label: 'Ngày nghỉ', type: 'date' },
  { name: 'source', label: 'Kênh tiếp nhận' }, { name: 'issueGroup', label: 'Nhóm vấn đề', required: true },
  { name: 'severity', label: 'Mức độ', type: 'select', required: true, options: option('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'), defaultValue: 'MEDIUM' },
  { name: 'ownerName', label: 'PIC do ADMIN giao' }, { name: 'deadline', label: 'Deadline do ADMIN giao', type: 'date' },
  { name: 'status', label: 'Trạng thái', type: 'select', required: true, options: option('NEW', 'VERIFYING', 'IN_PROGRESS', 'WAITING_RESPONSE', 'PENDING_APPROVAL', 'CLOSED'), defaultValue: 'NEW' },
  { name: 'affectedPeople', label: 'Số NLĐ ảnh hưởng', type: 'number', required: true, defaultValue: '1' },
  { name: 'description', label: 'Mô tả', type: 'textarea', required: true, wide: true },
  { name: 'attachmentNote', label: 'Tài liệu đính kèm / liên kết', type: 'textarea', wide: true },
  { name: 'resultText', label: 'Kết quả / phản hồi', type: 'textarea', wide: true },
  { name: 'responseDate', label: 'Ngày trả lời (hệ thống ghi)', type: 'date', readOnly: true },
  { name: 'overdueReason', label: 'Lý do quá hạn / ETA mới', type: 'textarea', wide: true },
]

export const activityFields: FieldConfig[] = [
  { name: 'activityCode', label: 'Mã hoạt động', required: true, section: 'Thông tin kế hoạch', sectionDescription: 'Chỉ nhập thông tin cần thiết để khởi tạo chương trình' },
  { name: 'name', label: 'Tên chương trình', required: true },
  { name: 'unionUnitId', label: 'Đơn vị', type: 'unit', required: true },
  { name: 'eventDate', label: 'Ngày tổ chức', type: 'date', required: true },
  { name: 'eventTime', label: 'Giờ tổ chức', type: 'time', required: true },
  { name: 'location', label: 'Địa điểm', required: true },
  { name: 'programPic', label: 'PIC chương trình', required: true },
  { name: 'objective', label: 'Mục tiêu', type: 'textarea', wide: true },
  { name: 'plannedBudget', label: 'Ngân sách dự kiến', type: 'number', step: '1000', required: true, defaultValue: '0' },
  { name: 'invitedCount', label: 'Quy mô dự kiến', type: 'number', required: true, defaultValue: '0' },
  { name: 'employeeGroup', label: 'Nhóm NLĐ / đối tượng', required: true },
  { name: 'status', label: 'Phê duyệt / trạng thái', type: 'select', required: true, options: option('PLANNED', 'APPROVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'), defaultValue: 'PLANNED' },
  { name: 'participantCount', label: 'Số người tham dự', type: 'number', defaultValue: '0', hidden: true },
  { name: 'participantList', label: 'Danh sách tham dự', type: 'textarea', hidden: true },
  { name: 'checkInCount', label: 'Số người check-in', type: 'number', defaultValue: '0', hidden: true },
  { name: 'quickFeedback', label: 'Phản hồi nhanh', type: 'textarea', hidden: true },
  { name: 'issues', label: 'Vấn đề phát sinh', type: 'textarea', hidden: true },
  { name: 'actualContent', label: 'Nội dung thực tế đã triển khai', type: 'textarea', hidden: true },
  { name: 'planDifference', label: 'Khác biệt so với kế hoạch', type: 'textarea', hidden: true },
  { name: 'workersReached', label: 'Số NLĐ được tiếp cận', type: 'number', defaultValue: '0', hidden: true },
  { name: 'outputProposal', label: 'Đề xuất sau chương trình', type: 'textarea', hidden: true },
  { name: 'actualCost', label: 'Chi phí thực tế', type: 'number', defaultValue: '0', hidden: true },
  { name: 'documentStatus', label: 'Chứng từ sau chương trình', type: 'select', defaultValue: 'INCOMPLETE', hidden: true },
  { name: 'communicationContent', label: 'Nội dung truyền thông', type: 'textarea', hidden: true },
  { name: 'usefulnessScore', label: 'Điểm đánh giá hữu ích', type: 'number', hidden: true },
  { name: 'strengths', label: 'Điều làm tốt', type: 'textarea', hidden: true },
  { name: 'weaknesses', label: 'Điều chưa tốt', type: 'textarea', hidden: true },
  { name: 'reportCompleted', label: 'Đã có báo cáo sau CT', type: 'checkbox', defaultValue: false, hidden: true },
  { name: 'followUpIssue', label: 'Vấn đề cần follow-up', type: 'textarea', hidden: true },
  { name: 'followUpOwner', label: 'PIC follow-up', hidden: true },
  { name: 'followUpDeadline', label: 'Deadline follow-up', type: 'date', hidden: true },
  { name: 'followUpStatus', label: 'Tình trạng follow-up', hidden: true },
  { name: 'lessonsLearned', label: 'Bài học sau chương trình', type: 'textarea', hidden: true },
]

export const financeFields: FieldConfig[] = [
  { name: 'entryCode', label: 'Mã phiếu', required: true }, { name: 'unionUnitId', label: 'Đơn vị', type: 'unit', required: true },
  { name: 'transactionDate', label: 'Ngày giao dịch', type: 'date', required: true },
  { name: 'entryType', label: 'Loại', type: 'select', required: true, options: option('INCOME', 'EXPENSE', 'ADVANCE'), defaultValue: 'EXPENSE' },
  { name: 'category', label: 'Nhóm nghiệp vụ', required: true }, { name: 'amount', label: 'Số tiền', type: 'number', step: '1000', required: true },
  { name: 'documentNumber', label: 'Số chứng từ' },
  { name: 'documentStatus', label: 'Tình trạng chứng từ', type: 'select', required: true, options: option('COMPLETE', 'INCOMPLETE', 'NOT_REQUIRED'), defaultValue: 'INCOMPLETE' },
  { name: 'description', label: 'Nội dung', type: 'textarea', required: true, wide: true },
]
