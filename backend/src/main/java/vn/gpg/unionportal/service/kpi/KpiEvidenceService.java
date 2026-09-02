package vn.gpg.unionportal.service.kpi;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.KpiModels.EvidenceAttachment;
import vn.gpg.unionportal.dto.KpiModels.EvidenceField;
import vn.gpg.unionportal.dto.KpiModels.EvidenceRecord;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.i18n.EnumLabels;
import vn.gpg.unionportal.model.*;
import vn.gpg.unionportal.model.kpi.KpiNoOccurrenceConfirmation;
import vn.gpg.unionportal.repository.*;
import vn.gpg.unionportal.repository.kpi.KpiNoOccurrenceConfirmationRepository;
import vn.gpg.unionportal.service.CurrentUserService;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the opaque IDs emitted by the KPI engine back to their authoritative source records.
 * Only a small, explicit field whitelist is returned; file bytes stay behind the existing secured
 * download endpoints.
 */
@Service
@Transactional(readOnly = true)
public class KpiEvidenceService {
    private final UnionUnitRepository units;
    private final MemberRepository members;
    private final MemberChangeRepository memberChanges;
    private final MonthlyReportRepository reports;
    private final WelfareRecordRepository welfare;
    private final LaborCaseRepository cases;
    private final UnionActivityRepository activities;
    private final FinanceEntryRepository finance;
    private final MemberDocumentRepository memberDocuments;
    private final WelfareDocumentRepository welfareDocuments;
    private final LaborCaseDocumentRepository caseDocuments;
    private final ActivityMediaRepository activityMedia;
    private final FinanceDocumentRepository financeDocuments;
    private final KpiNoOccurrenceConfirmationRepository noOccurrence;
    private final CurrentUserService currentUser;

    public KpiEvidenceService(UnionUnitRepository units, MemberRepository members,
                              MemberChangeRepository memberChanges, MonthlyReportRepository reports,
                              WelfareRecordRepository welfare, LaborCaseRepository cases,
                              UnionActivityRepository activities, FinanceEntryRepository finance,
                              MemberDocumentRepository memberDocuments,
                              WelfareDocumentRepository welfareDocuments,
                              LaborCaseDocumentRepository caseDocuments,
                              ActivityMediaRepository activityMedia,
                              FinanceDocumentRepository financeDocuments,
                              KpiNoOccurrenceConfirmationRepository noOccurrence,
                              CurrentUserService currentUser) {
        this.units = units;
        this.members = members;
        this.memberChanges = memberChanges;
        this.reports = reports;
        this.welfare = welfare;
        this.cases = cases;
        this.activities = activities;
        this.finance = finance;
        this.memberDocuments = memberDocuments;
        this.welfareDocuments = welfareDocuments;
        this.caseDocuments = caseDocuments;
        this.activityMedia = activityMedia;
        this.financeDocuments = financeDocuments;
        this.noOccurrence = noOccurrence;
        this.currentUser = currentUser;
    }

    public EvidenceRecord read(String resourceType, String recordId) {
        return switch (resourceType) {
            case "union-unit" -> unionUnit(longId(recordId));
            case "member" -> member(longId(recordId));
            case "member-change" -> memberChange(longId(recordId));
            case "monthly-report" -> monthlyReport(longId(recordId));
            case "report-obligation" -> reportObligation(recordId);
            case "welfare" -> sensitiveWelfare(longId(recordId));
            case "labor-case" -> sensitiveLaborCase(longId(recordId));
            case "activity" -> activity(longId(recordId));
            case "finance-entry" -> financeEntry(longId(recordId));
            case "no-occurrence" -> noOccurrence(longId(recordId));
            default -> throw new ResourceNotFoundException("Loại chứng cứ KPI không tồn tại: " + resourceType);
        };
    }

    private EvidenceRecord unionUnit(Long id) {
        UnionUnit item = units.findById(id)
                .orElseThrow(() -> missing("CĐCS", id));
        currentUser.requireUnitAccess(item.getId());
        return record("DM_CONG_DOAN", item.getCode(), item.getCode() + " · " + item.getName(), List.of(
                field("Công ty", item.getCompanyName()),
                field("Trạng thái pháp lý", label(item.getLegalStatus())),
                field("Số quyết định", item.getDecisionNumber()),
                field("Chủ tịch/BCH", item.getChairperson()),
                field("Bắt đầu nhiệm kỳ", item.getTermStart()),
                field("Kết thúc nhiệm kỳ", item.getTermEnd())), List.of());
    }

    private EvidenceRecord member(Long id) {
        Member item = members.findById(id).orElseThrow(() -> missing("đoàn viên", id));
        currentUser.requireUnitAccess(item.getUnionUnit().getId());
        List<EvidenceAttachment> attachments = memberDocuments.findByMemberIdOrderByCreatedAtDesc(id).stream()
                .map(document -> attachment(document.getId(), document.getFileName(),
                        "/member-documents/" + document.getId() + "/download"))
                .toList();
        return record("DOAN_VIEN", String.valueOf(id), item.getEmployeeCode() + " · " + item.getFullName(), List.of(
                field("CĐCS", item.getUnionUnit().getCode()),
                field("Nơi làm việc", item.getWorkplace()),
                field("Ngày vào làm", item.getStartWorkDate()),
                field("Ngày vào Công đoàn", item.getJoinDate()),
                field("Trạng thái đoàn viên", label(item.getMembershipStatus())),
                field("Trạng thái làm việc", label(item.getEmploymentStatus())),
                field("Cập nhật lúc", item.getUpdatedAt())), attachments);
    }

    private EvidenceRecord memberChange(Long id) {
        MemberChange item = memberChanges.findById(id).orElseThrow(() -> missing("biến động đoàn viên", id));
        currentUser.requireUnitAccess(item.getMember().getUnionUnit().getId());
        return record("DOAN_VIEN", String.valueOf(id), "Biến động · " + item.getMember().getEmployeeCode(), List.of(
                field("Loại biến động", item.getChangeType()),
                field("Ngày phát sinh", item.getEffectiveDate()),
                field("Người ghi nhận", item.getRecordedBy()),
                field("Ngày ghi nhận", item.getCreatedAt()),
                field("Mô tả", item.getDescription())), List.of());
    }

    private EvidenceRecord monthlyReport(Long id) {
        MonthlyReport item = reports.findById(id).orElseThrow(() -> missing("báo cáo tháng", id));
        currentUser.requireUnitAccess(item.getUnionUnit().getId());
        return record("BAO_CAO_DINH_KY", String.valueOf(id),
                "Báo cáo " + YearMonth.from(item.getReportMonth()), List.of(
                        field("CĐCS", item.getUnionUnit().getCode()),
                        field("Kỳ báo cáo", YearMonth.from(item.getReportMonth())),
                        field("Trạng thái", label(item.getStatus())),
                        field("Người lập", item.getPreparedBy()),
                        field("Nộp lúc", item.getSubmittedAt()),
                        field("Kế hoạch kỳ sau", item.getPlanNextMonth())), List.of());
    }

    private EvidenceRecord reportObligation(String recordId) {
        int separator = recordId.indexOf(':');
        if (separator <= 0 || separator == recordId.length() - 1) {
            throw new ResourceNotFoundException("Mã nghĩa vụ báo cáo KPI không hợp lệ");
        }
        Long unitId = longId(recordId.substring(0, separator));
        YearMonth month;
        try {
            month = YearMonth.parse(recordId.substring(separator + 1));
        } catch (RuntimeException invalidMonth) {
            throw new ResourceNotFoundException("Kỳ nghĩa vụ báo cáo KPI không hợp lệ");
        }
        UnionUnit unit = units.findById(unitId).orElseThrow(() -> missing("CĐCS", unitId));
        currentUser.requireUnitAccess(unitId);
        MonthlyReport submitted = reports.findByUnionUnitIdAndReportMonth(unitId, month.atDay(1)).orElse(null);
        List<EvidenceField> fields = new ArrayList<>();
        fields.add(field("CĐCS", unit.getCode() + " · " + unit.getName()));
        fields.add(field("Tháng phải nộp", month));
        fields.add(field("Tình trạng", submitted == null ? "Chưa có báo cáo" : label(submitted.getStatus())));
        if (submitted != null) {
            fields.add(field("ID báo cáo nguồn", submitted.getId()));
            fields.add(field("Nộp lúc", submitted.getSubmittedAt()));
        }
        return record("BAO_CAO_DINH_KY", recordId, "Nghĩa vụ báo cáo " + month, fields, List.of());
    }

    private EvidenceRecord sensitiveWelfare(Long id) {
        requireSensitiveAccess();
        WelfareRecord item = welfare.findById(id).orElseThrow(() -> missing("hồ sơ chăm lo", id));
        currentUser.requireUnitAccess(item.getUnionUnit().getId());
        List<EvidenceAttachment> attachments = welfareDocuments.findByWelfareRecordIdOrderByCreatedAtDesc(id).stream()
                .map(document -> attachment(document.getId(), document.getFileName(),
                        "/welfare-documents/" + document.getId() + "/download"))
                .toList();
        return record("CHAM_SOC_NLD", String.valueOf(id), item.getRecordCode(), List.of(
                field("CĐCS", item.getUnionUnit().getCode()),
                field("Loại chăm lo", label(item.getWelfareType())),
                field("Ngày phát sinh", item.getEventDate()),
                field("Hạn xử lý", item.getDeadline()),
                field("Trạng thái", label(item.getStatus())),
                field("Chính sách", item.getPolicyName()),
                field("Mức hỗ trợ", item.getAmount()),
                field("Tình trạng hồ sơ", label(item.getDocumentStatus())),
                field("Tình trạng biên nhận", label(item.getReceiptStatus()))), attachments);
    }

    private EvidenceRecord sensitiveLaborCase(Long id) {
        requireSensitiveAccess();
        LaborCase item = cases.findById(id).orElseThrow(() -> missing("kiến nghị", id));
        currentUser.requireUnitAccess(item.getUnionUnit().getId());
        List<EvidenceAttachment> attachments = caseDocuments.findAll((root, query, cb) ->
                        cb.equal(root.get("laborCase").get("id"), id)).stream()
                .map(document -> attachment(document.getId(), document.getFileName(),
                        "/case-documents/" + document.getId() + "/download"))
                .toList();
        return record("SO_KIEN_NGHI", String.valueOf(id), item.getCaseCode(), List.of(
                field("CĐCS", item.getUnionUnit().getCode()),
                field("Ngày tiếp nhận", item.getReceivedDate()),
                field("Nhóm vấn đề", item.getIssueGroup()),
                field("Mức độ", label(item.getSeverity())),
                field("Hạn xử lý", item.getDeadline()),
                field("Trạng thái", label(item.getStatus())),
                field("Ngày phản hồi", item.getResponseDate()),
                field("Duyệt đóng lúc", item.getApprovedAt())), attachments);
    }

    private EvidenceRecord activity(Long id) {
        UnionActivity item = activities.findById(id).orElseThrow(() -> missing("hoạt động", id));
        currentUser.requireUnitAccess(item.getUnionUnit().getId());
        List<EvidenceAttachment> attachments = activityMedia.findByActivityIdOrderByCreatedAtDesc(id).stream()
                .map(document -> attachment(document.getId(), document.getFileName(),
                        "/activity-media/" + document.getId() + "/download"))
                .toList();
        return record("HOAT_DONG", String.valueOf(id), item.getActivityCode() + " · " + item.getName(), List.of(
                field("CĐCS", item.getUnionUnit().getCode()),
                field("Ngày tổ chức", item.getEventDate()),
                field("Trạng thái", label(item.getStatus())),
                field("PIC", item.getProgramPic()),
                field("Ngân sách", item.getPlannedBudget()),
                field("Chi thực tế", item.getActualCost()),
                field("Dự kiến tham gia", item.getInvitedCount()),
                field("Thực tế tham gia", item.getParticipantCount()),
                field("Báo cáo hoàn tất", item.getReportCompleted())), attachments);
    }

    private EvidenceRecord financeEntry(Long id) {
        FinanceEntry item = finance.findById(id).orElseThrow(() -> missing("giao dịch", id));
        currentUser.requireUnitAccess(item.getUnionUnit().getId());
        List<EvidenceAttachment> attachments = financeDocuments.findByFinanceEntryIdOrderByCreatedAtDesc(id).stream()
                .map(document -> attachment(document.getId(), document.getFileName(),
                        "/finance-documents/" + document.getId() + "/download"))
                .toList();
        return record("TAI_CHINH_CD", String.valueOf(id), item.getEntryCode(), List.of(
                field("CĐCS", item.getUnionUnit().getCode()),
                field("Ngày giao dịch", item.getTransactionDate()),
                field("Loại", label(item.getEntryType())),
                field("Nhóm", item.getCategory()),
                field("Số tiền", item.getAmount()),
                field("Số chứng từ", item.getDocumentNumber()),
                field("Tình trạng chứng từ", label(item.getDocumentStatus()))), attachments);
    }

    private EvidenceRecord noOccurrence(Long id) {
        KpiNoOccurrenceConfirmation item = noOccurrence.findById(id)
                .orElseThrow(() -> missing("xác nhận không phát sinh", id));
        currentUser.requireUnitAccess(item.getUnionUnitId());
        return record("KPI_NO_OCCURRENCE", String.valueOf(id),
                "Xác nhận không phát sinh · " + item.getKpiCode(), List.of(
                        field("Phiên bản", item.getVersionId()),
                        field("KPI", item.getKpiCode()),
                        field("Từ ngày", item.getPeriodStart()),
                        field("Đến ngày", item.getPeriodEnd()),
                        field("Nguồn nghiệp vụ", item.getSourceModule()),
                        field("Nguồn đối soát độc lập", item.getReconciliationSourceModule()),
                        field("Người xác nhận", item.getConfirmedBy()),
                        field("Người duyệt", item.getApprovedBy()),
                        field("Duyệt lúc", item.getApprovedAt())), List.of());
    }

    private void requireSensitiveAccess() {
        if (!currentUser.isAdmin()) {
            throw new AccessDeniedException("Chỉ tài khoản có quyền giám sát được xem chứng cứ nhạy cảm");
        }
    }

    private Long longId(String raw) {
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException invalid) {
            throw new ResourceNotFoundException("Mã bản ghi chứng cứ KPI không hợp lệ");
        }
    }

    private ResourceNotFoundException missing(String type, Long id) {
        return new ResourceNotFoundException("Không tìm thấy " + type + " với id=" + id);
    }

    private EvidenceRecord record(String module, String id, String title, List<EvidenceField> fields,
                                  List<EvidenceAttachment> attachments) {
        return new EvidenceRecord(module, id, title,
                fields.stream().filter(item -> item.value() != null && !item.value().isBlank()).toList(), attachments);
    }

    private EvidenceField field(String label, Object value) {
        return new EvidenceField(label, value == null ? "" : String.valueOf(value));
    }

    private String label(Enum<?> value) {
        return EnumLabels.label(value);
    }

    private EvidenceAttachment attachment(Long id, String fileName, String path) {
        return new EvidenceAttachment(id, fileName, path);
    }
}
