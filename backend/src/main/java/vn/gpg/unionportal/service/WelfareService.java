package vn.gpg.unionportal.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ApiModels.WelfareRequest;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.mapper.EntityMapper;
import vn.gpg.unionportal.model.DomainEnums.DocumentStatus;
import vn.gpg.unionportal.model.DomainEnums.WelfareType;
import vn.gpg.unionportal.model.DomainEnums.WorkStatus;
import vn.gpg.unionportal.model.WelfareRecord;
import vn.gpg.unionportal.repository.WelfareRecordRepository;
import vn.gpg.unionportal.spec.SpecAggregates;
import vn.gpg.unionportal.spec.Specs;
import vn.gpg.unionportal.spec.WelfareSpecs;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class WelfareService {
    private static final Sort SORT = Sort.by(Sort.Direction.DESC, "eventDate");

    private final WelfareRecordRepository repository;
    private final EntityMapper mapper;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;
    private final SpecAggregates aggregates;
    private final WelfarePolicyService policyService;
    private final FinanceService financeService;

    public WelfareService(WelfareRecordRepository repository, EntityMapper mapper, CurrentUserService currentUser,
                          RealtimeEventPublisher events, SpecAggregates aggregates,
                          WelfarePolicyService policyService, FinanceService financeService) {
        this.repository = repository;
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.events = events;
        this.aggregates = aggregates;
        this.policyService = policyService;
        this.financeService = financeService;
    }

    public Page<WelfareRecord> page(ListQuery query) {
        return repository.findAll(Specs.nullSafe(filter(query)), query.pageable(SORT));
    }

    public List<WelfareRecord> search(ListQuery query) {
        return repository.findAll(Specs.nullSafe(filter(query)), SORT);
    }

    public ListFacets facets(ListQuery query) {
        LocalDate today = LocalDate.now();
        Specification<WelfareRecord> scope = Specs.nullSafe(Specs.unitScope(scopedUnitId(query)));
        Specification<WelfareRecord> filtered = Specs.nullSafe(filter(query));
        var counts = aggregates.countMetrics(WelfareRecord.class, filtered, Map.of(
                "birthday", Specs.eq("welfareType", WelfareType.BIRTHDAY),
                "visit", Specs.eq("welfareType", WelfareType.VISIT),
                "funeralOrWedding", Specs.in("welfareType", List.of(WelfareType.FUNERAL, WelfareType.WEDDING)),
                "unfinished", Specs.notIn("status", List.of(WorkStatus.COMPLETED, WorkStatus.CANCELLED)),
                "newRequests", Specs.eq("status", WorkStatus.PENDING_APPROVAL),
                "due", dueSoon(today)));
        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("total", counts.total());
        metrics.put("birthday", counts.value("birthday"));
        metrics.put("visit", counts.value("visit"));
        metrics.put("funeralOrWedding", counts.value("funeralOrWedding"));
        metrics.put("unfinished", counts.value("unfinished"));
        metrics.put("newRequests", counts.value("newRequests"));
        metrics.put("due", counts.value("due"));
        return new ListFacets(
                repository.count(scope),
                aggregates.distinctValues(WelfareRecord.class, scope, "status"),
                metrics);
    }

    /** Not finished and due today or tomorrow — same rule as the "Đến hạn" tracking filter. */
    private Specification<WelfareRecord> dueSoon(LocalDate today) {
        return (root, criteria, cb) -> cb.and(
                cb.notEqual(root.get("status"), WorkStatus.COMPLETED),
                cb.lessThanOrEqualTo(cb.coalesce(root.get("deadline"), root.get("eventDate")), today.plusDays(1)));
    }

    private Specification<WelfareRecord> filter(ListQuery query) {
        return WelfareSpecs.filter(query, scopedUnitId(query), LocalDate.now());
    }

    private Long scopedUnitId(ListQuery query) {
        return currentUser.scopedUnitId(query.unitId());
    }

    @Transactional
    public WelfareRecord create(WelfareRequest request) {
        currentUser.requireUnitAccess(request.unionUnitId());
        WelfareRequest policyRequest = applyPolicy(request, false);
        WorkStatus initialStatus = currentUser.isAdmin() ? policyRequest.status() : WorkStatus.PENDING_APPROVAL;
        var saved = repository.save(syncCompletedAt(mapper.apply(new WelfareRecord(), withWorkflowState(
                policyRequest, initialStatus, DocumentStatus.INCOMPLETE, DocumentStatus.INCOMPLETE, false))));
        events.changed("welfare", "CREATED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    @Transactional
    public WelfareRecord update(Long id, WelfareRequest request) {
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        currentUser.requireUnitAccess(request.unionUnitId());
        if (!currentUser.isAdmin() && entity.getStatus() != WorkStatus.PENDING_APPROVAL) {
            throw new AccessDeniedException("USER chỉ được sửa yêu cầu chăm lo khi đang chờ ADMIN duyệt");
        }
        boolean keepsExistingPolicy = Objects.equals(entity.getPolicyId(), request.policyId());
        WelfareRequest policyRequest = applyPolicy(request, keepsExistingPolicy);
        WorkStatus status = currentUser.isAdmin() ? policyRequest.status() : WorkStatus.PENDING_APPROVAL;
        WelfareRequest normalized = withWorkflowState(policyRequest, status, entity.getDocumentStatus(),
                entity.getReceiptStatus(), entity.getHasImage());
        var saved = repository.save(syncCompletedAt(mapper.apply(entity, normalized)));
        events.changed("welfare", "UPDATED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        if (!currentUser.isAdmin() && entity.getStatus() != WorkStatus.PENDING_APPROVAL) {
            throw new AccessDeniedException("USER chỉ được xóa yêu cầu chăm lo khi đang chờ ADMIN duyệt");
        }
        repository.delete(entity);
        events.changed("welfare", "DELETED", entity.getId(), entity.getUnionUnit().getId());
    }

    @Transactional
    public WelfareRecord approve(Long id) {
        if (!currentUser.isAdmin()) {
            throw new AccessDeniedException("Chỉ ADMIN được duyệt yêu cầu chăm lo");
        }
        var entity = findById(id);
        if (entity.getStatus() != WorkStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Chỉ có thể duyệt yêu cầu đang ở trạng thái Chờ duyệt");
        }
        entity.setStatus(WorkStatus.IN_PROGRESS);
        var saved = repository.save(entity);
        financeService.createForApprovedWelfare(saved);
        events.changed("welfare", "APPROVED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    /**
     * The person assigned to the CĐCS completes the approved care after it has actually been
     * delivered. Approval creates the finance entry; completion records the operational result.
     */
    @Transactional
    public WelfareRecord complete(Long id) {
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        if (entity.getStatus() != WorkStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Chỉ có thể hoàn thành hồ sơ chăm lo đang xử lý");
        }
        entity.setStatus(WorkStatus.COMPLETED);
        var saved = repository.save(syncCompletedAt(entity));
        events.changed("welfare", "COMPLETED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    /**
     * CARE02 scores completion against the deadline, so the moment of completion has to be its own column:
     * {@code updatedAt} moves again on every later edit and would silently rewrite history.
     */
    private WelfareRecord syncCompletedAt(WelfareRecord entity) {
        if (entity.getStatus() == WorkStatus.COMPLETED) {
            if (entity.getCompletedAt() == null) entity.setCompletedAt(Instant.now());
        } else {
            entity.setCompletedAt(null);
        }
        return entity;
    }

    private WelfareRecord findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ chăm lo với id=" + id));
    }

    private WelfareRequest applyPolicy(WelfareRequest request, boolean allowInactive) {
        if (request.policyId() == null) return request;
        var policy = allowInactive ? policyService.require(request.policyId()) : policyService.requireActive(request.policyId());
        return new WelfareRequest(
                request.recordCode(), policy.getWelfareType(), policy.getName(), request.unionUnitId(),
                request.beneficiaryName(), request.eventDate(), request.eventDate().plusWeeks(policy.getProcessingWeeks()),
                request.status(), request.amount(), policy.getSupportAmount(), request.documentStatus(),
                request.receiptStatus(), request.hasImage(), request.notes(), policy.getId());
    }

    private WelfareRequest withWorkflowState(WelfareRequest request, WorkStatus status,
                                             DocumentStatus documentStatus, DocumentStatus receiptStatus,
                                             boolean hasImage) {
        return new WelfareRequest(
                request.recordCode(), request.welfareType(), request.policyName(), request.unionUnitId(),
                request.beneficiaryName(), request.eventDate(), request.deadline(), status, request.amount(),
                request.standardAmount(), documentStatus, receiptStatus, hasImage, request.notes(), request.policyId());
    }
}
