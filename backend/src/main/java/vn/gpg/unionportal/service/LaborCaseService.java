package vn.gpg.unionportal.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.CaseGroupCount;
import vn.gpg.unionportal.dto.ApiModels.CaseApprovalRequest;
import vn.gpg.unionportal.dto.ApiModels.LaborCaseRequest;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.mapper.EntityMapper;
import vn.gpg.unionportal.model.DomainEnums.CaseSeverity;
import vn.gpg.unionportal.model.DomainEnums.CaseStatus;
import vn.gpg.unionportal.model.LaborCase;
import vn.gpg.unionportal.repository.LaborCaseRepository;
import vn.gpg.unionportal.repository.LaborCaseDocumentRepository;
import vn.gpg.unionportal.spec.LaborCaseSpecs;
import vn.gpg.unionportal.spec.SpecAggregates;
import vn.gpg.unionportal.spec.Specs;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class LaborCaseService {
    private static final Sort SORT = Sort.by("deadline");
    /** "Ảnh hưởng nhiều NLĐ" threshold, kept in step with {@code LaborCaseSpecs}. */
    private static final int WIDE_IMPACT_THRESHOLD = 10;

    private final LaborCaseRepository repository;
    private final LaborCaseDocumentRepository documents;
    private final EntityMapper mapper;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;
    private final SpecAggregates aggregates;

    public LaborCaseService(LaborCaseRepository repository, LaborCaseDocumentRepository documents,
                            EntityMapper mapper, CurrentUserService currentUser,
                            RealtimeEventPublisher events, SpecAggregates aggregates) {
        this.repository = repository;
        this.documents = documents;
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.events = events;
        this.aggregates = aggregates;
    }

    public Page<LaborCase> page(ListQuery query) {
        return repository.findAll(Specs.nullSafe(filter(query)), query.pageable(SORT));
    }

    public List<LaborCase> search(ListQuery query) {
        return repository.findAll(Specs.nullSafe(filter(query)), SORT);
    }

    public ListFacets facets(ListQuery query) {
        LocalDate today = LocalDate.now();
        Specification<LaborCase> scope = Specs.nullSafe(Specs.unitScope(scopedUnitId(query)));
        Specification<LaborCase> filtered = Specs.nullSafe(filter(query));
        Specification<LaborCase> open = filtered.and(Specs.notEq("status", CaseStatus.CLOSED));
        Specification<LaborCase> urgentEscalation = (root, criteria, cb) -> cb.or(
                root.get("severity").in(List.of(CaseSeverity.HIGH, CaseSeverity.CRITICAL)),
                cb.greaterThanOrEqualTo(root.get("affectedPeople"), WIDE_IMPACT_THRESHOLD));
        var counts = aggregates.countMetrics(LaborCase.class, filtered, Map.of(
                "open", open,
                "due24", open.and((root, criteria, cb) ->
                        cb.between(root.get("deadline"), today, today.plusDays(1))),
                "dueOrOverdue", open.and(Specs.onOrBefore("deadline", today)),
                "overdue", open.and(Specs.before("deadline", today)),
                "highSeverity", Specs.in("severity", List.of(CaseSeverity.HIGH, CaseSeverity.CRITICAL)),
                "urgentEscalation", open.and(urgentEscalation),
                "pendingApproval", Specs.eq("status", CaseStatus.PENDING_APPROVAL),
                "closed", Specs.eq("status", CaseStatus.CLOSED),
                "wideImpact", Specs.atLeast("affectedPeople", WIDE_IMPACT_THRESHOLD),
                "repeated", LaborCaseSpecs.repeated(scopedUnitId(query))));
        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("total", counts.total());
        metrics.put("open", counts.value("open"));
        metrics.put("due24", counts.value("due24"));
        metrics.put("dueOrOverdue", counts.value("dueOrOverdue"));
        metrics.put("overdue", counts.value("overdue"));
        metrics.put("highSeverity", counts.value("highSeverity"));
        metrics.put("urgentEscalation", counts.value("urgentEscalation"));
        metrics.put("pendingApproval", counts.value("pendingApproval"));
        metrics.put("closed", counts.value("closed"));
        metrics.put("wideImpact", counts.value("wideImpact"));
        metrics.put("repeated", counts.value("repeated"));
        metrics.put("affectedPeople", aggregates.sumLong(LaborCase.class, filtered, "affectedPeople"));
        return new ListFacets(
                repository.count(scope),
                aggregates.distinctValues(LaborCase.class, scope, "status"),
                metrics);
    }

    /** Per-issue-group rollup behind the case analytics bars. */
    public List<CaseGroupCount> issueGroups(ListQuery query) {
        return aggregates.caseGroups(Specs.nullSafe(filter(query)), LocalDate.now());
    }

    private Specification<LaborCase> filter(ListQuery query) {
        return LaborCaseSpecs.filter(query, scopedUnitId(query), LocalDate.now());
    }

    private Long scopedUnitId(ListQuery query) {
        return currentUser.scopedUnitId(query.unitId());
    }

    @Transactional
    public LaborCase create(LaborCaseRequest request) {
        currentUser.requireUnitAccess(request.unionUnitId());
        LaborCaseRequest normalized = withAssignment(request, null, null, CaseStatus.NEW, null);
        var saved = repository.save(mapper.apply(new LaborCase(), normalized));
        events.changed("cases", "CREATED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    @Transactional
    public LaborCase update(Long id, LaborCaseRequest request) {
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        currentUser.requireUnitAccess(request.unionUnitId());
        LaborCaseRequest normalized = request;
        if (!currentUser.isAdmin()) {
            if (entity.getStatus() == CaseStatus.PENDING_APPROVAL || entity.getStatus() == CaseStatus.CLOSED) {
                throw new AccessDeniedException("USER không thể sửa kiến nghị đang chờ duyệt hoặc đã đóng");
            }
            if (request.status() == CaseStatus.PENDING_APPROVAL || request.status() == CaseStatus.CLOSED) {
                throw new AccessDeniedException("USER phải dùng nút Gửi ADMIN duyệt để hoàn tất xử lý");
            }
            CaseStatus userStatus = entity.getStatus() == CaseStatus.NEW
                    ? CaseStatus.NEW
                    : request.status() == CaseStatus.WAITING_RESPONSE
                        ? CaseStatus.WAITING_RESPONSE
                        : CaseStatus.IN_PROGRESS;
            normalized = withAssignment(request, entity.getOwnerName(), entity.getDeadline(), userStatus,
                    entity.getResponseDate());
        } else if (request.status() == CaseStatus.CLOSED && entity.getStatus() != CaseStatus.CLOSED) {
            throw new IllegalArgumentException("Hãy dùng thao tác Duyệt & đóng hồ sơ");
        }
        requireOverdueDetails(normalized.deadline(), normalized.status(), normalized.overdueReason());
        var saved = repository.save(mapper.apply(entity, normalized));
        events.changed("cases", "UPDATED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        if (!currentUser.isAdmin()
                && (entity.getStatus() == CaseStatus.PENDING_APPROVAL || entity.getStatus() == CaseStatus.CLOSED)) {
            throw new AccessDeniedException("USER không thể xóa vụ việc đang chờ duyệt hoặc đã đóng");
        }
        repository.delete(entity);
        events.changed("cases", "DELETED", entity.getId(), entity.getUnionUnit().getId());
    }

    @Transactional
    public LaborCase submitForApproval(Long id) {
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        if (entity.getStatus() == CaseStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Kiến nghị đã được gửi và đang chờ ADMIN duyệt");
        }
        if (entity.getStatus() == CaseStatus.CLOSED) {
            throw new IllegalArgumentException("Kiến nghị đã đóng");
        }
        if (entity.getStatus() != CaseStatus.IN_PROGRESS && entity.getStatus() != CaseStatus.WAITING_RESPONSE) {
            throw new IllegalArgumentException("ADMIN phải duyệt tiếp nhận và giao PIC/deadline trước khi USER nộp kết quả");
        }
        if (entity.getOwnerName() == null || entity.getOwnerName().isBlank() || entity.getDeadline() == null) {
            throw new IllegalArgumentException("Vụ việc chưa có PIC và deadline do ADMIN giao");
        }
        if (entity.getResultText() == null || entity.getResultText().isBlank()) {
            throw new IllegalArgumentException("Cần cập nhật Kết quả / phản hồi trước khi gửi ADMIN duyệt");
        }
        if (!documents.existsByLaborCaseId(entity.getId())
                && (entity.getAttachmentNote() == null || entity.getAttachmentNote().isBlank())) {
            throw new IllegalArgumentException("Cần tải Tài liệu đính kèm hoặc ghi chú liên kết trước khi gửi ADMIN duyệt");
        }
        requireOverdueDetails(entity.getDeadline(), entity.getStatus(), entity.getOverdueReason());
        entity.setStatus(CaseStatus.PENDING_APPROVAL);
        entity.setResponseDate(LocalDate.now());
        var saved = repository.save(entity);
        events.changed("cases", "SUBMITTED_FOR_APPROVAL", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    @Transactional
    public LaborCase approve(Long id) {
        return approve(id, null);
    }

    @Transactional
    public LaborCase approve(Long id, CaseApprovalRequest approval) {
        if (!currentUser.isAdmin()) {
            throw new AccessDeniedException("Chỉ ADMIN được duyệt kết quả xử lý kiến nghị");
        }
        var entity = findById(id);
        if (entity.getStatus() == CaseStatus.NEW) {
            if (approval == null) {
                throw new IllegalArgumentException("ADMIN cần chọn PIC và deadline khi duyệt tiếp nhận kiến nghị");
            }
            if (approval.deadline().isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Deadline xử lý không được trước ngày duyệt");
            }
            entity.setOwnerName(approval.ownerName().trim());
            entity.setDeadline(approval.deadline());
            entity.setStatus(CaseStatus.IN_PROGRESS);
            var assigned = repository.save(entity);
            events.changed("cases", "ASSIGNMENT_APPROVED", assigned.getId(), assigned.getUnionUnit().getId());
            return assigned;
        }
        if (entity.getStatus() != CaseStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Chỉ có thể duyệt kiến nghị mới hoặc kết quả đang chờ duyệt");
        }
        requireOverdueDetails(entity.getDeadline(), entity.getStatus(), entity.getOverdueReason());
        entity.setStatus(CaseStatus.CLOSED);
        entity.setApprovedBy(currentUser.username());
        entity.setApprovedAt(Instant.now());
        if (entity.getResponseDate() == null) entity.setResponseDate(LocalDate.now());
        var saved = repository.save(entity);
        events.changed("cases", "APPROVED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    private LaborCase findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kiến nghị với id=" + id));
    }

    private LaborCaseRequest withAssignment(LaborCaseRequest request, String ownerName, LocalDate deadline,
                                            CaseStatus status, LocalDate responseDate) {
        return new LaborCaseRequest(
                request.caseCode(), request.receivedDate(), request.unionUnitId(), request.requesterName(),
                request.employeeCode(), request.jobTitle(), request.workplace(), request.startWorkDate(),
                request.leaveDate(), request.phone(), request.source(), request.issueGroup(), request.severity(),
                ownerName, deadline, status, request.description(), request.affectedPeople(),
                request.attachmentNote(), request.resultText(), responseDate, request.overdueReason());
    }

    private void requireOverdueDetails(LocalDate deadline, CaseStatus status, String overdueReason) {
        if (deadline != null && status != CaseStatus.CLOSED && deadline.isBefore(LocalDate.now())
                && (overdueReason == null || overdueReason.isBlank())) {
            throw new IllegalArgumentException("Kiến nghị quá hạn phải có Lý do quá hạn / ETA mới");
        }
    }
}
